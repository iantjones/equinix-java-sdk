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

import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.core.internal.Constants;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Null-tolerance of the JSON-to-model mapping helpers: a response that omits its items/data
 * array must read as an empty result, not NPE deep inside core.
 */
class ResponseHandlerMappingTest {

    @Test
    void mapPaginatedList_nullItemsReadsAsEmpty() {
        PaginatedList<String> mapped = ResponseHandler.<String, String>mapPaginatedList(null, null, (json, client) -> json);

        assertNotNull(mapped);
        assertTrue(mapped.isEmpty());
    }

    @Test
    void mapList_nullItemsReadsAsEmpty() {
        List<String> mapped = ResponseHandler.<String, String>mapList(null, null, (json, client) -> json);

        assertNotNull(mapped);
        assertTrue(mapped.isEmpty());
    }

    @Test
    void pageWithoutItemsArrayDeserializesToEmptyItems() throws Exception {
        // Endpoint returns pagination metadata but omits the data/items array on an empty result.
        Page<?> page = Constants.mapper().readValue("{\"pagination\":{\"total\":0}}",
                Constants.mapper().getTypeFactory().constructParametricType(Page.class, Object.class));

        assertNotNull(page.getItems(), "items must default to an empty list, not null");
        assertEquals(0, page.getItems().size());
        assertTrue(page.getPagination().isLastPage());
    }
}
