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

package api.equinix.javasdk.internetaccess.model.json.creators;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Request body for searching Equinix Internet Access (EIA) v2 services via
 * {@code POST /internetAccess/v2/services/search}.
 *
 * <p>Wraps an {@code AND} group of equality expressions. Each expression matches a service
 * property (a JSON pointer such as {@code /state} or {@code /type}) against one or more
 * values. Build it fluently:</p>
 *
 * <pre>{@code
 * ServiceSearchRequest filter = new ServiceSearchRequest()
 *     .equals("/state", "ACTIVE")
 *     .equals("/type", "SINGLE");
 * }</pre>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ServiceSearchRequest {

    @JsonProperty("filter")
    private final Filter filter = new Filter();

    /**
     * Adds an equality expression to the {@code AND} group.
     *
     * @param property the property pointer to match (e.g. {@code /state})
     * @param values the values to match against
     * @return this request for chaining
     */
    public ServiceSearchRequest equals(String property, String... values) {
        this.filter.and.add(new Expression(property, Arrays.asList(values)));
        return this;
    }

    /**
     * Adds an equality expression to the {@code AND} group.
     *
     * @param property the property pointer to match (e.g. {@code /state})
     * @param values the values to match against
     * @return this request for chaining
     */
    public ServiceSearchRequest equals(String property, List<String> values) {
        this.filter.and.add(new Expression(property, values));
        return this;
    }

    /**
     * <p>Getter for the field <code>filter</code>.</p>
     *
     * @return the filter
     */
    public Filter getFilter() {
        return filter;
    }

    /** The {@code filter} object of an EIA v2 service search. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Filter {

        @JsonProperty("and")
        private final List<Expression> and = new ArrayList<>();

        /**
         * <p>Getter for the field <code>and</code>.</p>
         *
         * @return the list of AND expressions
         */
        public List<Expression> getAnd() {
            return and;
        }
    }

    /** A single equality expression in an EIA v2 service search filter. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Expression {

        @JsonProperty("property")
        private final String property;

        @JsonProperty("operator")
        private final String operator = "=";

        @JsonProperty("values")
        private final List<String> values;

        Expression(String property, List<String> values) {
            this.property = property;
            this.values = values;
        }

        /**
         * <p>Getter for the field <code>property</code>.</p>
         *
         * @return the property pointer
         */
        public String getProperty() {
            return property;
        }

        /**
         * <p>Getter for the field <code>operator</code>.</p>
         *
         * @return the operator (always {@code =})
         */
        public String getOperator() {
            return operator;
        }

        /**
         * <p>Getter for the field <code>values</code>.</p>
         *
         * @return the values to match
         */
        public List<String> getValues() {
            return values;
        }
    }
}
