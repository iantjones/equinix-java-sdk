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

package com.eqixiac.equinix.core.http;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Debug request-body logging must never contain credential values. */
class RequestBodyRedactionTest {

    @Test
    void masksOAuthClientSecretInJson() {
        String body = "{\"grant_type\":\"client_credentials\",\"client_id\":\"abc\",\"client_secret\":\"s3cr\\\"et\"}";
        String logged = EquinixHttpClient.redactSecrets(body);
        assertFalse(logged.contains("s3cr"), logged);
        assertTrue(logged.contains("\"client_id\":\"abc\""), "non-secret fields stay readable: " + logged);
        assertTrue(logged.contains("\"client_secret\":\"***\""), logged);
    }

    @Test
    void masksFormEncodedTokens() {
        String logged = EquinixHttpClient.redactSecrets(
                "grant_type=urn%3Aietf&subject_token=eyJhbGciOi.payload.sig&scope=accesspolicy");
        assertEquals("grant_type=urn%3Aietf&subject_token=***&scope=accesspolicy", logged);
    }

    @Test
    void leavesOrdinaryBodiesAlone() {
        String body = "{\"name\":\"conn\",\"bandwidth\":100}";
        assertEquals(body, EquinixHttpClient.redactSecrets(body));
    }
}
