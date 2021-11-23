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

import api.equinix.javasdk.core.enums.RequestType;
import api.equinix.javasdk.core.http.Utils;
import api.equinix.javasdk.core.http.request.EquinixRequest;
import api.equinix.javasdk.core.http.response.EquinixResponse;
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.List;
import java.util.Map;

/**
 * <p>ClientBase class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
public class ClientBase {

    private final Config configClient;

    private final String functionalArea;

    private final String requestParent;

    /**
     * <p>Constructor for ClientBase.</p>
     *
     * @param configClient a {@link api.equinix.javasdk.core.client.Config} object.
     * @param functionalArea a {@link java.lang.String} object.
     * @param requestParent a {@link java.lang.String} object.
     */
    protected ClientBase(Config configClient, String functionalArea, String requestParent) {
        this.configClient = configClient;
        this.functionalArea = functionalArea;
        this.requestParent = requestParent;
    }

    /**
     * <p>Getter for the field <code>configClient</code>.</p>
     *
     * @return a {@link api.equinix.javasdk.core.client.Config} object.
     */
    protected Config getConfigClient() {
        return this.configClient;
    }

    /**
     * <p>invoke.</p>
     *
     * @param equinixRequest a {@link api.equinix.javasdk.core.http.request.EquinixRequest} object.
     * @param <T> a T object.
     * @return a {@link api.equinix.javasdk.core.http.response.EquinixResponse} object.
     */
    protected <T> EquinixResponse<T> invoke(EquinixRequest<T> equinixRequest) {
        return this.getConfigClient().getEquinixClient().invoke(equinixRequest);
    }

    /**
     * <p>buildRequestWithPathParams.</p>
     *
     * @param serviceEndpoint a {@link java.lang.String} object.
     * @param requestType a {@link api.equinix.javasdk.core.enums.RequestType} object.
     * @param pathParams a {@link java.util.Map} object.
     * @param <T> a T object.
     * @return a {@link api.equinix.javasdk.core.http.request.EquinixRequest} object.
     */
    protected  <T> EquinixRequest<T> buildRequestWithPathParams(String serviceEndpoint, RequestType requestType, Map<String, String> pathParams) {
        return buildRequest(serviceEndpoint, requestType, pathParams, null, null);
    }

    /**
     * <p>buildRequestWithQueryParams.</p>
     *
     * @param serviceEndpoint a {@link java.lang.String} object.
     * @param requestType a {@link api.equinix.javasdk.core.enums.RequestType} object.
     * @param queryParams a {@link java.util.Map} object.
     * @param <T> a T object.
     * @return a {@link api.equinix.javasdk.core.http.request.EquinixRequest} object.
     */
    protected  <T> EquinixRequest<T> buildRequestWithQueryParams(String serviceEndpoint, RequestType requestType, Map<String, List<String>> queryParams) {
        return buildRequest(serviceEndpoint, requestType, null, queryParams, null);
    }

    /**
     * <p>buildRequest.</p>
     *
     * @param serviceEndpoint a {@link java.lang.String} object.
     * @param requestType a {@link api.equinix.javasdk.core.enums.RequestType} object.
     * @param pathParams a {@link java.util.Map} object.
     * @param queryParams a {@link java.util.Map} object.
     * @param <T> a T object.
     * @return a {@link api.equinix.javasdk.core.http.request.EquinixRequest} object.
     */
    protected  <T> EquinixRequest<T> buildRequest(String serviceEndpoint, RequestType requestType, Map<String, String> pathParams, Map<String, List<String>> queryParams) {
        return buildRequest(serviceEndpoint, requestType, pathParams, queryParams, null);
    }

    /**
     * <p>buildRequest.</p>
     *
     * @param serviceEndpoint a {@link java.lang.String} object.
     * @param requestType a {@link api.equinix.javasdk.core.enums.RequestType} object.
     * @param typeRef a {@link com.fasterxml.jackson.core.type.TypeReference} object.
     * @param <T> a T object.
     * @return a {@link api.equinix.javasdk.core.http.request.EquinixRequest} object.
     */
    protected  <T> EquinixRequest<T> buildRequest(String serviceEndpoint, RequestType requestType, TypeReference<?> typeRef) {
        return buildRequest(serviceEndpoint, requestType, null, null, typeRef);
    }

    /**
     * <p>buildRequest.</p>
     *
     * @param serviceEndpoint a {@link java.lang.String} object.
     * @param requestType a {@link api.equinix.javasdk.core.enums.RequestType} object.
     * @param <T> a T object.
     * @return a {@link api.equinix.javasdk.core.http.request.EquinixRequest} object.
     */
    protected  <T> EquinixRequest<T> buildRequest(String serviceEndpoint, RequestType requestType) {
        return buildRequest(serviceEndpoint, requestType, null, null, null);
    }

    /**
     * <p>buildRequestWithPathParams.</p>
     *
     * @param serviceEndpoint a {@link java.lang.String} object.
     * @param requestType a {@link api.equinix.javasdk.core.enums.RequestType} object.
     * @param pathParams a {@link java.util.Map} object.
     * @param typeRef a {@link com.fasterxml.jackson.core.type.TypeReference} object.
     * @param <T> a T object.
     * @return a {@link api.equinix.javasdk.core.http.request.EquinixRequest} object.
     */
    protected  <T> EquinixRequest<T> buildRequestWithPathParams(String serviceEndpoint, RequestType requestType, Map<String, String> pathParams, TypeReference<?> typeRef) {
        return buildRequest(serviceEndpoint, requestType, pathParams, null, typeRef);
    }

    /**
     * <p>buildRequestWithQueryParams.</p>
     *
     * @param serviceEndpoint a {@link java.lang.String} object.
     * @param requestType a {@link api.equinix.javasdk.core.enums.RequestType} object.
     * @param queryParams a {@link java.util.Map} object.
     * @param typeRef a {@link com.fasterxml.jackson.core.type.TypeReference} object.
     * @param <T> a T object.
     * @return a {@link api.equinix.javasdk.core.http.request.EquinixRequest} object.
     */
    protected  <T> EquinixRequest<T> buildRequestWithQueryParams(String serviceEndpoint, RequestType requestType, Map<String, List<String>> queryParams, TypeReference<?> typeRef) {
        return buildRequest(serviceEndpoint, requestType, null, queryParams, typeRef);
    }

    /**
     * <p>buildRequest.</p>
     *
     * @param serviceEndpoint a {@link java.lang.String} object.
     * @param requestType a {@link api.equinix.javasdk.core.enums.RequestType} object.
     * @param pathParams a {@link java.util.Map} object.
     * @param queryParams a {@link java.util.Map} object.
     * @param typeRef a {@link com.fasterxml.jackson.core.type.TypeReference} object.
     * @param <T> a T object.
     * @return a {@link api.equinix.javasdk.core.http.request.EquinixRequest} object.
     */
    protected  <T> EquinixRequest<T> buildRequest(String serviceEndpoint, RequestType requestType,
                                                  Map<String, String> pathParams, Map<String, List<String>> queryParams,
                                                  TypeReference<?> typeRef) {
        return Utils.buildRequest(this.functionalArea, this.requestParent,
                serviceEndpoint, requestType, this.configClient.getEquinixClient(), pathParams, queryParams, typeRef);
    }
}
