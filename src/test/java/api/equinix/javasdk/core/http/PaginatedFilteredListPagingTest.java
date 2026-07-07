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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * POST-search paging semantics of {@link PaginatedFilteredList}:
 * <ul>
 *   <li>any body implementing {@code PaginatedPostBody} pages (previously a hard cast to
 *       {@code FilteredSortedPaginatedPost} threw {@code ClassCastException} for
 *       {@code FilteredPaginatedPost}-bodied searches like Fabric prices);</li>
 *   <li>a non-pageable body fails fast with a clear message, not a bare CCE/NPE;</li>
 *   <li>a failed page fetch rolls the body's offset back so a retried {@code next()} re-requests
 *       the same page instead of silently skipping one.</li>
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
        // The filter-only body (used by fabric prices/search) now carries pagination state.
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
        assertEquals(100, body.getPagination().getOffset(), "body offset advanced by one page");
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
