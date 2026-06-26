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

import api.equinix.javasdk.core.http.response.PaginatedFilteredList;
import api.equinix.javasdk.core.http.response.PaginatedList;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
    void equalsAndHashCodeByItems() {
        PaginatedList<String> a = new PaginatedList<>(List.of("a", "b"), null, null, null, null);
        PaginatedList<String> b = new PaginatedList<>(List.of("a", "b"), null, null, null, null);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }
}
