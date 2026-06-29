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
import api.equinix.javasdk.core.auth.EquinixStaticCredentialsProvider;
import api.equinix.javasdk.core.exception.EquinixClientException;
import api.equinix.javasdk.core.http.EquinixHttpClient;
import api.equinix.javasdk.core.http.RetryPolicy;
import api.equinix.javasdk.core.http.Utils;
import api.equinix.javasdk.core.http.request.EquinixRequest;
import api.equinix.javasdk.core.http.request.PaginatedRequest;
import api.equinix.javasdk.core.http.response.EquinixResponse;
import api.equinix.javasdk.core.model.OAuthToken;
import api.equinix.javasdk.core.enums.Protocol;
import api.equinix.javasdk.core.util.ApacheUtils;
import api.equinix.javasdk.core.util.ModelUtils;
import api.equinix.javasdk.core.util.ResourceFileUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import lombok.Setter;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.client.utils.URIUtils;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

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
    final private EquinixStaticCredentialsProvider equinixCredentialsProvider;

    @Getter @Setter
    private JsonNode clientResourceFile;

    @Getter
    private URI endPoint;

    @Getter
    private HttpHost httpHost;

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

    private final Boolean isSandBoxed;

    private final EquinixHttpClient equinixHttpClient;

    public EquinixClient(EquinixCredentials equinixCredentials, Boolean isSandBoxed) {
        this.equinixCredentialsProvider = new EquinixStaticCredentialsProvider(equinixCredentials);
        this.isSandBoxed = isSandBoxed;

        this.equinixCredentialsProvider.setCredentials(AuthScope.ANY, equinixCredentials);
        equinixHttpClient = new EquinixHttpClient();
        init();
    }

    private void init() throws EquinixClientException {
        try {
            String hostName;
            String hostNameLookup;

            String coreParams = "json/apiParams_Core.json";
            clientResourceFile = ResourceFileUtils.loadResourceFileJson(coreParams);

            hostNameLookup = isSandBoxed() ? "sandboxHostName" : "hostName";
            hostName = clientResourceFile.path("coreConfig").path(hostNameLookup).textValue();

            httpHost = URIUtils.extractHost(new URI(hostName));
            endPoint = new URI(hostName);
            setEndPoint(hostName);
        }
        catch (Exception e) {
            throw new EquinixClientException("Unable to initialize the EquinixClient with necessary JSON configuration.", e);
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

    public void appendApiParams(String fileName) throws EquinixClientException {
        try {
            JsonNode additionalResourceFile = ResourceFileUtils.loadResourceFileJson(fileName);

            assert additionalResourceFile != null;
            Iterator<Entry<String, JsonNode>> functionalAreasFields = additionalResourceFile.get("functionalAreas").fields();
            JsonNode coreFunctionalAreas = clientResourceFile.get("functionalAreas");

            while(functionalAreasFields.hasNext()) {
                Entry<String, JsonNode> functionalAreaField = functionalAreasFields.next();
                ((ObjectNode) coreFunctionalAreas).set(functionalAreaField.getKey(), functionalAreaField.getValue());
            }
        }
        catch (Exception e) {
            throw new EquinixClientException("Unable to append the EquinixClient with necessary JSON configuration.", e);
        }
    }

    /**
     *
     * @throws api.equinix.javasdk.core.exception.EquinixClientException if any.
     */
    public <T> EquinixResponse<T> invoke(EquinixRequest<T> equinixRequest) {

        EquinixResponse<T> equinixResponse;
        Utils.addRequestParams(equinixRequest);

        if(equinixRequest instanceof PaginatedRequest) {
            ((PaginatedRequest<T>) equinixRequest).setPagination();
        }

        equinixRequest.setQueryParameters(ModelUtils.cleanseQueryParameterList(equinixRequest.getQueryParameters()));

        try {
            if (equinixRequest.getHttpEntity() != null) {
                equinixRequest.setContent(equinixRequest.getHttpEntity().getContent());
            }
        }
        catch (IOException ioe) {
            throw new EquinixClientException(ioe);
        }

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
                authenticating.set(Boolean.FALSE);
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
                authenticating.set(Boolean.FALSE);
            }
        }
    }

    /**
     * Applies the standard headers (authorization + content type) to the request, authenticating
     * first if necessary.
     *
     * <p>{@link #ensureAuthenticated()} runs first: it authenticates lazily on the first call and
     * re-authenticates an expired token, single-flight under {@link #tokenLock}. The authorization
     * header is then derived from the published token — omitted only for the token request itself,
     * which carries its credentials in the body rather than a bearer header.</p>
     *
     * @param equinixRequest the request to decorate.
     */
    private <T> void setStandardHeaders(EquinixRequest<T> equinixRequest){
        ensureAuthenticated();

        Map<String, String> standardHeaders = new HashMap<>();
        OAuthToken currentToken = oAuthToken;
        if (currentToken != null && currentToken.getSessionToken() != null) {
            standardHeaders.put("authorization", "Bearer " + currentToken.getSessionToken());
        }
        String contentType = equinixRequest.getContentType();
        standardHeaders.put("content-type", contentType != null ? contentType : "application/json");
        // Preserve any headers already set on the request (e.g. via addHeader) rather than
        // wholesale-replacing the map; the standard auth/content-type headers take precedence.
        Map<String, String> existingHeaders = equinixRequest.getHeaders();
        if (existingHeaders != null) {
            existingHeaders.forEach(standardHeaders::putIfAbsent);
        }
        equinixRequest.setHeaders(standardHeaders);
    }

    @Override
    public void close() throws IOException {
        equinixHttpClient.close();
    }
}
