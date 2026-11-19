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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * The OpenCapacity endpoint catalog, read from {@code json/apiParams_OpenCapacity.json}. The file
 * has the same format as the catalogs of the Equinix clients: a functional area with a URI format
 * and a version, resource groups with a root URI, and named service endpoints with an HTTP method
 * and a request URI.
 *
 * <p>The catalog declares no host name. No provider implements OpenCapacity, so there is no host
 * to declare: the caller gives the provider's host to {@link OpenCapacity.Builder#endpoint}.
 *
 * <p>Not part of the public API.
 */
final class Endpoints {

    /** The classpath resource that holds the catalog. */
    static final String RESOURCE = "json/apiParams_OpenCapacity.json";

    /** One endpoint of the catalog. */
    record Operation(String name, String method, String uriTemplate, boolean authenticated) {

        /** Returns the path with each {@code {$name}} placeholder replaced by its encoded value. */
        String path(Map<String, String> variables) {
            String path = uriTemplate;
            for (Map.Entry<String, String> variable : variables.entrySet()) {
                path = path.replace("{$" + variable.getKey() + "}", Transport.encode(variable.getValue()));
            }
            if (path.contains("{$")) {
                throw new IllegalStateException("Endpoint " + name + " needs a value for " + path);
            }
            return path;
        }
    }

    private static final Map<String, Operation> OPERATIONS = new HashMap<>();
    private static final String BASE_PATH;

    static {
        try (InputStream in = Endpoints.class.getClassLoader().getResourceAsStream(RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException("The endpoint catalog " + RESOURCE + " is not on the classpath.");
            }
            JsonNode area = new ObjectMapper().readTree(in).path("functionalAreas").path("OpenCapacity");
            String format = area.path("uriFormat").asText()
                    .replace("{$version}", Integer.toString(area.path("defaultVersion").asInt()));
            BASE_PATH = format.substring(0, format.indexOf("/{$rootUri}"));
            for (Iterator<Map.Entry<String, JsonNode>> groups = area.fields(); groups.hasNext();) {
                JsonNode group = groups.next().getValue();
                JsonNode endpoints = group.path("serviceEndpoints");
                boolean authenticated = !group.path("unauthenticated").asBoolean(false);
                for (Iterator<Map.Entry<String, JsonNode>> it = endpoints.fields(); it.hasNext();) {
                    Map.Entry<String, JsonNode> endpoint = it.next();
                    String uri = format.replace("{$rootUri}", group.path("rootUri").asText())
                            .replace("{$requestUri}", endpoint.getValue().path("requestUri").asText(""));
                    if (uri.endsWith("/")) {
                        uri = uri.substring(0, uri.length() - 1);
                    }
                    OPERATIONS.put(endpoint.getKey(), new Operation(endpoint.getKey(),
                            endpoint.getValue().path("httpMethod").asText(), uri, authenticated));
                }
            }
        } catch (IOException unreadable) {
            throw new IllegalStateException("The endpoint catalog " + RESOURCE + " could not be read.", unreadable);
        }
    }

    private Endpoints() {
    }

    /**
     * Returns an endpoint by its name in the catalog.
     *
     * @throws IllegalStateException if the catalog has no endpoint with that name
     */
    static Operation operation(String name) {
        Operation operation = OPERATIONS.get(name);
        if (operation == null) {
            throw new IllegalStateException("The endpoint catalog " + RESOURCE + " has no endpoint " + name + ".");
        }
        return operation;
    }

    /** Returns the path that every endpoint starts with, for example {@code /capacity/v1}. */
    static String basePath() {
        return BASE_PATH;
    }

    /** Returns every endpoint of the catalog by name. */
    static Map<String, Operation> operations() {
        return Map.copyOf(OPERATIONS);
    }
}
