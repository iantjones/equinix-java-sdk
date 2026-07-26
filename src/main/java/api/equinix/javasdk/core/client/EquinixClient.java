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

package api.equinix.javasdk.core.client;

import api.equinix.javasdk.core.auth.EquinixCredentials;
import api.equinix.javasdk.core.auth.EquinixCredentialsProvider;
import api.equinix.javasdk.core.auth.EquinixStaticCredentialsProvider;
import api.equinix.javasdk.core.exception.EquinixClientException;
import api.equinix.javasdk.core.http.CircuitBreaker;
import api.equinix.javasdk.core.http.EquinixHttpClient;
import api.equinix.javasdk.core.http.RequestAssembler;
import api.equinix.javasdk.core.http.RetryPolicy;
import api.equinix.javasdk.core.http.request.EquinixRequest;
import api.equinix.javasdk.core.http.request.PaginatedRequest;
import api.equinix.javasdk.core.http.response.EquinixResponse;
import api.equinix.javasdk.core.model.OAuthToken;
import api.equinix.javasdk.core.util.ApacheUtils;
import api.equinix.javasdk.core.util.ModelUtils;
import api.equinix.javasdk.core.util.ResourceFileUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Low-level transport client: owns the OAuth2 token lifecycle, the pooled HTTP client, the
 * resolved endpoint, and the merged apiParams catalogue, and signs and dispatches every request.
 * The public facade {@link api.equinix.javasdk.EquinixClient} and the domain clients delegate to
 * an instance of this class.
 *
 * @author ianjones
 */
public class EquinixClient implements Closeable {

    @Getter
    final private EquinixCredentialsProvider equinixCredentialsProvider;

    /**
     * The merged apiParams catalogue. Published via a {@code volatile} field and only ever
     * replaced wholesale (copy-on-write in {@link #appendApiParams}) so request threads can read
     * it without locking. Deliberately has no public setter — the registry is owned by this
     * client.
     */
    @Getter
    private volatile JsonNode clientResourceFile;

    /** Guards the copy-on-write merge in {@link #appendApiParams}. */
    private final Object apiParamsLock = new Object();

    @Getter
    private volatile URI endPoint;

    private volatile OAuthToken oAuthToken;

    /**
     * Guards the read-validate-publish lifecycle of {@link #oAuthToken}. Reading and
     * (re)publishing the token are serialized through this monitor so that, under
     * concurrent load, at most one thread observes an invalid/expired token and acts
     * on it (single-flight), while the others block briefly and then reuse whatever
     * valid token is current. The field itself is {@code volatile} so callers that
     * only need a quick, lock-free snapshot via {@link #getOAuthToken()} still see
     * the most recently published instance.
     */
    private final Object tokenLock = new Object();

    /**
     * Acquires and publishes a fresh OAuth token on demand. Wired by the facade client (which
     * knows how to mint a token); used for lazy authentication on the first call and for
     * re-authentication once the current token has expired.
     */
    private volatile Runnable authenticator;

    /**
     * Re-entrancy guard: the token request itself flows through {@link #setStandardHeaders}, so
     * while a token is being acquired on this thread the lazy-auth path must not recurse.
     */
    private final ThreadLocal<Boolean> authenticating = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private final boolean isSandBoxed;

    private final EquinixHttpClient equinixHttpClient;

    public EquinixClient(EquinixCredentials equinixCredentials, boolean isSandBoxed) {
        this(new EquinixStaticCredentialsProvider(equinixCredentials), isSandBoxed);
    }

    /**
     * Creates a transport client that resolves its credentials through the given provider. The
     * provider is consulted on each authentication (first call and re-auth on expiry), so a custom
     * provider can rotate the underlying credentials without the client being rebuilt.
     *
     * @param credentialsProvider supplies the OAuth2 credentials to authenticate with
     * @param isSandBoxed {@code true} for the sandbox environment; {@code false} for production
     */
    public EquinixClient(EquinixCredentialsProvider credentialsProvider, boolean isSandBoxed) {
        this.equinixCredentialsProvider = Objects.requireNonNull(credentialsProvider, "credentialsProvider");
        this.isSandBoxed = isSandBoxed;
        equinixHttpClient = new EquinixHttpClient();
        init();
    }

    private void init() throws EquinixClientException {
        String coreParams = "json/apiParams_Core.json";
        try {
            JsonNode coreResourceFile = ResourceFileUtils.loadResourceFileJson(coreParams);
            // The resource loader returns null (not an exception) for a missing classpath
            // resource; name the file explicitly rather than NPE-ing into a generic message.
            if (coreResourceFile == null) {
                throw new EquinixClientException("Core apiParams resource not found on classpath: " + coreParams);
            }
            clientResourceFile = coreResourceFile;

            String hostNameLookup = isSandBoxed() ? "sandboxHostName" : "hostName";
            String hostName = coreResourceFile.path("coreConfig").path(hostNameLookup).textValue();

            setEndPoint(hostName);
        }
        catch (EquinixClientException ece) {
            throw ece;
        }
        catch (Exception e) {
            throw new EquinixClientException("Unable to initialize the EquinixClient with the JSON configuration from '"
                    + coreParams + "'.", e);
        }
    }

    /**
     *
     * @throws java.lang.IllegalArgumentException if any.
     */
    public void setEndPoint(String endPoint) throws IllegalArgumentException {
        this.endPoint = toURI(endPoint);
    }

    /**
     * Overrides the automatic retry behavior for transient failures (429/5xx, transient IO).
     *
     * @param retryPolicy the policy to apply; {@link api.equinix.javasdk.core.http.RetryPolicy#none()} disables retries
     */
    public void setRetryPolicy(RetryPolicy retryPolicy) {
        equinixHttpClient.setRetryPolicy(retryPolicy);
    }

    /**
     * Enables (or, with {@code null}, disables) the opt-in circuit breaker consulted for every
     * request attempt. See {@link api.equinix.javasdk.core.http.CircuitBreaker} for the
     * open/half-open/closed semantics. Disabled by default.
     *
     * @param circuitBreaker the breaker to enforce, or {@code null} to disable
     */
    public void setCircuitBreaker(CircuitBreaker circuitBreaker) {
        equinixHttpClient.setCircuitBreaker(circuitBreaker);
    }

    /**
     * Merges a domain's apiParams resource into the shared catalogue. Thread-safe: the merge is
     * copy-on-write under {@link #apiParamsLock} and published atomically through the
     * {@code volatile} {@code clientResourceFile} field (read via the Lombok-generated
     * {@code getClientResourceFile()}), so request
     * threads reading the tree concurrently (Jackson's {@code ObjectNode} is not safe for
     * concurrent read/write) always see either the old or the fully-merged catalogue.
     *
     * @param fileName the classpath resource name (e.g. {@code json/apiParams_Fabric.json})
     * @throws api.equinix.javasdk.core.exception.EquinixClientException if the resource is
     *         missing, malformed, or does not declare a {@code functionalAreas} object
     */
    public void appendApiParams(String fileName) throws EquinixClientException {
        try {
            JsonNode additionalResourceFile = ResourceFileUtils.loadResourceFileJson(fileName);
            // The resource loader returns null (not an exception) for a missing classpath
            // resource; fail fast naming the file instead of NPE-ing into a generic message.
            if (additionalResourceFile == null) {
                throw new EquinixClientException("apiParams resource not found on classpath: " + fileName);
            }
            JsonNode additionalFunctionalAreas = additionalResourceFile.get("functionalAreas");
            if (additionalFunctionalAreas == null || !additionalFunctionalAreas.isObject()) {
                throw new EquinixClientException("apiParams resource '" + fileName
                        + "' does not declare a 'functionalAreas' object.");
            }

            synchronized (apiParamsLock) {
                JsonNode merged = clientResourceFile.deepCopy();
                JsonNode coreFunctionalAreas = merged.get("functionalAreas");
                if (!(coreFunctionalAreas instanceof ObjectNode mergedAreas)) {
                    throw new EquinixClientException("Core apiParams catalogue is missing its 'functionalAreas' object.");
                }
                Iterator<Entry<String, JsonNode>> functionalAreasFields = additionalFunctionalAreas.fields();
                while (functionalAreasFields.hasNext()) {
                    Entry<String, JsonNode> functionalAreaField = functionalAreasFields.next();
                    mergedAreas.set(functionalAreaField.getKey(), functionalAreaField.getValue());
                }
                clientResourceFile = merged;
            }
        }
        catch (EquinixClientException ece) {
            throw ece;
        }
        catch (Exception e) {
            throw new EquinixClientException("Unable to append the EquinixClient with the JSON configuration from '"
                    + fileName + "'.", e);
        }
    }

    /**
     *
     * @throws api.equinix.javasdk.core.exception.EquinixClientException if any.
     */
    public <T> EquinixResponse<T> invoke(EquinixRequest<T> equinixRequest) {

        EquinixResponse<T> equinixResponse;
        RequestAssembler.addRequestParams(equinixRequest);

        if(equinixRequest instanceof PaginatedRequest) {
            ((PaginatedRequest<T>) equinixRequest).setPagination();
        }

        equinixRequest.setQueryParameters(ModelUtils.cleanseQueryParameterList(equinixRequest.getQueryParameters()));

        setStandardHeaders(equinixRequest);
        equinixResponse = equinixHttpClient.executeWithRetries(equinixRequest);
        return equinixResponse;
    }

    /**
     *
     * @return a boolean.
     */
    public boolean isSandBoxed() {
        return isSandBoxed;
    }

    private URI toURI(String endpoint) throws IllegalArgumentException {
        return ApacheUtils.toUri(endpoint, equinixHttpClient.getProtocol());
    }

    /**
     * Returns the currently published OAuth token, or {@code null} if the client has
     * not authenticated yet.
     *
     * <p>This is a lock-free {@code volatile} read returning the latest published
     * instance. For an atomic validity-then-use decision under concurrency, prefer
     * the internal single-flight path in {@link #setStandardHeaders} which captures
     * a consistent snapshot under {@link #tokenLock}.</p>
     *
     * @return the current {@link api.equinix.javasdk.core.model.OAuthToken}, may be {@code null}.
     */
    public OAuthToken getOAuthToken() {
        return oAuthToken;
    }

    /**
     * Publishes a (re)authenticated OAuth token for use by subsequent requests.
     *
     * <p>Publication is serialized through {@link #tokenLock} so that a token
     * obtained by a single-flight refresh becomes visible atomically with respect to
     * other threads inspecting validity. Once stored, the {@code volatile} field
     * guarantees the new instance is visible to all threads.</p>
     *
     * @param oAuthToken the freshly obtained token, may be {@code null} to clear.
     */
    public void setOAuthToken(OAuthToken oAuthToken) {
        synchronized (tokenLock) {
            this.oAuthToken = oAuthToken;
        }
    }

    /**
     * Wires the callback used to mint and publish an OAuth token for lazy authentication and
     * re-authentication on expiry. The facade client supplies this; the core client invokes it
     * when a request needs a token and none valid is published.
     *
     * @param authenticator the token-acquisition callback (it must publish via {@link #setOAuthToken})
     */
    public void setAuthenticator(Runnable authenticator) {
        this.authenticator = authenticator;
    }

    /**
     * Forces (re)authentication via the wired authenticator, single-flight under {@link #tokenLock}.
     * This is the explicit {@code authenticate()} entry point. The {@link #authenticating} guard
     * means the token request it issues (which itself flows through {@link #setStandardHeaders})
     * does not recurse into another authentication. No-op when no authenticator is wired.
     */
    public void authenticate() {
        Runnable auth = authenticator;
        if (auth == null || Boolean.TRUE.equals(authenticating.get())) {
            return;
        }
        synchronized (tokenLock) {
            authenticating.set(Boolean.TRUE);
            try {
                auth.run();
            } finally {
                // remove() (not set(FALSE)) so pooled/container worker threads don't retain a
                // per-thread entry referencing the SDK's classloader after the call completes.
                authenticating.remove();
            }
        }
    }

    /**
     * Ensures a valid token is published before a request is signed: authenticates lazily on the
     * first call and re-authenticates when the current token has expired. Single-flight under
     * {@link #tokenLock} (double-checked) so concurrent callers don't stampede the token endpoint.
     * No-op when no authenticator is wired, or when invoked re-entrantly from the token request
     * itself (guarded by {@link #authenticating}).
     */
    private void ensureAuthenticated() {
        Runnable auth = authenticator;
        if (auth == null || Boolean.TRUE.equals(authenticating.get())) {
            return;
        }
        OAuthToken token = oAuthToken;
        if (token != null && token.validSession()) {
            return;
        }
        synchronized (tokenLock) {
            token = oAuthToken;
            if (token != null && token.validSession()) {
                return;
            }
            authenticating.set(Boolean.TRUE);
            try {
                auth.run();
            } finally {
                // remove() (not set(FALSE)) so pooled/container worker threads don't retain a
                // per-thread entry referencing the SDK's classloader after the call completes.
                authenticating.remove();
            }
        }
    }

    /**
     * Applies the standard headers (authorization + content type) to the request, authenticating
     * first if necessary.
     *
     * <p>{@link #ensureAuthenticated()} runs first: it authenticates lazily on the first call and
     * re-authenticates an expired token, single-flight under {@link #tokenLock}. The authorization
     * header is then derived from the published token — omitted always for the token-mint request
     * itself (see {@link #isTokenMintRequest}), which carries its credentials in the body rather
     * than a bearer header. Signing the token request with the currently published token would be
     * wrong exactly when it matters most: during re-authentication the published token is the
     * expired one.</p>
     *
     * @param equinixRequest the request to decorate.
     */
    private <T> void setStandardHeaders(EquinixRequest<T> equinixRequest){
        boolean tokenMintRequest = isTokenMintRequest(equinixRequest);
        if (!tokenMintRequest) {
            ensureAuthenticated();
        }

        // Case-insensitive keys: HTTP header names are case-insensitive, so a caller-supplied
        // "Authorization"/"Content-Type" must be recognized as the same header as the lowercase
        // standard ones below (a case-sensitive merge would emit both, which some gateways reject).
        Map<String, String> standardHeaders = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        if (!tokenMintRequest) {
            OAuthToken currentToken = oAuthToken;
            if (currentToken != null && currentToken.getSessionToken() != null) {
                standardHeaders.put("authorization", "Bearer " + currentToken.getSessionToken());
            }
        }
        String contentType = equinixRequest.getContentType();
        standardHeaders.put("content-type", contentType != null ? contentType : "application/json");
        // Preserve any headers already set on the request (e.g. via addHeader) rather than
        // wholesale-replacing the map; the standard auth/content-type headers take precedence.
        Map<String, String> existingHeaders = equinixRequest.getHeaders();
        if (existingHeaders != null) {
            existingHeaders.forEach(standardHeaders::putIfAbsent);
        }
        if (tokenMintRequest) {
            // The token endpoint authenticates via the client_id/client_secret in the body; a
            // Bearer header must never accompany it (during re-auth it would be the expired
            // token). The map's case-insensitive comparator removes any casing variant.
            standardHeaders.remove("authorization");
        }
        equinixRequest.setHeaders(standardHeaders);
    }

    /**
     * Identifies the OAuth2 token-mint request itself, as built by
     * {@code CoreClientImpl#authenticate()} from the {@code Authentication/OAuth} entry of
     * {@code apiParams_Core.json}. That request authenticates with credentials in its body and
     * must never carry an {@code authorization} bearer header — in particular not the expired
     * token that is still published while re-authentication is in flight.
     */
    private static boolean isTokenMintRequest(EquinixRequest<?> equinixRequest) {
        return "Authentication".equals(equinixRequest.getFunctionalArea())
                && "OAuth".equals(equinixRequest.getRequestParent());
    }

    @Override
    public void close() throws IOException {
        equinixHttpClient.close();
    }
}
