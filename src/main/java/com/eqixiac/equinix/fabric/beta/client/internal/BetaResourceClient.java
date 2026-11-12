/*
 * Copyright 2026 Ian Jones. All Rights Reserved.
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
package com.eqixiac.equinix.fabric.beta.client.internal;

import com.eqixiac.equinix.core.client.Config;
import com.eqixiac.equinix.core.client.ResourceClientBase;
import com.eqixiac.equinix.core.enums.RequestType;
import com.eqixiac.equinix.core.http.ResponseHandler;
import com.eqixiac.equinix.core.http.SerializationHelper;
import com.eqixiac.equinix.core.http.request.EquinixRequest;
import com.eqixiac.equinix.core.http.request.PatchOperation;
import com.eqixiac.equinix.core.http.request.RequestBody;
import com.eqixiac.equinix.core.http.response.Page;
import com.eqixiac.equinix.core.http.response.PaginatedFilteredList;
import com.eqixiac.equinix.core.http.response.PaginatedList;
import com.eqixiac.equinix.core.model.FilteredSortedPaginatedPost;
import com.eqixiac.equinix.fabric.beta.Beta;
import com.eqixiac.equinix.fabric.model.implementation.filter.FilterPropertyList;
import com.eqixiac.equinix.fabric.model.implementation.sort.SortPropertyList;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Internal resource client for one Fabric Beta response type. The Beta models are plain read models, so
 * {@link #wrap(Object)} is the identity and one generic client serves every family: an instance is bound
 * to one apiParams resource group ({@code requestParent}) and one JSON model class, which is what the
 * shared paging pipeline needs to fetch further pages of a search or list.
 *
 * @param <J> the JSON model type
 */
@Beta
public class BetaResourceClient<J> extends ResourceClientBase<J, J> {

    /** The apiParams functional area every Beta endpoint is declared in. */
    public static final String FUNCTIONAL_AREA = "FabricBeta";

    private final Class<J> type;

    /**
     * Creates a client for one resource group and response type.
     *
     * @param config the core configuration
     * @param requestParent the apiParams resource group
     * @param type the JSON model class
     */
    public BetaResourceClient(Config config, String requestParent, Class<J> type) {
        super(config, FUNCTIONAL_AREA, requestParent, type);
        this.type = type;
    }

    @Override
    protected J wrap(J json) {
        return json;
    }

    /**
     * Sends a single-resource request (GET/POST/PUT/DELETE per apiParams), with an optional JSON body.
     *
     * @param endpoint the apiParams endpoint name
     * @param pathParams the path parameters
     * @param body the JSON body, or {@code null} for none
     * @return the response model, or {@code null} for an empty body
     */
    public J one(String endpoint, Map<String, String> pathParams, Object body) {
        EquinixRequest<J> request = buildRequestWithPathParams(endpoint, RequestType.SINGLE, pathParams, type);
        if (body != null) {
            SerializationHelper.serializeJson(request, body);
        }
        return ResponseHandler.handleSingletonResponse(invoke(request), request);
    }

    /**
     * Sends a PATCH of {@code op}/{@code path}/{@code value} operations.
     *
     * @param endpoint the apiParams endpoint name
     * @param pathParams the path parameters
     * @param operations the patch operations
     * @param jsonPatchContentType {@code true} for {@code application/json-patch+json}, {@code false} for
     *        {@code application/json} (as the spec declares per endpoint)
     * @return the response model
     */
    public J patch(String endpoint, Map<String, String> pathParams, List<PatchOperation> operations,
                   boolean jsonPatchContentType) {
        if (operations == null || operations.isEmpty()) {
            throw new IllegalArgumentException("At least one patch operation is required.");
        }
        EquinixRequest<J> request = buildRequestWithPathParams(endpoint, RequestType.SINGLE, pathParams, type);
        if (jsonPatchContentType) {
            request.setContentType(PatchOperation.CONTENT_TYPE);
        }
        SerializationHelper.serializeJson(request, operations);
        return ResponseHandler.handleSingletonResponse(invoke(request), request);
    }

    /**
     * POST search with a {@code filter}/{@code pagination}/{@code sort} body; further pages advance the
     * body's pagination offset.
     *
     * @param endpoint the apiParams endpoint name
     * @param pathParams the path parameters (empty for top-level searches)
     * @param filter the filter, or {@code null}
     * @param sort the sort, or {@code null}
     * @return the first page, lazily pageable
     */
    public PaginatedFilteredList<J> search(String endpoint, Map<String, String> pathParams,
                                           FilterPropertyList filter, SortPropertyList sort) {
        EquinixRequest<J> request = buildRequestWithPathParams(endpoint, RequestType.PAGINATED_POST, pathParams, type);
        SerializationHelper.serializeJson(request, new FilteredSortedPaginatedPost<>(filter, sort));
        Page<J> page = ResponseHandler.handlePaginatedListResponse(invoke(request), request);
        return ResponseHandler.toPaginatedFilteredList(page, this, (j, c) -> j);
    }

    /**
     * GET list paged with {@code offset}/{@code limit} query parameters.
     *
     * @param endpoint the apiParams endpoint name
     * @param pathParams the path parameters
     * @param queryParams extra query parameters, or {@code null}
     * @return the first page, lazily pageable
     */
    public PaginatedList<J> list(String endpoint, Map<String, String> pathParams, Map<String, List<String>> queryParams) {
        EquinixRequest<J> request = buildRequest(endpoint, RequestType.PAGINATED, pathParams, queryParams, type);
        Page<J> page = ResponseHandler.handlePaginatedListResponse(invoke(request), request);
        return ResponseHandler.toPaginatedList(page, this, (j, c) -> j);
    }

    /**
     * GET returning the raw response body.
     *
     * @param endpoint the apiParams endpoint name
     * @param pathParams the path parameters
     * @return the body bytes
     */
    public byte[] bytes(String endpoint, Map<String, String> pathParams) {
        return bytesOp(endpoint, pathParams, null);
    }

    /**
     * POST a {@code multipart/form-data} body with one file part and text parts.
     *
     * @param endpoint the apiParams endpoint name
     * @param fileField the file part name
     * @param fileBytes the file content
     * @param fileName the advertised file name, or {@code null}
     * @param textParts the text parts (null values are skipped)
     * @return the response model
     */
    public J multipart(String endpoint, String fileField, byte[] fileBytes, String fileName, Map<String, String> textParts) {
        EquinixRequest<J> request = buildRequest(endpoint, RequestType.SINGLE, type);
        String boundary = "----EquinixSdkBoundary" + UUID.randomUUID().toString().replace("-", "");
        request.setContentType("multipart/form-data; boundary=" + boundary);
        request.setBody(RequestBody.bytes(multipartBody(boundary, fileField, fileBytes, fileName, textParts),
                "multipart/form-data"));
        return ResponseHandler.handleSingletonResponse(invoke(request), request);
    }

    static byte[] multipartBody(String boundary, String fileField, byte[] fileBytes, String fileName,
                                Map<String, String> textParts) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            for (Map.Entry<String, String> part : new java.util.TreeMap<>(textParts).entrySet()) {
                if (part.getValue() == null) {
                    continue;
                }
                out.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"" + part.getKey()
                        + "\"\r\n\r\n" + part.getValue() + "\r\n").getBytes(StandardCharsets.UTF_8));
            }
            out.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"" + fileField + "\"; filename=\""
                    + (fileName == null ? "file" : fileName) + "\"\r\nContent-Type: application/octet-stream\r\n\r\n")
                    .getBytes(StandardCharsets.UTF_8));
            if (fileBytes != null) {
                out.write(fileBytes);
            }
            out.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to assemble multipart body", e);
        }
    }
}
