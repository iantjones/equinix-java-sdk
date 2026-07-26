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
import com.eqixiac.equinix.core.model.MetroId;
import com.eqixiac.equinix.design.optimizer.model.MetroRecommendation;
import com.eqixiac.equinix.design.optimizer.model.OptimizationResult;
import com.eqixiac.equinix.design.optimizer.model.ProviderAvailability;
import com.eqixiac.equinix.design.optimizer.model.ServiceProfileOption;
import com.eqixiac.equinix.design.value.ratecard.ReferenceRateCard;
import com.eqixiac.equinix.fabric.client.Metros;
import com.eqixiac.equinix.fabric.client.ServiceProfiles;
import com.eqixiac.equinix.fabric.model.Metro;
import com.eqixiac.equinix.fabric.model.ServiceProfile;
import com.eqixiac.equinix.fabric.model.implementation.AccessPointTypeConfig;
import com.eqixiac.equinix.fabric.model.implementation.ConnectedMetro;
import com.eqixiac.equinix.fabric.model.implementation.GeoCoordinate;
import com.eqixiac.equinix.fabric.model.implementation.ServiceProfileMetro;
import com.eqixiac.equinix.fabric.model.implementation.cloud.CloudProviderType;
import com.eqixiac.equinix.core.http.response.PaginatedFilteredList;
import com.eqixiac.equinix.core.http.response.PaginatedList;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The optimizer half of bandwidth-aware profile selection: {@code buildProviderIndex} must carry
 * EVERY matching profile's bandwidth capability per metro onto
 * {@link ProviderAvailability#getProfileOptions()}, not just the single {@code outranks} winner — so
 * the wizard can later pick a profile whose tiers cover the connection's speed. This is the data the
 * live GlobalPay failure discarded: the AWS hosted profile (tiers &le;500) and the AWS dedicated
 * profile both publish the metro, but only the winner's uuid used to survive.
 *
 * @see MetroOptimizerProviderResolutionTest
 */
@DisplayName("MetroOptimizer — provider index carries per-profile bandwidth capability")
class MetroOptimizerBandwidthAwarenessTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    @DisplayName("both AWS profiles' bandwidth tiers are carried as candidate options for the metro")
    void providerIndexCarriesEveryProfilesBandwidthCapability() throws Exception {
        // AWS publishes DC twice: a hosted profile ([50..500], ceiling 500) and a dedicated profile
        // ([1000, 10000], ceiling 10000). Both must survive as candidate options with their tiers.
        FabricGateway fabric = gatewayWith(
                profileWithConfigs("sp-aws-hosted", "AWS Direct Connect",
                        List.of(serviceProfileMetro("DC", "us-east-1", 500)),
                        List.of(apConfig(List.of(50, 100, 200, 300, 400, 500), false))),
                profileWithConfigs("sp-aws-dedicated", "AWS Direct Connect Dedicated",
                        List.of(serviceProfileMetro("DC", "us-east-1-dx", 10000)),
                        List.of(apConfig(List.of(1000, 10000), false))));

        OptimizationResult result = MetroOptimizer.builder(fabric)
                .addSite("HQ").nearestMetro(MetroCode.DC).headcount(500).done()
                .requireProvider(CloudProviderType.AWS).done()
                .addWorkload("Web Tier").bandwidthMbps(1000).done()
                .constraints().maxMetros(4).done()
                .rateCard(ReferenceRateCard.standard())
                .optimize();

        ProviderAvailability aws = availability(result, "DC", "Amazon Web Services");
        List<ServiceProfileOption> options = aws.getProfileOptions();
        assertNotNull(options, "the optimizer must carry candidate profile options, not just the winner");
        assertEquals(2, options.size(), "both matching profiles must be carried: " + options);

        ServiceProfileOption hosted = option(options, "sp-aws-hosted");
        assertEquals(List.of(50, 100, 200, 300, 400, 500), hosted.getSupportedBandwidths());
        assertEquals(Integer.valueOf(500), hosted.getVcBandwidthMax());
        assertTrue(hosted.covers(300), "hosted covers a 300 Mbps connection");
        assertFalse(hosted.covers(10000), "hosted does not cover 10000 Mbps");

        ServiceProfileOption dedicated = option(options, "sp-aws-dedicated");
        assertEquals(List.of(1000, 10000), dedicated.getSupportedBandwidths());
        assertEquals(Integer.valueOf(10000), dedicated.getVcBandwidthMax());
        assertEquals(List.of("us-east-1-dx"), dedicated.getSellerRegions(),
                "uuid and seller regions are carried as a pair per candidate");
        assertTrue(dedicated.covers(10000), "dedicated covers a 10000 Mbps connection");
        assertFalse(dedicated.covers(3000), "3000 is not a dedicated tier either");
    }

    // ── Helpers (mirroring MetroOptimizerProviderResolutionTest's stub shapes) ──

    private static ServiceProfileOption option(List<ServiceProfileOption> options, String uuid) {
        return options.stream().filter(o -> uuid.equals(o.getServiceProfileUuid())).findFirst()
                .orElseThrow(() -> new AssertionError("no option for " + uuid + " in " + options));
    }

    private static ProviderAvailability availability(OptimizationResult result, String metroCode, String label) {
        MetroRecommendation rec = result.getRecommendations().stream()
                .filter(r -> r.getMetroId().code().equals(metroCode))
                .findFirst().orElseThrow(() -> new AssertionError(metroCode + " not recommended"));
        return rec.getAvailableProviders().stream()
                .filter(p -> label.equals(p.getProviderLabel()) && p.isAvailable())
                .findFirst().orElseThrow(() -> new AssertionError("no available " + label + " at " + metroCode));
    }

    private FabricGateway gatewayWith(ServiceProfile... profiles) throws Exception {
        Metro dc = metro("DC", "Ashburn", Region.AMER, 39.0438, -77.4874,
                List.of(connectedMetro("DA", 10.0)));
        Metro da = metro("DA", "Dallas", Region.AMER, 32.7767, -96.7970,
                List.of(connectedMetro("DC", 10.0)));

        Metros metros = mock(Metros.class);
        when(metros.list()).thenReturn(new PaginatedList<>(List.of(dc, da), null, null, null, null));

        ServiceProfiles serviceProfiles = mock(ServiceProfiles.class);
        when(serviceProfiles.search())
                .thenReturn(new PaginatedFilteredList<>(List.of(profiles), null, null, null, null));

        FabricGateway fabric = mock(FabricGateway.class);
        when(fabric.metros()).thenReturn(metros);
        when(fabric.serviceProfiles()).thenReturn(serviceProfiles);
        return fabric;
    }

    private static ServiceProfile profileWithConfigs(String uuid, String name,
                                                     List<ServiceProfileMetro> metros,
                                                     List<AccessPointTypeConfig> configs) {
        ServiceProfile profile = mock(ServiceProfile.class);
        lenient().when(profile.getUuid()).thenReturn(uuid);
        lenient().when(profile.getName()).thenReturn(name);
        lenient().when(profile.metros()).thenReturn(metros);
        lenient().when(profile.getAccessPointTypeConfigs()).thenReturn(configs);
        return profile;
    }

    private static Metro metro(String code, String name, Region region, double lat, double lon,
                               List<ConnectedMetro> connected) throws Exception {
        Metro m = mock(Metro.class);
        lenient().when(m.metroId()).thenReturn(MetroId.of(code));
        lenient().when(m.getCode()).thenReturn(MetroCode.fromCode(code));
        lenient().when(m.getName()).thenReturn(name);
        lenient().when(m.getRegion()).thenReturn(region);
        lenient().when(m.geoCoordinates()).thenReturn(geo(lat, lon));
        lenient().when(m.getConnectedMetros()).thenReturn(connected);
        return m;
    }

    private static ConnectedMetro connectedMetro(String code, double avgLatency) throws Exception {
        return MAPPER.readValue("{\"code\":\"" + code + "\",\"avgLatency\":" + avgLatency + "}",
                ConnectedMetro.class);
    }

    private static ServiceProfileMetro serviceProfileMetro(String code, String sellerRegion, Integer vcBandwidthMax)
            throws Exception {
        StringBuilder json = new StringBuilder("{\"code\":\"").append(code).append("\",\"name\":\"").append(code)
                .append("\",\"sellerRegions\":{\"").append(sellerRegion).append("\":\"").append(sellerRegion)
                .append("\"}");
        if (vcBandwidthMax != null) {
            json.append(",\"vcBandwidthMax\":").append(vcBandwidthMax);
        }
        json.append("}");
        return MAPPER.readValue(json.toString(), ServiceProfileMetro.class);
    }

    private static AccessPointTypeConfig apConfig(List<Integer> supportedBandwidths, boolean allowCustom)
            throws Exception {
        StringBuilder json = new StringBuilder("{\"allowCustomBandwidth\":").append(allowCustom)
                .append(",\"supportedBandwidths\":[");
        for (int i = 0; i < supportedBandwidths.size(); i++) {
            if (i > 0) json.append(",");
            json.append(supportedBandwidths.get(i));
        }
        json.append("]}");
        return MAPPER.readValue(json.toString(), AccessPointTypeConfig.class);
    }

    private static GeoCoordinate geo(double lat, double lon) throws Exception {
        return MAPPER.readValue("{\"latitude\":" + lat + ",\"longitude\":" + lon + "}", GeoCoordinate.class);
    }
}
