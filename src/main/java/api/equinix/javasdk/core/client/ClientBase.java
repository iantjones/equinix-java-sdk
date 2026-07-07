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
import api.equinix.javasdk.core.http.RequestAssembler;
import api.equinix.javasdk.core.http.ResponseHandler;
import api.equinix.javasdk.core.http.SerializationHelper;
import api.equinix.javasdk.core.http.request.EquinixRequest;
import api.equinix.javasdk.core.http.response.EquinixResponse;
import api.equinix.javasdk.core.internal.Constants;
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 *
 * @author ianjones
 */
public class ClientBase {

    private final Config configClient;

    private final String functionalArea;

    private final String requestParent;

    protected ClientBase(Config configClient, String functionalArea, String requestParent) {
        this.configClient = configClient;
        this.functionalArea = functionalArea;
        this.requestParent = requestParent;
    }

    protected Config getConfigClient() {
        return this.configClient;
    }

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
            Objects.requireNonNull(requestType, "withType(RequestType) must be called before build()");
            return RequestAssembler.buildRequest(functionalArea, requestParent,
                    serviceEndpoint, requestType, configClient.getEquinixClient(),
                    pathParams, queryParams, typeRef);
        }
    }

    // -----------------------------------------------------------------------
    // Request-build helpers: the response type is derived from the resource's JSON
    // class (no hand-declared TypeReference needed). See RequestAssembler.deriveResponseType.
    // Requests that genuinely need a hand-declared TypeReference (generic response
    // envelopes, secondary types) use the fluent builder's withTypeRef(...) instead —
    // the old String+TypeReference buildRequest overloads have been removed.
    // -----------------------------------------------------------------------

    protected <T> EquinixRequest<T> buildRequest(String serviceEndpoint, RequestType requestType, Class<?> jsonClass) {
        return RequestAssembler.buildRequest(this.functionalArea, this.requestParent,
                serviceEndpoint, requestType, this.configClient.getEquinixClient(), null, null, jsonClass);
    }

    protected <T> EquinixRequest<T> buildRequestWithPathParams(String serviceEndpoint, RequestType requestType,
                                                               Map<String, String> pathParams, Class<?> jsonClass) {
        return RequestAssembler.buildRequest(this.functionalArea, this.requestParent,
                serviceEndpoint, requestType, this.configClient.getEquinixClient(), pathParams, null, jsonClass);
    }

    protected <T> EquinixRequest<T> buildRequestWithQueryParams(String serviceEndpoint, RequestType requestType,
                                                                Map<String, List<String>> queryParams, Class<?> jsonClass) {
        return RequestAssembler.buildRequest(this.functionalArea, this.requestParent,
                serviceEndpoint, requestType, this.configClient.getEquinixClient(), null, queryParams, jsonClass);
    }

    protected <T> EquinixRequest<T> buildRequest(String serviceEndpoint, RequestType requestType,
                                                 Map<String, String> pathParams, Map<String, List<String>> queryParams,
                                                 Class<?> jsonClass) {
        return RequestAssembler.buildRequest(this.functionalArea, this.requestParent,
                serviceEndpoint, requestType, this.configClient.getEquinixClient(), pathParams, queryParams, jsonClass);
    }

    // -----------------------------------------------------------------------
    // Generic operation helpers — build → (serialize) → invoke → handle, with the
    // response type given explicitly. Available to ALL clients (not just CRUD ones)
    // so non-paginated / secondary-type endpoints (health, statistics, pricing,
    // agreements, legacy environment/power, bulk lists, action results) collapse to
    // one-liners without forcing a model/wrapper that does not exist.
    // -----------------------------------------------------------------------

    protected <R> R getAs(String serviceEndpoint, Class<R> responseType) {
        return getAs(serviceEndpoint, null, null, responseType);
    }

    protected <R> R getAs(String serviceEndpoint, Map<String, String> pathParams,
                          Map<String, List<String>> queryParams, Class<R> responseType) {
        EquinixRequest<R> request = buildRequest(serviceEndpoint, RequestType.SINGLE, pathParams, queryParams, responseType);
        return ResponseHandler.handleSingletonResponse(invoke(request), request);
    }

    protected <R> R postAs(String serviceEndpoint, Object body, Class<R> responseType) {
        EquinixRequest<R> request = buildRequest(serviceEndpoint, RequestType.SINGLE, responseType);
        if (body != null) {
            SerializationHelper.serializeJson(request, body);
        }
        return ResponseHandler.handleSingletonResponse(invoke(request), request);
    }

    protected <R> R postForType(String serviceEndpoint, Object body, TypeReference<?> typeReference) {
        return postForType(serviceEndpoint, null, null, body, typeReference);
    }

    protected <R> List<R> listAs(String serviceEndpoint, Map<String, String> pathParams,
                                 Map<String, List<String>> queryParams, Class<R> elementType) {
        EquinixRequest<R> request = buildRequest(serviceEndpoint, RequestType.LIST, pathParams, queryParams, elementType);
        return ResponseHandler.handleListResponse(invoke(request), request);
    }

    protected <R> List<R> postListAs(String serviceEndpoint, Object body, Class<R> elementType) {
        EquinixRequest<R> request = buildRequest(serviceEndpoint, RequestType.LIST, elementType);
        if (body != null) {
            SerializationHelper.serializeJson(request, body);
        }
        return ResponseHandler.handleListResponse(invoke(request), request);
    }

    protected Map<String, String> mapOp(String serviceEndpoint, RequestType requestType, Map<String, String> pathParams,
                                        Map<String, List<String>> queryParams, Object body) {
        EquinixRequest<Object> request = buildRequest(serviceEndpoint, requestType, pathParams, queryParams, Object.class);
        if (body != null) {
            SerializationHelper.serializeJson(request, body);
        }
        return ResponseHandler.handleMapResponse(request, invoke(request));
    }

    protected boolean booleanOp(String serviceEndpoint, RequestType requestType, Map<String, String> pathParams,
                                Map<String, List<String>> queryParams, Object body) {
        EquinixRequest<Object> request = buildRequest(serviceEndpoint, requestType, pathParams, queryParams, Object.class);
        if (body != null) {
            SerializationHelper.serializeJson(request, body);
        }
        return ResponseHandler.handleBooleanResponse(invoke(request), request);
    }

    protected String stringOp(String serviceEndpoint, RequestType requestType, Map<String, String> pathParams,
                              Map<String, List<String>> queryParams, Object body) {
        EquinixRequest<Object> request = buildRequest(serviceEndpoint, requestType, pathParams, queryParams, Object.class);
        if (body != null) {
            SerializationHelper.serializeJson(request, body);
        }
        return ResponseHandler.handleStringResponse(invoke(request));
    }

    protected byte[] bytesOp(String serviceEndpoint, Map<String, String> pathParams, Map<String, List<String>> queryParams) {
        EquinixRequest<Object> request = buildRequest(serviceEndpoint, RequestType.SINGLE, pathParams, queryParams, Object.class);
        return ResponseHandler.handleByteResponse(invoke(request));
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
            SerializationHelper.serializeJson(request, body);
        }
        ResponseHandler.handleBooleanResponse(invoke(request), request);
    }

    protected <R> R postForType(String serviceEndpoint, Map<String, String> pathParams, Object body, TypeReference<?> typeReference) {
        return postForType(serviceEndpoint, pathParams, null, body, typeReference);
    }

    protected <R> R postForType(String serviceEndpoint, Map<String, String> pathParams,
                                Map<String, List<String>> queryParams, Object body, TypeReference<?> typeReference) {
        EquinixRequest<R> request = this.newRequest(serviceEndpoint).withType(RequestType.SINGLE)
                .withPathParams(pathParams).withQueryParams(queryParams).withTypeRef(typeReference).build();
        if (body != null) {
            SerializationHelper.serializeJson(request, body);
        }
        return ResponseHandler.handleSingletonResponse(invoke(request), request);
    }

    /**
     * POST a body, then return the created resource's uuid parsed from the response {@code Location}
     * header (for endpoints that return {@code 201 Location: .../{uuid}} rather than a body). Pair
     * with {@code getByUuid(...)} to return the created resource.
     */
    protected String createReturningLocationUuid(String serviceEndpoint, Map<String, String> pathParams,
                                                 Map<String, List<String>> queryParams, Object body) {
        EquinixRequest<Object> request = buildRequest(serviceEndpoint, RequestType.SINGLE, pathParams, queryParams, Object.class);
        if (body != null) {
            SerializationHelper.serializeJson(request, body);
        }
        return ResponseHandler.extractFromHeader(invoke(request), "Location", Constants.UUID_PATTERN);
    }
}
