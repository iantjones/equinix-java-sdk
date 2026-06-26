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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Locks the composition contract for {@link PaginatedList}/{@link PaginatedFilteredList}: they now
 * implement {@link List} by delegation rather than extending {@link ArrayList}, so the full List
 * surface (get/size/iteration/stream/copy-into-ArrayList) must still work while the type is no
 * longer an {@code ArrayList}.
 */
class PaginatedListCompositionTest {

    @Test
    void paginatedListIsAListButNotAnArrayList() {
        PaginatedList<String> list = new PaginatedList<>();
        list.add("a");
        list.add("b");
        list.add("c");

        assertInstanceOf(List.class, list);
        // composition, not inheritance: PaginatedList must not be an ArrayList subtype
        assertFalse(ArrayList.class.isAssignableFrom(list.getClass()), "must not extend ArrayList");
    }

    @Test
    void paginatedListSupportsFullListSurface() {
        PaginatedList<String> list = new PaginatedList<>();
        list.add("a");
        list.add("b");

        assertEquals(2, list.size());
        assertEquals("a", list.get(0));
        assertTrue(list.contains("b"));

        // iteration
        StringBuilder iterated = new StringBuilder();
        for (String s : list) {
            iterated.append(s);
        }
        assertEquals("ab", iterated.toString());

        // stream (Collection default method, backed by the delegated iterator)
        assertEquals(2, list.stream().count());

        // copy into a plain ArrayList (requires Collection compatibility)
        List<String> copy = new ArrayList<>(list);
        assertEquals(list, copy);
    }

    @Test
    void fullConstructorSeedsItems() {
        List<String> seed = List.of("x", "y");
        PaginatedList<String> list = new PaginatedList<>(seed, null, null, null, null);

        assertEquals(2, list.size());
        assertEquals("x", list.get(0));
        assertEquals(seed, list);
    }

    @Test
    void paginatedFilteredListIsAListButNotAnArrayList() {
        PaginatedFilteredList<String> list = new PaginatedFilteredList<>();
        list.add("a");

        assertInstanceOf(List.class, list);
        assertFalse(ArrayList.class.isAssignableFrom(list.getClass()), "must not extend ArrayList");
        assertEquals(1, list.size());
        assertEquals("a", list.get(0));
    }
}
