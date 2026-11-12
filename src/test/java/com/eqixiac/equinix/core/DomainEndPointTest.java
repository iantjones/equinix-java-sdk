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

package com.eqixiac.equinix.core;

import com.eqixiac.equinix.Equinix;
import com.eqixiac.equinix.IAM;
import com.eqixiac.equinix.STS;
import com.eqixiac.equinix.core.auth.BasicEquinixCredentials;
import com.eqixiac.equinix.core.client.EquinixClient;
import com.eqixiac.equinix.core.exception.EquinixClientException;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Per-functional-area host routing. IAM (Access v1) and STS v1 are served by their own hosts;
 * {@code api.equinix.com} answers 404 for both (probed 2026-11-11), while the OAuth login and every
 * other domain stay on the gateway.
 */
class DomainEndPointTest {

    private static BasicEquinixCredentials creds() {
        return new BasicEquinixCredentials("id", "secret");
    }

    @Test
    void iamAndStsUseTheirSpecHostsWhileOtherAreasUseTheGateway() throws Exception {
        try (IAM iam = new IAM(creds()); STS sts = new STS(creds())) {
            assertEquals(URI.create("https://access.eqix.equinix.com"),
                    iam.getEquinixClient().getEndPointFor("IAM"));
            assertEquals(URI.create("https://sts.eqix.equinix.com"),
                    sts.getEquinixClient().getEndPointFor("STS"));
            assertEquals(URI.create("https://api.equinix.com"),
                    sts.getEquinixClient().getEndPointFor("Authentication"));
        }
    }

    @Test
    void sharedSessionRoutesEachDomainToItsOwnHost() throws Exception {
        try (Equinix session = new Equinix(creds())) {
            session.iam();
            session.sts();
            EquinixClient core = session.iam().getEquinixClient();
            assertEquals(URI.create("https://access.eqix.equinix.com"), core.getEndPointFor("IAM"));
            assertEquals(URI.create("https://sts.eqix.equinix.com"), core.getEndPointFor("STS"));
            assertEquals(URI.create("https://api.equinix.com"), core.getEndPointFor("Fabric"));
        }
    }

    @Test
    void clientWideOverrideWinsOverCatalogueHost() throws Exception {
        try (IAM iam = new IAM(creds())) {
            iam.getEquinixClient().setEndPoint("http://localhost:9999");
            assertEquals(URI.create("http://localhost:9999"), iam.getEquinixClient().getEndPointFor("IAM"));
        }
    }

    @Test
    void explicitDomainOverrideWinsOverEverything() throws Exception {
        try (IAM iam = new IAM(creds())) {
            EquinixClient core = iam.getEquinixClient();
            core.setEndPoint("http://localhost:9999");
            core.setDomainEndPoint("IAM", "https://access.example.test");
            assertEquals(URI.create("https://access.example.test"), core.getEndPointFor("IAM"));
            core.setDomainEndPoint("IAM", null);
            assertEquals(URI.create("http://localhost:9999"), core.getEndPointFor("IAM"));
        }
    }

    @Test
    void credentialLessStsRefusesAuthenticatedOperationsWithAClearError() throws Exception {
        try (STS sts = STS.withoutCredentials()) {
            EquinixClientException ex = assertThrows(EquinixClientException.class,
                    () -> sts.oidcProviders().list("project-1"));
            assertEquals(true, String.valueOf(ex.getMessage()).contains("without Equinix credentials")
                    || (ex.getCause() != null && String.valueOf(ex.getCause().getMessage())
                            .contains("without Equinix credentials")), ex.toString());
        }
    }
}
