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

package com.eqixiac.opencapacity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;

/**
 * The endpoint catalog {@code json/apiParams_OpenCapacity.json} against the bundled specification:
 * every operation of the specification has one endpoint in the catalog with the same method, path,
 * and authentication, and the catalog has no endpoint that the specification lacks.
 */
class EndpointCatalogTest {

    @Test
    void catalogDeclaresExactlyTheOperationsOfTheSpecification() throws IOException {
        JsonNode spec;
        try (InputStream in = getClass().getResourceAsStream("/opencapacity/opencapacity-1.0.json")) {
            spec = Transport.MAPPER.readTree(in);
        }
        Map<String, String> specified = new TreeMap<>();
        for (Iterator<Map.Entry<String, JsonNode>> paths = spec.path("paths").fields(); paths.hasNext();) {
            Map.Entry<String, JsonNode> path = paths.next();
            for (Iterator<Map.Entry<String, JsonNode>> methods = path.getValue().fields(); methods.hasNext();) {
                Map.Entry<String, JsonNode> method = methods.next();
                if (method.getValue().has("operationId")) {
                    boolean open = method.getValue().has("security") && method.getValue().path("security").isEmpty();
                    specified.put(method.getKey().toUpperCase() + " " + path.getKey().replace("{", "{$"),
                            open ? "open" : "token");
                }
            }
        }
        Map<String, String> declared = new TreeMap<>();
        for (Endpoints.Operation operation : Endpoints.operations().values()) {
            assertTrue(operation.uriTemplate().startsWith(Endpoints.basePath() + "/"), operation.name());
            declared.put(operation.method() + " " + operation.uriTemplate().substring(Endpoints.basePath().length()),
                    operation.authenticated() ? "token" : "open");
        }
        assertEquals(specified, declared);
        assertEquals(15, declared.size());
        assertEquals("/capacity/v1", Endpoints.basePath());
    }

    @Test
    void pathVariablesAreEncodedAndRequired() {
        Endpoints.Operation withdraw = Endpoints.operation("WithdrawSignal");
        assertEquals("POST", withdraw.method());
        assertEquals("/capacity/v1/signals/sig%2F1/withdraw", withdraw.path(Map.of("signalId", "sig/1")));
        assertThrows(IllegalStateException.class, () -> withdraw.path(Map.of()));
        assertThrows(IllegalStateException.class, () -> Endpoints.operation("OrderCapacity"));
        assertFalse(Endpoints.operation("GetCapabilities").authenticated());
    }

    @Test
    void endpointAcceptsTheHostWithOrWithoutTheCatalogBasePath() {
        OpenCapacity.builder().endpoint("https://provider.example").build();
        OpenCapacity.builder().endpoint("https://provider.example/capacity/v1/").build();
    }
}
