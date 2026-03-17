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

package api.equinix.javasdk.fabric.peering;

import api.equinix.javasdk.core.enums.MetroCode;
import api.equinix.javasdk.fabric.peering.enums.ConnectivityType;
import api.equinix.javasdk.fabric.peering.model.IxPresenceDetail;
import api.equinix.javasdk.fabric.peering.model.PresenceCell;
import api.equinix.javasdk.fabric.peering.model.PresenceMatrix;
import org.junit.jupiter.api.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link PresenceMatrix} — the ASN x Metro grid that forms
 * the core of the peering intelligence analysis.
 */
@DisplayName("Presence Matrix")
class PresenceMatrixTest {

    private static PresenceMatrix matrix;

    // AWS=16509, Microsoft=8075, Google=15169
    private static final long AWS = 16509L;
    private static final long MSFT = 8075L;
    private static final long GOOG = 15169L;

    @BeforeAll
    static void buildMatrix() {
        List<Long> asns = Arrays.asList(AWS, MSFT, GOOG);
        List<MetroCode> metros = Arrays.asList(MetroCode.DC, MetroCode.CH, MetroCode.DA, MetroCode.SG);

        Map<Long, String> labels = new LinkedHashMap<>();
        labels.put(AWS, "AWS");
        labels.put(MSFT, "Microsoft");
        labels.put(GOOG, "Google");

        Map<Long, Map<MetroCode, PresenceCell>> cells = new LinkedHashMap<>();

        // AWS: present at DC (IX+RS, 100G), CH (IX, 40G), DA (IX, 10G), not SG
        cells.put(AWS, buildRow(AWS, metros, new boolean[]{true, true, true, false},
                new int[]{100000, 40000, 10000, 0}, new boolean[]{true, false, false, false}));

        // Microsoft: present at DC (IX, 200G), CH (IX, 100G), not DA, SG (IX, 10G)
        cells.put(MSFT, buildRow(MSFT, metros, new boolean[]{true, true, false, true},
                new int[]{200000, 100000, 0, 10000}, new boolean[]{true, true, false, true}));

        // Google: present at DC (IX, 400G), CH (IX, 200G), DA (IX, 100G), SG (IX, 50G)
        cells.put(GOOG, buildRow(GOOG, metros, new boolean[]{true, true, true, true},
                new int[]{400000, 200000, 100000, 50000}, new boolean[]{true, true, true, true}));

        matrix = PresenceMatrix.builder()
                .asns(asns)
                .asnLabels(labels)
                .metros(metros)
                .cells(cells)
                .build();
    }

    @Nested
    @DisplayName("Cell access")
    class CellAccessTests {

        @Test
        @DisplayName("get() should return correct cell for known ASN and metro")
        void getKnownCell() {
            PresenceCell cell = matrix.get(AWS, MetroCode.DC);
            assertNotNull(cell);
            assertTrue(cell.isIxPresent());
            assertEquals(100000, cell.getTotalIxCapacityMbps());
        }

        @Test
        @DisplayName("get() should return cell even when ASN is not present (IX = false)")
        void getAbsentPresence() {
            PresenceCell cell = matrix.get(AWS, MetroCode.SG);
            assertNotNull(cell);
            assertFalse(cell.isIxPresent());
            assertEquals(ConnectivityType.NONE, cell.getConnectivityType());
        }

        @Test
        @DisplayName("get() should return null for unknown ASN")
        void getUnknownAsn() {
            assertNull(matrix.get(99999L, MetroCode.DC));
        }
    }

    @Nested
    @DisplayName("Row and column views")
    class RowColumnTests {

        @Test
        @DisplayName("forAsn() should return all metros for an ASN")
        void forAsn() {
            Map<MetroCode, PresenceCell> awsRow = matrix.forAsn(AWS);
            assertEquals(4, awsRow.size());
            assertTrue(awsRow.get(MetroCode.DC).isIxPresent());
            assertFalse(awsRow.get(MetroCode.SG).isIxPresent());
        }

        @Test
        @DisplayName("forMetro() should return all ASNs at a metro")
        void forMetro() {
            Map<Long, PresenceCell> dcColumn = matrix.forMetro(MetroCode.DC);
            assertEquals(3, dcColumn.size()); // all 3 ASNs present
            assertTrue(dcColumn.get(AWS).isIxPresent());
            assertTrue(dcColumn.get(MSFT).isIxPresent());
            assertTrue(dcColumn.get(GOOG).isIxPresent());
        }

        @Test
        @DisplayName("forAsn() for unknown ASN should return empty map")
        void forUnknownAsn_returnsEmpty() {
            assertTrue(matrix.forAsn(99999L).isEmpty());
        }
    }

    @Nested
    @DisplayName("Metro filtering")
    class MetroFilterTests {

        @Test
        @DisplayName("metrosWithAllAsns should find metros where all ASNs have IX")
        void metrosWithAll() {
            List<MetroCode> shared = matrix.metrosWithAllAsns(Arrays.asList(AWS, MSFT, GOOG));
            // DC and CH have all 3
            assertTrue(shared.contains(MetroCode.DC));
            assertTrue(shared.contains(MetroCode.CH));
            assertFalse(shared.contains(MetroCode.DA)); // MSFT not in DA
            assertFalse(shared.contains(MetroCode.SG)); // AWS not in SG
        }

        @Test
        @DisplayName("metrosWithAnyAsn should find metros where any ASN has IX")
        void metrosWithAny() {
            List<MetroCode> any = matrix.metrosWithAnyAsn(Arrays.asList(AWS, MSFT));
            // DC, CH (both), DA (AWS only), SG (MSFT only) = 4
            assertEquals(4, any.size());
        }

        @Test
        @DisplayName("asnCountByMetro should count IX-present ASNs per metro")
        void asnCount() {
            Map<MetroCode, Integer> counts = matrix.asnCountByMetro();
            assertEquals(3, counts.get(MetroCode.DC)); // all 3
            assertEquals(3, counts.get(MetroCode.CH)); // all 3
            assertEquals(2, counts.get(MetroCode.DA)); // AWS + Google
            assertEquals(2, counts.get(MetroCode.SG)); // MSFT + Google
        }
    }

    @Nested
    @DisplayName("Output formatting")
    class OutputTests {

        @Test
        @DisplayName("toTableString should produce non-empty ASCII table")
        void tableString() {
            String table = matrix.toTableString();
            assertNotNull(table);
            assertFalse(table.isEmpty());
            assertTrue(table.contains("AWS"));
            assertTrue(table.contains("DC"));
        }

        @Test
        @DisplayName("toDetailedTableString should include capacity")
        void detailedTable() {
            String table = matrix.toDetailedTableString();
            assertNotNull(table);
            assertTrue(table.contains("IX:"));
        }

        @Test
        @DisplayName("toMarkdown should produce valid Markdown table")
        void markdown() {
            String md = matrix.toMarkdown();
            assertNotNull(md);
            assertTrue(md.contains("| ASN | Network |"));
            assertTrue(md.contains("|-----|---------|"));
            assertTrue(md.contains("16509"));
            assertTrue(md.contains("AWS"));
        }
    }

    @Nested
    @DisplayName("PresenceCell symbols")
    class CellSymbolTests {

        @Test
        @DisplayName("IX-only cell should show IX symbol")
        void ixSymbol() {
            PresenceCell cell = PresenceCell.builder()
                    .asn(AWS).metro(MetroCode.DC)
                    .connectivityType(ConnectivityType.IX_PEERING)
                    .ixPresent(true).fabricAvailable(false).facilityPresent(false)
                    .ixSessionCount(1).totalIxCapacityMbps(10000)
                    .routeServerPeer(false).bfdSupported(false)
                    .ixSessions(Collections.emptyList())
                    .build();
            assertEquals(" IX ", cell.symbol());
        }

        @Test
        @DisplayName("BOTH cell should show IX+F symbol")
        void bothSymbol() {
            PresenceCell cell = PresenceCell.builder()
                    .asn(AWS).metro(MetroCode.DC)
                    .connectivityType(ConnectivityType.BOTH)
                    .ixPresent(true).fabricAvailable(true).facilityPresent(false)
                    .ixSessionCount(1).totalIxCapacityMbps(10000)
                    .routeServerPeer(false).bfdSupported(false)
                    .ixSessions(Collections.emptyList())
                    .build();
            assertEquals("IX+F", cell.symbol());
        }

        @Test
        @DisplayName("NONE cell should show -- symbol")
        void noneSymbol() {
            PresenceCell cell = PresenceCell.builder()
                    .asn(AWS).metro(MetroCode.SG)
                    .connectivityType(ConnectivityType.NONE)
                    .ixPresent(false).fabricAvailable(false).facilityPresent(false)
                    .ixSessionCount(0).totalIxCapacityMbps(0)
                    .routeServerPeer(false).bfdSupported(false)
                    .ixSessions(Collections.emptyList())
                    .build();
            assertEquals(" -- ", cell.symbol());
        }

        @Test
        @DisplayName("detailedSymbol should include capacity and route server marker")
        void detailedSymbol() {
            PresenceCell cell = PresenceCell.builder()
                    .asn(AWS).metro(MetroCode.DC)
                    .connectivityType(ConnectivityType.IX_PEERING)
                    .ixPresent(true).fabricAvailable(false).facilityPresent(false)
                    .ixSessionCount(1).totalIxCapacityMbps(100000)
                    .routeServerPeer(true).bfdSupported(false)
                    .ixSessions(Collections.emptyList())
                    .build();
            String sym = cell.detailedSymbol();
            assertTrue(sym.contains("IX:"));
            assertTrue(sym.contains("G"));
            assertTrue(sym.contains("*")); // route server marker
        }
    }

    // ---- Helpers ----

    private static Map<MetroCode, PresenceCell> buildRow(long asn, List<MetroCode> metros,
                                                          boolean[] present, int[] capacities,
                                                          boolean[] routeServer) {
        Map<MetroCode, PresenceCell> row = new LinkedHashMap<>();
        for (int i = 0; i < metros.size(); i++) {
            MetroCode metro = metros.get(i);
            ConnectivityType type = present[i] ? ConnectivityType.IX_PEERING : ConnectivityType.NONE;
            row.put(metro, PresenceCell.builder()
                    .asn(asn)
                    .metro(metro)
                    .connectivityType(type)
                    .ixPresent(present[i])
                    .facilityPresent(present[i])
                    .fabricAvailable(false)
                    .ixSessionCount(present[i] ? 1 : 0)
                    .totalIxCapacityMbps(capacities[i])
                    .routeServerPeer(routeServer[i])
                    .bfdSupported(false)
                    .ixSessions(present[i]
                            ? Collections.singletonList(IxPresenceDetail.builder()
                                .metro(metro).ixId(i + 1).ixName("Equinix " + metro.name())
                                .speedMbps(capacities[i]).routeServerPeer(routeServer[i])
                                .bfdSupport(false).operational(true).build())
                            : Collections.emptyList())
                    .build());
        }
        return row;
    }
}
