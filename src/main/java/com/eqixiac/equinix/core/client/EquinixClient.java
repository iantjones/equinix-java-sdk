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

package com.eqixiac.equinix.core.client;

import com.eqixiac.equinix.core.auth.BearerToken;
import com.eqixiac.equinix.core.auth.BearerTokenProvider;
import com.eqixiac.equinix.core.auth.EquinixCredentials;
import com.eqixiac.equinix.core.auth.EquinixCredentialsProvider;
import com.eqixiac.equinix.core.auth.EquinixStaticCredentialsProvider;
import com.eqixiac.equinix.core.exception.EquinixAuthenticationException;
import com.eqixiac.equinix.core.exception.EquinixClientException;
import com.eqixiac.equinix.core.http.CircuitBreaker;
import com.eqixiac.equinix.core.http.EquinixHttpClient;
import com.eqixiac.equinix.core.http.RequestAssembler;
import com.eqixiac.equinix.core.http.RetryPolicy;
import com.eqixiac.equinix.core.http.request.EquinixRequest;
import com.eqixiac.equinix.core.http.request.PaginatedRequest;
import com.eqixiac.equinix.core.http.response.EquinixResponse;
import com.eqixiac.equinix.core.model.OAuthToken;
import com.eqixiac.equinix.core.util.ApacheUtils;
import com.eqixiac.equinix.core.util.ModelUtils;
import com.eqixiac.equinix.core.util.ResourceFileUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.time.Clock;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Low-level transport client: owns the token lifecycle, the pooled HTTP client, the resolved
 * endpoint, and the merged apiParams catalogue, and signs and dispatches every request. The public
 * facade {@link com.eqixiac.equinix.EquinixClient} and the domain clients delegate to an instance
 * of this class.
 *
 * <h2>Token sources</h2>
 * <p>A client is built over exactly one token source: OAuth2 client credentials
 * ({@link EquinixCredentialsProvider}; the facade wires the token mint through
 * {@link #setAuthenticator}), or a {@link BearerTokenProvider} that supplies ready-made tokens
 * (for example an STS-issued access-policy token). Either way the token is obtained lazily on the
 * first authenticated request and again when it expires, single-flight.</p>
 *
 * <h2>401 recovery</h2>
 * <p>When an authenticated request is answered with HTTP 401 the cached token is discarded, a new
 * one is obtained once (single-flight: concurrent requests rejected with the same token cause one
 * token request) and the request is replayed once. A second 401 is thrown. There is no replay for
 * the token request itself, for endpoints declared {@code "unauthenticated"}, or when no token
 * source is wired. Replaying is safe for every method because a 401 means the request was rejected
 * before it was processed.</p>
 *
 * @author ianjones
 */
public class EquinixClient implements Closeable {

    /**
     * The OAuth2 client-credentials source, or {@code null} when this client was built over a
     * {@link BearerTokenProvider}.
     */
    @Getter
    final private EquinixCredentialsProvider equinixCredentialsProvider;

    /**
     * The bearer-token source, or {@code null} when this client authenticates with client
     * credentials.
     */
    @Getter
    private final BearerTokenProvider bearerTokenProvider;

    /** The clock token validity is judged on; replaceable for deterministic tests. */
    private volatile Clock clock = Clock.systemUTC();

    /**
     * The merged apiParams catalogue. Published via a {@code volatile} field and only ever
     * replaced wholesale (copy-on-write in {@link #appendApiParams}) so request threads can read
     * it without locking. Deliberately has no public setter - the registry is owned by this
     * client.
     */
    @Getter
    private volatile JsonNode clientResourceFile;

    /** Guards the copy-on-write merge in {@link #appendApiParams}. */
    private final Object apiParamsLock = new Object();

    @Getter
    private volatile URI endPoint;

    /**
     * {@code true} once a caller has replaced the catalogue's default host via
     * {@link #setEndPoint(String)} (a gateway, proxy or WireMock). From then on every functional
     * area routes to that endpoint unless it has its own {@link #setDomainEndPoint explicit override},
     * because a caller-chosen endpoint must win over the catalogue's per-domain hosts.
     */
    private volatile boolean endPointOverridden;

    /** Caller-supplied per-functional-area endpoints; these always win. */
    private final java.util.concurrent.ConcurrentMap<String, URI> domainEndPoints =
            new java.util.concurrent.ConcurrentHashMap<>();

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

    /**
     * Creates a transport client over fixed OAuth2 client credentials.
     *
     * @param equinixCredentials the client credentials
     * @param isSandBoxed {@code true} for the sandbox environment; {@code false} for production
     */
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
        this(Objects.requireNonNull(credentialsProvider, "credentialsProvider"), null, isSandBoxed);
    }

    /**
     * Creates a transport client that signs requests with tokens from the given provider instead
     * of minting OAuth2 client-credentials tokens. No token-mint authenticator is needed or used:
     * the provider is asked for a token on the first authenticated call, before a known expiry,
     * and once after an HTTP 401.
     *
     * @param bearerTokenProvider supplies the bearer tokens
     * @param isSandBoxed {@code true} for the sandbox environment; {@code false} for production
     */
    public EquinixClient(BearerTokenProvider bearerTokenProvider, boolean isSandBoxed) {
        this(null, Objects.requireNonNull(bearerTokenProvider, "bearerTokenProvider"), isSandBoxed);
    }

    private EquinixClient(EquinixCredentialsProvider credentialsProvider, BearerTokenProvider bearerTokenProvider,
                          boolean isSandBoxed) {
        this.equinixCredentialsProvider = credentialsProvider;
        this.bearerTokenProvider = bearerTokenProvider;
        this.isSandBoxed = isSandBoxed;
        equinixHttpClient = new EquinixHttpClient();
        init();
    }

    /**
     * Replaces the clock used to judge token expiry and {@code Retry-After} dates (default
     * {@link Clock#systemUTC()}). Intended for deterministic tests and simulations.
     *
     * @param clock the clock; must not be {@code null}
     */
    public void setClock(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock");
        equinixHttpClient.setClock(clock);
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

            this.endPoint = toURI(hostName);
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
     * Replaces the client-wide endpoint (a gateway, proxy or WireMock); from then on it wins over
     * the catalogue's per-domain hosts unless a {@link #setDomainEndPoint domain override} is set.
     *
     * @param endPoint the host, with or without scheme
     * @throws java.lang.IllegalArgumentException if the endpoint is not a valid URI
     */
    public void setEndPoint(String endPoint) throws IllegalArgumentException {
        this.endPoint = toURI(endPoint);
        this.endPointOverridden = true;
    }

    /**
     * Routes one functional area (for example {@code "IAM"} or {@code "STS"}) to its own host,
     * overriding both the catalogue's per-domain host and {@link #setEndPoint(String)}.
     *
     * @param functionalArea the functional-area key from the apiParams catalogue
     * @param endPoint the host (with or without scheme); {@code null} removes the override
     */
    public void setDomainEndPoint(String functionalArea, String endPoint) {
        Objects.requireNonNull(functionalArea, "functionalArea");
        if (endPoint == null) {
            domainEndPoints.remove(functionalArea);
        } else {
            domainEndPoints.put(functionalArea, toURI(endPoint));
        }
    }

    /**
     * Resolves the host a functional area's requests are sent to. Precedence:
     * <ol>
     *   <li>an explicit {@link #setDomainEndPoint} override;</li>
     *   <li>the client-wide endpoint, when a caller replaced it with {@link #setEndPoint};</li>
     *   <li>the functional area's own {@code hostName} / {@code sandboxHostName} in its apiParams
     *       resource (IAM is served by {@code access.eqix.equinix.com} and STS by
     *       {@code sts.eqix.equinix.com}; {@code api.equinix.com} answers 404 for both);</li>
     *   <li>the client-wide default endpoint.</li>
     * </ol>
     *
     * @param functionalArea the functional-area key, may be {@code null}
     * @return the endpoint for that area, never {@code null}
     */
    public URI getEndPointFor(String functionalArea) {
        if (functionalArea != null) {
            URI explicit = domainEndPoints.get(functionalArea);
            if (explicit != null) {
                return explicit;
            }
            if (!endPointOverridden) {
                JsonNode area = clientResourceFile.path("functionalAreas").path(functionalArea);
                String host = area.path(isSandBoxed ? "sandboxHostName" : "hostName").textValue();
                if (host != null && !host.isBlank()) {
                    return toURI(host);
                }
            }
        }
        return endPoint;
    }

    /**
     * Overrides the automatic retry behavior for transient failures (429/5xx, transient IO).
     *
     * @param retryPolicy the policy to apply; {@link com.eqixiac.equinix.core.http.RetryPolicy#none()} disables retries
     */
    public void setRetryPolicy(RetryPolicy retryPolicy) {
        equinixHttpClient.setRetryPolicy(retryPolicy);
    }

    /**
     * Enables (or, with {@code null}, disables) the opt-in circuit breaker consulted for every
     * request attempt. See {@link com.eqixiac.equinix.core.http.CircuitBreaker} for the
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
     * @throws com.eqixiac.equinix.core.exception.EquinixClientException if the resource is
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
     * Resolves, signs and dispatches a request, with the retry policy, the circuit breaker and
     * the one-shot 401 recovery described on this class.
     *
     * @param equinixRequest the request to send
     * @param <T> the response payload type
     * @return the successful response
     * @throws com.eqixiac.equinix.core.exception.EquinixClientException if the request cannot be built or sent
     * @throws com.eqixiac.equinix.core.exception.EquinixServiceException if the API answers with an error
     */
    public <T> EquinixResponse<T> invoke(EquinixRequest<T> equinixRequest) {
        RequestAssembler.addRequestParams(equinixRequest);

        if(equinixRequest instanceof PaginatedRequest) {
            ((PaginatedRequest<T>) equinixRequest).setPagination();
        }

        equinixRequest.setQueryParameters(ModelUtils.cleanseQueryParameterList(equinixRequest.getQueryParameters()));

        OAuthToken signedWith = setStandardHeaders(equinixRequest);
        try {
            return equinixHttpClient.executeWithRetries(equinixRequest);
        }
        catch (EquinixAuthenticationException unauthorized) {
            if (!mayRecoverFrom401(equinixRequest)) {
                throw unauthorized;
            }
            refreshAfterUnauthorized(signedWith);
            // Replay exactly once with the new token; a second 401 propagates.
            setStandardHeaders(equinixRequest);
            return equinixHttpClient.executeWithRetries(equinixRequest);
        }
    }

    /**
     * A 401 is recoverable only for an authenticated request, when a token source is wired, and
     * not while this thread is itself acquiring a token (the client-credentials mint request).
     */
    private boolean mayRecoverFrom401(EquinixRequest<?> equinixRequest) {
        return hasTokenSource()
                && !Boolean.TRUE.equals(authenticating.get())
                && !isTokenMintRequest(equinixRequest)
                && !isUnauthenticatedRequest(equinixRequest);
    }

    private boolean hasTokenSource() {
        return bearerTokenProvider != null || authenticator != null;
    }

    /**
     * Discards the token a request was rejected with and obtains a new one, single-flight: if
     * another thread already replaced that token, its replacement is reused and no token request
     * is made.
     */
    private void refreshAfterUnauthorized(OAuthToken rejected) {
        synchronized (tokenLock) {
            if (oAuthToken != rejected) {
                return;
            }
            oAuthToken = null;
            acquireTokenLocked();
        }
    }

    /**
     * Obtains and publishes a token from the wired source. Caller holds {@link #tokenLock}.
     */
    private void acquireTokenLocked() {
        BearerTokenProvider provider = bearerTokenProvider;
        if (provider != null) {
            BearerToken token = provider.getToken();
            if (token == null) {
                throw new EquinixClientException("The BearerTokenProvider returned a null token.");
            }
            oAuthToken = OAuthToken.fromBearerToken(token);
            return;
        }
        Runnable auth = authenticator;
        if (auth == null) {
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
     * @return the current {@link com.eqixiac.equinix.core.model.OAuthToken}, may be {@code null}.
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
     * re-authentication on expiry or after a 401. The facade client supplies this; the core client
     * invokes it when a request needs a token and none valid is published. Ignored when this client
     * was built over a {@link BearerTokenProvider}, which then is the only token source.
     *
     * @param authenticator the token-acquisition callback (it must publish via {@link #setOAuthToken})
     */
    public void setAuthenticator(Runnable authenticator) {
        this.authenticator = authenticator;
    }

    /**
     * Forces (re)authentication from the wired token source (the client-credentials authenticator
     * or the {@link BearerTokenProvider}), single-flight under {@link #tokenLock}. This is the
     * explicit {@code authenticate()} entry point. The {@link #authenticating} guard means the
     * token request it issues (which itself flows through {@link #setStandardHeaders}) does not
     * recurse into another authentication. No-op when no token source is wired.
     */
    public void authenticate() {
        if (!hasTokenSource() || Boolean.TRUE.equals(authenticating.get())) {
            return;
        }
        synchronized (tokenLock) {
            acquireTokenLocked();
        }
    }

    /**
     * Ensures a valid token is published before a request is signed: authenticates lazily on the
     * first call and re-authenticates when the current token has expired. Single-flight under
     * {@link #tokenLock} (double-checked) so concurrent callers don't stampede the token endpoint.
     * No-op when no token source is wired, or when invoked re-entrantly from the token request
     * itself (guarded by {@link #authenticating}).
     */
    private void ensureAuthenticated() {
        if (!hasTokenSource() || Boolean.TRUE.equals(authenticating.get())) {
            return;
        }
        OAuthToken token = oAuthToken;
        if (token != null && token.validSession(clock.instant())) {
            return;
        }
        synchronized (tokenLock) {
            token = oAuthToken;
            if (token != null && token.validSession(clock.instant())) {
                return;
            }
            acquireTokenLocked();
        }
    }

    /**
     * Applies the standard headers (authorization + content type) to the request, authenticating
     * first if necessary.
     *
     * <p>{@link #ensureAuthenticated()} runs first: it authenticates lazily on the first call and
     * re-authenticates an expired token, single-flight under {@link #tokenLock}. The authorization
     * header is then derived from the published token - omitted always for the token-mint request
     * itself (see {@link #isTokenMintRequest}), which carries its credentials in the body rather
     * than a bearer header. Signing the token request with the currently published token would be
     * wrong exactly when it matters most: during re-authentication the published token is the
     * expired one.</p>
     *
     * @param equinixRequest the request to decorate.
     * @return the token the request was signed with, or {@code null} when it carries none
     */
    private <T> OAuthToken setStandardHeaders(EquinixRequest<T> equinixRequest){
        boolean tokenMintRequest = isTokenMintRequest(equinixRequest);
        boolean unauthenticated = tokenMintRequest || isUnauthenticatedRequest(equinixRequest);
        if (!unauthenticated) {
            ensureAuthenticated();
        }

        // Case-insensitive keys: HTTP header names are case-insensitive, so a caller-supplied
        // "Authorization"/"Content-Type" must be recognized as the same header as the lowercase
        // standard ones below (a case-sensitive merge would emit both, which some gateways reject).
        Map<String, String> standardHeaders = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        OAuthToken signedWith = null;
        if (!unauthenticated) {
            OAuthToken currentToken = oAuthToken;
            if (currentToken != null && currentToken.getSessionToken() != null) {
                standardHeaders.put("authorization", "Bearer " + currentToken.getSessionToken());
                signedWith = currentToken;
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
        if (unauthenticated) {
            // The token endpoint authenticates via the client_id/client_secret in the body; a
            // Bearer header must never accompany it (during re-auth it would be the expired
            // token). The map's case-insensitive comparator removes any casing variant.
            standardHeaders.remove("authorization");
        }
        equinixRequest.setHeaders(standardHeaders);
        return signedWith;
    }

    /**
     * Identifies the OAuth2 token-mint request itself, as built by
     * {@code CoreClientImpl#authenticate()} from the {@code Authentication/OAuth} entry of
     * {@code apiParams_Core.json}. That request authenticates with credentials in its body and
     * must never carry an {@code authorization} bearer header - in particular not the expired
     * token that is still published while re-authentication is in flight.
     */
    private static boolean isTokenMintRequest(EquinixRequest<?> equinixRequest) {
        return "Authentication".equals(equinixRequest.getFunctionalArea())
                && "OAuth".equals(equinixRequest.getRequestParent());
    }

    /**
     * Identifies a request to an endpoint the service serves without a bearer token, declared by
     * {@code "unauthenticated": true} on its resource in the apiParams catalogue (the STS token
     * exchange, granted-access-policy listing, JWKS and OpenID discovery). Such a request neither
     * triggers an Equinix client-credentials login nor carries an {@code authorization} header, so a
     * workload holding only an OIDC ID token can use them.
     */
    private static boolean isUnauthenticatedRequest(EquinixRequest<?> equinixRequest) {
        JsonNode area = equinixRequest.getFunctionalAreaJson();
        String parent = equinixRequest.getRequestParent();
        return area != null && parent != null
                && area.path(parent).path("unauthenticated").asBoolean(false);
    }

    @Override
    public void close() throws IOException {
        equinixHttpClient.close();
    }
}
