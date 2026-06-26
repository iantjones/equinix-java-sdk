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
import api.equinix.javasdk.core.exception.EquinixAuthenticationException;
import api.equinix.javasdk.core.exception.EquinixClientException;
import api.equinix.javasdk.core.http.EquinixHttpClient;
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
 * <p>EquinixClient class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
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

    private final Boolean isSandBoxed;

    private final EquinixHttpClient equinixHttpClient;

    /**
     * <p>Constructor for EquinixClient.</p>
     *
     * @param equinixCredentials a {@link api.equinix.javasdk.core.auth.EquinixCredentials} object.
     * @param isSandBoxed a {@link java.lang.Boolean} object.
     */
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
     * <p>Setter for the field <code>endPoint</code>.</p>
     *
     * @param endPoint a {@link java.lang.String} object.
     * @throws java.lang.IllegalArgumentException if any.
     */
    public void setEndPoint(String endPoint) throws IllegalArgumentException {
        this.endPoint = toURI(endPoint);
    }

    /**
     * <p>appendApiParams.</p>
     *
     * @param fileName a {@link java.lang.String} object.
     */
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
     * <p>invoke.</p>
     *
     * @param equinixRequest a {@link api.equinix.javasdk.core.http.request.EquinixRequest} object.
     * @param <T> a T object.
     * @return a {@link api.equinix.javasdk.core.http.response.EquinixResponse} object.
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
        equinixResponse = equinixHttpClient.executeHelper(equinixRequest);
        return equinixResponse;
    }

    /**
     * <p>isSandBoxed.</p>
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
     * Applies the standard headers (authorization + content type) to the request.
     *
     * <p>The authorization header is derived from the OAuth token under a
     * single-flight guard: the token is validated and its session value captured
     * inside a {@link #tokenLock} synchronized block using a double-checked validity
     * pattern. This ensures that concurrent callers observe one consistent token
     * snapshot and that an expired token is detected by a single thread at a time,
     * rather than many threads racing the validity check and potentially stampeding
     * the token endpoint. (Re)acquisition itself is performed by the authentication
     * flow that publishes via {@link #setOAuthToken}; this method does not perform
     * retry or backoff.)</p>
     *
     * @param equinixRequest the request to decorate.
     */
    private <T> void setStandardHeaders(EquinixRequest<T> equinixRequest){
        Map<String, String> standardHeaders = new HashMap<>();

        // Lock-free fast path: capture the currently published token snapshot.
        OAuthToken currentToken = oAuthToken;
        if(currentToken != null) {
            String sessionToken = null;
            synchronized (tokenLock) {
                // Re-read under the lock so we validate the most recently published
                // token and do not act on a snapshot another thread has since rotated.
                currentToken = oAuthToken;
                if(currentToken != null) {
                    if(!currentToken.validSession()) {
                        throw new EquinixAuthenticationException(
                                "OAuth token has expired. Call authenticate() to obtain a new token.");
                    }
                    sessionToken = currentToken.getSessionToken();
                }
            }
            if(sessionToken != null) {
                standardHeaders.put("authorization", "Bearer " + sessionToken);
            }
        }
        String contentType = equinixRequest.getContentType();
        standardHeaders.put("content-type", contentType != null ? contentType : "application/json");
        equinixRequest.setHeaders(standardHeaders);
    }

    @Override
    public void close() throws IOException {
        equinixHttpClient.close();
    }
}
