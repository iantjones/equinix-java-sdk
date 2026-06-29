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
import api.equinix.javasdk.core.http.request.PaginatedPostRequest;
import api.equinix.javasdk.core.http.request.PaginatedRequest;
import api.equinix.javasdk.core.http.request.PatchOperation;
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.core.http.response.PageablePost;
import api.equinix.javasdk.core.http.response.PaginatedFilteredList;
import api.equinix.javasdk.core.http.response.PaginatedList;
import com.fasterxml.jackson.databind.ser.FilterProvider;

import java.util.List;
import java.util.Map;

/**
 * Generic base for internal resource clients, collapsing the request-build → invoke →
 * response-handle boilerplate that was previously copy-pasted across every {@code *ClientImpl}.
 *
 * <p>Parameterized over the public model type {@code M} and its JSON model {@code J}. The
 * response {@code TypeReference} is derived from {@code J} (see
 * {@link api.equinix.javasdk.core.http.Utils#deriveResponseType}), and the paging
 * {@code nextPage(...)} methods are provided once here. Subclasses supply the JSON class and a
 * {@link #wrap(Object)} factory (which binds {@code this} client for refresh/paging) and then
 * implement each operation as a one-line call to the protected helpers below, passing only the
 * service-endpoint name.</p>
 *
 * <pre>{@code
 * public class ServiceProfileClientImpl extends ResourceClientBase<ServiceProfile, ServiceProfileJson>
 *         implements ServiceProfileClient<ServiceProfile> {
 *     public ServiceProfileClientImpl(FabricConfigImpl c) {
 *         super(c, "Fabric", "ServiceProfiles", ServiceProfileJson.class);
 *     }
 *     protected ServiceProfile wrap(ServiceProfileJson j) { return new ServiceProfileWrapper(j, this); }
 *     public Page<ServiceProfile, ServiceProfileJson> list() { return listPage("ListServiceProfiles"); }
 *     public ServiceProfileJson getByUuid(String uuid)      { return getOne("GetServiceProfile", uuid); }
 *     // ...
 * }
 * }</pre>
 *
 * @param <M> the public model type
 * @param <J> the JSON model type
 * @author ianjones
 */
public abstract class ResourceClientBase<M, J> extends ClientBase implements PageablePost<M> {

    private final Class<J> jsonClass;

    protected ResourceClientBase(Config configClient, String functionalArea, String requestParent, Class<J> jsonClass) {
        super(configClient, functionalArea, requestParent);
        this.jsonClass = jsonClass;
    }

    /**
     * Wraps a JSON model into its public model type, binding this client so the returned object
     * can refresh/delete/page itself.
     *
     * @param json the deserialized JSON model
     * @return the public model wrapper
     */
    protected abstract M wrap(J json);

    // ---- standard operation helpers (endpoint name is the only per-call input) ----

    protected Page<M, J> listPage(String serviceEndpoint) {
        EquinixRequest<M> request = buildRequest(serviceEndpoint, RequestType.PAGINATED, jsonClass);
        return Utils.handlePaginatedListResponse(invoke(request), request);
    }

    protected Page<M, J> listPage(String serviceEndpoint, Map<String, java.util.List<String>> queryParams) {
        EquinixRequest<M> request = buildRequestWithQueryParams(serviceEndpoint, RequestType.PAGINATED, queryParams, jsonClass);
        return Utils.handlePaginatedListResponse(invoke(request), request);
    }

    protected Page<M, J> searchPage(String serviceEndpoint, Object filterSortBody) {
        EquinixRequest<M> request = buildRequest(serviceEndpoint, RequestType.PAGINATED_POST, jsonClass);
        Utils.serializeJson(request, filterSortBody);
        return Utils.handlePaginatedListResponse(invoke(request), request);
    }

    protected J getOne(String serviceEndpoint, String uuid) {
        EquinixRequest<J> request = buildRequestWithPathParams(serviceEndpoint, RequestType.SINGLE, Map.of("uuid", uuid), jsonClass);
        return Utils.handleSingletonResponse(invoke(request), request);
    }

    protected J postOne(String serviceEndpoint, Object creatorBody) {
        EquinixRequest<J> request = buildRequest(serviceEndpoint, RequestType.SINGLE, jsonClass);
        Utils.serializeJson(request, creatorBody);
        return Utils.handleSingletonResponse(invoke(request), request);
    }

    protected J updateOne(String serviceEndpoint, String uuid, Object body) {
        EquinixRequest<J> request = buildRequestWithPathParams(serviceEndpoint, RequestType.SINGLE, Map.of("uuid", uuid), jsonClass);
        Utils.serializeJson(request, body);
        return Utils.handleSingletonResponse(invoke(request), request);
    }

    protected J deleteOne(String serviceEndpoint, String uuid) {
        EquinixRequest<J> request = buildRequestWithPathParams(serviceEndpoint, RequestType.SINGLE, Map.of("uuid", uuid), jsonClass);
        return Utils.handleSingletonResponse(invoke(request), request);
    }

    /**
     * PATCH a resource by uuid with an RFC&nbsp;6902 JSON Patch operations array, sent with
     * content-type {@code application/json-patch+json}. The endpoint's HTTP method (PATCH) comes
     * from {@code apiParams}; the returned JSON is the server's updated resource.
     */
    protected J patchOne(String serviceEndpoint, String uuid, List<PatchOperation> operations) {
        EquinixRequest<J> request = buildRequestWithPathParams(serviceEndpoint, RequestType.SINGLE, Map.of("uuid", uuid), jsonClass);
        request.setContentType(PatchOperation.CONTENT_TYPE);
        Utils.serializeJson(request, operations);
        return Utils.handleSingletonResponse(invoke(request), request);
    }

    // ---- specialized operation helpers (filtered create, dry-run, bulk, secondary response types) ----

    protected J postOne(String serviceEndpoint, Object body, FilterProvider filters) {
        EquinixRequest<J> request = buildRequest(serviceEndpoint, RequestType.SINGLE, jsonClass);
        if (filters != null) {
            request.setFilters(filters);
        }
        Utils.serializeJson(request, body);
        return Utils.handleSingletonResponse(invoke(request), request);
    }

    protected J dryRunCreate(String serviceEndpoint, Object body) {
        return dryRunCreate(serviceEndpoint, body, null);
    }

    protected J dryRunCreate(String serviceEndpoint, Object body, FilterProvider filters) {
        EquinixRequest<J> request = buildRequest(serviceEndpoint, RequestType.SINGLE, jsonClass);
        request.addSingleQueryParameter("dryRun", "true");
        if (filters != null) {
            request.setFilters(filters);
        }
        Utils.serializeJson(request, body);
        return Utils.handleSingletonResponse(invoke(request), request);
    }

    // (postForType / getAs for secondary response types are inherited from ClientBase.)

    // ---- path-parameter variants (for sub-resources nested under a parent id, or code-keyed gets) ----

    protected Page<M, J> listPagePath(String serviceEndpoint, Map<String, String> pathParams) {
        EquinixRequest<M> request = buildRequestWithPathParams(serviceEndpoint, RequestType.PAGINATED, pathParams, jsonClass);
        return Utils.handlePaginatedListResponse(invoke(request), request);
    }

    protected J getOne(String serviceEndpoint, Map<String, String> pathParams) {
        EquinixRequest<J> request = buildRequestWithPathParams(serviceEndpoint, RequestType.SINGLE, pathParams, jsonClass);
        return Utils.handleSingletonResponse(invoke(request), request);
    }

    protected J postOne(String serviceEndpoint, Map<String, String> pathParams, Object body) {
        EquinixRequest<J> request = buildRequestWithPathParams(serviceEndpoint, RequestType.SINGLE, pathParams, jsonClass);
        Utils.serializeJson(request, body);
        return Utils.handleSingletonResponse(invoke(request), request);
    }

    protected J updateOne(String serviceEndpoint, Map<String, String> pathParams, Object body) {
        EquinixRequest<J> request = buildRequestWithPathParams(serviceEndpoint, RequestType.SINGLE, pathParams, jsonClass);
        Utils.serializeJson(request, body);
        return Utils.handleSingletonResponse(invoke(request), request);
    }

    protected J deleteOne(String serviceEndpoint, Map<String, String> pathParams) {
        EquinixRequest<J> request = buildRequestWithPathParams(serviceEndpoint, RequestType.SINGLE, pathParams, jsonClass);
        return Utils.handleSingletonResponse(invoke(request), request);
    }

    protected J patchOne(String serviceEndpoint, Map<String, String> pathParams, List<PatchOperation> operations) {
        EquinixRequest<J> request = buildRequestWithPathParams(serviceEndpoint, RequestType.SINGLE, pathParams, jsonClass);
        request.setContentType(PatchOperation.CONTENT_TYPE);
        Utils.serializeJson(request, operations);
        return Utils.handleSingletonResponse(invoke(request), request);
    }

    // ---- paging (provided once for all resources) ----

    @Override
    public PaginatedList<M> nextPage(PaginatedRequest<M> equinixRequest) {
        Page<M, J> page = Utils.handlePaginatedListResponse(invoke(equinixRequest), equinixRequest);
        return Utils.toPaginatedList(page, this, (j, client) -> wrap(j));
    }

    @Override
    public PaginatedFilteredList<M> nextPage(PaginatedPostRequest<M> equinixRequest) {
        Utils.serializeJson(equinixRequest, equinixRequest.getObjectToSerialize());
        Page<M, J> page = Utils.handlePaginatedListResponse(invoke(equinixRequest), equinixRequest);
        return Utils.toPaginatedFilteredList(page, this, (j, client) -> wrap(j));
    }
}
