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

import api.equinix.javasdk.core.http.request.PaginatedRequest;
import api.equinix.javasdk.core.http.response.Pageable;
import api.equinix.javasdk.core.http.response.PaginatedFilteredList;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.core.http.response.Pagination;
import api.equinix.javasdk.core.internal.Constants;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Locks the redesigned contract for {@link PaginatedList}/{@link PaginatedFilteredList}: they are an
 * {@link Iterable} view of a page (plus pagination + auto-paging), deliberately <em>not</em> a
 * {@link List}/{@link Collection}. The ergonomic surface (iterate/stream/get/size/toList) must work,
 * while the mutable-collection surface is intentionally absent.
 */
class PaginatedListIterableTest {

    @Test
    void isIterableNotAListOrCollection() {
        PaginatedList<String> list = new PaginatedList<>(List.of("a", "b"), null, null, null, null);
        assertInstanceOf(Iterable.class, list);
        assertFalse(List.class.isAssignableFrom(list.getClass()), "must not be a List");
        assertFalse(Collection.class.isAssignableFrom(list.getClass()), "must not be a Collection");
    }

    @Test
    void supportsIterationStreamGetSize() {
        PaginatedList<String> list = new PaginatedList<>(List.of("a", "b", "c"), null, null, null, null);

        assertEquals(3, list.size());
        assertFalse(list.isEmpty());
        assertEquals("a", list.get(0));

        StringBuilder iterated = new StringBuilder();
        for (String s : list) {
            iterated.append(s);
        }
        assertEquals("abc", iterated.toString());

        assertEquals(3, list.stream().count());
        assertEquals(List.of("a", "b", "c"), list.toList());
    }

    @Test
    void toListIsAnUnmodifiableSnapshot() {
        PaginatedList<String> list = new PaginatedList<>(List.of("x"), null, null, null, null);
        List<String> snapshot = list.toList();

        assertEquals(List.of("x"), snapshot);
        assertThrows(UnsupportedOperationException.class, () -> snapshot.add("y"));
    }

    @Test
    void copyIntoMutableArrayListViaToList() {
        PaginatedList<String> list = new PaginatedList<>(List.of("a", "b"), null, null, null, null);

        List<String> copy = new ArrayList<>(list.toList());
        copy.add("c");

        assertEquals(3, copy.size());
        assertEquals(2, list.size(), "original page is unaffected by mutating the copy");
    }

    @Test
    void filteredListIsAlsoIterableNotAList() {
        PaginatedFilteredList<String> list = new PaginatedFilteredList<>(List.of("a"), null, null, null, null);

        assertInstanceOf(Iterable.class, list);
        assertFalse(List.class.isAssignableFrom(list.getClass()), "must not be a List");
        assertEquals(1, list.size());
        assertEquals("a", list.get(0));
    }

    @Test
    void identitySemantics_noValueEquality() {
        // A live, page-growing view must not define value equality over its mutable loaded items:
        // two lists from different queries with coincidentally equal pages are NOT the same result,
        // and a hash key whose hashCode changes on next() breaks silently.
        PaginatedList<String> a = new PaginatedList<>(List.of("a", "b"), null, null, null, null);
        PaginatedList<String> b = new PaginatedList<>(List.of("a", "b"), null, null, null, null);

        assertNotEquals(a, b, "distinct instances must not compare equal");
        assertEquals(a, a);
    }

    // --- auto-paging (next()/loadAll()) — previously untested ---

    private static PaginatedList<String> twoPageList() throws Exception {
        Pagination notLast = Constants.mapper().readValue("{\"offset\":0,\"limit\":1,\"total\":2}", Pagination.class);
        Pagination last = Constants.mapper().readValue("{\"offset\":1,\"limit\":1,\"total\":2}", Pagination.class);
        Pageable<String> client = req -> new PaginatedList<>(List.of("b"), null, req, null, last);
        return new PaginatedList<>(List.of("a"), client, new PaginatedRequest<>(), null, notLast);
    }

    @Test
    void nextLoadsAndAppendsTheFollowingPage() throws Exception {
        PaginatedList<String> page = twoPageList();
        assertEquals(1, page.size());
        assertTrue(page.hasNextPage());

        page.next();

        assertEquals(2, page.size());
        assertEquals(List.of("a", "b"), page.toList());
        assertFalse(page.hasNextPage());

        page.next(); // no-op once on the last page
        assertEquals(2, page.size());
    }

    @Test
    void loadAllAccumulatesAllPages() throws Exception {
        PaginatedList<String> page = twoPageList();

        PaginatedList<String> same = page.loadAll();

        assertSame(page, same, "loadAll returns this");
        assertEquals(2, page.size());
        assertEquals(List.of("a", "b"), page.toList());
        assertFalse(page.hasNextPage());
    }
}
