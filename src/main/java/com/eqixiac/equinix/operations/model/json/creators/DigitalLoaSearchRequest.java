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

package com.eqixiac.equinix.operations.model.json.creators;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Search criteria body for {@code POST /diloa/v1/digitalLoas/search} ({@code LoaSearch}):
 * {@code {"filter":{"and":[criterion | {"or":[criterion, ...]}, ...]}}}.
 *
 * <p>The spec requires {@code filter.and} with at least one item ({@code minItems: 1}) and each
 * criterion's {@code values} with at least one value. The live service enforces it (HTTP 400
 * {@code EQ-7200011} "Size on field filter.and", observed 2026-11-11), so {@link Builder#build()}
 * rejects an empty filter before any request.</p>
 *
 * <pre>{@code
 * DigitalLoaSearchRequest request = DigitalLoaSearchRequest.builder()
 *     .and(Criterion.equalTo("/state", "ACTIVE", "PENDING"))
 *     .or(Criterion.equalTo("/provider/name", "Acme"), Criterion.equalTo("/requestor/name", "Acme"))
 *     .build();
 * }</pre>
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DigitalLoaSearchRequest {

    @JsonProperty("filter")
    private final Filter filter;

    private DigitalLoaSearchRequest(List<Object> and) {
        this.filter = new Filter(List.copyOf(and));
    }

    /**
     * Starts a search request.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /** Builder for {@link DigitalLoaSearchRequest}. */
    public static final class Builder {

        private final List<Object> and = new ArrayList<>();

        private Builder() {
        }

        /**
         * Adds a criterion that must match.
         *
         * @param criterion the criterion
         * @return this builder
         */
        public Builder and(Criterion criterion) {
            and.add(Objects.requireNonNull(criterion, "criterion"));
            return this;
        }

        /**
         * Adds a group of criteria of which at least one must match ({@code {"or":[...]}}).
         *
         * @param criteria the alternatives, at least one
         * @return this builder
         */
        public Builder or(Criterion... criteria) {
            if (criteria.length == 0) {
                throw new IllegalArgumentException("an or group needs at least one criterion");
            }
            and.add(new OrGroup(List.copyOf(Arrays.asList(criteria))));
            return this;
        }

        /**
         * Builds the request.
         *
         * @return the request
         * @throws IllegalStateException when no criterion was added (the service requires a
         *                               non-empty {@code filter.and})
         */
        public DigitalLoaSearchRequest build() {
            if (and.isEmpty()) {
                throw new IllegalStateException("Digital LOA search requires at least one criterion in filter.and"
                        + " (live HTTP 400 EQ-7200011 \"Size on field filter.and\")");
            }
            return new DigitalLoaSearchRequest(and);
        }
    }

    /** The {@code filter} object ({@code LoaSearchItem}). */
    @Getter
    public static final class Filter {

        @JsonProperty("and")
        private final List<Object> and;

        Filter(List<Object> and) {
            this.and = and;
        }
    }

    /** An {@code {"or":[...]}} group ({@code Expression}). */
    @Getter
    public static final class OrGroup {

        @JsonProperty("or")
        private final List<Criterion> or;

        OrGroup(List<Criterion> or) {
            this.or = or;
        }
    }

    /**
     * One search criterion ({@code LoaCriterion}): a {@code property}, an {@code operator}
     * ({@code =}, {@code IN}, {@code LIKE}, {@code CONTAINS}) and one or more {@code values}.
     * Spec properties: {@code /token}, {@code /products/zSide/patchPanel/location/ibx},
     * {@code /state}, {@code /provider/name}, {@code /requestor/name}, {@code /provider/orgIds},
     * {@code /requestor/orgIds}.
     */
    @Getter
    public static final class Criterion {

        @JsonProperty("property")
        private final String property;

        @JsonProperty("operator")
        private final String operator;

        @JsonProperty("values")
        private final List<String> values;

        private Criterion(String property, String operator, List<String> values) {
            if (property == null || property.isBlank()) {
                throw new IllegalArgumentException("property must not be blank");
            }
            if (operator == null || operator.isBlank()) {
                throw new IllegalArgumentException("operator must not be blank");
            }
            if (values == null || values.isEmpty()) {
                throw new IllegalArgumentException("values needs at least one value (spec minItems: 1)");
            }
            this.property = property;
            this.operator = operator;
            this.values = List.copyOf(values);
        }

        /**
         * A criterion with an explicit operator.
         *
         * @param property the property pointer, for example {@code /state}
         * @param operator {@code =}, {@code IN}, {@code LIKE} or {@code CONTAINS}
         * @param values   one or more values
         * @return the criterion
         */
        public static Criterion of(String property, String operator, String... values) {
            return new Criterion(property, operator, Arrays.asList(values));
        }

        /**
         * An equality criterion ({@code =}).
         *
         * @param property the property pointer, for example {@code /state}
         * @param values   one or more values
         * @return the criterion
         */
        public static Criterion equalTo(String property, String... values) {
            return of(property, "=", values);
        }

        /**
         * An equality criterion ({@code =}) over a list of values.
         *
         * @param property the property pointer
         * @param values   one or more values
         * @return the criterion
         */
        public static Criterion equalTo(String property, List<String> values) {
            return new Criterion(property, "=", values);
        }
    }
}
