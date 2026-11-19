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

import com.eqixiac.opencapacity.model.Problem;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;

/** HTTP, JSON, and token handling for one provider. Not part of the public API. */
final class Transport {

    static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

    /** A response body with the headers the client reads. */
    record Reply(int status, HttpHeaders headers, String body) {
        String etag() {
            return headers.firstValue("ETag").orElse(null);
        }
    }

    private final URI base;
    private final HttpClient http;
    private final Duration timeout;
    private TokenSource tokens;

    Transport(URI base, Duration timeout) {
        this.base = base;
        this.timeout = timeout;
        this.http = HttpClient.newBuilder().connectTimeout(timeout).build();
    }

    void tokens(TokenSource source) {
        this.tokens = source;
    }

    URI base() {
        return base;
    }

    static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    static JavaType type(Class<?> raw, Class<?>... parameters) {
        return parameters.length == 0
                ? MAPPER.constructType(raw)
                : MAPPER.getTypeFactory().constructParametricType(raw, parameters);
    }

    <T> T read(Reply reply, JavaType type) {
        try {
            return MAPPER.readValue(reply.body(), type);
        } catch (JsonProcessingException unreadable) {
            throw new OpenCapacityException(
                    "The provider returned a body that is not valid for this operation: "
                            + unreadable.getOriginalMessage(), unreadable);
        }
    }

    Reply send(String method, String path, Object body, Map<String, String> headers, boolean authenticated) {
        Reply reply = exchange(method, path, body, headers, authenticated);
        if (reply.status() == 401 && authenticated && tokens != null) {
            tokens.invalidate();
            reply = exchange(method, path, body, headers, true);
        }
        if (reply.status() >= 400) {
            throw failure(method, path, reply);
        }
        return reply;
    }

    private HttpRequest.Builder request(String method, String path, Object body, Map<String, String> headers,
            boolean authenticated) {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(base + path)).timeout(timeout);
        if (authenticated) {
            if (tokens == null) {
                throw new IllegalStateException(
                        "No credentials are configured. Call credentials() or tokenSource() on the builder.");
            }
            request.header("Authorization", "Bearer " + tokens.token());
        }
        headers.forEach(request::header);
        if (body == null) {
            request.method(method, HttpRequest.BodyPublishers.noBody());
        } else {
            try {
                request.header("Content-Type", "application/json");
                request.method(method, HttpRequest.BodyPublishers.ofString(MAPPER.writeValueAsString(body)));
            } catch (JsonProcessingException unwritable) {
                throw new OpenCapacityException("The request body could not be written as JSON", unwritable);
            }
        }
        return request;
    }

    private Reply exchange(String method, String path, Object body, Map<String, String> headers,
            boolean authenticated) {
        HttpRequest.Builder request = request(method, path, body, headers, authenticated);
        if (!headers.containsKey("Accept")) {
            request.header("Accept", "application/json");
        }
        try {
            HttpResponse<String> response = http.send(request.build(), HttpResponse.BodyHandlers.ofString());
            return new Reply(response.statusCode(), response.headers(), response.body());
        } catch (IOException failed) {
            throw new OpenCapacityException(method + " " + path + " failed: " + failed.getMessage(), failed);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new OpenCapacityException(method + " " + path + " was interrupted", interrupted);
        }
    }

    /** Opens a response as a stream of lines, for server-sent events. */
    InputStream stream(String path, Map<String, String> headers) {
        HttpRequest.Builder request = request("GET", path, null, headers, true).timeout(Duration.ofDays(1));
        try {
            HttpResponse<InputStream> response = http.send(request.build(), HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() >= 400) {
                String text = new String(response.body().readAllBytes(), StandardCharsets.UTF_8);
                throw failure("GET", path, new Reply(response.statusCode(), response.headers(), text));
            }
            return response.body();
        } catch (IOException failed) {
            throw new OpenCapacityException("GET " + path + " failed: " + failed.getMessage(), failed);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new OpenCapacityException("GET " + path + " was interrupted", interrupted);
        }
    }

    private static OpenCapacityException failure(String method, String path, Reply reply) {
        Problem problem = null;
        try {
            if (reply.body() != null && !reply.body().isBlank()) {
                problem = MAPPER.readValue(reply.body(), Problem.class);
            }
        } catch (JsonProcessingException notAProblem) {
            problem = null;
        }
        StringBuilder message = new StringBuilder(method).append(' ').append(path)
                .append(" returned ").append(reply.status());
        if (problem != null && problem.code() != null) {
            message.append(' ').append(problem.code());
        }
        if (problem != null && problem.detail() != null) {
            message.append(": ").append(problem.detail());
        }
        reply.headers().firstValue("Retry-After")
                .ifPresent(seconds -> message.append(" Retry after ").append(seconds).append(" seconds."));
        return new OpenCapacityException(message.toString(), reply.status(), problem);
    }

    /** Says whether a URI is safe to send a secret to: HTTPS, or HTTP on the local machine. */
    static boolean isSafe(URI uri) {
        if ("https".equalsIgnoreCase(uri.getScheme())) {
            return true;
        }
        String host = uri.getHost();
        return "http".equalsIgnoreCase(uri.getScheme())
                && ("localhost".equals(host) || "127.0.0.1".equals(host) || "[::1]".equals(host));
    }

    /** The OAuth 2.0 client credentials flow against one token endpoint. */
    static final class ClientCredentials implements TokenSource {
        private final Transport transport;
        private final String clientId;
        private final String clientSecret;
        private URI tokenUrl;
        private String token;
        private Instant expires = Instant.MIN;

        ClientCredentials(Transport transport, String clientId, String clientSecret, URI tokenUrl) {
            this.transport = transport;
            this.clientId = Objects.requireNonNull(clientId, "clientId");
            this.clientSecret = Objects.requireNonNull(clientSecret, "clientSecret");
            this.tokenUrl = tokenUrl;
        }

        @Override
        public synchronized String token() {
            if (token == null || !Instant.now().isBefore(expires)) {
                fetch();
            }
            return token;
        }

        @Override
        public synchronized void invalidate() {
            token = null;
        }

        private void fetch() {
            if (tokenUrl == null) {
                Transport.Reply reply = transport.send("GET", "/capabilities", null, Map.of(), false);
                JsonNode url = transport.<JsonNode>read(reply, type(JsonNode.class)).path("authentication").path("tokenUrl");
                if (!url.isTextual()) {
                    throw new OpenCapacityException(
                            "The provider's capabilities document has no authentication.tokenUrl. "
                                    + "Set tokenUrl() on the builder.", 0, null);
                }
                tokenUrl = URI.create(url.asText());
            }
            if (!isSafe(tokenUrl)) {
                throw new OpenCapacityException(
                        "Refusing to send credentials to " + tokenUrl + " because it is not HTTPS.", 0, null);
            }
            String basic = Base64.getEncoder()
                    .encodeToString((clientId + ":" + clientSecret).getBytes(StandardCharsets.UTF_8));
            HttpRequest request = HttpRequest.newBuilder(tokenUrl)
                    .timeout(transport.timeout)
                    .header("Authorization", "Basic " + basic)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString("grant_type=client_credentials"))
                    .build();
            try {
                HttpResponse<String> response = transport.http.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200) {
                    throw new OpenCapacityException(
                            "The token endpoint returned " + response.statusCode()
                                    + ". Check the client ID and secret for this provider.",
                            response.statusCode(), null);
                }
                JsonNode json = MAPPER.readTree(response.body());
                if (!json.path("access_token").isTextual()) {
                    throw new OpenCapacityException("The token endpoint returned no access_token.", 200, null);
                }
                token = json.path("access_token").asText();
                long seconds = json.path("expires_in").asLong(300);
                expires = Instant.now().plusSeconds(Math.max(1, seconds - 30));
            } catch (IOException failed) {
                throw new OpenCapacityException("The token request failed: " + failed.getMessage(), failed);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw new OpenCapacityException("The token request was interrupted", interrupted);
            }
        }
    }
}
