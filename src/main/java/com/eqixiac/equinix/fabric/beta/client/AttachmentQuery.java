/*
 * Copyright 2026 Ian Jones. All Rights Reserved.
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
package com.eqixiac.equinix.fabric.beta.client;

import com.eqixiac.equinix.core.enums.SortOrder;
import com.eqixiac.equinix.fabric.beta.Beta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Optional query parameters of the Application Connect "attached ..." list endpoints: a state filter
 * (sent as {@code state} or {@code attachmentStatus}, as each endpoint names it), {@code order} and
 * {@code style}. Paging ({@code offset}/{@code limit}) is handled by the returned list.
 *
 * <pre>{@code
 * beta.appLinks().listAttachedAppDomains(linkId,
 *         new AttachmentQuery().states("ATTACHED").order(SortOrder.ASC).style(AttachmentQuery.Style.FULL));
 * }</pre>
 */
@Beta
public class AttachmentQuery {

    /** Detail level of the response ({@code Style} in the spec; default {@code MEDIUM}). */
    public enum Style {
        /** Minimal detail. */
        MIN,
        /** Medium detail (the service default). */
        MEDIUM,
        /** Full detail. */
        FULL
    }

    private final List<String> states = new ArrayList<>();
    private SortOrder order;
    private Style style;

    /** Creates an empty query. */
    public AttachmentQuery() {
    }

    /**
     * Adds lifecycle-state filter values (for example {@code PROVISIONED} or {@code ATTACHED}; the generated
     * state enums' {@code getValue()} gives the wire values).
     *
     * @param values the state values
     * @return this query
     */
    public AttachmentQuery states(String... values) {
        states.addAll(Arrays.asList(values));
        return this;
    }

    /**
     * Sets the sort order (service default {@code DESC}).
     *
     * @param order the order
     * @return this query
     */
    public AttachmentQuery order(SortOrder order) {
        this.order = order;
        return this;
    }

    /**
     * Sets the response detail level.
     *
     * @param style the style
     * @return this query
     */
    public AttachmentQuery style(Style style) {
        this.style = style;
        return this;
    }

    /**
     * Renders the query parameters.
     *
     * @param stateParameterName the endpoint's name for the state filter ({@code state} or {@code attachmentStatus})
     * @return the query parameters
     */
    public Map<String, List<String>> toQueryParams(String stateParameterName) {
        Map<String, List<String>> q = new LinkedHashMap<>();
        if (!states.isEmpty()) {
            q.put(stateParameterName, List.copyOf(states));
        }
        if (order != null) {
            q.put("order", List.of(order.name()));
        }
        if (style != null) {
            q.put("style", List.of(style.name()));
        }
        return q;
    }
}
