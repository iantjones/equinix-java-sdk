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

    @Getter @Setter
    private OAuthToken oAuthToken;

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

    private <T> void setStandardHeaders(EquinixRequest<T> equinixRequest){
        Map<String, String> standardHeaders = new HashMap<>();
        if(oAuthToken != null) {
            if(!oAuthToken.validSession()) {
                throw new EquinixAuthenticationException(
                        "OAuth token has expired. Call authenticate() to obtain a new token.");
            }
            standardHeaders.put("authorization", "Bearer " + oAuthToken.getSessionToken());
        }
        standardHeaders.put("content-type", "application/json");
        equinixRequest.setHeaders(standardHeaders);
    }

    @Override
    public void close() throws IOException {
        equinixHttpClient.close();
    }
}
