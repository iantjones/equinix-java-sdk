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
 * {@link api.equinix.javasdk.core.http.RequestAssembler#deriveResponseType}), and the paging
 * {@code nextPage(...)} methods are provided once here. Subclasses supply the JSON class and a
 * {@link #wrap(Object)} factory (which binds {@code this} client for refresh/paging) and then
 * implement each operation as a one-line call to the protected helpers below, passing only the
 * service-endpoint name.</p>
 *
 * <p>Clients whose apiParams endpoint names follow the standard convention can additionally pass
 * a resource name at construction and use the no-endpoint-name helper overloads: the default
 * endpoint names are derived as {@code List}+plural, {@code Search}+plural, {@code Get}/{@code
 * Create}/{@code Update}/{@code Delete}+singular. Operations whose apiParams names deviate keep
 * calling the explicit-name helpers.</p>
 *
 * <pre>{@code
 * public class ServiceProfileClientImpl extends ResourceClientBase<ServiceProfile, ServiceProfileJson>
 *         implements ServiceProfileClient<ServiceProfile> {
 *     public ServiceProfileClientImpl(FabricConfigImpl c) {
 *         super(c, "Fabric", "ServiceProfiles", ServiceProfileJson.class);
 *     }
 *     protected ServiceProfile wrap(ServiceProfileJson j) { return new ServiceProfileWrapper(j, this); }
 *     public Page<ServiceProfileJson> list()          { return listPage("ListServiceProfiles"); }
 *     public ServiceProfileJson getByUuid(String uuid) { return getOne("GetServiceProfile", uuid); }
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

    /** Singular resource name for derived endpoint names (e.g. "ServiceProfile"), or null. */
    private final String resourceNameSingular;

    /** Plural resource name for derived endpoint names (e.g. "ServiceProfiles"), or null. */
    private final String resourceNamePlural;

    protected ResourceClientBase(Config configClient, String functionalArea, String requestParent, Class<J> jsonClass) {
        this(configClient, functionalArea, requestParent, jsonClass, null, null);
    }

    /**
     * Creates a resource client whose default endpoint names are derived from the resource name
     * (plural defaults to {@code resourceName + "s"}); use the explicit-name helpers for any
     * operation whose apiParams name deviates from the convention.
     *
     * @param resourceName the singular resource name as it appears in apiParams endpoint names
     */
    protected ResourceClientBase(Config configClient, String functionalArea, String requestParent, Class<J> jsonClass,
                                 String resourceName) {
        this(configClient, functionalArea, requestParent, jsonClass, resourceName, resourceName + "s");
    }

    /**
     * Creates a resource client whose default endpoint names are derived from an irregularly
     * pluralized resource name.
     *
     * @param resourceNameSingular the singular resource name (drives Get/Create/Update/Delete)
     * @param resourceNamePlural the plural resource name (drives List/Search)
     */
    protected ResourceClientBase(Config configClient, String functionalArea, String requestParent, Class<J> jsonClass,
                                 String resourceNameSingular, String resourceNamePlural) {
        super(configClient, functionalArea, requestParent);
        this.jsonClass = jsonClass;
        this.resourceNameSingular = resourceNameSingular;
        this.resourceNamePlural = resourceNamePlural;
    }

    private String derivedEndpoint(String verb, String resourceName) {
        if (resourceName == null) {
            throw new IllegalStateException("No resource name was supplied at construction; either construct "
                    + getClass().getSimpleName() + " with a resource name to use derived endpoint names, or call "
                    + "the explicit-endpoint-name helper instead.");
        }
        return verb + resourceName;
    }

    /**
     * Wraps a JSON model into its public model type, binding this client so the returned object
     * can refresh/delete/page itself.
     *
     * @param json the deserialized JSON model
     * @return the public model wrapper
     */
    protected abstract M wrap(J json);

    // ---- derived-endpoint-name helpers (resource name supplied once at construction) ----

    /** {@code list()} via the derived {@code List<Plural>} endpoint name. */
    protected Page<J> listPage() {
        return listPage(derivedEndpoint("List", resourceNamePlural));
    }

    /** {@code search(body)} via the derived {@code Search<Plural>} endpoint name. */
    protected Page<J> searchPage(Object filterSortBody) {
        return searchPage(derivedEndpoint("Search", resourceNamePlural), filterSortBody);
    }

    /** {@code get(uuid)} via the derived {@code Get<Singular>} endpoint name. */
    protected J getOneByUuid(String uuid) {
        return getOne(derivedEndpoint("Get", resourceNameSingular), uuid);
    }

    /** {@code create(body)} via the derived {@code Create<Singular>} endpoint name. */
    protected J createOne(Object creatorBody) {
        return postOne(derivedEndpoint("Create", resourceNameSingular), creatorBody);
    }

    /** {@code update(uuid, body)} via the derived {@code Update<Singular>} endpoint name. */
    protected J updateOneByUuid(String uuid, Object body) {
        return updateOne(derivedEndpoint("Update", resourceNameSingular), uuid, body);
    }

    /** {@code delete(uuid)} via the derived {@code Delete<Singular>} endpoint name. */
    protected J deleteOneByUuid(String uuid) {
        return deleteOne(derivedEndpoint("Delete", resourceNameSingular), uuid);
    }

    // ---- standard operation helpers (endpoint name is the only per-call input) ----

    protected Page<J> listPage(String serviceEndpoint) {
        EquinixRequest<J> request = buildRequest(serviceEndpoint, RequestType.PAGINATED, jsonClass);
        return ResponseHandler.handlePaginatedListResponse(invoke(request), request);
    }

    protected Page<J> listPage(String serviceEndpoint, Map<String, java.util.List<String>> queryParams) {
        EquinixRequest<J> request = buildRequestWithQueryParams(serviceEndpoint, RequestType.PAGINATED, queryParams, jsonClass);
        return ResponseHandler.handlePaginatedListResponse(invoke(request), request);
    }

    protected Page<J> searchPage(String serviceEndpoint, Object filterSortBody) {
        EquinixRequest<J> request = buildRequest(serviceEndpoint, RequestType.PAGINATED_POST, jsonClass);
        SerializationHelper.serializeJson(request, filterSortBody);
        return ResponseHandler.handlePaginatedListResponse(invoke(request), request);
    }

    protected J getOne(String serviceEndpoint, String uuid) {
        EquinixRequest<J> request = buildRequestWithPathParams(serviceEndpoint, RequestType.SINGLE, Map.of("uuid", uuid), jsonClass);
        return ResponseHandler.handleSingletonResponse(invoke(request), request);
    }

    protected J postOne(String serviceEndpoint, Object creatorBody) {
        EquinixRequest<J> request = buildRequest(serviceEndpoint, RequestType.SINGLE, jsonClass);
        SerializationHelper.serializeJson(request, creatorBody);
        return ResponseHandler.handleSingletonResponse(invoke(request), request);
    }

    protected J updateOne(String serviceEndpoint, String uuid, Object body) {
        EquinixRequest<J> request = buildRequestWithPathParams(serviceEndpoint, RequestType.SINGLE, Map.of("uuid", uuid), jsonClass);
        SerializationHelper.serializeJson(request, body);
        return ResponseHandler.handleSingletonResponse(invoke(request), request);
    }

    protected J deleteOne(String serviceEndpoint, String uuid) {
        EquinixRequest<J> request = buildRequestWithPathParams(serviceEndpoint, RequestType.SINGLE, Map.of("uuid", uuid), jsonClass);
        return ResponseHandler.handleSingletonResponse(invoke(request), request);
    }

    /**
     * PATCH a resource by uuid with an RFC&nbsp;6902 JSON Patch operations array, sent with
     * content-type {@code application/json-patch+json}. The endpoint's HTTP method (PATCH) comes
     * from {@code apiParams}; the returned JSON is the server's updated resource.
     */
    protected J patchOne(String serviceEndpoint, String uuid, List<PatchOperation> operations) {
        EquinixRequest<J> request = buildRequestWithPathParams(serviceEndpoint, RequestType.SINGLE, Map.of("uuid", uuid), jsonClass);
        request.setContentType(PatchOperation.CONTENT_TYPE);
        SerializationHelper.serializeJson(request, operations);
        return ResponseHandler.handleSingletonResponse(invoke(request), request);
    }

    // ---- specialized operation helpers (filtered create, dry-run, bulk, secondary response types) ----

    protected J postOne(String serviceEndpoint, Object body, FilterProvider filters) {
        EquinixRequest<J> request = buildRequest(serviceEndpoint, RequestType.SINGLE, jsonClass);
        if (filters != null) {
            request.setFilters(filters);
        }
        SerializationHelper.serializeJson(request, body);
        return ResponseHandler.handleSingletonResponse(invoke(request), request);
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
        SerializationHelper.serializeJson(request, body);
        return ResponseHandler.handleSingletonResponse(invoke(request), request);
    }

    // (postForType / getAs for secondary response types are inherited from ClientBase.)

    // ---- path-parameter variants (for sub-resources nested under a parent id, or code-keyed gets) ----

    protected Page<J> listPagePath(String serviceEndpoint, Map<String, String> pathParams) {
        EquinixRequest<J> request = buildRequestWithPathParams(serviceEndpoint, RequestType.PAGINATED, pathParams, jsonClass);
        return ResponseHandler.handlePaginatedListResponse(invoke(request), request);
    }

    protected J getOne(String serviceEndpoint, Map<String, String> pathParams) {
        EquinixRequest<J> request = buildRequestWithPathParams(serviceEndpoint, RequestType.SINGLE, pathParams, jsonClass);
        return ResponseHandler.handleSingletonResponse(invoke(request), request);
    }

    protected J postOne(String serviceEndpoint, Map<String, String> pathParams, Object body) {
        EquinixRequest<J> request = buildRequestWithPathParams(serviceEndpoint, RequestType.SINGLE, pathParams, jsonClass);
        SerializationHelper.serializeJson(request, body);
        return ResponseHandler.handleSingletonResponse(invoke(request), request);
    }

    protected J updateOne(String serviceEndpoint, Map<String, String> pathParams, Object body) {
        EquinixRequest<J> request = buildRequestWithPathParams(serviceEndpoint, RequestType.SINGLE, pathParams, jsonClass);
        SerializationHelper.serializeJson(request, body);
        return ResponseHandler.handleSingletonResponse(invoke(request), request);
    }

    protected J deleteOne(String serviceEndpoint, Map<String, String> pathParams) {
        EquinixRequest<J> request = buildRequestWithPathParams(serviceEndpoint, RequestType.SINGLE, pathParams, jsonClass);
        return ResponseHandler.handleSingletonResponse(invoke(request), request);
    }

    protected J patchOne(String serviceEndpoint, Map<String, String> pathParams, List<PatchOperation> operations) {
        EquinixRequest<J> request = buildRequestWithPathParams(serviceEndpoint, RequestType.SINGLE, pathParams, jsonClass);
        request.setContentType(PatchOperation.CONTENT_TYPE);
        SerializationHelper.serializeJson(request, operations);
        return ResponseHandler.handleSingletonResponse(invoke(request), request);
    }

    // ---- paging (provided once for all resources) ----

    @Override
    public PaginatedList<M> nextPage(PaginatedRequest<M> equinixRequest) {
        Page<J> page = ResponseHandler.handlePaginatedListResponse(invoke(equinixRequest), equinixRequest);
        return ResponseHandler.toPaginatedList(page, this, (j, client) -> wrap(j));
    }

    @Override
    public PaginatedFilteredList<M> nextPage(PaginatedPostRequest<M> equinixRequest) {
        // No explicit re-serialization needed: the wire entity is rebuilt from the request's
        // RequestBody at dispatch, so the body's advanced pagination offset is picked up there.
        Page<J> page = ResponseHandler.handlePaginatedListResponse(invoke(equinixRequest), equinixRequest);
        return ResponseHandler.toPaginatedFilteredList(page, this, (j, client) -> wrap(j));
    }
}
