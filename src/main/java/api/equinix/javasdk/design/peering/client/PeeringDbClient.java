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

package api.equinix.javasdk.design.peering.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Lightweight HTTP client for the PeeringDB REST API.
 *
 * <p>Provides access to PeeringDB's network, internet exchange, facility, and organization
 * data with optional API key authentication. Uses Apache HttpClient (the same transport
 * library as the Equinix SDK core) for HTTP communication and Jackson for JSON
 * deserialization.</p>
 *
 * <h3>Equinix-Scoped Data</h3>
 * <p>This client includes built-in support for loading the Equinix organization
 * (org_id=2) with all nested IXes and facilities in a single call via
 * {@link #loadEquinixCatalog()}. Subsequent per-ASN queries can then be filtered
 * against the cached Equinix IX and facility IDs.</p>
 *
 * <h3>Authentication</h3>
 * <p>PeeringDB allows unauthenticated read access with rate limiting (~20 requests/minute).
 * Providing an API key via the constructor increases rate limits and grants access to
 * contact information fields.</p>
 *
 * <h3>Rate limiting</h3>
 * <p>An HTTP 429 from PeeringDB is retried up to {@value #MAX_RATE_LIMIT_RETRIES} times,
 * honouring the {@code Retry-After} response header (seconds or HTTP-date form) and falling
 * back to a small exponential backoff when the header is absent or unparsable. The total time
 * spent waiting for a single logical request is capped at {@value #MAX_RATE_LIMIT_WAIT_MS} ms —
 * a {@code Retry-After} beyond the remaining budget fails fast instead of stalling the caller.
 * Multi-ASN lookups should prefer the batched collection overloads ({@code getNetworks},
 * {@code getEquinixIxPresence(Collection)}, {@code getEquinixFacPresence(Collection)}), which
 * use PeeringDB's {@code asn__in} query operator to collapse N per-ASN requests into one
 * request per endpoint — the single most effective way to stay under the anonymous limit.</p>
 *
 * <h3>Lifecycle</h3>
 * <p>The client owns a {@link CloseableHttpClient} and is itself {@link AutoCloseable}:
 * whoever constructs a {@code PeeringDbClient} owns closing it (the analysis engine never
 * closes a client it was handed). {@code PeeringIntelligence.Builder.analyze()} constructs its
 * own client and closes it when the analysis completes.</p>
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * try (PeeringDbClient client = new PeeringDbClient("your-api-key")) {
 *     client.loadEquinixCatalog();
 *
 *     PeeringDbNetwork aws = client.getNetwork(16509);
 *     List<PeeringDbNetIxlan> awsIxPresence = client.getEquinixIxPresence(16509);
 *     List<PeeringDbNetFac> awsFacPresence = client.getEquinixFacPresence(16509);
 * }
 * }</pre>
 *
 * @author ianjones
 * @see <a href="https://docs.peeringdb.com/api_specs/">PeeringDB API Documentation</a>
 */
public class PeeringDbClient implements AutoCloseable {

    static final String DEFAULT_BASE_URL = "https://www.peeringdb.com/api";
    static final int EQUINIX_ORG_ID = 2;

    /** Maximum retries after an HTTP 429 (so at most {@code MAX_RATE_LIMIT_RETRIES + 1} attempts). */
    static final int MAX_RATE_LIMIT_RETRIES = 3;

    /** Hard cap on the TOTAL milliseconds slept across all 429 retries of one logical request. */
    static final long MAX_RATE_LIMIT_WAIT_MS = 30_000L;

    /** Base for the fallback exponential backoff (1s, 2s, 4s) when 429 carries no usable Retry-After. */
    static final long DEFAULT_BACKOFF_BASE_MS = 1_000L;

    /**
     * Maximum ASNs placed into a single {@code asn__in} query. PeeringDB accepts long comma
     * lists, but chunking keeps the URL well under practical length limits.
     */
    static final int MAX_ASNS_PER_REQUEST = 150;

    private final CloseableHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;

    /**
     * The PeeringDB API base URL. Defaults to {@link #DEFAULT_BASE_URL}; a test-only,
     * package-private constructor can point it at a local stub server so the HTTP/parsing
     * paths can be exercised without hitting the public PeeringDB API.
     */
    private final String baseUrl;

    private PeeringDbOrg equinixOrg;
    private Map<Integer, PeeringDbIx> equinixIxMap;
    private Map<Integer, PeeringDbFacility> equinixFacMap;
    private Set<Integer> equinixIxIds;
    private Set<Integer> equinixFacIds;

    /**
     * Creates a PeeringDB client with API key authentication for higher rate limits.
     *
     * @param apiKey the PeeringDB API key, or {@code null} for unauthenticated access
     */
    public PeeringDbClient(String apiKey) {
        this(apiKey, DEFAULT_BASE_URL);
    }

    /**
     * Creates a PeeringDB client with unauthenticated access (~20 requests/minute).
     */
    public PeeringDbClient() {
        this(null, DEFAULT_BASE_URL);
    }

    /**
     * Creates a PeeringDB client pointed at an alternate API base URL.
     *
     * <p><b>Test seam.</b> This exists so the HTTP/parsing paths can be exercised against a
     * local stub server; production code should use the no-arg or API-key constructors, which
     * target the real PeeringDB endpoint.</p>
     *
     * @param apiKey  the PeeringDB API key, or {@code null} for unauthenticated access
     * @param baseUrl the API base URL (e.g. {@code http://localhost:PORT/api})
     * @return a client that issues requests against {@code baseUrl}
     */
    public static PeeringDbClient withBaseUrl(String apiKey, String baseUrl) {
        return new PeeringDbClient(apiKey, baseUrl);
    }

    /**
     * Base-URL-overriding constructor backing {@link #withBaseUrl(String, String)}.
     *
     * @param apiKey  the PeeringDB API key, or {@code null} for unauthenticated access
     * @param baseUrl the API base URL (e.g. {@code http://localhost:PORT/api})
     */
    PeeringDbClient(String apiKey, String baseUrl) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.httpClient = HttpClients.createDefault();
        this.objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
    }

    /**
     * Loads the complete Equinix organization catalog from PeeringDB, including all
     * internet exchanges ({@code ix_set}) and facilities ({@code fac_set}).
     *
     * <p>This makes a single API call to {@code /api/org/2?depth=2} and caches the
     * results. All subsequent calls to {@link #getEquinixIxPresence(long)} and
     * {@link #getEquinixFacPresence(long)} filter against this cached catalog.</p>
     *
     * @throws IOException if the API call fails
     */
    public void loadEquinixCatalog() throws IOException {
        String url = baseUrl + "/org/" + EQUINIX_ORG_ID + "?depth=2";
        JsonNode root = executeGet(url);
        JsonNode data = root.get("data");

        if (data == null || !data.isArray() || data.size() == 0) {
            throw new IOException("PeeringDB returned no data for Equinix org (id=" + EQUINIX_ORG_ID + ")");
        }

        this.equinixOrg = objectMapper.treeToValue(data.get(0), PeeringDbOrg.class);

        this.equinixIxMap = new LinkedHashMap<>();
        if (equinixOrg.getIxSet() != null) {
            for (PeeringDbIx ix : equinixOrg.getIxSet()) {
                equinixIxMap.put(ix.getId(), ix);
            }
        }

        this.equinixFacMap = new LinkedHashMap<>();
        if (equinixOrg.getFacSet() != null) {
            for (PeeringDbFacility fac : equinixOrg.getFacSet()) {
                equinixFacMap.put(fac.getId(), fac);
            }
        }

        this.equinixIxIds = Collections.unmodifiableSet(equinixIxMap.keySet());
        this.equinixFacIds = Collections.unmodifiableSet(equinixFacMap.keySet());
    }

    /**
     * Retrieves network metadata for a given ASN.
     *
     * @param asn the autonomous system number
     * @return the network metadata, or {@code null} if not found
     * @throws IOException if the API call fails
     */
    public PeeringDbNetwork getNetwork(long asn) throws IOException {
        String url = baseUrl + "/net?asn=" + asn;
        JsonNode root = executeGet(url);
        JsonNode data = root.get("data");

        if (data == null || !data.isArray() || data.size() == 0) {
            return null;
        }
        return objectMapper.treeToValue(data.get(0), PeeringDbNetwork.class);
    }

    /**
     * Retrieves all IX LAN presence records for a given ASN.
     *
     * @param asn the autonomous system number
     * @return all {@code netixlan} entries for this ASN across all IXes
     * @throws IOException if the API call fails
     */
    public List<PeeringDbNetIxlan> getNetIxlans(long asn) throws IOException {
        String url = baseUrl + "/netixlan?asn=" + asn;
        return getList(url, new TypeReference<List<PeeringDbNetIxlan>>() {});
    }

    /**
     * Retrieves all facility presence records for a given ASN.
     *
     * @param asn the autonomous system number
     * @return all {@code netfac} entries for this ASN across all facilities
     * @throws IOException if the API call fails
     */
    public List<PeeringDbNetFac> getNetFacs(long asn) throws IOException {
        String url = baseUrl + "/netfac?asn=" + asn;
        return getList(url, new TypeReference<List<PeeringDbNetFac>>() {});
    }

    // ---- Batched (asn__in) lookups ----

    /**
     * Retrieves network metadata for multiple ASNs in batched {@code net?asn__in=...} requests —
     * one HTTP request per {@value #MAX_ASNS_PER_REQUEST} ASNs instead of one per ASN.
     *
     * <p>A single-element collection delegates to {@code getNetwork(long)} (the plain
     * {@code asn=} query), so subclass stubs and per-ASN behaviour stay identical for that case.</p>
     *
     * @param asns the autonomous system numbers (duplicates are collapsed)
     * @return map of ASN to network metadata; ASNs PeeringDB does not know are absent from the map
     * @throws IOException if any API call fails
     */
    public Map<Long, PeeringDbNetwork> getNetworks(Collection<Long> asns) throws IOException {
        Set<Long> distinct = distinctAsns(asns);
        Map<Long, PeeringDbNetwork> result = new LinkedHashMap<>();
        if (distinct.isEmpty()) {
            return result;
        }
        if (distinct.size() == 1) {
            long asn = distinct.iterator().next();
            PeeringDbNetwork net = getNetwork(asn);
            if (net != null) {
                result.put(asn, net);
            }
            return result;
        }
        for (List<Long> chunk : chunked(distinct)) {
            String url = baseUrl + "/net?asn__in=" + joinAsns(chunk);
            for (PeeringDbNetwork net : getList(url, new TypeReference<List<PeeringDbNetwork>>() {})) {
                result.put(net.getAsn(), net);
            }
        }
        return result;
    }

    /**
     * Retrieves IX LAN presence records for multiple ASNs in batched
     * {@code netixlan?asn__in=...} requests, grouped by ASN.
     *
     * <p>A single-element collection delegates to {@code getNetIxlans(long)}.</p>
     *
     * @param asns the autonomous system numbers (duplicates are collapsed)
     * @return map of ASN to its {@code netixlan} entries; every requested ASN is present as a key
     *         (with an empty list when it has no entries)
     * @throws IOException if any API call fails
     */
    public Map<Long, List<PeeringDbNetIxlan>> getNetIxlans(Collection<Long> asns) throws IOException {
        Set<Long> distinct = distinctAsns(asns);
        Map<Long, List<PeeringDbNetIxlan>> result = new LinkedHashMap<>();
        for (Long asn : distinct) {
            result.put(asn, new ArrayList<>());
        }
        if (distinct.isEmpty()) {
            return result;
        }
        if (distinct.size() == 1) {
            long asn = distinct.iterator().next();
            result.get(asn).addAll(getNetIxlans(asn));
            return result;
        }
        for (List<Long> chunk : chunked(distinct)) {
            String url = baseUrl + "/netixlan?asn__in=" + joinAsns(chunk);
            for (PeeringDbNetIxlan nix : getList(url, new TypeReference<List<PeeringDbNetIxlan>>() {})) {
                List<PeeringDbNetIxlan> bucket = result.get(nix.getAsn());
                if (bucket != null) {
                    bucket.add(nix);
                }
            }
        }
        return result;
    }

    /**
     * Retrieves facility presence records for multiple ASNs in batched
     * {@code netfac?asn__in=...} requests, grouped by ASN ({@code local_asn}).
     *
     * <p>A single-element collection delegates to {@code getNetFacs(long)}.</p>
     *
     * @param asns the autonomous system numbers (duplicates are collapsed)
     * @return map of ASN to its {@code netfac} entries; every requested ASN is present as a key
     *         (with an empty list when it has no entries)
     * @throws IOException if any API call fails
     */
    public Map<Long, List<PeeringDbNetFac>> getNetFacs(Collection<Long> asns) throws IOException {
        Set<Long> distinct = distinctAsns(asns);
        Map<Long, List<PeeringDbNetFac>> result = new LinkedHashMap<>();
        for (Long asn : distinct) {
            result.put(asn, new ArrayList<>());
        }
        if (distinct.isEmpty()) {
            return result;
        }
        if (distinct.size() == 1) {
            long asn = distinct.iterator().next();
            result.get(asn).addAll(getNetFacs(asn));
            return result;
        }
        for (List<Long> chunk : chunked(distinct)) {
            String url = baseUrl + "/netfac?asn__in=" + joinAsns(chunk);
            for (PeeringDbNetFac nf : getList(url, new TypeReference<List<PeeringDbNetFac>>() {})) {
                List<PeeringDbNetFac> bucket = result.get(nf.getLocalAsn());
                if (bucket != null) {
                    bucket.add(nf);
                }
            }
        }
        return result;
    }

    /**
     * Retrieves IX LAN presence records for a given ASN, filtered to Equinix IXes only.
     *
     * <p>Requires {@link #loadEquinixCatalog()} to have been called first.</p>
     *
     * @param asn the autonomous system number
     * @return {@code netixlan} entries at Equinix internet exchanges only
     * @throws IOException if the API call fails
     * @throws IllegalStateException if the Equinix catalog has not been loaded
     */
    public List<PeeringDbNetIxlan> getEquinixIxPresence(long asn) throws IOException {
        ensureCatalogLoaded();
        return getNetIxlans(asn).stream()
                .filter(nix -> equinixIxIds.contains(nix.getIxId()))
                .collect(Collectors.toList());
    }

    /**
     * Retrieves facility presence records for a given ASN, filtered to Equinix facilities only.
     *
     * <p>Requires {@link #loadEquinixCatalog()} to have been called first.</p>
     *
     * @param asn the autonomous system number
     * @return {@code netfac} entries at Equinix facilities only
     * @throws IOException if the API call fails
     * @throws IllegalStateException if the Equinix catalog has not been loaded
     */
    public List<PeeringDbNetFac> getEquinixFacPresence(long asn) throws IOException {
        ensureCatalogLoaded();
        return getNetFacs(asn).stream()
                .filter(nf -> equinixFacIds.contains(nf.getFacId()))
                .collect(Collectors.toList());
    }

    /**
     * Retrieves IX LAN presence records for multiple ASNs, filtered to Equinix IXes only,
     * using batched {@code asn__in} requests (one per {@value #MAX_ASNS_PER_REQUEST} ASNs
     * instead of one per ASN).
     *
     * <p>A single-element collection delegates to {@link #getEquinixIxPresence(long)}, preserving
     * the plain {@code asn=} wire shape (and any subclass behaviour) for that case. Multi-ASN
     * calls require {@link #loadEquinixCatalog()} to have been called first.</p>
     *
     * @param asns the autonomous system numbers (duplicates are collapsed)
     * @return map of ASN to its {@code netixlan} entries at Equinix IXes; every requested ASN is
     *         present as a key (with an empty list when it has none)
     * @throws IOException if any API call fails
     * @throws IllegalStateException if the Equinix catalog has not been loaded
     */
    public Map<Long, List<PeeringDbNetIxlan>> getEquinixIxPresence(Collection<Long> asns) throws IOException {
        Set<Long> distinct = distinctAsns(asns);
        if (distinct.size() == 1) {
            long asn = distinct.iterator().next();
            Map<Long, List<PeeringDbNetIxlan>> single = new LinkedHashMap<>();
            single.put(asn, getEquinixIxPresence(asn));
            return single;
        }
        ensureCatalogLoaded();
        Map<Long, List<PeeringDbNetIxlan>> all = getNetIxlans(distinct);
        Map<Long, List<PeeringDbNetIxlan>> result = new LinkedHashMap<>();
        for (Map.Entry<Long, List<PeeringDbNetIxlan>> entry : all.entrySet()) {
            result.put(entry.getKey(), entry.getValue().stream()
                    .filter(nix -> equinixIxIds.contains(nix.getIxId()))
                    .collect(Collectors.toList()));
        }
        return result;
    }

    /**
     * Retrieves facility presence records for multiple ASNs, filtered to Equinix facilities only,
     * using batched {@code asn__in} requests.
     *
     * <p>A single-element collection delegates to {@link #getEquinixFacPresence(long)}. Multi-ASN
     * calls require {@link #loadEquinixCatalog()} to have been called first.</p>
     *
     * @param asns the autonomous system numbers (duplicates are collapsed)
     * @return map of ASN to its {@code netfac} entries at Equinix facilities; every requested ASN
     *         is present as a key (with an empty list when it has none)
     * @throws IOException if any API call fails
     * @throws IllegalStateException if the Equinix catalog has not been loaded
     */
    public Map<Long, List<PeeringDbNetFac>> getEquinixFacPresence(Collection<Long> asns) throws IOException {
        Set<Long> distinct = distinctAsns(asns);
        if (distinct.size() == 1) {
            long asn = distinct.iterator().next();
            Map<Long, List<PeeringDbNetFac>> single = new LinkedHashMap<>();
            single.put(asn, getEquinixFacPresence(asn));
            return single;
        }
        ensureCatalogLoaded();
        Map<Long, List<PeeringDbNetFac>> all = getNetFacs(distinct);
        Map<Long, List<PeeringDbNetFac>> result = new LinkedHashMap<>();
        for (Map.Entry<Long, List<PeeringDbNetFac>> entry : all.entrySet()) {
            result.put(entry.getKey(), entry.getValue().stream()
                    .filter(nf -> equinixFacIds.contains(nf.getFacId()))
                    .collect(Collectors.toList()));
        }
        return result;
    }

    /**
     * Returns the cached Equinix organization data.
     *
     * @return the Equinix org, or {@code null} if not yet loaded
     */
    public PeeringDbOrg getEquinixOrg() {
        return equinixOrg;
    }

    /**
     * Returns the map of Equinix IX ID to IX metadata.
     *
     * @return unmodifiable map of IX ID to {@link PeeringDbIx}
     * @throws IllegalStateException if the Equinix catalog has not been loaded
     */
    public Map<Integer, PeeringDbIx> getEquinixIxMap() {
        ensureCatalogLoaded();
        return Collections.unmodifiableMap(equinixIxMap);
    }

    /**
     * Returns the map of Equinix facility ID to facility metadata.
     *
     * @return unmodifiable map of facility ID to {@link PeeringDbFacility}
     * @throws IllegalStateException if the Equinix catalog has not been loaded
     */
    public Map<Integer, PeeringDbFacility> getEquinixFacMap() {
        ensureCatalogLoaded();
        return Collections.unmodifiableMap(equinixFacMap);
    }

    /**
     * Returns the set of all Equinix IX IDs for filtering.
     *
     * @return unmodifiable set of PeeringDB IX IDs belonging to Equinix
     * @throws IllegalStateException if the Equinix catalog has not been loaded
     */
    public Set<Integer> getEquinixIxIds() {
        ensureCatalogLoaded();
        return equinixIxIds;
    }

    /**
     * Returns the set of all Equinix facility IDs for filtering.
     *
     * @return unmodifiable set of PeeringDB facility IDs belonging to Equinix
     * @throws IllegalStateException if the Equinix catalog has not been loaded
     */
    public Set<Integer> getEquinixFacIds() {
        ensureCatalogLoaded();
        return equinixFacIds;
    }

    /**
     * Looks up an Equinix IX by its PeeringDB ID.
     *
     * @param ixId the PeeringDB IX ID
     * @return the IX metadata, or {@code null} if not an Equinix IX
     */
    public PeeringDbIx getEquinixIx(int ixId) {
        ensureCatalogLoaded();
        return equinixIxMap.get(ixId);
    }

    /**
     * Looks up an Equinix facility by its PeeringDB ID.
     *
     * @param facId the PeeringDB facility ID
     * @return the facility metadata, or {@code null} if not an Equinix facility
     */
    public PeeringDbFacility getEquinixFacility(int facId) {
        ensureCatalogLoaded();
        return equinixFacMap.get(facId);
    }

    private void ensureCatalogLoaded() {
        if (equinixOrg == null) {
            throw new IllegalStateException(
                    "Equinix catalog not loaded. Call loadEquinixCatalog() before querying Equinix-scoped data.");
        }
    }

    /**
     * Executes a GET with rate-limit awareness: an HTTP 429 is retried up to
     * {@value #MAX_RATE_LIMIT_RETRIES} times, honouring {@code Retry-After} (seconds or
     * HTTP-date) with a small exponential-backoff fallback, and the total wait across all
     * retries of this one request is capped at {@value #MAX_RATE_LIMIT_WAIT_MS} ms. Any other
     * non-200 status — and a 429 that outlives the retry/wait budget — is an {@link IOException}.
     */
    private JsonNode executeGet(String url) throws IOException {
        long sleptMs = 0L;
        for (int attempt = 0; ; attempt++) {
            HttpGet request = new HttpGet(url);
            request.setHeader("Accept", "application/json");
            request.setHeader("User-Agent", "equinix-java-sdk/PeeringIntelligence");

            if (apiKey != null && !apiKey.isEmpty()) {
                request.setHeader("AUTHORIZATION", "Api-Key " + apiKey);
            }

            int statusCode;
            String body;
            String retryAfter = null;
            // The response is fully consumed and closed BEFORE any backoff sleep, so a pooled
            // connection is never held hostage while waiting out a rate-limit window.
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                statusCode = response.getStatusLine().getStatusCode();
                body = EntityUtils.toString(response.getEntity());
                Header retryAfterHeader = response.getFirstHeader("Retry-After");
                if (retryAfterHeader != null) {
                    retryAfter = retryAfterHeader.getValue();
                }
            }

            if (statusCode == 200) {
                return objectMapper.readTree(body);
            }

            if (statusCode != 429 || attempt >= MAX_RATE_LIMIT_RETRIES) {
                throw new IOException("PeeringDB API returned HTTP " + statusCode + ": " + body);
            }

            long waitMs = retryAfterMillis(retryAfter, attempt);
            long remainingBudgetMs = MAX_RATE_LIMIT_WAIT_MS - sleptMs;
            if (waitMs > remainingBudgetMs) {
                // Fail fast instead of stalling: the server asked for more waiting than this
                // request's total backoff budget allows.
                throw new IOException("PeeringDB API returned HTTP 429 and asked to wait " + waitMs
                        + "ms, exceeding the remaining " + remainingBudgetMs + "ms rate-limit backoff "
                        + "budget (max " + MAX_RATE_LIMIT_WAIT_MS + "ms per request): " + body);
            }
            sleep(waitMs);
            sleptMs += waitMs;
        }
    }

    /**
     * Resolves the wait for a 429: the {@code Retry-After} header when parsable — integer seconds
     * or an RFC-1123 HTTP-date (clamped to now) — otherwise a small exponential backoff
     * ({@value #DEFAULT_BACKOFF_BASE_MS} ms doubled per attempt). Package-private for tests.
     *
     * @param retryAfter the raw {@code Retry-After} header value, or {@code null} when absent
     * @param attempt    the zero-based retry attempt, driving the fallback backoff
     * @return the milliseconds to wait before retrying (never negative)
     */
    static long retryAfterMillis(String retryAfter, int attempt) {
        if (retryAfter != null && !retryAfter.isBlank()) {
            String value = retryAfter.trim();
            try {
                return Math.max(0L, Long.parseLong(value) * 1000L);
            } catch (NumberFormatException notSeconds) {
                try {
                    ZonedDateTime when = ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME);
                    return Math.max(0L, Duration.between(Instant.now(), when.toInstant()).toMillis());
                } catch (DateTimeParseException notDate) {
                    // Unparsable header: fall through to the default backoff.
                }
            }
        }
        return DEFAULT_BACKOFF_BASE_MS << Math.min(attempt, 10);
    }

    private static void sleep(long waitMs) throws IOException {
        if (waitMs <= 0) {
            return;
        }
        try {
            Thread.sleep(waitMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while backing off from PeeringDB rate limiting", e);
        }
    }

    /** The requested ASNs, de-duplicated with their first-seen order preserved. */
    private static Set<Long> distinctAsns(Collection<Long> asns) {
        return asns == null ? Collections.emptySet() : new LinkedHashSet<>(asns);
    }

    /** Splits the ASNs into {@value #MAX_ASNS_PER_REQUEST}-sized chunks for {@code asn__in} URLs. */
    private static List<List<Long>> chunked(Collection<Long> asns) {
        List<List<Long>> chunks = new ArrayList<>();
        List<Long> current = new ArrayList<>();
        for (Long asn : asns) {
            current.add(asn);
            if (current.size() == MAX_ASNS_PER_REQUEST) {
                chunks.add(current);
                current = new ArrayList<>();
            }
        }
        if (!current.isEmpty()) {
            chunks.add(current);
        }
        return chunks;
    }

    private static String joinAsns(Collection<Long> asns) {
        return asns.stream().map(String::valueOf).collect(Collectors.joining(","));
    }

    /**
     * Closes the underlying HTTP client. Quiet by design: a close failure releases nothing
     * actionable to the caller, and the analysis result it would otherwise mask is already in
     * hand. Safe to call more than once. Ownership: whoever constructed this client closes it —
     * the analysis engine never closes a client it was handed.
     */
    @Override
    public void close() {
        try {
            httpClient.close();
        } catch (IOException e) {
            // Release-only failure; nothing for the caller to act on.
        }
    }

    @SuppressWarnings("unchecked")
    private <T> List<T> getList(String url, TypeReference<List<T>> typeRef) throws IOException {
        JsonNode root = executeGet(url);
        JsonNode data = root.get("data");

        if (data == null || !data.isArray()) {
            return Collections.emptyList();
        }

        return (List<T>) objectMapper.readValue(data.traverse(), typeRef);
    }
}
