/*
 * Copyright 2026 Ian Jones. All Rights Reserved.
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

package com.eqixiac.equinix.fabric.beta;

import com.eqixiac.equinix.Fabric;
import com.eqixiac.equinix.core.WireMockTestBase;
import com.eqixiac.equinix.fabric.beta.client.FabricBeta;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

/**
 * WireMock contract test for {@code fabric.beta().logos()}: for every operation it asserts the HTTP method,
 * path, query, headers and body the SDK sends, and that the spec's response example reads back without loss.
 * Request and response fixtures are the spec's own examples, copied verbatim (file names carry the example name).
 */
class FabricBetaLogosWireMockTest extends WireMockTestBase {

    static Fabric fabric;
    static FabricBeta beta;

    @BeforeAll
    static void setUp() {
        fabric = new Fabric(testCredentials());
        redirectToWireMock(fabric);
        fabric.authenticate();
        beta = fabric.beta();
    }

    @AfterAll
    static void tearDown() throws Exception {
        if (fabric != null) fabric.close();
    }

    @BeforeEach
    void reset() {
        resetStubs();
    }

    /** {@code POST /fabric/v4/logos} (createLogo). */
    @Test
    void createLogo() throws Exception {
        wireMock.stubFor(post(urlPathEqualTo("/fabric/v4/logos"))
                .willReturn(aResponse().withStatus(201).withHeader("Content-Type", "application/json")
                        .withBody("{\"uuid\":\"logo-1\",\"name\":\"acme\"}")));
        var result = beta.logos().create(new byte[]{(byte) 0x89, 'P', 'N', 'G'}, "acme.png", "acme", "LOGO", "Acme logo");
        wireMock.verify(1, postRequestedFor(urlPathEqualTo("/fabric/v4/logos"))
                .withHeader("Authorization", matching("Bearer .+"))
                .withHeader("Content-Type", containing("multipart/form-data; boundary="))
                .withRequestBodyPart(aMultipart().withName("logo").build())
                .withRequestBodyPart(aMultipart().withName("name").withBody(equalTo("acme")).build())
                .withRequestBodyPart(aMultipart().withName("type").withBody(equalTo("LOGO")).build())
                .withRequestBodyPart(aMultipart().withName("description").withBody(equalTo("Acme logo")).build()));
        org.junit.jupiter.api.Assertions.assertEquals("logo-1", result.getUuid());
    }

    /** {@code GET /fabric/v4/logos/{uuid}} (getLogoByUuid). */
    @Test
    void getLogoByUuid() throws Exception {
        wireMock.stubFor(get(urlPathEqualTo("/fabric/v4/logos/uuid-0001"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "multipart/mixed")
                        .withBody(new byte[]{1, 2, 3})));
        byte[] result = beta.logos().download("uuid-0001");
        wireMock.verify(1, getRequestedFor(urlPathEqualTo("/fabric/v4/logos/uuid-0001"))
                .withHeader("Authorization", matching("Bearer .+")));
        org.junit.jupiter.api.Assertions.assertArrayEquals(new byte[]{1, 2, 3}, result);
    }

    /** {@code DELETE /fabric/v4/logos/{uuid}} (deleteLogoByUuid). */
    @Test
    void deleteLogoByUuid() throws Exception {
        wireMock.stubFor(delete(urlPathEqualTo("/fabric/v4/logos/uuid-0001"))
                .willReturn(aResponse().withStatus(202).withHeader("Content-Type", "application/json")
                        .withBody("{}")));
        Object result = beta.logos().delete("uuid-0001");
        wireMock.verify(1, deleteRequestedFor(urlPathEqualTo("/fabric/v4/logos/uuid-0001"))
                .withHeader("Authorization", matching("Bearer .+"))
                .withRequestBody(absent()));
    }
}
