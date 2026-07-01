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
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
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
 * <h3>Usage</h3>
 * <pre>{@code
 * PeeringDbClient client = new PeeringDbClient("your-api-key");
 * client.loadEquinixCatalog();
 *
 * PeeringDbNetwork aws = client.getNetwork(16509);
 * List<PeeringDbNetIxlan> awsIxPresence = client.getEquinixIxPresence(16509);
 * List<PeeringDbNetFac> awsFacPresence = client.getEquinixFacPresence(16509);
 * }</pre>
 *
 * @author ianjones
 * @see <a href="https://docs.peeringdb.com/api_specs/">PeeringDB API Documentation</a>
 */
public class PeeringDbClient {

    static final String DEFAULT_BASE_URL = "https://www.peeringdb.com/api";
    static final int EQUINIX_ORG_ID = 2;

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

    private JsonNode executeGet(String url) throws IOException {
        HttpGet request = new HttpGet(url);
        request.setHeader("Accept", "application/json");
        request.setHeader("User-Agent", "equinix-java-sdk/PeeringIntelligence");

        if (apiKey != null && !apiKey.isEmpty()) {
            request.setHeader("AUTHORIZATION", "Api-Key " + apiKey);
        }

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            int statusCode = response.getStatusLine().getStatusCode();
            String body = EntityUtils.toString(response.getEntity());

            if (statusCode != 200) {
                throw new IOException("PeeringDB API returned HTTP " + statusCode + ": " + body);
            }

            return objectMapper.readTree(body);
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
