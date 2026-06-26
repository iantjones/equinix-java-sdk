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
import api.equinix.javasdk.core.http.response.Page;
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

    // -----------------------------------------------------------------------
    // Fluent request builder
    // -----------------------------------------------------------------------

    /**
     * Creates a fluent request builder for the given service endpoint.
     *
     * <pre>{@code
     * EquinixRequest<Connection> request = this.newRequest("GetConnection")
     *     .withType(RequestType.SINGLE)
     *     .withPathParams(Map.of("uuid", uuid))
     *     .withTypeRef(ConnectionJson.getSingleTypeRef())
     *     .build();
     * }</pre>
     *
     * @param serviceEndpoint the service endpoint name from the apiParams JSON
     * @return a new {@link EquinixRequestBuilder}
     */
    protected EquinixRequestBuilder newRequest(String serviceEndpoint) {
        return new EquinixRequestBuilder(serviceEndpoint);
    }

    /**
     * Fluent builder for constructing {@link EquinixRequest} instances.
     * Replaces the multiple overloaded buildRequest methods with a single,
     * readable builder chain.
     */
    protected class EquinixRequestBuilder {
        private final String serviceEndpoint;
        private RequestType requestType;
        private Map<String, String> pathParams;
        private Map<String, List<String>> queryParams;
        private TypeReference<?> typeRef;

        private EquinixRequestBuilder(String serviceEndpoint) {
            this.serviceEndpoint = serviceEndpoint;
        }

        public EquinixRequestBuilder withType(RequestType requestType) {
            this.requestType = requestType;
            return this;
        }

        public EquinixRequestBuilder withPathParams(Map<String, String> pathParams) {
            this.pathParams = pathParams;
            return this;
        }

        public EquinixRequestBuilder withQueryParams(Map<String, List<String>> queryParams) {
            this.queryParams = queryParams;
            return this;
        }

        public EquinixRequestBuilder withTypeRef(TypeReference<?> typeRef) {
            this.typeRef = typeRef;
            return this;
        }

        public <T> EquinixRequest<T> build() {
            return Utils.buildRequest(functionalArea, requestParent,
                    serviceEndpoint, requestType, configClient.getEquinixClient(),
                    pathParams, queryParams, typeRef);
        }
    }

    // -----------------------------------------------------------------------
    // Legacy convenience methods (delegate to the core buildRequest)
    // -----------------------------------------------------------------------

    protected  <T> EquinixRequest<T> buildRequestWithPathParams(String serviceEndpoint, RequestType requestType, Map<String, String> pathParams) {
        return buildRequest(serviceEndpoint, requestType, pathParams, null, (TypeReference<?>) null);
    }

    protected  <T> EquinixRequest<T> buildRequestWithQueryParams(String serviceEndpoint, RequestType requestType, Map<String, List<String>> queryParams) {
        return buildRequest(serviceEndpoint, requestType, null, queryParams, (TypeReference<?>) null);
    }

    protected  <T> EquinixRequest<T> buildRequest(String serviceEndpoint, RequestType requestType, Map<String, String> pathParams, Map<String, List<String>> queryParams) {
        return buildRequest(serviceEndpoint, requestType, pathParams, queryParams, (TypeReference<?>) null);
    }

    protected  <T> EquinixRequest<T> buildRequest(String serviceEndpoint, RequestType requestType, TypeReference<?> typeRef) {
        return buildRequest(serviceEndpoint, requestType, null, null, typeRef);
    }

    protected  <T> EquinixRequest<T> buildRequest(String serviceEndpoint, RequestType requestType) {
        return buildRequest(serviceEndpoint, requestType, null, null, (TypeReference<?>) null);
    }

    protected  <T> EquinixRequest<T> buildRequestWithPathParams(String serviceEndpoint, RequestType requestType, Map<String, String> pathParams, TypeReference<?> typeRef) {
        return buildRequest(serviceEndpoint, requestType, pathParams, null, typeRef);
    }

    protected  <T> EquinixRequest<T> buildRequestWithQueryParams(String serviceEndpoint, RequestType requestType, Map<String, List<String>> queryParams, TypeReference<?> typeRef) {
        return buildRequest(serviceEndpoint, requestType, null, queryParams, typeRef);
    }

    protected  <T> EquinixRequest<T> buildRequest(String serviceEndpoint, RequestType requestType,
                                                  Map<String, String> pathParams, Map<String, List<String>> queryParams,
                                                  TypeReference<?> typeRef) {
        return Utils.buildRequest(this.functionalArea, this.requestParent,
                serviceEndpoint, requestType, this.configClient.getEquinixClient(), pathParams, queryParams, typeRef);
    }

    // -----------------------------------------------------------------------
    // Derived-type variants: response type is inferred from the resource's JSON
    // class (no hand-declared TypeReference needed). See Utils.deriveResponseType.
    // -----------------------------------------------------------------------

    protected <T> EquinixRequest<T> buildRequest(String serviceEndpoint, RequestType requestType, Class<?> jsonClass) {
        return Utils.buildRequest(this.functionalArea, this.requestParent,
                serviceEndpoint, requestType, this.configClient.getEquinixClient(), null, null, jsonClass);
    }

    protected <T> EquinixRequest<T> buildRequestWithPathParams(String serviceEndpoint, RequestType requestType,
                                                               Map<String, String> pathParams, Class<?> jsonClass) {
        return Utils.buildRequest(this.functionalArea, this.requestParent,
                serviceEndpoint, requestType, this.configClient.getEquinixClient(), pathParams, null, jsonClass);
    }

    protected <T> EquinixRequest<T> buildRequestWithQueryParams(String serviceEndpoint, RequestType requestType,
                                                                Map<String, List<String>> queryParams, Class<?> jsonClass) {
        return Utils.buildRequest(this.functionalArea, this.requestParent,
                serviceEndpoint, requestType, this.configClient.getEquinixClient(), null, queryParams, jsonClass);
    }

    protected <T> EquinixRequest<T> buildRequest(String serviceEndpoint, RequestType requestType,
                                                 Map<String, String> pathParams, Map<String, List<String>> queryParams,
                                                 Class<?> jsonClass) {
        return Utils.buildRequest(this.functionalArea, this.requestParent,
                serviceEndpoint, requestType, this.configClient.getEquinixClient(), pathParams, queryParams, jsonClass);
    }

    // -----------------------------------------------------------------------
    // Generic operation helpers — build → (serialize) → invoke → handle, with the
    // response type given explicitly. Available to ALL clients (not just CRUD ones)
    // so non-paginated / secondary-type endpoints (health, statistics, pricing,
    // agreements, legacy environment/power, bulk lists, action results) collapse to
    // one-liners without forcing a model/wrapper that does not exist.
    // -----------------------------------------------------------------------

    /** GET a single response of an explicit type. */
    protected <R> R getAs(String serviceEndpoint, Class<R> responseType) {
        return getAs(serviceEndpoint, null, null, responseType);
    }

    /** GET a single response of an explicit type, addressed by path and/or query parameters. */
    protected <R> R getAs(String serviceEndpoint, Map<String, String> pathParams,
                          Map<String, List<String>> queryParams, Class<R> responseType) {
        EquinixRequest<R> request = buildRequest(serviceEndpoint, RequestType.SINGLE, pathParams, queryParams, responseType);
        return Utils.handleSingletonResponse(invoke(request), request);
    }

    /** POST a body and read a single response of an explicit type. */
    protected <R> R postAs(String serviceEndpoint, Object body, Class<R> responseType) {
        EquinixRequest<R> request = buildRequest(serviceEndpoint, RequestType.SINGLE, responseType);
        if (body != null) {
            Utils.serializeJson(request, body);
        }
        return Utils.handleSingletonResponse(invoke(request), request);
    }

    /** POST a body and deserialize a response of an explicit {@link TypeReference} (e.g. a bulk {@code List<M>}). */
    protected <R> R postForType(String serviceEndpoint, Object body, TypeReference<?> typeReference) {
        EquinixRequest<R> request = buildRequest(serviceEndpoint, RequestType.SINGLE, typeReference);
        if (body != null) {
            Utils.serializeJson(request, body);
        }
        return Utils.handleSingletonResponse(invoke(request), request);
    }

    /** GET a non-paginated list of an explicit element type. */
    protected <R> List<R> listAs(String serviceEndpoint, Map<String, String> pathParams,
                                 Map<String, List<String>> queryParams, Class<R> elementType) {
        EquinixRequest<R> request = buildRequest(serviceEndpoint, RequestType.LIST, pathParams, queryParams, elementType);
        return Utils.handleListResponse(invoke(request), request);
    }

    /** POST a body and read a non-paginated list of an explicit element type. */
    protected <R> List<R> postListAs(String serviceEndpoint, Object body, Class<R> elementType) {
        EquinixRequest<R> request = buildRequest(serviceEndpoint, RequestType.LIST, elementType);
        if (body != null) {
            Utils.serializeJson(request, body);
        }
        return Utils.handleListResponse(invoke(request), request);
    }

    /** Execute a request whose response is a flat {@code Map<String,String>} (e.g. terms/options endpoints). */
    protected Map<String, String> mapOp(String serviceEndpoint, RequestType requestType, Map<String, String> pathParams,
                                        Map<String, List<String>> queryParams, Object body) {
        EquinixRequest<Object> request = buildRequest(serviceEndpoint, requestType, pathParams, queryParams, Object.class);
        if (body != null) {
            Utils.serializeJson(request, body);
        }
        return Utils.handleMapResponse(request, invoke(request));
    }

    /** Execute a request whose success is conveyed only by the HTTP status (returns {@code true} on 2xx). */
    protected boolean booleanOp(String serviceEndpoint, RequestType requestType, Map<String, String> pathParams,
                                Map<String, List<String>> queryParams, Object body) {
        EquinixRequest<Object> request = buildRequest(serviceEndpoint, requestType, pathParams, queryParams, Object.class);
        if (body != null) {
            Utils.serializeJson(request, body);
        }
        return Utils.handleBooleanResponse(invoke(request), request);
    }

    /** Execute a request whose response body is returned verbatim as a string. */
    protected String stringOp(String serviceEndpoint, RequestType requestType, Map<String, String> pathParams,
                              Map<String, List<String>> queryParams, Object body) {
        EquinixRequest<Object> request = buildRequest(serviceEndpoint, requestType, pathParams, queryParams, Object.class);
        if (body != null) {
            Utils.serializeJson(request, body);
        }
        return Utils.handleStringResponse(invoke(request));
    }

    /** GET a binary response (e.g. a document download). */
    protected byte[] bytesOp(String serviceEndpoint, Map<String, String> pathParams, Map<String, List<String>> queryParams) {
        EquinixRequest<Object> request = buildRequest(serviceEndpoint, RequestType.SINGLE, pathParams, queryParams, Object.class);
        return Utils.handleByteResponse(invoke(request));
    }

    /**
     * Execute a request for its side effect only — the response body is validated (throws on API
     * error) and discarded. Useful for update/action endpoints whose result is then re-fetched
     * (e.g. {@code voidOp("UpdateX", SINGLE, pathParams, null, body); return getByUuid(uuid);}).
     */
    protected void voidOp(String serviceEndpoint, RequestType requestType, Map<String, String> pathParams,
                          Map<String, List<String>> queryParams, Object body) {
        EquinixRequest<Object> request = buildRequest(serviceEndpoint, requestType, pathParams, queryParams, Object.class);
        if (body != null) {
            Utils.serializeJson(request, body);
        }
        Utils.handleBooleanResponse(invoke(request), request);
    }

    /** POST a body under path parameters and deserialize a response of an explicit {@link TypeReference}. */
    protected <R> R postForType(String serviceEndpoint, Map<String, String> pathParams, Object body, TypeReference<?> typeReference) {
        EquinixRequest<R> request = buildRequestWithPathParams(serviceEndpoint, RequestType.SINGLE, pathParams, typeReference);
        if (body != null) {
            Utils.serializeJson(request, body);
        }
        return Utils.handleSingletonResponse(invoke(request), request);
    }
}
