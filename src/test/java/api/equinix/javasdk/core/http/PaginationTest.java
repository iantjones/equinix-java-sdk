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

import api.equinix.javasdk.core.http.response.Pagination;
import api.equinix.javasdk.core.internal.Constants;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Null-safety of {@link Pagination}'s page-math accessors. Some Equinix endpoints omit pagination
 * fields (e.g. {@code total} on an empty result), so the accessors must degrade gracefully rather
 * than NPE on unboxing or divide by a zero/absent limit.
 */
class PaginationTest {

    private static Pagination parse(String json) throws Exception {
        return Constants.objectMapper.readValue(json, Pagination.class);
    }

    @Test
    void fullMetadataComputesPageMath() throws Exception {
        Pagination p = parse("{\"offset\":20,\"limit\":20,\"total\":100}");
        assertEquals(1, p.getPageNumber());
        assertEquals(20, p.getPageSize());
        assertFalse(p.getIsFirstPage());
        assertFalse(p.getIsLastPage());
    }

    @Test
    void lastPageDetected() throws Exception {
        Pagination p = parse("{\"offset\":80,\"limit\":20,\"total\":100}");
        assertEquals(4, p.getPageNumber());
        assertTrue(p.getIsLastPage());
    }

    @Test
    void missingTotalIsTreatedAsLastPageWithoutNpe() throws Exception {
        Pagination p = parse("{\"offset\":0,\"limit\":20}");
        assertEquals(0, p.getPageNumber());
        assertTrue(p.getIsFirstPage());
        // total absent -> stop paging (do not loop / NPE)
        assertTrue(p.getIsLastPage());
    }

    @Test
    void allFieldsAbsentDegradesGracefully() throws Exception {
        Pagination p = parse("{}");
        assertEquals(0, p.getPageNumber());
        assertEquals(0, p.getPageSize());
        assertTrue(p.getIsFirstPage());
        assertTrue(p.getIsLastPage());
    }
}
