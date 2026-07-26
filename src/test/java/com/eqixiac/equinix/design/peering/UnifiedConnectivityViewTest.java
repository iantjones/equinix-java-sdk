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

package com.eqixiac.equinix.design.peering;

import com.eqixiac.equinix.core.model.MetroId;
import com.eqixiac.equinix.design.peering.enums.ConnectivityType;
import com.eqixiac.equinix.design.peering.model.UnifiedConnectivityView;
import com.eqixiac.equinix.design.peering.model.UnifiedConnectivityView.MetroConnectivity;
import org.junit.jupiter.api.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link UnifiedConnectivityView} — the per-ASN combined IX + Fabric view.
 */
@DisplayName("UnifiedConnectivityView")
class UnifiedConnectivityViewTest {

    private UnifiedConnectivityView view;

    @BeforeEach
    void build() {
        List<MetroConnectivity> metros = Arrays.asList(
                MetroConnectivity.builder()
                        .metro(MetroId.of("DC"))
                        .connectivityType(ConnectivityType.IX_PEERING)
                        .hasIxPeering(true).hasFabric(false)
                        .ixCapacityMbps(100000).routeServerAvailable(true)
                        .bfdAvailable(true)
                        .ixSessions(Collections.emptyList())
                        .fabricServiceProfileUuid(null)
                        .build(),
                MetroConnectivity.builder()
                        .metro(MetroId.of("DA"))
                        .connectivityType(ConnectivityType.IX_PEERING)
                        .hasIxPeering(true).hasFabric(false)
                        .ixCapacityMbps(10000).routeServerAvailable(false)
                        .bfdAvailable(false)
                        .ixSessions(Collections.emptyList())
                        .fabricServiceProfileUuid(null)
                        .build(),
                MetroConnectivity.builder()
                        .metro(MetroId.of("SG"))
                        .connectivityType(ConnectivityType.FABRIC_CONNECTION)
                        .hasIxPeering(false).hasFabric(true)
                        .ixCapacityMbps(0).routeServerAvailable(false)
                        .bfdAvailable(false)
                        .ixSessions(Collections.emptyList())
                        .fabricServiceProfileUuid("uuid-abc-123")
                        .build()
        );

        view = UnifiedConnectivityView.builder()
                .asn(16509L)
                .label("AWS")
                .metroConnectivity(metros)
                .reachableMetroCount(3)
                .totalIxCapacityMbps(110000)
                .fabricAvailableAnywhere(true)
                .build();
    }

    @Test
    @DisplayName("forMetro should return correct connectivity")
    void forMetro() {
        MetroConnectivity dc = view.forMetro(MetroId.of("DC"));
        assertNotNull(dc);
        assertTrue(dc.isHasIxPeering());
        assertFalse(dc.isHasFabric());
        assertEquals(100000, dc.getIxCapacityMbps());
    }

    @Test
    @DisplayName("forMetro should return null for absent metro")
    void forAbsentMetro() {
        assertNull(view.forMetro(MetroId.of("HK")));
    }

    @Test
    @DisplayName("ixPeeringMetros should filter correctly")
    void ixPeeringMetros() {
        List<MetroConnectivity> ixMetros = view.ixPeeringMetros();
        assertEquals(2, ixMetros.size()); // DC and DA
    }

    @Test
    @DisplayName("fabricMetros should filter correctly")
    void fabricMetros() {
        List<MetroConnectivity> fabMetros = view.fabricMetros();
        assertEquals(1, fabMetros.size()); // SG only
        assertEquals(MetroId.of("SG"), fabMetros.get(0).getMetro());
    }

    @Test
    @DisplayName("toMarkdown should produce valid Markdown table")
    void toMarkdown() {
        String md = view.toMarkdown();
        assertNotNull(md);
        assertTrue(md.contains("### Unified Connectivity: AWS (AS16509)"));
        assertTrue(md.contains("| Metro | IX Peering |"));
        assertTrue(md.contains("DC"));
        assertTrue(md.contains("DA"));
        assertTrue(md.contains("SG"));
    }

    @Test
    @DisplayName("Summary stats should be accurate")
    void summaryStats() {
        assertEquals(3, view.getReachableMetroCount());
        assertEquals(110000, view.getTotalIxCapacityMbps());
        assertTrue(view.isFabricAvailableAnywhere());
    }
}
