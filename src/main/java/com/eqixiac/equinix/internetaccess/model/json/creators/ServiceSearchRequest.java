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

package com.eqixiac.equinix.internetaccess.model.json.creators;

import com.eqixiac.equinix.core.enums.SortOrder;
import com.eqixiac.equinix.internetaccess.enums.ServiceSearchSortProperty;
import com.eqixiac.equinix.internetaccess.enums.ServiceState;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Request body for searching Equinix Internet Access (EIA) v2 services via
 * {@code POST /internetAccess/v2/services/search}.
 *
 * <p>Wraps an {@code AND} group of equality expressions. Each expression matches a service
 * property (a JSON pointer such as {@code /state} or {@code /name}) against one or more
 * values. Build it fluently:</p>
 *
 * <pre>{@code
 * ServiceSearchRequest filter = new ServiceSearchRequest()
 *     .state(ServiceState.ACTIVE)
 *     .equals("/locations/metroCode", "DC")
 *     .sort(ServiceSearchSortProperty.NAME, SortOrder.ASC);
 * }</pre>
 *
 * <p><b>Live behaviour (observed 2026-11-11).</b> {@code /type} is listed by the spec and by the
 * service's own "allowed properties" message, but every value (including {@code SINGLE}, which
 * the service returns on read) is rejected with HTTP 400 {@code EQ-7108002 "Invalid value"}, so
 * this builder refuses a {@code /type} filter. {@code /state} works. Sorting is the
 * {@code sort} query parameter, not part of the body.</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
public class ServiceSearchRequest {

    /** The filter. */
    @JsonProperty("filter")
    private final Filter filter = new Filter();

    /** Sort directives, sent as the {@code sort} query parameter. */
    @JsonIgnore
    private final List<String> sort = new ArrayList<>();

    /**
     * Filters by service state ({@code /state}).
     *
     * @param states the states to match; {@code UNKNOWN} is rejected
     * @return this request for chaining
     */
    public ServiceSearchRequest state(ServiceState... states) {
        List<String> values = new ArrayList<>();
        for (ServiceState state : states) {
            if (state == null || state == ServiceState.UNKNOWN) {
                throw new IllegalArgumentException("state must be a known ServiceState, was " + state);
            }
            values.add(state.name());
        }
        return equals("/state", values);
    }

    /**
     * Adds a sort directive ({@code sort} query parameter, for example {@code /name,-/state}).
     *
     * @param property  the property to sort by
     * @param direction the direction; {@code null} means ascending
     * @return this request for chaining
     */
    public ServiceSearchRequest sort(ServiceSearchSortProperty property, SortOrder direction) {
        if (property == null) {
            throw new IllegalArgumentException("sort property must not be null");
        }
        this.sort.add((direction == SortOrder.DESC ? "-" : "") + property.getPointer());
        return this;
    }

    /**
     * Returns the {@code sort} query-parameter value.
     *
     * @return the comma-joined sort directives, or {@code null} when none were added
     */
    @JsonIgnore
    public String getSortParameter() {
        return sort.isEmpty() ? null : String.join(",", sort);
    }

    /**
     * Adds an equality expression to the {@code AND} group.
     *
     * @param property the property pointer to match (e.g. {@code /state})
     * @param values the values to match against
     * @return this request for chaining
     * @throws IllegalArgumentException for {@code /type}, which the live service rejects
     */
    public ServiceSearchRequest equals(String property, String... values) {
        return equals(property, Arrays.asList(values));
    }

    /**
     * Adds an equality expression to the {@code AND} group.
     *
     * @param property the property pointer to match (e.g. {@code /state})
     * @param values the values to match against
     * @return this request for chaining
     * @throws IllegalArgumentException for {@code /type}, which the live service rejects
     */
    public ServiceSearchRequest equals(String property, List<String> values) {
        if ("/type".equals(property)) {
            throw new IllegalArgumentException("/type is not a supported filter on POST /internetAccess/v2/services/search"
                    + " (live HTTP 400 EQ-7108002 for every value, 2026-11-11); filter on /state and check getType()");
        }
        this.filter.and.add(new Expression(property, List.copyOf(values)));
        return this;
    }

    /** The {@code filter} object: an {@code and} group of expressions. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Getter
    public static class Filter {

        /** The list of AND expressions. */
        @JsonProperty("and")
        private final List<Expression> and = new ArrayList<>();
    }

    /** One equality expression ({@code AndQuery}). */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Getter
    public static class Expression {

        /** The property pointer. */
        @JsonProperty("property")
        private final String property;

        /** The operator (always {@code =}). */
        @JsonProperty("operator")
        private final String operator = "=";

        /** The values to match. */
        @JsonProperty("values")
        private final List<String> values;

        Expression(String property, List<String> values) {
            this.property = property;
            this.values = values;
        }
    }
}
