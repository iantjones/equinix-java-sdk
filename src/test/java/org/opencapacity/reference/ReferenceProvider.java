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

package org.opencapacity.reference;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * An in-memory OpenCapacity provider for tests and demonstrations. It serves the whole API on a
 * local port and answers from the synthetic inventory in {@link Inventory}.
 *
 * <p>Every name, code, and number it returns is invented. It holds no data of any real provider.
 * It keeps nothing on disk and has no use in production.
 *
 * <p>The class depends on the JDK and Jackson only, and on no client library, so that a client can
 * be tested against an implementation that was written separately from it.
 */
public final class ReferenceProvider implements AutoCloseable {

    /** How much a provider discloses in an answer. */
    public enum Disclosure {
        /** A grade per location and one entry per desired feature. */
        GRADE_ONLY,
        /** Adds timing, a density range, a cabinet band, and interconnection presence. */
        BANDED,
        /** Adds a power band, latency bands, and reasons. */
        FULL
    }

    /** The client ID the provider accepts at its token endpoint. */
    public static final String CLIENT_ID = "example-client";
    /** The client secret the provider accepts at its token endpoint. */
    public static final String CLIENT_SECRET = "example-secret";

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String PREFIX = "/capacity/v1";
    private static final double REVIEW_THRESHOLD_KW = 5000;
    private static final Set<String> FEATURES = Set.of(
            "LIQUID_COOLING", "DIRECT_TO_CHIP_COOLING", "REAR_DOOR_HEAT_EXCHANGER", "PRIVATE_CAGE", "PRIVATE_SUITE");

    private final HttpServer server;
    private final HttpClient webhooks = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final Inventory inventory = Inventory.example();
    private final Set<String> tokens = ConcurrentHashMap.newKeySet();
    private final Map<String, Stored> signals = new LinkedHashMap<>();
    private final Map<String, Stored> subscriptions = new LinkedHashMap<>();
    private final Map<String, List<ObjectNode>> events = new HashMap<>();
    private final Map<String, ObjectNode> lastAnswers = new HashMap<>();
    private final Map<String, String> secrets = new HashMap<>();
    private final Map<String, String> idempotency = new HashMap<>();
    private Clock clock = Clock.systemUTC();
    private Disclosure disclosure = Disclosure.BANDED;
    private boolean areaLocations = true;
    private int queryQuota = Integer.MAX_VALUE;
    private int queries;
    private int counter;
    private long streamHoldMillis = 300;

    private static final class Stored {
        final ObjectNode body;
        int version = 1;

        Stored(ObjectNode body) {
            this.body = body;
        }
    }

    private static final class Failure extends RuntimeException {
        private static final long serialVersionUID = 1L;
        final int status;
        final String code;
        final String pointer;
        final String retryAfter;

        Failure(int status, String code, String detail, String pointer, String retryAfter) {
            super(detail);
            this.status = status;
            this.code = code;
            this.pointer = pointer;
            this.retryAfter = retryAfter;
        }

        Failure(int status, String code, String detail) {
            this(status, code, detail, null, null);
        }
    }

    private ReferenceProvider() throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/", this::handle);
        server.setExecutor(Executors.newCachedThreadPool(runnable -> {
            Thread thread = new Thread(runnable, "opencapacity-reference");
            thread.setDaemon(true);
            return thread;
        }));
        server.start();
    }

    /**
     * Starts a provider on a free local port.
     *
     * @return the running provider
     * @throws IOException if no port can be opened
     */
    public static ReferenceProvider start() throws IOException {
        return new ReferenceProvider();
    }

    /**
     * Returns the API base URL to give to a client.
     *
     * @return the base URL, including the version path
     */
    public String endpoint() {
        return "http://127.0.0.1:" + server.getAddress().getPort() + PREFIX;
    }

    /**
     * Returns the token endpoint.
     *
     * @return the OAuth 2.0 token URL
     */
    public String tokenUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/oauth2/token";
    }

    /**
     * Returns the synthetic inventory, so that a test can change it.
     *
     * @return the inventory
     */
    public Inventory inventory() {
        return inventory;
    }

    /**
     * Sets how much the provider discloses.
     *
     * @param level the disclosure profile
     * @return this provider
     */
    public synchronized ReferenceProvider disclosure(Disclosure level) {
        this.disclosure = level;
        return this;
    }

    /**
     * Sets whether the provider accepts a point and a radius as a location.
     *
     * @param accepted {@code true} to accept areas
     * @return this provider
     */
    public synchronized ReferenceProvider areaLocations(boolean accepted) {
        this.areaLocations = accepted;
        return this;
    }

    /**
     * Sets how many queries the provider answers before it returns 429.
     *
     * @param quota the number of queries
     * @return this provider
     */
    public synchronized ReferenceProvider queryQuota(int quota) {
        this.queryQuota = quota;
        this.queries = 0;
        return this;
    }

    /**
     * Sets the clock the provider reads the date and time from.
     *
     * @param source the clock
     * @return this provider
     */
    public synchronized ReferenceProvider clock(Clock source) {
        this.clock = source;
        return this;
    }

    /**
     * Sets how long an event stream stays open after the last event.
     *
     * @param millis the time in milliseconds
     * @return this provider
     */
    public synchronized ReferenceProvider streamHoldMillis(long millis) {
        this.streamHoldMillis = millis;
        return this;
    }

    /** Makes every token the provider issued invalid, so that the next request gets a 401. */
    public void revokeTokens() {
        tokens.clear();
    }

    @Override
    public void close() {
        server.stop(0);
    }

    // ------------------------------------------------------------------ provider-side actions

    /**
     * Acts as the provider's planner: takes a signal into planning and attaches the current answer.
     *
     * @param signalId the signal
     */
    public synchronized void acknowledge(String signalId) {
        Stored signal = signals.get(signalId);
        signal.body.put("state", "ACKNOWLEDGED");
        signal.body.set("answer", evaluate(signal.body.path("requirement"), null));
        touch(signal);
        signalChanged(signalId);
    }

    /**
     * Acts as the provider: closes a signal.
     *
     * @param signalId the signal
     * @param reason FULFILLED, DECLINED, DUPLICATE, or OTHER
     */
    public synchronized void close(String signalId, String reason) {
        Stored signal = signals.get(signalId);
        signal.body.put("state", "CLOSED").put("closeReason", reason);
        touch(signal);
        signalChanged(signalId);
    }

    /**
     * Evaluates every active subscription again and sends an event where the answer changed. Call
     * it after a change to the {@link #inventory()}.
     */
    public synchronized void reevaluate() {
        for (Map.Entry<String, Stored> entry : subscriptions.entrySet()) {
            ObjectNode body = entry.getValue().body;
            if (!"ACTIVE".equals(body.path("state").asText())) {
                continue;
            }
            JsonNode requirement = requirementOf(body);
            if (requirement == null) {
                continue;
            }
            ObjectNode now = evaluate(requirement, null);
            ObjectNode before = lastAnswers.get(entry.getKey());
            String reason = before == null ? "INITIAL" : change(before, now);
            if (reason == null || ("DETAIL_CHANGE".equals(reason) && !"ANY_CHANGE".equals(body.path("notifyOn").asText()))) {
                continue;
            }
            answerEvent(entry.getKey(), reason, now);
        }
    }

    private JsonNode requirementOf(ObjectNode subscription) {
        if (subscription.has("requirement")) {
            return subscription.get("requirement");
        }
        Stored signal = signals.get(subscription.path("signalId").asText());
        if (signal == null) {
            return null;
        }
        String state = signal.body.path("state").asText();
        return "RECEIVED".equals(state) || "ACKNOWLEDGED".equals(state) ? signal.body.get("requirement") : null;
    }

    private static String change(ObjectNode before, ObjectNode now) {
        ArrayNode a = (ArrayNode) before.get("results");
        ArrayNode b = (ArrayNode) now.get("results");
        if (a.size() != b.size()) {
            return "GRADE_CHANGE";
        }
        boolean detail = false;
        for (int i = 0; i < a.size(); i++) {
            if (!a.get(i).path("grade").equals(b.get(i).path("grade"))) {
                return "GRADE_CHANGE";
            }
            detail |= !a.get(i).equals(b.get(i));
        }
        return detail ? "DETAIL_CHANGE" : null;
    }

    private void signalChanged(String signalId) {
        Stored signal = signals.get(signalId);
        for (Map.Entry<String, Stored> entry : subscriptions.entrySet()) {
            ObjectNode body = entry.getValue().body;
            if (!signalId.equals(body.path("signalId").asText(null))) {
                continue;
            }
            if (signal == null) {
                body.put("state", "EXPIRED");
                touch(entry.getValue());
                ObjectNode data = JSON.createObjectNode().put("subscriptionState", "EXPIRED");
                emit(entry.getKey(), "subscription.updated", data);
            } else {
                ObjectNode data = JSON.createObjectNode().put("signalId", signalId)
                        .put("signalState", signal.body.path("state").asText());
                emit(entry.getKey(), "signal.updated", data);
            }
        }
    }

    // ------------------------------------------------------------------ HTTP

    private void handle(HttpExchange exchange) throws IOException {
        try {
            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();
            if ("/oauth2/token".equals(path)) {
                token(exchange);
                return;
            }
            if (!path.startsWith(PREFIX)) {
                throw new Failure(404, "NOT_FOUND", "No such path.");
            }
            String rest = path.substring(PREFIX.length());
            if ("/capabilities".equals(rest) && "GET".equals(method)) {
                send(exchange, 200, capabilities(), null);
                return;
            }
            String authorization = exchange.getRequestHeaders().getFirst("Authorization");
            if (authorization == null || !authorization.startsWith("Bearer ")
                    || !tokens.contains(authorization.substring(7))) {
                throw new Failure(401, "UNAUTHENTICATED", "Send a valid bearer token.");
            }
            route(exchange, method, rest.split("/"), query(exchange.getRequestURI()));
        } catch (Failure failure) {
            ObjectNode problem = JSON.createObjectNode()
                    .put("type", "https://opencapacity.example/problems/" + failure.code.toLowerCase().replace('_', '-'))
                    .put("title", failure.code.replace('_', ' ').toLowerCase())
                    .put("status", failure.status).put("code", failure.code).put("detail", failure.getMessage());
            if (failure.pointer != null) {
                problem.putArray("errors").addObject().put("pointer", failure.pointer).put("detail", failure.getMessage());
            }
            if (failure.retryAfter != null) {
                exchange.getResponseHeaders().set("Retry-After", failure.retryAfter);
            }
            exchange.getResponseHeaders().set("Content-Type", "application/problem+json");
            byte[] bytes = JSON.writeValueAsBytes(problem);
            exchange.sendResponseHeaders(failure.status, bytes.length);
            exchange.getResponseBody().write(bytes);
        } catch (RuntimeException unexpected) {
            byte[] bytes = ("{\"type\":\"about:blank\",\"title\":\"internal error\",\"status\":500,"
                    + "\"code\":\"INTERNAL_ERROR\"}").getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/problem+json");
            exchange.sendResponseHeaders(500, bytes.length);
            exchange.getResponseBody().write(bytes);
        } finally {
            exchange.close();
        }
    }

    private void token(HttpExchange exchange) throws IOException {
        String expected = "Basic " + java.util.Base64.getEncoder()
                .encodeToString((CLIENT_ID + ":" + CLIENT_SECRET).getBytes(StandardCharsets.UTF_8));
        if (!expected.equals(exchange.getRequestHeaders().getFirst("Authorization"))) {
            throw new Failure(401, "UNAUTHENTICATED", "Unknown client.");
        }
        String token = UUID.randomUUID().toString();
        tokens.add(token);
        send(exchange, 200, JSON.createObjectNode().put("access_token", token)
                .put("token_type", "Bearer").put("expires_in", 3600), null);
    }

    private static Map<String, String> query(URI uri) {
        Map<String, String> values = new HashMap<>();
        if (uri.getRawQuery() != null) {
            for (String pair : uri.getRawQuery().split("&")) {
                int equals = pair.indexOf('=');
                if (equals > 0) {
                    values.put(pair.substring(0, equals),
                            java.net.URLDecoder.decode(pair.substring(equals + 1), StandardCharsets.UTF_8));
                }
            }
        }
        return values;
    }

    private static void send(HttpExchange exchange, int status, JsonNode body, String etag) throws IOException {
        if (etag != null) {
            exchange.getResponseHeaders().set("ETag", etag);
        }
        if (body == null) {
            exchange.sendResponseHeaders(status, -1);
            return;
        }
        byte[] bytes = JSON.writeValueAsBytes(body);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
    }

    private static ObjectNode body(HttpExchange exchange) throws IOException {
        JsonNode node;
        try {
            node = JSON.readTree(exchange.getRequestBody());
        } catch (IOException malformed) {
            throw new Failure(422, "INVALID_REQUEST", "The body is not JSON.");
        }
        if (node == null || !node.isObject()) {
            throw new Failure(422, "INVALID_REQUEST", "The body must be a JSON object.");
        }
        return (ObjectNode) node;
    }

    private void route(HttpExchange exchange, String method, String[] parts, Map<String, String> query)
            throws IOException {
        String resource = parts.length > 1 ? parts[1] : "";
        String id = parts.length > 2 ? parts[2] : null;
        String action = parts.length > 3 ? parts[3] : null;
        String key = method + " " + resource + (id == null ? "" : "/{id}") + (action == null ? "" : "/" + action);
        switch (key) {
            case "GET locations" -> send(exchange, 200, locations(query), null);
            case "POST queries" -> send(exchange, 200, runQuery(body(exchange)), null);
            case "POST signals" -> createSignal(exchange);
            case "GET signals" -> send(exchange, 200, page(signals, query, "state"), null);
            case "GET signals/{id}" -> reply(exchange, 200, stored(signals, id), id);
            case "PUT signals/{id}" -> replaceSignal(exchange, id);
            case "DELETE signals/{id}" -> deleteSignal(exchange, id);
            case "POST signals/{id}/withdraw" -> withdraw(exchange, id);
            case "POST subscriptions" -> createSubscription(exchange);
            case "GET subscriptions" -> send(exchange, 200, page(subscriptions, query, null), null);
            case "GET subscriptions/{id}" -> reply(exchange, 200, stored(subscriptions, id), id);
            case "PUT subscriptions/{id}" -> replaceSubscription(exchange, id);
            case "DELETE subscriptions/{id}" -> deleteSubscription(exchange, id);
            case "GET subscriptions/{id}/events" -> events(exchange, id, query);
            default -> throw new Failure(404, "NOT_FOUND", "No such operation: " + key);
        }
    }

    private synchronized Stored stored(Map<String, Stored> map, String id) {
        Stored found = map.get(id);
        if (found == null) {
            throw new Failure(404, "NOT_FOUND", "Nothing with this identifier exists.");
        }
        return found;
    }

    private static String etag(String id, Stored stored) {
        return "\"" + id + "-" + stored.version + "\"";
    }

    private synchronized void reply(HttpExchange exchange, int status, Stored stored, String id) throws IOException {
        ObjectNode copy = stored.body.deepCopy();
        if (copy.has("delivery")) {
            ((ObjectNode) copy.get("delivery")).remove("signingSecret");
        }
        send(exchange, status, copy, etag(id, stored));
    }

    private void touch(Stored stored) {
        stored.version++;
        stored.body.put("updatedAt", now());
    }

    private String now() {
        return Instant.now(clock).truncatedTo(ChronoUnit.SECONDS).toString();
    }

    private void checkMatch(HttpExchange exchange, String id, Stored stored) {
        String match = exchange.getRequestHeaders().getFirst("If-Match");
        if (match == null || !match.equals(etag(id, stored))) {
            throw new Failure(412, "PRECONDITION_FAILED",
                    "The If-Match value does not match the current version. Read it again and repeat.");
        }
    }

    private synchronized ObjectNode page(Map<String, Stored> map, Map<String, String> query, String filter) {
        int limit = Math.min(200, Integer.parseInt(query.getOrDefault("limit", "50")));
        int offset = Integer.parseInt(query.getOrDefault("cursor", "0"));
        List<ObjectNode> matching = new ArrayList<>();
        for (Stored stored : map.values()) {
            if (filter == null || !query.containsKey(filter)
                    || query.get(filter).equals(stored.body.path(filter).asText())) {
                ObjectNode copy = stored.body.deepCopy();
                if (copy.has("delivery")) {
                    ((ObjectNode) copy.get("delivery")).remove("signingSecret");
                }
                matching.add(copy);
            }
        }
        ObjectNode result = JSON.createObjectNode();
        ArrayNode data = result.putArray("data");
        for (int i = offset; i < Math.min(matching.size(), offset + limit); i++) {
            data.add(matching.get(i));
        }
        ObjectNode pagination = result.putObject("pagination").put("limit", limit);
        if (offset + limit < matching.size()) {
            pagination.put("next", Integer.toString(offset + limit));
        }
        return result;
    }

    // ------------------------------------------------------------------ discovery

    private synchronized ObjectNode capabilities() {
        ObjectNode root = JSON.createObjectNode().put("specVersion", "1.0.0");
        root.putObject("provider").put("name", "Example Data Centers (reference provider, synthetic data)")
                .put("url", "https://provider.example");
        root.putArray("conformance").add("CORE").add("SIGNALS").add("SUBSCRIPTIONS");
        root.putObject("authentication").put("tokenUrl", tokenUrl());
        root.putArray("locationLevels").add("REGION").add("COUNTRY").add("METRO").add("CAMPUS").add("FACILITY");
        root.put("areaLocations", areaLocations).put("finestAnswerLevel", "METRO");
        ArrayNode features = root.putArray("features");
        FEATURES.stream().sorted().forEach(features::add);
        root.put("defaultPowerFactor", Inventory.POWER_FACTOR);
        root.putArray("deliveryTypes").add("WEBHOOK").add("PULL");
        String banded = disclosure == Disclosure.GRADE_ONLY ? "NEVER" : "ALWAYS";
        String full = disclosure == Disclosure.FULL ? "ALWAYS" : "NEVER";
        root.putObject("disclosure").put("timing", banded).put("density", banded).put("cabinets", banded)
                .put("power", full).put("interconnection", banded).put("reasons", full);
        root.putObject("limits").put("maxLocationsPerRequirement", 10).put("answerValiditySeconds", 86400)
                .put("maxSignalLifetimeDays", 365).put("eventRetentionHours", 168);
        return root;
    }

    private synchronized ObjectNode locations(Map<String, String> query) {
        List<ObjectNode> all = inventory.locations(JSON, query.get("level"), query.get("within"));
        int limit = Math.min(200, Integer.parseInt(query.getOrDefault("limit", "50")));
        int offset = Integer.parseInt(query.getOrDefault("cursor", "0"));
        ObjectNode result = JSON.createObjectNode();
        ArrayNode data = result.putArray("data");
        for (int i = offset; i < Math.min(all.size(), offset + limit); i++) {
            data.add(all.get(i));
        }
        ObjectNode pagination = result.putObject("pagination").put("limit", limit);
        if (offset + limit < all.size()) {
            pagination.put("next", Integer.toString(offset + limit));
        }
        return result;
    }

    // ------------------------------------------------------------------ queries

    private synchronized ObjectNode runQuery(ObjectNode request) {
        if (++queries > queryQuota) {
            throw new Failure(429, "QUOTA_EXCEEDED", "The query quota for this client is used up.", null, "3600");
        }
        return evaluate(validate(request.path("requirement")), request.path("reference").asText(null));
    }

    private JsonNode validate(JsonNode requirement) {
        JsonNode locations = requirement.path("locations");
        if (!locations.isArray() || locations.isEmpty()) {
            throw new Failure(422, "INVALID_REQUEST", "A requirement needs at least one location.",
                    "/requirement/locations", null);
        }
        if (locations.size() > 10) {
            throw new Failure(422, "TOO_MANY_LOCATIONS", "A requirement can name at most 10 locations.",
                    "/requirement/locations", null);
        }
        for (int i = 0; i < locations.size(); i++) {
            if ("AREA".equals(locations.get(i).path("level").asText()) && !areaLocations) {
                throw new Failure(422, "UNSUPPORTED_LOCATION_LEVEL",
                        "This provider does not accept a point and a radius as a location.",
                        "/requirement/locations/" + i, null);
            }
        }
        JsonNode demand = requirement.path("demand");
        boolean cabinets = demand.has("cabinets") && demand.has("density");
        if (!demand.has("power") && !cabinets) {
            throw new Failure(422, "INVALID_REQUEST", "A demand needs power, or cabinets and density.",
                    "/requirement/demand", null);
        }
        if (demand.has("power") && cabinets) {
            double total = Inventory.kilo(demand.get("power"));
            int count = demand.path("cabinets").path("count").asInt();
            JsonNode density = demand.get("density");
            if ((density.has("max") && total > count * density.get("max").asDouble())
                    || (density.has("min") && total < count * density.get("min").asDouble())) {
                throw new Failure(422, "INCONSISTENT_DEMAND",
                        "The total power does not fit the cabinet count at the density.",
                        "/requirement/demand/power", null);
            }
        }
        return requirement;
    }

    private ObjectNode evaluate(JsonNode requirement, String reference) {
        Instant at = Instant.now(clock).truncatedTo(ChronoUnit.SECONDS);
        ObjectNode answer = JSON.createObjectNode().put("answerId", "ans-" + String.format("%04d", ++counter));
        if (reference != null) {
            answer.put("reference", reference);
        }
        answer.put("generatedAt", at.toString()).put("validUntil", at.plus(1, ChronoUnit.DAYS).toString());
        ArrayNode results = JSON.createArrayNode();
        LocalDate today = LocalDate.ofInstant(at, ZoneOffset.UTC);
        for (JsonNode location : requirement.path("locations")) {
            results.addAll(inventory.answer(JSON, location, requirement, today, disclosure, REVIEW_THRESHOLD_KW, FEATURES));
        }
        String best = "NOT_DISCLOSED";
        for (String grade : List.of("UNAVAILABLE", "POSSIBLE", "AVAILABLE")) {
            for (JsonNode result : results) {
                if (grade.equals(result.path("grade").asText())) {
                    best = grade;
                }
            }
        }
        answer.put("grade", best);
        answer.set("results", results);
        return answer;
    }

    // ------------------------------------------------------------------ signals

    private synchronized void createSignal(HttpExchange exchange) throws IOException {
        String replay = exchange.getRequestHeaders().getFirst("Idempotency-Key");
        if (replay != null && idempotency.containsKey(replay)) {
            String existing = idempotency.get(replay);
            reply(exchange, 201, stored(signals, existing), existing);
            return;
        }
        ObjectNode body = body(exchange);
        validate(body.path("requirement"));
        checkContact(body);
        String id = "sig-" + String.format("%04d", signals.size() + 1) + "-" + Integer.toHexString(++counter);
        body.put("signalId", id).put("state", "RECEIVED").put("revision", 1)
                .put("createdAt", now()).put("updatedAt", now());
        if (!body.has("expiresAt")) {
            body.put("expiresAt", Instant.now(clock).truncatedTo(ChronoUnit.SECONDS).plus(365, ChronoUnit.DAYS).toString());
        }
        Stored stored = new Stored(body);
        signals.put(id, stored);
        if (replay != null) {
            idempotency.put(replay, id);
        }
        exchange.getResponseHeaders().set("Location", PREFIX + "/signals/" + id);
        reply(exchange, 201, stored, id);
    }

    private static void checkContact(ObjectNode body) {
        if (body.path("confidentiality").path("contactPermitted").asBoolean(false) && !body.has("contact")) {
            throw new Failure(422, "INVALID_REQUEST", "contactPermitted is true, so a contact is required.",
                    "/contact", null);
        }
    }

    private synchronized void replaceSignal(HttpExchange exchange, String id) throws IOException {
        Stored stored = stored(signals, id);
        checkMatch(exchange, id, stored);
        String state = stored.body.path("state").asText();
        if ("CLOSED".equals(state) || "EXPIRED".equals(state)) {
            throw new Failure(409, "STATE_CONFLICT", "The signal is " + state + ". Create a new signal.");
        }
        ObjectNode body = body(exchange);
        validate(body.path("requirement"));
        checkContact(body);
        for (String kept : List.of("signalId", "createdAt")) {
            body.set(kept, stored.body.get(kept));
        }
        if (!body.has("expiresAt")) {
            body.set("expiresAt", stored.body.get("expiresAt"));
        }
        body.put("state", "RECEIVED").put("revision", stored.body.path("revision").asInt() + 1);
        stored.body.removeAll();
        stored.body.setAll(body);
        touch(stored);
        signalChanged(id);
        reevaluate();
        reply(exchange, 200, stored, id);
    }

    private synchronized void withdraw(HttpExchange exchange, String id) throws IOException {
        Stored stored = stored(signals, id);
        String state = stored.body.path("state").asText();
        if ("CLOSED".equals(state) || "EXPIRED".equals(state)) {
            throw new Failure(409, "STATE_CONFLICT", "The signal is " + state + " and cannot be withdrawn.");
        }
        if (!"WITHDRAWN".equals(state)) {
            stored.body.put("state", "WITHDRAWN");
            stored.body.remove("answer");
            touch(stored);
            signalChanged(id);
        }
        reply(exchange, 200, stored, id);
    }

    private synchronized void deleteSignal(HttpExchange exchange, String id) throws IOException {
        stored(signals, id);
        signals.remove(id);
        signalChanged(id);
        send(exchange, 204, null, null);
    }

    // ------------------------------------------------------------------ subscriptions

    private synchronized void createSubscription(HttpExchange exchange) throws IOException {
        ObjectNode body = body(exchange);
        boolean byRequirement = body.has("requirement");
        if (byRequirement == body.has("signalId")) {
            throw new Failure(422, "INVALID_REQUEST", "Send exactly one of requirement and signalId.");
        }
        if (byRequirement) {
            validate(body.path("requirement"));
        } else {
            stored(signals, body.path("signalId").asText());
        }
        String type = body.path("delivery").path("type").asText();
        if (!"WEBHOOK".equals(type) && !"PULL".equals(type)) {
            throw new Failure(422, "INVALID_REQUEST", "delivery.type must be WEBHOOK or PULL.", "/delivery/type", null);
        }
        String id = "sub-" + String.format("%04d", subscriptions.size() + 1) + "-" + Integer.toHexString(++counter);
        body.put("subscriptionId", id).put("createdAt", now()).put("updatedAt", now());
        if (!body.has("notifyOn")) {
            body.put("notifyOn", "GRADE_CHANGE");
        }
        Stored stored = new Stored(body);
        subscriptions.put(id, stored);
        events.put(id, new ArrayList<>());
        ObjectNode created;
        if ("WEBHOOK".equals(type)) {
            String secret = "whsec_" + UUID.randomUUID().toString().replace("-", "");
            secrets.put(id, secret);
            body.put("state", "PENDING_VERIFICATION");
            created = body.deepCopy();
            ((ObjectNode) created.get("delivery")).put("signingSecret", secret);
            verify(id);
        } else {
            body.put("state", "ACTIVE");
            initial(id);
            created = body.deepCopy();
        }
        send(exchange, 201, created, etag(id, stored));
    }

    private void initial(String id) {
        JsonNode requirement = requirementOf(subscriptions.get(id).body);
        if (requirement != null) {
            answerEvent(id, "INITIAL", evaluate(requirement, null));
        }
    }

    private void answerEvent(String id, String reason, ObjectNode answer) {
        lastAnswers.put(id, answer);
        ObjectNode data = JSON.createObjectNode().put("reason", reason);
        data.set("answer", answer);
        emit(id, "answer.updated", data);
    }

    private void emit(String id, String type, ObjectNode extra) {
        List<ObjectNode> list = events.get(id);
        long sequence = list.size() + 1L;
        ObjectNode event = JSON.createObjectNode().put("specversion", "1.0").put("id", "evt-" + id + "-" + sequence)
                .put("source", endpoint()).put("type", "org.opencapacity." + type).put("subject", id)
                .put("time", now()).put("datacontenttype", "application/json");
        ObjectNode data = event.putObject("data").put("subscriptionId", id).put("sequence", sequence);
        data.setAll(extra);
        list.add(event);
        Stored subscription = subscriptions.get(id);
        subscription.body.put("lastSequence", sequence);
        if ("WEBHOOK".equals(subscription.body.path("delivery").path("type").asText())
                && "ACTIVE".equals(subscription.body.path("state").asText())) {
            deliver(id, event);
        }
        notifyAll();
    }

    private HttpResponse<String> post(String id, ObjectNode event) throws IOException, InterruptedException {
        String raw = JSON.writeValueAsString(event);
        long timestamp = Instant.now(clock).getEpochSecond();
        HttpRequest request = HttpRequest.newBuilder(URI.create(subscriptions.get(id).body.path("delivery").path("url").asText()))
                .timeout(Duration.ofSeconds(5))
                .header("Content-Type", "application/cloudevents+json")
                .header("OpenCapacity-Signature", "t=" + timestamp + ",v1=" + hmac(secrets.get(id), timestamp + "." + raw))
                .POST(HttpRequest.BodyPublishers.ofString(raw)).build();
        return webhooks.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private static String hmac(String secret, String text) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(text.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.GeneralSecurityException unavailable) {
            throw new IllegalStateException(unavailable);
        }
    }

    private void deliver(String id, ObjectNode event) {
        try {
            if (post(id, event).statusCode() / 100 != 2) {
                suspend(id);
            }
        } catch (IOException unreachable) {
            suspend(id);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    private void suspend(String id) {
        Stored subscription = subscriptions.get(id);
        subscription.body.put("state", "SUSPENDED");
        touch(subscription);
    }

    /** Sends the verification event and activates the subscription if the endpoint echoes the challenge. */
    private void verify(String id) {
        String challenge = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        List<ObjectNode> list = events.get(id);
        long sequence = list.size() + 1L;
        ObjectNode event = JSON.createObjectNode().put("specversion", "1.0").put("id", "evt-" + id + "-" + sequence)
                .put("source", endpoint()).put("type", "org.opencapacity.subscription.verification").put("subject", id)
                .put("time", now()).put("datacontenttype", "application/json");
        event.putObject("data").put("subscriptionId", id).put("sequence", sequence).put("challenge", challenge);
        list.add(event);
        Stored subscription = subscriptions.get(id);
        subscription.body.put("lastSequence", sequence);
        try {
            HttpResponse<String> response = post(id, event);
            if (response.statusCode() == 200 && challenge.equals(JSON.readTree(response.body()).path("challenge").asText())) {
                subscription.body.put("state", "ACTIVE");
                initial(id);
            }
        } catch (IOException unreachable) {
            subscription.body.put("state", "PENDING_VERIFICATION");
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    private synchronized void replaceSubscription(HttpExchange exchange, String id) throws IOException {
        Stored stored = stored(subscriptions, id);
        checkMatch(exchange, id, stored);
        ObjectNode body = body(exchange);
        if (body.has("requirement") == body.has("signalId")) {
            throw new Failure(422, "INVALID_REQUEST", "Send exactly one of requirement and signalId.");
        }
        if (body.has("requirement")) {
            validate(body.path("requirement"));
        }
        String oldUrl = stored.body.path("delivery").path("url").asText("");
        String oldState = stored.body.path("state").asText();
        for (String kept : List.of("subscriptionId", "createdAt", "lastSequence")) {
            if (stored.body.has(kept)) {
                body.set(kept, stored.body.get(kept));
            }
        }
        if (!body.has("notifyOn")) {
            body.put("notifyOn", "GRADE_CHANGE");
        }
        boolean webhook = "WEBHOOK".equals(body.path("delivery").path("type").asText());
        boolean reverify = webhook && (!oldUrl.equals(body.path("delivery").path("url").asText())
                || "SUSPENDED".equals(oldState) || "PENDING_VERIFICATION".equals(oldState));
        String state = body.path("paused").asBoolean(false) ? "PAUSED" : reverify ? "PENDING_VERIFICATION" : "ACTIVE";
        body.put("state", state);
        stored.body.removeAll();
        stored.body.setAll(body);
        touch(stored);
        if ("PENDING_VERIFICATION".equals(state)) {
            secrets.computeIfAbsent(id, key -> "whsec_" + UUID.randomUUID().toString().replace("-", ""));
            verify(id);
        }
        reply(exchange, 200, stored, id);
    }

    private synchronized void deleteSubscription(HttpExchange exchange, String id) throws IOException {
        stored(subscriptions, id);
        subscriptions.remove(id);
        events.remove(id);
        lastAnswers.remove(id);
        secrets.remove(id);
        send(exchange, 204, null, null);
    }

    private void events(HttpExchange exchange, String id, Map<String, String> query) throws IOException {
        long after;
        synchronized (this) {
            stored(subscriptions, id);
            String lastEventId = exchange.getRequestHeaders().getFirst("Last-Event-ID");
            after = Long.parseLong(lastEventId != null ? lastEventId : query.getOrDefault("after", "0"));
        }
        String accept = exchange.getRequestHeaders().getFirst("Accept");
        if (accept == null || !accept.contains("text/event-stream")) {
            synchronized (this) {
                int limit = Math.min(200, Integer.parseInt(query.getOrDefault("limit", "50")));
                List<ObjectNode> list = events.get(id);
                ObjectNode result = JSON.createObjectNode();
                ArrayNode data = result.putArray("data");
                int index = (int) after;
                while (index < list.size() && data.size() < limit) {
                    data.add(list.get(index++));
                }
                ObjectNode pagination = result.putObject("pagination").put("limit", limit);
                if (index < list.size()) {
                    pagination.put("next", Integer.toString(index));
                }
                send(exchange, 200, result, null);
            }
            return;
        }
        exchange.getResponseHeaders().set("Content-Type", "text/event-stream");
        exchange.sendResponseHeaders(200, 0);
        OutputStream out = exchange.getResponseBody();
        long sent = after;
        long deadline = System.currentTimeMillis() + streamHoldMillis;
        while (true) {
            List<ObjectNode> batch = new ArrayList<>();
            synchronized (this) {
                List<ObjectNode> list = events.get(id);
                if (list == null) {
                    return;
                }
                while (sent < list.size()) {
                    batch.add(list.get((int) sent++));
                }
                if (batch.isEmpty()) {
                    long wait = deadline - System.currentTimeMillis();
                    if (wait <= 0) {
                        return;
                    }
                    try {
                        wait(wait);
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                    continue;
                }
            }
            for (ObjectNode event : batch) {
                String frame = "id: " + event.path("data").path("sequence").asLong() + "\nevent: "
                        + event.path("type").asText() + "\ndata: " + JSON.writeValueAsString(event) + "\n\n";
                out.write(frame.getBytes(StandardCharsets.UTF_8));
            }
            out.flush();
            deadline = System.currentTimeMillis() + streamHoldMillis;
        }
    }
}
