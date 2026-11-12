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

package com.eqixiac.equinix.core.http;

import com.eqixiac.equinix.core.http.request.PaginatedPostRequest;
import com.eqixiac.equinix.core.http.request.PaginatedRequest;
import com.eqixiac.equinix.core.http.request.RequestBody;
import com.eqixiac.equinix.core.http.response.Page;
import com.eqixiac.equinix.core.http.response.Pageable;
import com.eqixiac.equinix.core.http.response.PageablePost;
import com.eqixiac.equinix.core.http.response.PaginatedFilteredList;
import com.eqixiac.equinix.core.http.response.PaginatedList;
import com.eqixiac.equinix.core.http.response.Pagination;
import com.eqixiac.equinix.core.internal.Constants;
import com.eqixiac.equinix.core.model.FilteredPaginatedPost;
import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * {@code loadAll()} must terminate when the server makes no progress, and the Customer Portal
 * {@code page} envelope must be read as pagination.
 */
class PagingStallGuardTest {

    private static Pagination pagination(String json) throws Exception {
        return Constants.mapper().readValue(json, Pagination.class);
    }

    @Test
    @Timeout(5)
    void getListStopsWhenServerIgnoresOffset() throws Exception {
        // e.g. GET /fabric/v4/routers/{id}/actions takes no offset yet reports limit 1, total 2.
        Pagination firstPage = pagination("{\"offset\":0,\"limit\":1,\"total\":2}");
        AtomicInteger calls = new AtomicInteger();
        Pageable<String> client = req -> {
            calls.incrementAndGet();
            return new PaginatedList<>(List.of("a"), null, req, null, firstPage);
        };
        PaginatedList<String> list = new PaginatedList<>(List.of("a"), client, new PaginatedRequest<>(), null, firstPage);

        list.loadAll();

        assertEquals(1, calls.get(), "one probe, then stop");
        assertEquals(List.of("a"), list.stream().toList(), "the repeated page is not re-appended");
        assertFalse(list.hasNextPage());
    }

    @Test
    @Timeout(5)
    void getListStopsOnEmptyPageWhileTotalPromisesMore() throws Exception {
        Pagination firstPage = pagination("{\"offset\":0,\"limit\":2,\"total\":10}");
        Pagination secondPage = pagination("{\"offset\":2,\"limit\":2,\"total\":10}");
        Pageable<String> client = req -> new PaginatedList<>(List.of(), null, req, null, secondPage);
        PaginatedList<String> list = new PaginatedList<>(List.of("a", "b"), client, new PaginatedRequest<>(), null, firstPage);

        list.loadAll();

        assertEquals(2, list.size());
        assertFalse(list.hasNextPage());
    }

    @Test
    @Timeout(5)
    void postSearchStopsWhenServerIgnoresOffset() throws Exception {
        Pagination firstPage = pagination("{\"offset\":0,\"limit\":1,\"total\":3}");
        FilteredPaginatedPost<String> body = new FilteredPaginatedPost<>("filter");
        PaginatedPostRequest<String> request = new PaginatedPostRequest<>();
        request.setServiceEndpoint("SearchWidgets");
        request.setBody(RequestBody.json(body));
        PageablePost<String> client = new PageablePost<>() {
            @Override
            public PaginatedFilteredList<String> nextPage(PaginatedPostRequest<String> req) {
                return new PaginatedFilteredList<>(List.of("x"), null, req, null, firstPage);
            }

            @Override
            public PaginatedList<String> nextPage(PaginatedRequest<String> req) {
                throw new UnsupportedOperationException();
            }
        };
        PaginatedFilteredList<String> list = new PaginatedFilteredList<>(List.of("x"), client, request, null, firstPage);

        list.loadAll();

        assertEquals(1, list.size());
        assertFalse(list.hasNextPage());
    }

    @Test
    void operationsPageEnvelopeIsReadAsPagination() throws Exception {
        // Shape of GET /v1/attachments, /v1/finance/accounts, /v1/reportCenter/reports and
        // POST /v1/assets/search in their specs.
        String json = "{\"data\":[{\"id\":1},{\"id\":2}],"
                + "\"page\":{\"total\":1200,\"limit\":2,\"offset\":0,\"sorts\":[\"-createdDate\"]},"
                + "\"_links\":{\"next\":{\"href\":\"/attachments?limit=2&offset=2\"}}}";

        Page<Object> page = Constants.mapper().readValue(json, new TypeReference<Page<Object>>() { });

        assertEquals(2, page.getItems().size());
        assertNotNull(page.getPagination(), "the 'page' block must populate pagination");
        assertEquals(1200L, page.getPagination().getTotal());
        assertFalse(page.getPagination().isLastPage());
    }
}
