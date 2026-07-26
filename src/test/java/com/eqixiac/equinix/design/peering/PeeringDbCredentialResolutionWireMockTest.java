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

import com.eqixiac.equinix.Design;
import com.eqixiac.equinix.Equinix;
import com.eqixiac.equinix.EquinixConfig;
import com.eqixiac.equinix.Fabric;
import com.eqixiac.equinix.FabricGateway;
import com.eqixiac.equinix.core.WireMockTestBase;
import com.eqixiac.equinix.core.http.response.PaginatedList;
import com.eqixiac.equinix.core.model.MetroId;
import com.eqixiac.equinix.fabric.client.Metros;
import com.eqixiac.equinix.fabric.model.Metro;
import com.eqixiac.equinix.fabric.model.implementation.GeoCoordinate;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Proves the PeeringDB API-key resolution order — explicit argument, then
 * {@code EquinixConfig.peeringDbApiKey}, then the {@code PEERINGDB_API_KEY} environment variable,
 * then anonymous — at the wire level: each test runs {@code analyze()} against a WireMock-stubbed
 * PeeringDB and asserts the presence (and exact value) or absence of the
 * {@code Authorization: Api-Key ...} header on the PeeringDB requests.
 *
 * <p>The env-var tier is exercised through the builder's package-private {@code envLookup} seam
 * (Java offers no portable way to set a real environment variable in a test); the config tier is
 * exercised through the real {@link Fabric} facade and a real {@link Equinix} session +
 * {@link Design} facade, both redirected to WireMock. The engine's Fabric metro-geo load is
 * best-effort, so the facade tests deliberately leave {@code /fabric/v4/metros} unstubbed.</p>
 */
@DisplayName("PeeringDB API-key resolution (explicit > config > env > anonymous)")
class PeeringDbCredentialResolutionWireMockTest extends WireMockTestBase {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final long AWS = 16509L;

    @BeforeEach
    void resetBeforeEach() {
        resetStubs();
        stubPeeringDb();
    }

    private String baseUrl() {
        return wireMockUrl() + "/api";
    }

    private void stubPeeringDb() {
        wireMock.stubFor(get(urlPathEqualTo("/api/org/2"))
                .willReturn(okJson(loadFixture("/json/peering/peeringdb_org_response.json"))));
        wireMock.stubFor(get(urlPathEqualTo("/api/net")).withQueryParam("asn", equalTo("16509"))
                .willReturn(okJson(loadFixture("/json/peering/peeringdb_net_16509_response.json"))));
        wireMock.stubFor(get(urlPathEqualTo("/api/netixlan")).withQueryParam("asn", equalTo("16509"))
                .willReturn(okJson(loadFixture("/json/peering/peeringdb_netixlan_16509_response.json"))));
        wireMock.stubFor(get(urlPathEqualTo("/api/netfac")).withQueryParam("asn", equalTo("16509"))
                .willReturn(okJson(loadFixture("/json/peering/peeringdb_netfac_16509_response.json"))));
    }

    private void analyzeWith(PeeringIntelligence.Builder builder) {
        builder.peeringDbBaseUrl(baseUrl()).addAsn(AWS, "AWS").analyze();
    }

    @Test
    @DisplayName("an explicit key is sent as Authorization: Api-Key on every PeeringDB request")
    void explicitKeyIsSent() {
        analyzeWith(PeeringIntelligence.builder(stubFabric(), "explicit-key"));

        wireMock.verify(getRequestedFor(urlPathEqualTo("/api/net"))
                .withHeader("Authorization", equalTo("Api-Key explicit-key")));
        wireMock.verify(getRequestedFor(urlPathEqualTo("/api/netixlan"))
                .withHeader("Authorization", equalTo("Api-Key explicit-key")));
    }

    @Test
    @DisplayName("with no key anywhere, requests carry no Authorization header (anonymous)")
    void anonymousWhenNoKeyAnywhere() {
        analyzeWith(PeeringIntelligence.builder(stubFabric(), null)
                .envLookup(name -> null));

        wireMock.verify(getRequestedFor(urlPathEqualTo("/api/net"))
                .withoutHeader("Authorization"));
    }

    @Test
    @DisplayName("the PEERINGDB_API_KEY environment variable supplies the key when nothing is explicit")
    void envVariableSuppliesKey() {
        analyzeWith(PeeringIntelligence.builder(stubFabric(), null)
                .envLookup(name -> PeeringIntelligence.PEERINGDB_API_KEY_ENV.equals(name) ? "env-key" : null));

        wireMock.verify(getRequestedFor(urlPathEqualTo("/api/net"))
                .withHeader("Authorization", equalTo("Api-Key env-key")));
    }

    @Test
    @DisplayName("an explicit key beats the environment variable")
    void explicitKeyBeatsEnv() {
        analyzeWith(PeeringIntelligence.builder(stubFabric(), "explicit-key")
                .envLookup(name -> "env-key"));

        wireMock.verify(getRequestedFor(urlPathEqualTo("/api/net"))
                .withHeader("Authorization", equalTo("Api-Key explicit-key")));
    }

    @Test
    @DisplayName("a blank explicit key is treated as absent and falls back to the environment")
    void blankExplicitKeyFallsBackToEnv() {
        analyzeWith(PeeringIntelligence.builder(stubFabric(), "  ")
                .envLookup(name -> "env-key"));

        wireMock.verify(getRequestedFor(urlPathEqualTo("/api/net"))
                .withHeader("Authorization", equalTo("Api-Key env-key")));
    }

    @Test
    @DisplayName("EquinixConfig.peeringDbApiKey flows through fabric.peeringIntelligence()")
    void configKeyFlowsThroughFabricFacade() throws Exception {
        EquinixConfig config = EquinixConfig.builder()
                .autoLoadMetros(false)
                .peeringDbApiKey("config-key")
                .build();

        try (Fabric fabric = new Fabric(testCredentials(), config)) {
            redirectToWireMock(fabric);
            fabric.authenticate();

            analyzeWith(fabric.peeringIntelligence());
        }

        wireMock.verify(getRequestedFor(urlPathEqualTo("/api/net"))
                .withHeader("Authorization", equalTo("Api-Key config-key")));
    }

    @Test
    @DisplayName("a session-level key flows through eq.design().peeringIntelligence()")
    void sessionKeyFlowsThroughDesignFacade() throws Exception {
        EquinixConfig config = EquinixConfig.builder()
                .autoLoadMetros(false)
                .peeringDbApiKey("session-key")
                .build();

        try (Equinix eq = new Equinix(testCredentials(), config)) {
            redirectToWireMock(eq.fabric());
            eq.fabric().authenticate();

            Design design = eq.design();
            analyzeWith(design.peeringIntelligence());
        }

        wireMock.verify(getRequestedFor(urlPathEqualTo("/api/net"))
                .withHeader("Authorization", equalTo("Api-Key session-key")));
    }

    @Test
    @DisplayName("EquinixConfig defaults to no PeeringDB key")
    void configDefaultsToNoKey() {
        assertNull(EquinixConfig.defaults().getPeeringDbApiKey());
    }

    // ---- minimal Fabric stub (the engine's metro-geo load is best-effort) ----

    private static FabricGateway stubFabric() {
        try {
            Metro dc = mock(Metro.class);
            when(dc.metroId()).thenReturn(MetroId.of("DC"));
            when(dc.geoCoordinates()).thenReturn(
                    MAPPER.readValue("{\"latitude\":39.0,\"longitude\":-77.5}", GeoCoordinate.class));
            when(dc.getIbxs()).thenReturn(List.of("DC11"));

            Metros metrosClient = mock(Metros.class);
            when(metrosClient.list()).thenReturn(new PaginatedList<>(List.of(dc), null, null, null, null));

            FabricGateway fabric = mock(FabricGateway.class);
            when(fabric.metros()).thenReturn(metrosClient);
            return fabric;
        } catch (Exception e) {
            throw new IllegalStateException("failed to build Fabric stub", e);
        }
    }
}
