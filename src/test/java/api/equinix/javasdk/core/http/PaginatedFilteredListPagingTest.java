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
import api.equinix.javasdk.core.http.request.PaginatedPostRequest;
import api.equinix.javasdk.core.http.request.PaginatedRequest;
import api.equinix.javasdk.core.http.request.RequestBody;
import api.equinix.javasdk.core.http.response.Pageable;
import api.equinix.javasdk.core.http.response.PageablePost;
import api.equinix.javasdk.core.http.response.PaginatedFilteredList;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.core.http.response.Pagination;
import api.equinix.javasdk.core.internal.Constants;
import api.equinix.javasdk.core.model.FilteredPaginatedPost;
import api.equinix.javasdk.core.model.FilteredSortedPaginatedPost;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The unified POST/GET paging-advance semantics shared by {@link PaginatedFilteredList} and
 * {@link PaginatedList}:
 * <ul>
 *   <li>any body implementing {@code PaginatedPostBody} pages — through EITHER list type
 *       (previously {@code PaginatedList.fetchNextPage} hard-cast the request to
 *       {@code PaginatedRequest} and POST-search pages wrapped in a {@code PaginatedList} threw
 *       {@code ClassCastException} on page 2);</li>
 *   <li>a non-pageable body fails fast with a clear message, not a bare CCE/NPE;</li>
 *   <li>the advance is computed from the SERVER-reported pagination (next offset = server offset
 *       + server limit), so a server that clamps the requested limit does not cause records to be
 *       silently skipped;</li>
 *   <li>a failed page fetch seeks the pagination back so a retried {@code next()} re-requests the
 *       same page instead of silently skipping one.</li>
 * </ul>
 */
class PaginatedFilteredListPagingTest {

    private static Pagination responsePagination(String json) throws Exception {
        return Constants.mapper().readValue(json, Pagination.class);
    }

    private static PageablePost<String> pageablePost(PaginatedFilteredList<String> nextResult) {
        return new PageablePost<>() {
            @Override
            public PaginatedFilteredList<String> nextPage(PaginatedPostRequest<String> equinixRequest) {
                return nextResult;
            }

            @Override
            public PaginatedList<String> nextPage(PaginatedRequest<String> equinixRequest) {
                throw new UnsupportedOperationException();
            }
        };
    }

    private static PaginatedPostRequest<String> postRequest(Object body) {
        PaginatedPostRequest<String> request = new PaginatedPostRequest<>();
        request.setServiceEndpoint("SearchWidgets");
        request.setBody(RequestBody.json(body));
        return request;
    }

    @Test
    void filteredPaginatedPostBodyPages_advancingItsPagination() throws Exception {
        // The filter-only body (used by fabric prices/search) carries pagination state.
        FilteredPaginatedPost<String> body = new FilteredPaginatedPost<>("filter");
        assertEquals(0, body.getPagination().getOffset());

        Pagination notLast = responsePagination("{\"offset\":0,\"limit\":100,\"total\":150}");
        Pagination last = responsePagination("{\"offset\":100,\"limit\":100,\"total\":150}");

        PaginatedFilteredList<String> secondPage = new PaginatedFilteredList<>(List.of("b"), null, postRequest(body), null, last);
        PaginatedFilteredList<String> list = new PaginatedFilteredList<>(List.of("a"),
                pageablePost(secondPage), postRequest(body), null, notLast);

        assertTrue(list.hasNextPage());
        list.next();

        assertEquals(List.of("a", "b"), list.toList());
        assertFalse(list.hasNextPage());
        assertEquals(100, body.getPagination().getOffset(), "body offset advanced by one server page");
    }

    @Test
    void filteredSortedPaginatedPostBodyStillPages() throws Exception {
        FilteredSortedPaginatedPost<String, String> body = new FilteredSortedPaginatedPost<>("filter", "sort");

        Pagination notLast = responsePagination("{\"offset\":0,\"limit\":100,\"total\":150}");
        Pagination last = responsePagination("{\"offset\":100,\"limit\":100,\"total\":150}");

        PaginatedFilteredList<String> secondPage = new PaginatedFilteredList<>(List.of("b"), null, postRequest(body), null, last);
        PaginatedFilteredList<String> list = new PaginatedFilteredList<>(List.of("a"),
                pageablePost(secondPage), postRequest(body), null, notLast);

        list.next();

        assertEquals(2, list.size());
        assertEquals(100, body.getPagination().getOffset());
    }

    @Test
    void bodyOffsetAdvancesFromServerReportedPagination_notTheRequestedPageSize() throws Exception {
        // Caller asks for 500 per page; the server clamps to 100 (e.g. Fabric caps limit at 100).
        // The next page must start where the server's page ENDED (offset 100), not at the caller's
        // requested page size (offset 500) — otherwise records 100..499 are silently skipped.
        FilteredPaginatedPost<String> body = new FilteredPaginatedPost<>("filter");
        body.getPagination().setLimit(500);

        Pagination clampedNotLast = responsePagination("{\"offset\":0,\"limit\":100,\"total\":150}");
        Pagination last = responsePagination("{\"offset\":100,\"limit\":100,\"total\":150}");

        PaginatedFilteredList<String> secondPage = new PaginatedFilteredList<>(List.of("b"), null, postRequest(body), null, last);
        PaginatedFilteredList<String> list = new PaginatedFilteredList<>(List.of("a"),
                pageablePost(secondPage), postRequest(body), null, clampedNotLast);

        list.next();

        assertEquals(100, body.getPagination().getOffset(), "next offset = server offset + server limit");
        assertEquals(100, body.getPagination().getLimit(), "limit follows the server-honored (clamped) page size");
    }

    @Test
    void nonPageableBodyFailsFastWithClearMessage() throws Exception {
        // A search body that carries no pagination state cannot page; previously this was a bare
        // ClassCastException from inside next().
        Pagination notLast = responsePagination("{\"offset\":0,\"limit\":100,\"total\":150}");
        PaginatedFilteredList<String> list = new PaginatedFilteredList<>(List.of("a"),
                pageablePost(null), postRequest("not-a-pageable-body"), null, notLast);

        EquinixClientException e = assertThrows(EquinixClientException.class, list::next);

        assertTrue(e.getMessage().contains("SearchWidgets"), "message must name the endpoint: " + e.getMessage());
        assertTrue(e.getMessage().contains("String"), "message must name the body type: " + e.getMessage());
    }

    @Test
    void failedFetchRollsBackBodyOffset_soRetryFetchesTheSamePage() throws Exception {
        FilteredPaginatedPost<String> body = new FilteredPaginatedPost<>("filter");
        Pagination notLast = responsePagination("{\"offset\":0,\"limit\":100,\"total\":150}");
        Pagination last = responsePagination("{\"offset\":100,\"limit\":100,\"total\":150}");

        PaginatedFilteredList<String> secondPage = new PaginatedFilteredList<>(List.of("b"), null, postRequest(body), null, last);

        PageablePost<String> failsOnce = new PageablePost<>() {
            private boolean failed;

            @Override
            public PaginatedFilteredList<String> nextPage(PaginatedPostRequest<String> equinixRequest) {
                if (!failed) {
                    failed = true;
                    throw new EquinixClientException("transient failure");
                }
                return secondPage;
            }

            @Override
            public PaginatedList<String> nextPage(PaginatedRequest<String> equinixRequest) {
                throw new UnsupportedOperationException();
            }
        };

        PaginatedFilteredList<String> list = new PaginatedFilteredList<>(List.of("a"), failsOnce, postRequest(body), null, notLast);

        assertThrows(EquinixClientException.class, list::next);
        assertEquals(0, body.getPagination().getOffset(), "offset rolled back after the failed fetch");

        list.next(); // retry fetches the SAME page (offset advanced exactly once overall)

        assertEquals(List.of("a", "b"), list.toList());
        assertEquals(100, body.getPagination().getOffset());
    }

    @Test
    void paginatedFilteredListPagesQueryParamPaginatedRequests() throws Exception {
        // POST searches whose spec paginates via offset/limit QUERY PARAMETERS (assets v1, EIA
        // services/prices) carry a PaginatedRequest: the offset path must work through
        // PaginatedFilteredList too (unified dispatch), advancing the request's query window.
        PaginatedRequest<String> request = new PaginatedRequest<>();
        request.setServiceEndpoint("SearchWidgets");

        Pagination notLast = responsePagination("{\"offset\":0,\"limit\":100,\"total\":150}");
        Pagination last = responsePagination("{\"offset\":100,\"limit\":100,\"total\":150}");

        PaginatedList<String> secondPage = new PaginatedList<>(List.of("b"), null, request, null, last);
        PageablePost<String> offsetPathClient = new PageablePost<>() {
            @Override
            public PaginatedFilteredList<String> nextPage(PaginatedPostRequest<String> equinixRequest) {
                throw new UnsupportedOperationException("POST-body path must not be used for a PaginatedRequest");
            }

            @Override
            public PaginatedList<String> nextPage(PaginatedRequest<String> equinixRequest) {
                return secondPage;
            }
        };

        PaginatedFilteredList<String> list = new PaginatedFilteredList<>(List.of("a"), offsetPathClient, request, null, notLast);

        list.next();

        assertEquals(List.of("a", "b"), list.toList());
        assertEquals(100, request.getOffset(), "query offset advanced from the server-reported page");
        assertFalse(list.hasNextPage());
    }

    // ---------------------------------------------------------------------------------------
    // PaginatedList side of the unified dispatch
    // ---------------------------------------------------------------------------------------

    @Test
    void paginatedListPagesPostSearchBodies_regressionForPage2ClassCastException() throws Exception {
        // Assets/billing-accounts/system-alerts searches wrap POST-search pages in a
        // PaginatedList; its old fetchNextPage cast the request to PaginatedRequest and exploded
        // with ClassCastException on page 2. The unified dispatch must advance the body instead.
        FilteredPaginatedPost<String> body = new FilteredPaginatedPost<>("filter");
        PaginatedPostRequest<String> request = postRequest(body);

        Pagination notLast = responsePagination("{\"offset\":0,\"limit\":100,\"total\":150}");
        Pagination last = responsePagination("{\"offset\":100,\"limit\":100,\"total\":150}");

        PaginatedFilteredList<String> secondPage = new PaginatedFilteredList<>(List.of("b"), null, request, null, last);
        PageablePost<String> client = pageablePost(secondPage);

        PaginatedList<String> list = new PaginatedList<>(List.of("a"), client, request, null, notLast);

        assertTrue(list.hasNextPage());
        list.next();

        assertEquals(List.of("a", "b"), list.toList());
        assertEquals(100, body.getPagination().getOffset(), "body offset advanced through the PaginatedList path");
        assertFalse(list.hasNextPage());
    }

    @Test
    void paginatedListWithNonPageablePostBodyFailsFastWithClearMessage() throws Exception {
        Pagination notLast = responsePagination("{\"offset\":0,\"limit\":100,\"total\":150}");
        PaginatedList<String> list = new PaginatedList<>(List.of("a"),
                pageablePost(null), postRequest(Map.of("filter", "f")), null, notLast);

        EquinixClientException e = assertThrows(EquinixClientException.class, list::next);

        assertTrue(e.getMessage().contains("SearchWidgets"), "message must name the endpoint: " + e.getMessage());
        assertTrue(e.getMessage().contains("does not carry pagination state"), e.getMessage());
    }

    @Test
    void paginatedListAdvancesOffsetFromServerReportedPagination_notTheRequestedPageSize() throws Exception {
        // Server-clamp regression: caller requests limit=500, server clamps to 100 and reports
        // pagination {offset:0, limit:100}. Page 2 must be requested at offset 100 (server offset
        // + server limit), NOT offset 500 — the old nextPage() advanced by the requested pageSize.
        PaginatedRequest<String> request = new PaginatedRequest<>();
        request.addSingleQueryParameter("offset", "0");
        request.addSingleQueryParameter("limit", "500");
        request.seedPagingFromQueryParams();
        assertEquals(500, request.getPageSize());

        Pagination clampedNotLast = responsePagination("{\"offset\":0,\"limit\":100,\"total\":150}");
        Pagination last = responsePagination("{\"offset\":100,\"limit\":100,\"total\":150}");

        int[] offsetAtFetch = {-1};
        int[] pageSizeAtFetch = {-1};
        PaginatedList<String> secondPage = new PaginatedList<>(List.of("b"), null, request, null, last);
        Pageable<String> recording = equinixRequest -> {
            offsetAtFetch[0] = equinixRequest.getOffset();
            pageSizeAtFetch[0] = equinixRequest.getPageSize();
            return secondPage;
        };

        PaginatedList<String> list = new PaginatedList<>(List.of("a"), recording, request, null, clampedNotLast);

        list.next();

        assertEquals(100, offsetAtFetch[0], "page 2 must start where the server's clamped page ended");
        assertEquals(100, pageSizeAtFetch[0], "page 2 must request the server-honored (clamped) limit");
        assertEquals(List.of("a", "b"), list.toList());
    }

    @Test
    void paginatedListRollsBackRequestOffsetOnFailedFetch() throws Exception {
        Pagination notLast = responsePagination("{\"offset\":0,\"limit\":1,\"total\":2}");
        Pagination last = responsePagination("{\"offset\":1,\"limit\":1,\"total\":2}");
        PaginatedRequest<String> request = new PaginatedRequest<>();

        PaginatedList<String> secondPage = new PaginatedList<>(List.of("b"), null, request, null, last);
        Pageable<String> failsOnce = new Pageable<>() {
            private boolean failed;

            @Override
            public PaginatedList<String> nextPage(PaginatedRequest<String> equinixRequest) {
                if (!failed) {
                    failed = true;
                    throw new EquinixClientException("transient failure");
                }
                return secondPage;
            }
        };

        PaginatedList<String> list = new PaginatedList<>(List.of("a"), failsOnce, request, null, notLast);
        int initialOffset = request.getOffset();

        assertThrows(EquinixClientException.class, list::next);
        assertEquals(initialOffset, request.getOffset(), "offset rolled back after the failed fetch");

        list.next();

        assertEquals(List.of("a", "b"), list.toList());
    }
}
