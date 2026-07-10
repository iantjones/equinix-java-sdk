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

package api.equinix.javasdk.core.http.request;

import api.equinix.javasdk.core.auth.EquinixCredentialsProvider;
import api.equinix.javasdk.core.enums.HttpMethod;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ser.FilterProvider;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Base request type for all Equinix API calls. Concrete and directly instantiable for the simple
 * GET/single/list cases; {@link PaginatedRequest} and {@link PaginatedPostRequest} extend it to add
 * offset/limit paging and POST-search paging respectively.
 *
 * <p>The request body — when the operation has one — is carried as a single {@link RequestBody}
 * (JSON payload, form fields, or raw bytes); the wire entity is built from it by
 * {@link RequestFactory} at dispatch time. No transport (Apache HttpClient) type appears in this
 * class's API.</p>
 *
 * <p>The type parameter {@code T} threads the operation's model type from request construction
 * through {@link api.equinix.javasdk.core.http.response.EquinixResponse} handling; it carries no
 * state on the request itself.</p>
 *
 * @author ianjones
 */
@Getter
@Setter
public class EquinixRequest<T> {

    private EquinixCredentialsProvider equinixCredentialsProvider;
    private URI endPoint;
    private String resourcePath;
    private HttpMethod httpMethod;

    /**
     * The request body, or {@code null} for body-less operations. This is the request's only
     * payload representation; serialization to the wire happens in {@link RequestFactory} once
     * per dispatch attempt.
     */
    private RequestBody body;

    private JsonNode functionalAreaJson;

    private String functionalArea;
    private String requestParent;
    private String serviceEndpoint;

    /**
     * Body content-type for this request. Defaults to {@code application/json}; resource clients
     * set it to {@code application/json-patch+json} for RFC&nbsp;6902 JSON Patch updates. Drives both
     * the {@code Content-Type} header and the serialized entity's content type.
     */
    private String contentType = "application/json";

    private TypeReference<?> typeReference;
    /**
     * Response type derived at runtime from the resource's JSON class (via Jackson's
     * {@code TypeFactory}); used in preference to {@link #typeReference} when set, so resource
     * clients need not hand-declare a {@code TypeReference} per operation.
     */
    private JavaType javaType;
    private FilterProvider filters;

    /**
     * Optional page-item mapper for <em>dual-shape</em> paginated requests: sub-resource list
     * operations whose element type differs from the owning client's own JSON model (e.g. the
     * accounts client paging account <em>agreements</em>). The producer maps page 1 itself, but
     * pages 2+ are fetched through the owning client's shared {@code nextPage(...)}, which by
     * default re-maps every item with the client's class-level {@code wrap()} — typed for the
     * client's own JSON model, so a differently-typed element would throw
     * {@link ClassCastException}. Producers of such requests set this mapper (mirroring their
     * page-1 mapping — often the identity, when the JSON model implements the public interface
     * directly) and the shared paging pipeline applies it instead of {@code wrap()}.
     *
     * <p>Client-side carrier only: declared {@code transient} to make explicit that it plays no
     * part in any serialized form of the request — it never travels on the wire.</p>
     */
    private transient java.util.function.UnaryOperator<Object> pageItemMapper;

    protected Map<String, List<String>> queryParameters = new HashMap<>();

    @Setter(AccessLevel.NONE)
    private Map<String, String> headers = new HashMap<>();

    @Setter(AccessLevel.NONE)
    private Map<String, String> pathParameters = new HashMap<>();

    /**
     * Sets the query parameters, defensively deep-copying into a mutable map with mutable value
     * lists. Callers frequently pass immutable maps and lists ({@code Map.of("category",
     * List.of("COLO"))}); pagination later mutates the map (adding {@code offset}/{@code limit})
     * and {@code addQueryParameter}/{@code addSingleQueryParameter} append to the value lists, so
     * storing the caller's instances directly would throw
     * {@link java.lang.UnsupportedOperationException} (and alias the caller's collections). This
     * override (in place of Lombok's generated setter) guarantees both levels are always mutable
     * and unaliased.
     *
     * @param queryParameters the query parameters (may be {@code null} or immutable)
     */
    public void setQueryParameters(Map<String, List<String>> queryParameters) {
        this.queryParameters = copyOfListValuedMap(queryParameters);
    }

    /**
     * Sets the headers, defensively copying into a mutable map (mirrors
     * {@link #setQueryParameters}: {@code addHeader} mutates the stored map, so an immutable
     * caller-supplied map must not be stored by reference).
     *
     * @param headers the headers (may be {@code null} or immutable)
     */
    public void setHeaders(Map<String, String> headers) {
        this.headers = (headers == null) ? new HashMap<>() : new HashMap<>(headers);
    }

    /**
     * Sets the path parameters, defensively copying into a mutable map (mirrors
     * {@link #setQueryParameters}: {@code addPathParameter} mutates the stored map, so an
     * immutable caller-supplied map — e.g. {@code Map.of("uuid", uuid)} — must not be stored by
     * reference).
     *
     * @param pathParameters the path parameters (may be {@code null} or immutable)
     */
    public void setPathParameters(Map<String, String> pathParameters) {
        this.pathParameters = (pathParameters == null) ? new HashMap<>() : new HashMap<>(pathParameters);
    }

    private static Map<String, List<String>> copyOfListValuedMap(Map<String, List<String>> source) {
        Map<String, List<String>> copy = new HashMap<>();
        if (source != null) {
            source.forEach((key, values) -> copy.put(key, values == null ? null : new ArrayList<>(values)));
        }
        return copy;
    }

    public void addHeader(String headerName, String headerValue) {
        headers.put(headerName, headerValue);
    }

    public void addQueryParameter(String parameterName, List<String> parameterValues) {
        queryParameters.computeIfAbsent(parameterName, k -> new ArrayList<>()).addAll(parameterValues);
    }

    public void addQueryParameters(Map<String, List<String>> parameterValues) {
        queryParameters.putAll(parameterValues);
    }

    public void replaceQueryParameter(String parameterName, List<String> parameterValues) {
        queryParameters.put(parameterName, new ArrayList<>(parameterValues));
    }

    public void addSingleQueryParameter(String parameterName, String parameterValue) {
        queryParameters.computeIfAbsent(parameterName, k -> new ArrayList<>()).add(parameterValue);
    }

    public void addPathParameter(String parameterName, String parameterValue) {
        pathParameters.put(parameterName, parameterValue);
    }
}
