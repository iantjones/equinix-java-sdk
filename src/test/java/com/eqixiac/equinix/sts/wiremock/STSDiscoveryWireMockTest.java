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

package com.eqixiac.equinix.sts.wiremock;

import com.eqixiac.equinix.STS;
import com.eqixiac.equinix.core.WireMockTestBase;
import com.eqixiac.equinix.core.exception.EquinixServerException;
import com.eqixiac.equinix.sts.model.Jwks;
import com.eqixiac.equinix.sts.model.OpenIdConfiguration;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Map;

import static com.eqixiac.equinix.core.ResponseStubs.*;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * WireMock-based API tests for the STS unauthenticated discovery client ({@code sts.discovery()}).
 *
 * <p>Exercises the two read-only discovery endpoints:</p>
 * <ul>
 *   <li>{@code getJwks()} — GET {@code /jwks} (operationId {@code getJwks}), asserting the parsed
 *       JSON Web Key Set.</li>
 *   <li>{@code getOpenIdConfiguration()} — GET {@code /.well-known/openid-configuration}
 *       (operationId {@code getOpenIdConfiguration}), asserting the parsed discovery document.</li>
 * </ul>
 *
 * <p>The {@code Discovery} functional area uses {@code overrideUriFormat: {$requestUri}}, so the
 * resolved paths sit at the gateway root ({@code /jwks} and {@code /.well-known/openid-configuration})
 * with no version segment.</p>
 */
class STSDiscoveryWireMockTest extends WireMockTestBase {

    static STS sts;

    @BeforeAll
    static void setUp() {
        sts = new STS(testCredentials());
        redirectToWireMock(sts);
        sts.authenticate();
    }

    @AfterAll
    static void tearDown() throws Exception {
        if (sts != null) sts.close();
    }

    @BeforeEach
    void resetBeforeEach() {
        resetStubs();
    }

    @Nested
    @DisplayName("getJwks()")
    class GetJwks {

        @Test
        @DisplayName("GETs /jwks and parses the key set")
        void parsesKeySet() {
            stubSingleton(wireMock, "/jwks", "/json/sts/jwks.json");

            Jwks jwks = sts.discovery().getJwks();

            assertNotNull(jwks);
            assertNotNull(jwks.getKeys());
            assertEquals(2, jwks.getKeys().size());

            // Keys deserialize as raw JSON objects (Map) since the spec models them as open objects.
            assertInstanceOf(Map.class, jwks.getKeys().get(0));
            @SuppressWarnings("unchecked")
            Map<String, Object> firstKey = (Map<String, Object>) jwks.getKeys().get(0);
            assertEquals("RSA", firstKey.get("kty"));
            assertEquals("sts-key-2025-01", firstKey.get("kid"));
            assertEquals("RS256", firstKey.get("alg"));

            @SuppressWarnings("unchecked")
            Map<String, Object> secondKey = (Map<String, Object>) jwks.getKeys().get(1);
            assertEquals("EC", secondKey.get("kty"));
            assertEquals("ES256", secondKey.get("alg"));

            wireMock.verify(getRequestedFor(urlPathEqualTo("/jwks")));
        }

        @Test
        @DisplayName("parses an empty key set")
        void parsesEmptyKeySet() {
            wireMock.stubFor(get(urlPathEqualTo("/jwks"))
                    .willReturn(okJson("{\"keys\":[]}")));

            Jwks jwks = sts.discovery().getJwks();

            assertNotNull(jwks);
            assertNotNull(jwks.getKeys());
            assertTrue(jwks.getKeys().isEmpty());

            wireMock.verify(getRequestedFor(urlPathEqualTo("/jwks")));
        }

        @Test
        @DisplayName("500 throws EquinixServerException")
        void serverError() {
            stubErrorInline(wireMock, "/jwks", 500,
                    "{\"errorCode\":\"ERR-500\",\"errorMessage\":\"Internal server error\"}");

            assertThrows(EquinixServerException.class, () -> sts.discovery().getJwks());

            wireMock.verify(getRequestedFor(urlPathEqualTo("/jwks")));
        }
    }

    @Nested
    @DisplayName("getOpenIdConfiguration()")
    class GetOpenIdConfiguration {

        @Test
        @DisplayName("GETs /.well-known/openid-configuration and parses the discovery document")
        void parsesDiscoveryDocument() {
            stubSingleton(wireMock, "/\\.well-known/openid-configuration",
                    "/json/sts/openid_configuration.json");

            OpenIdConfiguration config = sts.discovery().getOpenIdConfiguration();

            assertNotNull(config);
            assertEquals("https://sts.eqix.equinix.com", config.getIssuer());
            assertEquals("https://sts.eqix.equinix.com/jwks", config.getJwksUri());
            assertEquals("https://sts.eqix.equinix.com/use/token", config.getTokenEndpoint());
            assertEquals(List.of("public"), config.getSubjectTypesSupported());
            assertEquals(List.of("token"), config.getResponseTypesSupported());
            assertEquals(List.of("RS256", "ES256"), config.getIdTokenSigningAlgValuesSupported());
            assertTrue(config.getClaimsSupported().contains("scope"));

            wireMock.verify(getRequestedFor(urlPathEqualTo("/.well-known/openid-configuration")));
        }

        @Test
        @DisplayName("500 throws EquinixServerException")
        void serverError() {
            stubErrorInline(wireMock, "/\\.well-known/openid-configuration", 500,
                    "{\"errorCode\":\"ERR-500\",\"errorMessage\":\"Internal server error\"}");

            assertThrows(EquinixServerException.class, () -> sts.discovery().getOpenIdConfiguration());

            wireMock.verify(getRequestedFor(urlPathEqualTo("/.well-known/openid-configuration")));
        }
    }
}
