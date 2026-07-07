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

package api.equinix.javasdk.core.http;

import api.equinix.javasdk.core.exception.EquinixClientException;
import api.equinix.javasdk.core.http.request.EquinixRequest;
import api.equinix.javasdk.core.http.response.EquinixResponse;
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.core.http.response.Pageable;
import api.equinix.javasdk.core.http.response.PageablePost;
import api.equinix.javasdk.core.http.response.PaginatedFilteredList;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.core.internal.Constants;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Deserializes and maps {@link EquinixResponse} bodies: singleton/list/paginated JSON handling,
 * raw string/byte/map handling, header extraction, and the JSON-to-model mapping helpers that
 * turn deserialized {@link Page}s into user-facing {@link PaginatedList}s.
 *
 * <p>Split out of the former monolithic {@code Utils} class; see {@link RequestAssembler},
 * {@link ParameterMapper} and {@link SerializationHelper} for the request-side helpers.</p>
 *
 * @author ianjones
 */
public final class ResponseHandler {

    private static final String RESPONSE_BODY_READ_EXCEPTION = "Error reading response body content.";

    private ResponseHandler() {
    }

    /**
     * Deserializes a response body using the request's derived {@link com.fasterxml.jackson.databind.JavaType}
     * when present, otherwise its hand-declared {@code TypeReference} (back-compat for
     * not-yet-migrated clients).
     */
    @SuppressWarnings("unchecked")
    private static <X> X readResponseBody(EquinixResponse<?> equinixResponse, EquinixRequest<?> equinixRequest)
            throws java.io.IOException {
        return equinixRequest.getJavaType() != null
                ? (X) Constants.mapper().readValue(equinixResponse.getContent(), equinixRequest.getJavaType())
                : (X) Constants.mapper().readValue(equinixResponse.getContent(), equinixRequest.getTypeReference());
    }

    /**
     * Deserializes a paginated list/search response into a {@link Page} of raw JSON items and
     * attaches the request/response pair for lazy paging. The request may be typed over the
     * public model or the JSON model — only the page's item type {@code <J>} matters here.
     *
     * @param <J> the page's item (JSON model) type
     * @throws api.equinix.javasdk.core.exception.EquinixClientException if any.
     */
    public static <J> Page<J> handlePaginatedListResponse(EquinixResponse<?> equinixResponse, EquinixRequest<?> equinixRequest) throws EquinixClientException {
        try {
            Page<J> responsePage = readResponseBody(equinixResponse, equinixRequest);
            responsePage.setAssociatedRequest(equinixRequest);
            responsePage.setAssociatedResponse(equinixResponse);
            return responsePage;
        }
        catch (Exception ioe) {
            throw new EquinixClientException(Constants.JSON_DESERIALIZE_EXCEPTION, ioe);
        }
    }

    /**
     *
     * @throws api.equinix.javasdk.core.exception.EquinixClientException if any.
     */
    public static <T, S> List<S> handleListResponse(EquinixResponse<T> equinixResponse, EquinixRequest<T> equinixRequest) throws EquinixClientException  {
        try {
            return readResponseBody(equinixResponse, equinixRequest);
        }
        catch (Exception ioe) {
            throw new EquinixClientException(Constants.JSON_DESERIALIZE_EXCEPTION, ioe);
        }
    }

    /**
     *
     * @throws api.equinix.javasdk.core.exception.EquinixClientException if any.
     */
    public static <S, T> S handleSingletonResponse(EquinixResponse<T> equinixResponse, EquinixRequest<T> equinixRequest) throws EquinixClientException  {
        // An empty 2xx body (e.g. a DELETE/action that returns 202/204 with no content) has nothing
        // to map — treat as success with no object rather than attempting (and failing) a parse.
        // Detected structurally (status/entity/content-length) rather than by matching Jackson's
        // exception message text, which is not a stable API contract.
        if (isEmptyBody(equinixResponse)) {
            equinixResponse.drainQuietly();
            return null;
        }
        try {
            return readResponseBody(equinixResponse, equinixRequest);
        }
        catch (com.fasterxml.jackson.databind.exc.MismatchedInputException mie) {
            // Fallback for empty bodies that are not structurally detectable (e.g. chunked
            // transfer with no content, where content-length is -1).
            if (mie.getMessage() != null && mie.getMessage().contains("No content to map")) {
                return null;
            }
            throw new EquinixClientException(Constants.JSON_DESERIALIZE_EXCEPTION, mie);
        }
        catch (Exception ioe) {
            throw new EquinixClientException(Constants.JSON_DESERIALIZE_EXCEPTION, ioe);
        }
    }

    private static boolean isEmptyBody(EquinixResponse<?> equinixResponse) {
        // 204 No Content, no body stream at all, or an explicitly zero-length body.
        return equinixResponse.getStatusCode() == 204
                || equinixResponse.getContent() == null
                || equinixResponse.getContentLength() == 0;
    }

    public static <T> String extractFromHeader(EquinixResponse<T> equinixResponse, String headerName, Pattern extractionPattern) {
        try {
            // The response's header map is case-insensitive (RFC 7230 §3.2), so a lowercased
            // header from an HTTP/2-fronted gateway is still found.
            String headerValue = equinixResponse.getHeaders().get(headerName);
            if (headerValue == null) {
                throw new EquinixClientException("Cannot find desired response header or failed to match pattern.");
            }
            Matcher headerMatcher = extractionPattern.matcher(headerValue);
            return headerMatcher.find() ? headerMatcher.group(1) : null;
        }
        finally {
            // This handler reads only headers; drain the body so the pooled connection is released.
            equinixResponse.drainQuietly();
        }
    }

    public static <T> Boolean handleBooleanResponse(EquinixResponse<T> equinixResponse, EquinixRequest<T> equinixRequest) {
        boolean successful = equinixResponse.isSuccessful();
        // This handler reads only the status line; drain the body so the pooled connection is released.
        equinixResponse.drainQuietly();
        return successful;
    }

    /**
     * Reads the raw response body as a UTF-8 string; {@code null} when the response has no body.
     *
     * @throws api.equinix.javasdk.core.exception.EquinixClientException if any.
     */
    public static <T> String handleStringResponse(EquinixResponse<T> equinixResponse) throws EquinixClientException  {
        byte[] bytes = handleByteResponse(equinixResponse);
        return bytes != null ? new String(bytes, StandardCharsets.UTF_8) : null;
    }

    /**
     * Reads the raw response body bytes; {@code null} when the response has no body.
     *
     * @throws api.equinix.javasdk.core.exception.EquinixClientException if any.
     */
    public static <T> byte[] handleByteResponse(EquinixResponse<T> equinixResponse) throws EquinixClientException  {
        if (equinixResponse.getContent() == null) {
            return null;
        }
        try (InputStream in = equinixResponse.getContent()) {
            return in.readAllBytes();
        }
        catch (Exception ioe) {
            throw new EquinixClientException(RESPONSE_BODY_READ_EXCEPTION, ioe);
        }
    }

    /**
     *
     * @throws api.equinix.javasdk.core.exception.EquinixClientException if any.
     */
    public static <T> HashMap<String, String> handleMapResponse(EquinixRequest<T> equinixRequest, EquinixResponse<T> equinixResponse) throws EquinixClientException  {
        try {
            return readResponseBody(equinixResponse, equinixRequest);
        }
        catch (Exception ioe) {
            throw new EquinixClientException(Constants.JSON_DESERIALIZE_EXCEPTION, ioe);
        }
    }

    // ---- JSON-to-model mapping helpers ----

    public static <T, S> PaginatedList<T> mapPaginatedList(List<S> paginatedList, Pageable<T> serviceClient,
                                                           BiFunction<? super S, ? super Pageable<T>, ? extends T> objectMapper){
        List<T> mapped = safeList(paginatedList).stream()
                .map(jsonObject -> objectMapper.apply(jsonObject, serviceClient))
                .collect(Collectors.toList());
        return new PaginatedList<>(mapped, serviceClient, null, null, null);
    }

    public static <T, S> PaginatedFilteredList<T> mapPaginatedFilteredList(List<S> paginatedList, PageablePost<T> serviceClient,
                                                                           BiFunction<? super S, ? super PageablePost<T>, ? extends T> objectMapper){
        List<T> mapped = safeList(paginatedList).stream()
                .map(jsonObject -> objectMapper.apply(jsonObject, serviceClient))
                .collect(Collectors.toList());
        return new PaginatedFilteredList<>(mapped, serviceClient, null, null, null);
    }

    /**
     * Builds a fully-populated {@link PaginatedList} from a deserialized {@link Page} in one step:
     * maps each JSON item to its model wrapper and attaches the page's request/response/pagination
     * for lazy paging. Replaces the repeated map-then-reconstruct boilerplate in resource clients.
     *
     * @param page the deserialized page (carries items + associated request/response + pagination)
     * @param serviceClient the paging client used to fetch subsequent pages
     * @param objectMapper maps a JSON item to its model wrapper
     * @param <T> the model type
     * @param <S> the JSON type
     * @return a paginated list ready for {@code next()}/{@code loadAll()}
     */
    public static <T, S> PaginatedList<T> toPaginatedList(Page<S> page, Pageable<T> serviceClient,
                                                          BiFunction<? super S, ? super Pageable<T>, ? extends T> objectMapper) {
        return new PaginatedList<>(mapPaginatedList(page.getItems(), serviceClient, objectMapper),
                serviceClient, page.getAssociatedRequest(), page.getAssociatedResponse(), page.getPagination());
    }

    /**
     * Builds a fully-populated {@link PaginatedFilteredList} from a deserialized {@link Page} in one step
     * (the POST-search counterpart to {@link #toPaginatedList}).
     *
     * @param page the deserialized page
     * @param serviceClient the paging client used to fetch subsequent pages
     * @param objectMapper maps a JSON item to its model wrapper
     * @param <T> the model type
     * @param <S> the JSON type
     * @return a paginated, filtered list ready for {@code next()}/{@code loadAll()}
     */
    public static <T, S> PaginatedFilteredList<T> toPaginatedFilteredList(Page<S> page, PageablePost<T> serviceClient,
                                                                          BiFunction<? super S, ? super PageablePost<T>, ? extends T> objectMapper) {
        return new PaginatedFilteredList<>(mapPaginatedFilteredList(page.getItems(), serviceClient, objectMapper),
                serviceClient, page.getAssociatedRequest(), page.getAssociatedResponse(), page.getPagination());
    }

    public static <T, S> List<T> mapList(List<S> itemList, Pageable<T> serviceClient,
                                         BiFunction<? super S, ? super Pageable<T>, ? extends T> objectMapper){
        return safeList(itemList).stream()
                .map(jsonObject -> objectMapper.apply(jsonObject, serviceClient))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * Null-tolerant list view: a response that omits its items/data array deserializes to
     * {@code null}, which should read as an empty result rather than NPE-ing deep in mapping code.
     */
    private static <S> List<S> safeList(List<S> list) {
        return list != null ? list : List.of();
    }
}
