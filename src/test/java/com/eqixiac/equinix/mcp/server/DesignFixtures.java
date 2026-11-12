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

package com.eqixiac.equinix.mcp.server;

import com.eqixiac.equinix.FabricGateway;
import com.eqixiac.equinix.core.enums.MetroCode;
import com.eqixiac.equinix.core.enums.Region;
import com.eqixiac.equinix.core.http.response.PaginatedFilteredList;
import com.eqixiac.equinix.core.http.response.PaginatedList;
import com.eqixiac.equinix.core.model.MetroId;
import com.eqixiac.equinix.fabric.client.Metros;
import com.eqixiac.equinix.fabric.client.ServiceProfiles;
import com.eqixiac.equinix.fabric.model.Metro;
import com.eqixiac.equinix.fabric.model.MetroRegistry;
import com.eqixiac.equinix.fabric.model.ServiceProfile;
import com.eqixiac.equinix.fabric.model.implementation.ConnectedMetro;
import com.eqixiac.equinix.fabric.model.implementation.GeoCoordinate;
import com.eqixiac.equinix.fabric.model.implementation.ServiceProfileMetro;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The offline fixture the design tool tests share: a Mockito {@link FabricGateway} with three AMER
 * metros (DC, DA, SV, with measured round-trip latencies) and one AWS service profile offered in DC and
 * DA, plus the specs the tests send. No HTTP anywhere; live pricing lookups fail fast against the
 * stub and fall back to the bundled reference figures, and FX is offline
 * ({@code junit-platform.properties}).
 */
final class DesignFixtures {

    static final ObjectMapper MAPPER = new ObjectMapper();

    /** One site in DC, AWS us-east-1, one 1 Gbps workload, at most one metro. */
    static final String SPEC_DC_AWS = """
            {"name": "dc-aws", "sites": [{"label": "HQ", "metro": "DC"}],
             "clouds": [{"id": "aws", "provider": "AWS", "region": "us-east-1", "monthlyEgressGb": 5000}],
             "workloads": [{"label": "web", "type": "GENERAL_COMPUTE", "bandwidthMbps": 1000, "clouds": ["aws"]}],
             "constraints": {"maxMetros": 1}}
            """;

    /** As {@link #SPEC_DC_AWS} with two metros allowed and a MULTI_METRO redundancy tier. */
    static final String SPEC_TWO_METROS = """
            {"name": "two-metros", "sites": [{"label": "HQ", "metro": "DC"}, {"label": "West", "metro": "SV"}],
             "clouds": [{"id": "aws", "provider": "AWS", "region": "us-east-1"}],
             "workloads": [{"label": "web", "bandwidthMbps": 1000, "clouds": ["aws"]}],
             "constraints": {"maxMetros": 2, "minimumRedundancy": "MULTI_METRO"}}
            """;

    private DesignFixtures() {
    }

    static FabricGateway gateway() throws Exception {
        Metro dc = metro("DC", "Ashburn", Region.AMER, 39.0438, -77.4874,
                List.of(connectedMetro("DA", 10.0), connectedMetro("SV", 60.0)));
        Metro da = metro("DA", "Dallas", Region.AMER, 32.7767, -96.7970,
                List.of(connectedMetro("DC", 10.0), connectedMetro("SV", 45.0)));
        Metro sv = metro("SV", "Silicon Valley", Region.AMER, 37.3382, -121.8863,
                List.of(connectedMetro("DC", 60.0), connectedMetro("DA", 45.0)));
        Metros metros = mock(Metros.class);
        when(metros.list()).thenReturn(new PaginatedList<>(List.of(dc, da, sv), null, null, null, null));
        ServiceProfile aws = mock(ServiceProfile.class);
        when(aws.getUuid()).thenReturn("sp-aws-1");
        when(aws.getName()).thenReturn("Amazon Web Services Direct Connect");
        when(aws.metros()).thenReturn(List.of(serviceProfileMetro("DC", "us-east-1"),
                serviceProfileMetro("DA", "us-east-1")));
        ServiceProfiles profiles = mock(ServiceProfiles.class);
        when(profiles.search()).thenReturn(new PaginatedFilteredList<>(List.of(aws), null, null, null, null));
        FabricGateway fabric = mock(FabricGateway.class);
        when(fabric.metros()).thenReturn(metros);
        when(fabric.serviceProfiles()).thenReturn(profiles);
        return fabric;
    }

    static ServerContext context() throws Exception {
        FabricGateway fabric = gateway();
        return ServerContext.builder()
                .fabric(fabric)
                .metroRegistry(MetroRegistry.load(fabric.metros()))
                .environment(Map.of())
                .build();
    }

    static ToolRegistration tool(String name) {
        return EquinixMcpServer.catalog(EnumSet.of(Toolset.FABRIC)).stream()
                .filter(t -> t.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new AssertionError("tool not in catalog: " + name));
    }

    static ObjectNode call(String name, ServerContext ctx, String argsJson) throws Exception {
        JsonNode args = MAPPER.readTree(argsJson);
        ObjectNode payload = tool(name).getHandler().handle(args, ctx);
        return UntrustedText.label(payload);
    }

    /** {@code {"spec": <spec>, ...extra}}. */
    static String withSpec(String spec, String extraFields) {
        return "{\"spec\": " + spec + (extraFields == null || extraFields.isBlank() ? "" : ", " + extraFields) + "}";
    }

    static Metro metro(String code, String name, Region region, double lat, double lon,
                       List<ConnectedMetro> connected) throws Exception {
        Metro m = mock(Metro.class);
        when(m.metroId()).thenReturn(MetroId.of(code));
        when(m.getCode()).thenReturn(MetroCode.fromCode(code));
        when(m.getName()).thenReturn(name);
        when(m.getRegion()).thenReturn(region);
        when(m.geoCoordinates()).thenReturn(geo(lat, lon));
        when(m.getConnectedMetros()).thenReturn(connected);
        return m;
    }

    static ConnectedMetro connectedMetro(String code, double avgLatency) throws Exception {
        return MAPPER.readValue("{\"code\":\"" + code + "\",\"avgLatency\":" + avgLatency + "}", ConnectedMetro.class);
    }

    static ServiceProfileMetro serviceProfileMetro(String code, String sellerRegion) throws Exception {
        return MAPPER.readValue("{\"code\":\"" + code + "\",\"name\":\"" + code
                + "\",\"sellerRegions\":{\"" + sellerRegion + "\":\"" + sellerRegion + "\"}}", ServiceProfileMetro.class);
    }

    static GeoCoordinate geo(double lat, double lon) throws Exception {
        return MAPPER.readValue("{\"latitude\":" + lat + ",\"longitude\":" + lon + "}", GeoCoordinate.class);
    }
}
