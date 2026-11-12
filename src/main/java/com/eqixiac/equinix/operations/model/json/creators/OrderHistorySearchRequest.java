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

import com.eqixiac.equinix.core.enums.SortOrder;
import com.eqixiac.equinix.operations.enums.OrderHistoryDateRange;
import com.eqixiac.equinix.operations.enums.OrderHistoryProductType;
import com.eqixiac.equinix.operations.enums.OrderHistorySearchSource;
import com.eqixiac.equinix.operations.enums.OrderHistorySortField;
import com.eqixiac.equinix.operations.enums.OrderHistoryStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Request body for searching order history
 * ({@code POST /v1/retrieve-orders}, {@code OrderHistoryAPIRequest}).
 *
 * <p>{@code filters} carries the optional {@code ibxs}, {@code productTypes}, {@code orderStatus},
 * {@code dateRange}, {@code fromDate} and {@code toDate}; {@code source} restricts the fields a
 * free-text {@code q} query matches; {@code sorts} carries the sort {@code name}/{@code direction}
 * pairs; {@code page} carries paging ({@code number}/{@code size}).</p>
 *
 * <p>Every enum-valued field is typed with the values the live service accepts (observed
 * 2026-11-11), so a value the service would reject with HTTP 400 {@code EQX-171-1212} (for example
 * the product type {@code TROUBLE_TICKET}) cannot be sent. The {@code UNKNOWN} read-side fallback of
 * those enums is rejected by the builders.</p>
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderHistorySearchRequest {

    @JsonProperty("filters")
    private final Filters filters;

    @JsonProperty("source")
    private final List<OrderHistorySearchSource> source;

    @JsonProperty("q")
    private final String q;

    @JsonProperty("sorts")
    private final List<Sort> sorts;

    @JsonProperty("page")
    private final PageRequest page;

    private OrderHistorySearchRequest(Builder builder) {
        this.filters = builder.filters;
        this.source = builder.source;
        this.q = builder.q;
        this.sorts = builder.sorts;
        this.page = builder.page;
    }

    /**
     * Returns a new builder for an order history search request.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    private static <E extends Enum<E>> List<E> known(List<E> values, String field) {
        if (values == null) {
            return null;
        }
        for (E value : values) {
            Objects.requireNonNull(value, field + " must not contain null");
            if ("UNKNOWN".equals(value.name())) {
                throw new IllegalArgumentException(field + " must not contain UNKNOWN (a read-side fallback)");
            }
        }
        return List.copyOf(values);
    }

    /** Builder for {@link OrderHistorySearchRequest}. */
    public static class Builder {
        private Filters filters;
        private List<OrderHistorySearchSource> source;
        private String q;
        private List<Sort> sorts;
        private PageRequest page;

        private Builder() {
        }

        /**
         * Sets the search filters.
         *
         * @param filters the filters, or {@code null} for none
         * @return this builder
         */
        public Builder filters(Filters filters) {
            this.filters = filters;
            return this;
        }

        /**
         * Restricts the fields a free-text {@link #q(String) q} query matches.
         *
         * @param source the fields to match, or {@code null} for the service default
         * @return this builder
         */
        public Builder source(List<OrderHistorySearchSource> source) {
            this.source = known(source, "source");
            return this;
        }

        /**
         * Varargs form of {@link #source(List)}.
         *
         * @param source the fields to match
         * @return this builder
         */
        public Builder source(OrderHistorySearchSource... source) {
            return source(Arrays.asList(source));
        }

        /**
         * Sets the free-text query.
         *
         * @param q the query text, for example an order number
         * @return this builder
         */
        public Builder q(String q) {
            this.q = q;
            return this;
        }

        /**
         * Sets the sort directives.
         *
         * @param sorts the sort directives, in priority order
         * @return this builder
         */
        public Builder sorts(List<Sort> sorts) {
            this.sorts = sorts == null ? null : List.copyOf(sorts);
            return this;
        }

        /**
         * Sets the page to return.
         *
         * @param page the zero-based page number and size
         * @return this builder
         */
        public Builder page(PageRequest page) {
            this.page = page;
            return this;
        }

        /**
         * Builds the request.
         *
         * @return the request
         */
        public OrderHistorySearchRequest build() {
            return new OrderHistorySearchRequest(this);
        }
    }

    /**
     * Filters for an order history search: {@code ibxs}, {@code productTypes}, {@code orderStatus},
     * a relative {@code dateRange} (for example {@link OrderHistoryDateRange#PAST_30_DAYS}) or an
     * absolute {@code fromDate}/{@code toDate} window ({@code YYYY-MM-DD}).
     */
    @Getter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Filters {

        @JsonProperty("ibxs")
        private final List<String> ibxs;

        @JsonProperty("productTypes")
        private final List<OrderHistoryProductType> productTypes;

        @JsonProperty("orderStatus")
        private final List<OrderHistoryStatus> orderStatus;

        @JsonProperty("dateRange")
        private final OrderHistoryDateRange dateRange;

        /** {@code YYYY-MM-DD}. */
        @JsonProperty("fromDate")
        private final String fromDate;

        /** {@code YYYY-MM-DD}. */
        @JsonProperty("toDate")
        private final String toDate;

        private Filters(Builder builder) {
            this.ibxs = builder.ibxs;
            this.productTypes = builder.productTypes;
            this.orderStatus = builder.orderStatus;
            this.dateRange = builder.dateRange;
            this.fromDate = builder.fromDate == null ? null : builder.fromDate.toString();
            this.toDate = builder.toDate == null ? null : builder.toDate.toString();
        }

        /**
         * Returns a new filters builder.
         *
         * @return a new builder
         */
        public static Builder builder() {
            return new Builder();
        }

        /** Builder for {@link Filters}. */
        public static class Builder {
            private List<String> ibxs;
            private List<OrderHistoryProductType> productTypes;
            private List<OrderHistoryStatus> orderStatus;
            private OrderHistoryDateRange dateRange;
            private LocalDate fromDate;
            private LocalDate toDate;

            private Builder() {
            }

            /**
             * Filters by IBX code.
             *
             * @param ibxs IBX codes, for example {@code SV1}
             * @return this builder
             */
            public Builder ibxs(List<String> ibxs) {
                this.ibxs = ibxs == null ? null : List.copyOf(ibxs);
                return this;
            }

            /**
             * Filters by product (order) type.
             *
             * @param productTypes the product types; {@code UNKNOWN} is rejected
             * @return this builder
             */
            public Builder productTypes(List<OrderHistoryProductType> productTypes) {
                this.productTypes = known(productTypes, "productTypes");
                return this;
            }

            /**
             * Varargs form of {@link #productTypes(List)}.
             *
             * @param productTypes the product types
             * @return this builder
             */
            public Builder productTypes(OrderHistoryProductType... productTypes) {
                return productTypes(Arrays.asList(productTypes));
            }

            /**
             * Filters by order status.
             *
             * @param orderStatus the statuses; {@code UNKNOWN} is rejected
             * @return this builder
             */
            public Builder orderStatus(List<OrderHistoryStatus> orderStatus) {
                this.orderStatus = known(orderStatus, "orderStatus");
                return this;
            }

            /**
             * Varargs form of {@link #orderStatus(List)}.
             *
             * @param orderStatus the statuses
             * @return this builder
             */
            public Builder orderStatus(OrderHistoryStatus... orderStatus) {
                return orderStatus(Arrays.asList(orderStatus));
            }

            /**
             * Filters by a relative date window.
             *
             * @param dateRange the window; {@code UNKNOWN} is rejected
             * @return this builder
             */
            public Builder dateRange(OrderHistoryDateRange dateRange) {
                if (dateRange == OrderHistoryDateRange.UNKNOWN) {
                    throw new IllegalArgumentException("dateRange must not be UNKNOWN (a read-side fallback)");
                }
                this.dateRange = dateRange;
                return this;
            }

            /**
             * Sets the start of an absolute date window.
             *
             * @param fromDate the first day, inclusive
             * @return this builder
             */
            public Builder fromDate(LocalDate fromDate) {
                this.fromDate = fromDate;
                return this;
            }

            /**
             * Sets the end of an absolute date window.
             *
             * @param toDate the last day, inclusive
             * @return this builder
             */
            public Builder toDate(LocalDate toDate) {
                this.toDate = toDate;
                return this;
            }

            /**
             * Builds the filters.
             *
             * @return the filters
             */
            public Filters build() {
                return new Filters(this);
            }
        }
    }

    /**
     * A sort directive: the field {@code name} and the {@code direction}.
     */
    @Getter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Sort {

        @JsonProperty("name")
        private final OrderHistorySortField name;

        @JsonProperty("direction")
        private final SortOrder direction;

        /**
         * @param name      the field to sort by; {@code UNKNOWN} is rejected
         * @param direction the sort direction, or {@code null} for the service default
         */
        public Sort(OrderHistorySortField name, SortOrder direction) {
            Objects.requireNonNull(name, "name");
            if (name == OrderHistorySortField.UNKNOWN) {
                throw new IllegalArgumentException("name must not be UNKNOWN (a read-side fallback)");
            }
            this.name = name;
            this.direction = direction;
        }
    }

    /**
     * Paging request: the zero-based page {@code number} and the page {@code size} (spec maximum 200).
     */
    @Getter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PageRequest {

        @JsonProperty("number")
        private final Integer number;

        @JsonProperty("size")
        private final Integer size;

        /**
         * @param number the zero-based page number
         * @param size   the page size
         */
        public PageRequest(Integer number, Integer size) {
            this.number = number;
            this.size = size;
        }
    }
}
