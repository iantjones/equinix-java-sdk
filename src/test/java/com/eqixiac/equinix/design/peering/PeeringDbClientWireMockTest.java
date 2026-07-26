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

import com.eqixiac.equinix.core.WireMockTestBase;
import com.eqixiac.equinix.design.peering.client.PeeringDbClient;
import com.eqixiac.equinix.design.peering.client.PeeringDbFacility;
import com.eqixiac.equinix.design.peering.client.PeeringDbIx;
import com.eqixiac.equinix.design.peering.client.PeeringDbNetFac;
import com.eqixiac.equinix.design.peering.client.PeeringDbNetIxlan;
import com.eqixiac.equinix.design.peering.client.PeeringDbNetwork;
import com.eqixiac.equinix.design.peering.client.PeeringDbOrg;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Exercises the real {@link PeeringDbClient} HTTP + Jackson-parsing paths against a WireMock
 * stub of the PeeringDB REST API — the catalog load, per-ASN queries, Equinix-ID filtering,
 * the cached accessors, and the not-loaded guard. These paths were previously never executed:
 * every other peering test either overrides the client wholesale or deserializes fixtures
 * directly, so the client's own {@code executeGet}/{@code getList}/{@code loadEquinixCatalog}
 * logic (URL construction, status handling, tree/stream deserialization, ID filtering) had
 * zero coverage.
 *
 * <p>The client is pointed at WireMock via the package-external {@link PeeringDbClient#withBaseUrl}
 * test seam (see the {@code sdkChangeNeeded} note in the engagement); no Equinix OAuth is
 * involved — PeeringDB is an independent public API, so the base class's token stub is unused
 * here.</p>
 */
@DisplayName("PeeringDbClient — real HTTP/parsing against a stubbed PeeringDB")
class PeeringDbClientWireMockTest extends WireMockTestBase {

    private static final long AWS = 16509L;

    private PeeringDbClient client() {
        // baseUrl mirrors PeeringDB's "/api" suffix so the client's own path building is exercised.
        return PeeringDbClient.withBaseUrl(null, wireMockUrl() + "/api");
    }

    @BeforeEach
    void resetBeforeEach() {
        resetStubs();
    }

    private void stubCatalog() {
        wireMock.stubFor(get(urlPathEqualTo("/api/org/2"))
                .withQueryParam("depth", equalTo("2"))
                .willReturn(okJson(loadFixture("/json/peering/peeringdb_org_response.json"))));
    }

    private void stubAsn16509() {
        wireMock.stubFor(get(urlPathEqualTo("/api/net"))
                .withQueryParam("asn", equalTo("16509"))
                .willReturn(okJson(loadFixture("/json/peering/peeringdb_net_16509_response.json"))));
        wireMock.stubFor(get(urlPathEqualTo("/api/netixlan"))
                .withQueryParam("asn", equalTo("16509"))
                .willReturn(okJson(loadFixture("/json/peering/peeringdb_netixlan_16509_response.json"))));
        wireMock.stubFor(get(urlPathEqualTo("/api/netfac"))
                .withQueryParam("asn", equalTo("16509"))
                .willReturn(okJson(loadFixture("/json/peering/peeringdb_netfac_16509_response.json"))));
    }

    // ---- Catalog load ----

    @Test
    @DisplayName("loadEquinixCatalog parses org/2?depth=2 into the cached IX and facility maps")
    void loadCatalogPopulatesCaches() throws IOException {
        stubCatalog();
        PeeringDbClient client = client();

        client.loadEquinixCatalog();

        PeeringDbOrg org = client.getEquinixOrg();
        assertNotNull(org);
        assertEquals(2, org.getId());
        assertEquals("Equinix", org.getAka());

        Map<Integer, PeeringDbIx> ixMap = client.getEquinixIxMap();
        Map<Integer, PeeringDbFacility> facMap = client.getEquinixFacMap();
        assertEquals(3, ixMap.size(), "org fixture has 3 Equinix IXes");
        assertEquals(3, facMap.size(), "org fixture has 3 Equinix facilities");

        assertEquals(Set.of(100, 101, 102), client.getEquinixIxIds());
        assertEquals(Set.of(200, 201, 202), client.getEquinixFacIds());

        assertEquals("Equinix Ashburn", client.getEquinixIx(100).getName());
        assertEquals("Ashburn", client.getEquinixIx(100).getCity());
        assertEquals("Equinix DC11 - Ashburn", client.getEquinixFacility(200).getName());
        assertEquals(39.0181, client.getEquinixFacility(200).getLatitude(), 1e-9);

        // The catalog request went to the right path with the depth filter.
        wireMock.verify(getRequestedFor(urlPathEqualTo("/api/org/2"))
                .withQueryParam("depth", equalTo("2")));
    }

    @Test
    @DisplayName("returned Equinix ID sets are unmodifiable")
    void idSetsAreUnmodifiable() throws IOException {
        stubCatalog();
        PeeringDbClient client = client();
        client.loadEquinixCatalog();

        Set<Integer> ixIds = client.getEquinixIxIds();
        assertThrows(UnsupportedOperationException.class, () -> ixIds.add(12345));
    }

    // ---- Per-ASN queries (unfiltered) ----

    @Test
    @DisplayName("getNetwork deserializes the first data element of /net")
    void getNetworkParsesFirstElement() throws IOException {
        stubAsn16509();
        PeeringDbNetwork net = client().getNetwork(AWS);

        assertNotNull(net);
        assertEquals(AWS, net.getAsn());
        assertEquals("Amazon.com, Inc.", net.getName());
        assertEquals("Selective", net.getPolicyGeneral());
        assertEquals("Content", net.getInfoType());
        assertTrue(net.isInfoIpv6());
    }

    @Test
    @DisplayName("getNetwork returns null when /net has an empty data array")
    void getNetworkNullOnEmpty() throws IOException {
        wireMock.stubFor(get(urlPathEqualTo("/api/net"))
                .withQueryParam("asn", equalTo("64999"))
                .willReturn(okJson("{\"data\": []}")));
        assertNull(client().getNetwork(64999L));
    }

    @Test
    @DisplayName("getNetIxlans deserializes the full IX-presence list (unfiltered by org)")
    void getNetIxlansParsesList() throws IOException {
        stubAsn16509();
        List<PeeringDbNetIxlan> all = client().getNetIxlans(AWS);

        // All three rows — including the non-Equinix ix_id 999 — come back unfiltered.
        assertEquals(3, all.size());
        assertEquals(100, all.get(0).getIxId());
        assertEquals(100000, all.get(0).getSpeed());
        assertTrue(all.get(0).isRsPeer());
        assertEquals(999, all.get(2).getIxId());
    }

    @Test
    @DisplayName("getNetFacs deserializes the full facility-presence list (unfiltered by org)")
    void getNetFacsParsesList() throws IOException {
        stubAsn16509();
        List<PeeringDbNetFac> all = client().getNetFacs(AWS);

        assertEquals(3, all.size());
        assertEquals(200, all.get(0).getFacId());
        assertEquals("Ashburn", all.get(0).getCity());
        assertEquals(888, all.get(2).getFacId());
    }

    @Test
    @DisplayName("getList returns an empty list when the data field is absent")
    void getListEmptyWhenNoData() throws IOException {
        wireMock.stubFor(get(urlPathEqualTo("/api/netixlan"))
                .withQueryParam("asn", equalTo("64998"))
                .willReturn(okJson("{\"meta\": {}}")));
        assertTrue(client().getNetIxlans(64998L).isEmpty());
    }

    // ---- Equinix-scoped filtering ----

    @Test
    @DisplayName("getEquinixIxPresence keeps only netixlans at Equinix IXes")
    void ixPresenceFiltersToEquinix() throws IOException {
        stubCatalog();
        stubAsn16509();
        PeeringDbClient client = client();
        client.loadEquinixCatalog();

        List<PeeringDbNetIxlan> equinixOnly = client.getEquinixIxPresence(AWS);

        // ix 100 + 101 are Equinix; ix 999 is dropped.
        assertEquals(2, equinixOnly.size());
        assertTrue(equinixOnly.stream().allMatch(n -> Set.of(100, 101).contains(n.getIxId())));
        assertTrue(equinixOnly.stream().noneMatch(n -> n.getIxId() == 999));
    }

    @Test
    @DisplayName("getEquinixFacPresence keeps only netfacs at Equinix facilities")
    void facPresenceFiltersToEquinix() throws IOException {
        stubCatalog();
        stubAsn16509();
        PeeringDbClient client = client();
        client.loadEquinixCatalog();

        List<PeeringDbNetFac> equinixOnly = client.getEquinixFacPresence(AWS);

        // fac 200 + 201 are Equinix; fac 888 is dropped.
        assertEquals(2, equinixOnly.size());
        assertTrue(equinixOnly.stream().allMatch(n -> Set.of(200, 201).contains(n.getFacId())));
        assertTrue(equinixOnly.stream().noneMatch(n -> n.getFacId() == 888));
    }

    // ---- Not-loaded guard ----

    @Test
    @DisplayName("Equinix-scoped calls throw IllegalStateException before the catalog is loaded")
    void guardBeforeCatalogLoaded() {
        PeeringDbClient client = client();

        assertThrows(IllegalStateException.class, () -> client.getEquinixIxPresence(AWS));
        assertThrows(IllegalStateException.class, () -> client.getEquinixFacPresence(AWS));
        assertThrows(IllegalStateException.class, client::getEquinixIxIds);
        assertThrows(IllegalStateException.class, client::getEquinixFacIds);
        assertThrows(IllegalStateException.class, client::getEquinixIxMap);
        assertThrows(IllegalStateException.class, client::getEquinixFacMap);
        assertThrows(IllegalStateException.class, () -> client.getEquinixIx(100));
        assertThrows(IllegalStateException.class, () -> client.getEquinixFacility(200));

        // getEquinixOrg is the one Equinix accessor that does NOT guard — it returns null until loaded.
        assertNull(client.getEquinixOrg());
    }

    // ---- Error mapping ----

    @Test
    @DisplayName("a non-200 catalog response surfaces as IOException with the status and body")
    void catalogErrorSurfacesAsIoException() {
        wireMock.stubFor(get(urlPathEqualTo("/api/org/2"))
                .willReturn(aResponse().withStatus(503).withBody("upstream down")));
        PeeringDbClient client = client();

        IOException ex = assertThrows(IOException.class, client::loadEquinixCatalog);
        assertTrue(ex.getMessage().contains("503"), () -> "message was: " + ex.getMessage());
        assertTrue(ex.getMessage().contains("upstream down"));
    }

    @Test
    @DisplayName("an empty catalog data array is rejected as an IOException")
    void emptyCatalogRejected() {
        wireMock.stubFor(get(urlPathEqualTo("/api/org/2"))
                .willReturn(okJson("{\"data\": []}")));

        IOException ex = assertThrows(IOException.class, () -> client().loadEquinixCatalog());
        assertTrue(ex.getMessage().contains("no data"), () -> "message was: " + ex.getMessage());
    }
}
