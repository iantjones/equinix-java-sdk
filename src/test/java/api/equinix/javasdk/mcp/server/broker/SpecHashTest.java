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

package api.equinix.javasdk.mcp.server.broker;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("SpecHash — canonical form and the SHA-256 binding")
class SpecHashTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static String canonical(String json) throws Exception {
        return SpecHash.canonicalize(MAPPER.readTree(json), MAPPER);
    }

    @Test
    @DisplayName("key order and whitespace never change the canonical form or the hash")
    void keyOrderInsensitive() throws Exception {
        String a = canonical("{\"name\":\"X\",\"bandwidth_mbps\":1000,\"a_side\":{\"port_uuid\":\"p\",\"link_protocol\":{\"vlan_tag\":5,\"type\":\"dot1q\"}}}");
        String b = canonical("{ \"a_side\" : { \"link_protocol\" : { \"type\" : \"dot1q\", \"vlan_tag\" : 5 }, \"port_uuid\" : \"p\" }, \"bandwidth_mbps\" : 1000, \"name\" : \"X\" }");
        assertEquals(a, b, "same spec, different key order/whitespace, one canonical form");
        assertEquals(SpecHash.sha256Hex(a), SpecHash.sha256Hex(b));
    }

    @Test
    @DisplayName("any value change changes the hash — the token binding is exact")
    void valueChangesChangeHash() throws Exception {
        String base = canonical("{\"name\":\"X\",\"bandwidth_mbps\":1000}");
        String bumped = canonical("{\"name\":\"X\",\"bandwidth_mbps\":2000}");
        String renamed = canonical("{\"name\":\"Y\",\"bandwidth_mbps\":1000}");
        assertNotEquals(SpecHash.sha256Hex(base), SpecHash.sha256Hex(bumped));
        assertNotEquals(SpecHash.sha256Hex(base), SpecHash.sha256Hex(renamed));
    }

    @Test
    @DisplayName("array order is significant, matching the API's semantics")
    void arrayOrderSignificant() throws Exception {
        String ab = canonical("{\"notification_emails\":[\"a@x.io\",\"b@x.io\"]}");
        String ba = canonical("{\"notification_emails\":[\"b@x.io\",\"a@x.io\"]}");
        assertNotEquals(SpecHash.sha256Hex(ab), SpecHash.sha256Hex(ba));
    }

    @Test
    @DisplayName("the hash is 64 lower-case hex chars")
    void hashShape() throws Exception {
        String hash = SpecHash.sha256Hex(canonical("{\"name\":\"X\"}"));
        assertTrue(hash.matches("^[0-9a-f]{64}$"), hash);
    }
}
