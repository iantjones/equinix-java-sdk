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

import com.eqixiac.equinix.core.auth.BasicEquinixCredentials;
import com.eqixiac.equinix.core.client.EquinixClient;
import com.eqixiac.equinix.core.exception.EquinixClientException;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The merged apiParams catalogue on the transport client: registration failures must name the
 * offending resource file (previously a disabled-by-default {@code assert} let a missing file
 * NPE into a generic message), and registration must be copy-on-write so request threads reading
 * the published tree never observe a half-merged catalogue.
 */
class ApiParamsRegistryTest {

    private static EquinixClient newClient() {
        return new EquinixClient(new BasicEquinixCredentials("id", "secret"), false);
    }

    @Test
    void missingApiParamsResourceFailsFastNamingTheFile() {
        try (EquinixClient client = newClient()) {
            EquinixClientException e = assertThrows(EquinixClientException.class,
                    () -> client.appendApiParams("json/apiParams_DoesNotExist.json"));
            assertTrue(e.getMessage().contains("json/apiParams_DoesNotExist.json"),
                    "message must name the missing resource: " + e.getMessage());
        } catch (java.io.IOException ignored) {
            // close() only
        }
    }

    @Test
    void appendMergesWithoutClobberingExistingAreas() {
        try (EquinixClient client = newClient()) {
            client.appendApiParams("json/apiParams_Fabric.json");
            JsonNode afterFabric = client.getClientResourceFile();
            assertTrue(afterFabric.path("functionalAreas").has("Fabric"));

            client.appendApiParams("json/apiParams_InternetAccess.json");
            JsonNode afterBoth = client.getClientResourceFile();

            assertTrue(afterBoth.path("functionalAreas").has("Fabric"), "earlier area survives later merges");
            assertTrue(afterBoth.path("functionalAreas").has("InternetAccess"));
        } catch (java.io.IOException ignored) {
            // close() only
        }
    }

    @Test
    void appendIsCopyOnWrite_publishingANewTree() {
        try (EquinixClient client = newClient()) {
            JsonNode before = client.getClientResourceFile();

            client.appendApiParams("json/apiParams_Fabric.json");
            JsonNode after = client.getClientResourceFile();

            assertNotSame(before, after, "merge must publish a new tree, not mutate the one being read");
            assertFalse(before.path("functionalAreas").has("Fabric"),
                    "a snapshot taken before the merge must be unaffected");
        } catch (java.io.IOException ignored) {
            // close() only
        }
    }
}
