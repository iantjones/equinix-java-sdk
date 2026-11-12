/*
 * Copyright 2021 Ian Jones. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
 * OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package com.eqixiac.equinix.design.optimizer;

import com.eqixiac.equinix.FabricGateway;
import com.eqixiac.equinix.core.enums.MetroCode;
import com.eqixiac.equinix.core.enums.Region;
import com.eqixiac.equinix.core.http.response.PaginatedFilteredList;
import com.eqixiac.equinix.core.http.response.PaginatedList;
import com.eqixiac.equinix.core.model.MetroId;
import com.eqixiac.equinix.fabric.client.Metros;
import com.eqixiac.equinix.fabric.client.ServiceProfiles;
import com.eqixiac.equinix.fabric.enums.GeoScopeType;
import com.eqixiac.equinix.fabric.model.Metro;
import com.eqixiac.equinix.fabric.model.ServiceProfile;
import com.eqixiac.equinix.fabric.model.implementation.ConnectedMetro;
import com.eqixiac.equinix.fabric.model.implementation.GeoCoordinate;
import com.eqixiac.equinix.fabric.model.implementation.ServiceProfileMetro;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Stubbed Fabric catalogue shapes (live /fabric/v4/metros and service-profile search) for optimizer tests. */
final class OptimizerFixtures {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private OptimizerFixtures() {
    }

    static Metro metro(String code, String name, Region region, double lat, double lon, List<GeoScopeType> scopes,
                       ConnectedMetro... connected) {
        Metro m = mock(Metro.class);
        when(m.metroId()).thenReturn(MetroId.of(code));
        when(m.getCode()).thenReturn(MetroCode.fromCode(code));
        when(m.getName()).thenReturn(name);
        when(m.getRegion()).thenReturn(region);
        when(m.geoCoordinates()).thenReturn(geo(lat, lon));
        when(m.getConnectedMetros()).thenReturn(List.of(connected));
        when(m.getGeoScopes()).thenReturn(scopes);
        return m;
    }

    static ConnectedMetro link(String code, double avgLatency) {
        return read("{\"code\":\"" + code + "\",\"avgLatency\":" + avgLatency + "}", ConnectedMetro.class);
    }

    static ServiceProfile profile(String uuid, String name, String... metroAndRegion) {
        ServiceProfile p = mock(ServiceProfile.class);
        when(p.getUuid()).thenReturn(uuid);
        when(p.getName()).thenReturn(name);
        java.util.List<ServiceProfileMetro> metros = new java.util.ArrayList<>();
        for (String mr : metroAndRegion) {
            String[] parts = mr.split(":");
            metros.add(read("{\"code\":\"" + parts[0] + "\",\"sellerRegions\":{\"" + parts[1] + "\":\"" + parts[1]
                    + "\"}}", ServiceProfileMetro.class));
        }
        when(p.metros()).thenReturn(metros);
        return p;
    }

    static FabricGateway gateway(List<Metro> metros, List<ServiceProfile> profiles) {
        Metros metroClient = mock(Metros.class);
        when(metroClient.list()).thenReturn(new PaginatedList<>(metros, null, null, null, null));
        ServiceProfiles profileClient = mock(ServiceProfiles.class);
        when(profileClient.search()).thenReturn(new PaginatedFilteredList<>(profiles, null, null, null, null));
        FabricGateway fabric = mock(FabricGateway.class);
        when(fabric.metros()).thenReturn(metroClient);
        when(fabric.serviceProfiles()).thenReturn(profileClient);
        return fabric;
    }

    /** DC, NY, SV (AMER, US), LD (EMEA, UK), FR (EMEA, EU), with live-measured round trips. */
    static FabricGateway standard() {
        List<Metro> metros = List.of(
                metro("DC", "Ashburn", Region.AMER, 39.0438, -77.4874, List.of(GeoScopeType.CONUS),
                        link("NY", 6.3), link("SV", 65.8), link("LD", 73.4)),
                metro("NY", "New York", Region.AMER, 40.7128, -74.0060, List.of(GeoScopeType.CONUS),
                        link("DC", 6.3), link("LD", 70.1)),
                metro("SV", "Silicon Valley", Region.AMER, 37.3382, -121.8863, List.of(GeoScopeType.CONUS),
                        link("DC", 65.8), link("LD", 139.0)),
                metro("LD", "London", Region.EMEA, 51.5074, -0.1278, List.of(GeoScopeType.UK),
                        link("DC", 73.4), link("SV", 139.0), link("FR", 9.5)),
                metro("FR", "Frankfurt", Region.EMEA, 50.1109, 8.6821, List.of(GeoScopeType.EU), link("LD", 9.5)));
        List<ServiceProfile> profiles = List.of(
                profile("sp-aws", "AWS Direct Connect", "DC:us-east-1", "NY:us-east-1", "SV:us-west-1",
                        "LD:eu-west-2", "FR:eu-central-1"),
                profile("sp-azure", "Azure ExpressRoute", "DC:eastus", "LD:uksouth"));
        return gateway(metros, profiles);
    }

    private static GeoCoordinate geo(double lat, double lon) {
        return read("{\"latitude\":" + lat + ",\"longitude\":" + lon + "}", GeoCoordinate.class);
    }

    private static <T> T read(String json, Class<T> type) {
        try {
            return MAPPER.readValue(json, type);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
