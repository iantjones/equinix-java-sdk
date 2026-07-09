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

package api.equinix.javasdk.core.http.response;

import api.equinix.javasdk.core.exception.EquinixClientException;
import api.equinix.javasdk.core.http.request.EquinixRequest;
import api.equinix.javasdk.core.http.request.PaginatedPostRequest;
import api.equinix.javasdk.core.http.request.PaginatedRequest;
import api.equinix.javasdk.core.model.PaginatedPostBody;

/**
 * The single paging-advance path shared by {@link PaginatedList} and
 * {@link PaginatedFilteredList}. Seeks the carried request's pagination state — wherever that
 * state lives — from the <em>server-reported</em> pagination of the page currently held:
 *
 * <ul>
 *   <li>a {@link PaginatedPostRequest} whose body implements {@link PaginatedPostBody} pages by
 *       seeking the body's serialized pagination (core {@code Pagination} object, IBX SmartView's
 *       {@code SearchPagination}, flat body {@code offset}/{@code limit} members, ...);</li>
 *   <li>a {@link PaginatedRequest} pages by seeking its {@code offset}/{@code limit} query
 *       parameters (also used for POST searches whose spec paginates via query parameters, e.g.
 *       assets v1 and EIA services/prices searches);</li>
 *   <li>anything else fails fast with a clear unsupported-pagination
 *       {@link EquinixClientException} naming the request/body type, instead of the historical
 *       {@code ClassCastException} on page&nbsp;2.</li>
 * </ul>
 *
 * <p>Advancing from the server-reported window ({@code next offset = server offset + server
 * limit}) rather than by the caller-requested page size is what keeps paging correct when the
 * server clamps the requested limit (e.g. Fabric caps {@code limit} at 100): a caller-side
 * advance by the requested size would silently skip every record in between.</p>
 */
final class PagingDispatcher {

    private PagingDispatcher() {
    }

    /**
     * Seeks the request to the page immediately after the given server-reported page.
     */
    static void advance(EquinixRequest<?> equinixRequest, Pagination serverPagination) {
        seek(equinixRequest, currentOffset(serverPagination) + serverLimit(serverPagination),
                serverLimit(serverPagination));
    }

    /**
     * Seeks the request back to the given server-reported page, so a caller that catches a failed
     * fetch and retries {@code next()} re-requests the SAME page instead of silently skipping one.
     */
    static void rollback(EquinixRequest<?> equinixRequest, Pagination serverPagination) {
        seek(equinixRequest, currentOffset(serverPagination), serverLimit(serverPagination));
    }

    private static long currentOffset(Pagination serverPagination) {
        return serverPagination.getOffset() != null ? serverPagination.getOffset() : 0L;
    }

    private static long serverLimit(Pagination serverPagination) {
        // hasNextPage() guarantees a non-null, non-zero limit before any advance is attempted.
        return serverPagination.getLimit() != null ? serverPagination.getLimit() : 0L;
    }

    private static void seek(EquinixRequest<?> equinixRequest, long offset, long limit) {
        if (equinixRequest instanceof PaginatedPostRequest<?> postRequest) {
            Object body = postRequest.getSearchBody();
            if (body instanceof PaginatedPostBody paginatedBody) {
                paginatedBody.seekPage(offset, limit);
                return;
            }
            throw new EquinixClientException("Cannot fetch the next page for search endpoint '"
                    + postRequest.getServiceEndpoint() + "': the request body"
                    + (body != null ? " of type " + body.getClass().getName() : "")
                    + " does not carry pagination state. Search bodies must implement "
                    + PaginatedPostBody.class.getName() + " to be pageable.");
        }
        if (equinixRequest instanceof PaginatedRequest<?> paginatedRequest) {
            paginatedRequest.seekPage(offset, limit);
            return;
        }
        throw new EquinixClientException("Cannot fetch the next page"
                + (equinixRequest != null ? " for endpoint '" + equinixRequest.getServiceEndpoint() + "'" : "")
                + ": unsupported pagination. The carried request"
                + (equinixRequest != null ? " of type " + equinixRequest.getClass().getName() : "")
                + " is neither a PaginatedPostRequest with a PaginatedPostBody body nor a PaginatedRequest.");
    }
}
