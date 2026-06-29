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

package api.equinix.javasdk.customerportal.model.json.creators;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

/**
 * Request body for searching order history
 * ({@code POST /v1/retrieve-orders}, {@code OrderHistoryAPIRequest}).
 *
 * <p>{@code filters} carries the optional {@code ibxs}, {@code productTypes}, {@code orderStatus},
 * {@code dateRange}, {@code fromDate} and {@code toDate}; {@code source} restricts the fields a
 * free-text {@code q} query matches; {@code sorts} carries the sort {@code name}/{@code direction}
 * pairs; {@code page} carries paging ({@code number}/{@code size}). Enum-valued fields
 * ({@code productTypes}, {@code orderStatus}, sort {@code direction}) are kept as strings.</p>
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderHistorySearchRequest {

    @JsonProperty("filters")
    private final Filters filters;

    @JsonProperty("source")
    private final List<String> source;

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

    public static class Builder {
        private Filters filters;
        private List<String> source;
        private String q;
        private List<Sort> sorts;
        private PageRequest page;

        private Builder() {
        }

        public Builder filters(Filters filters) {
            this.filters = filters;
            return this;
        }

        public Builder source(List<String> source) {
            this.source = source;
            return this;
        }

        public Builder q(String q) {
            this.q = q;
            return this;
        }

        public Builder sorts(List<Sort> sorts) {
            this.sorts = sorts;
            return this;
        }

        public Builder page(PageRequest page) {
            this.page = page;
            return this;
        }

        public OrderHistorySearchRequest build() {
            return new OrderHistorySearchRequest(this);
        }
    }

    /**
     * Filters for an order history search: {@code ibxs}, {@code productTypes}, {@code orderStatus},
     * a relative {@code dateRange} (e.g. {@code LAST_30_DAYS}) or an absolute {@code fromDate}/{@code
     * toDate} window.
     */
    @Getter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Filters {

        @JsonProperty("ibxs")
        private final List<String> ibxs;

        @JsonProperty("productTypes")
        private final List<String> productTypes;

        @JsonProperty("orderStatus")
        private final List<String> orderStatus;

        @JsonProperty("dateRange")
        private final String dateRange;

        @JsonProperty("fromDate")
        private final String fromDate;

        @JsonProperty("toDate")
        private final String toDate;

        private Filters(Builder builder) {
            this.ibxs = builder.ibxs;
            this.productTypes = builder.productTypes;
            this.orderStatus = builder.orderStatus;
            this.dateRange = builder.dateRange;
            this.fromDate = builder.fromDate;
            this.toDate = builder.toDate;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private List<String> ibxs;
            private List<String> productTypes;
            private List<String> orderStatus;
            private String dateRange;
            private String fromDate;
            private String toDate;

            private Builder() {
            }

            public Builder ibxs(List<String> ibxs) {
                this.ibxs = ibxs;
                return this;
            }

            public Builder productTypes(List<String> productTypes) {
                this.productTypes = productTypes;
                return this;
            }

            public Builder orderStatus(List<String> orderStatus) {
                this.orderStatus = orderStatus;
                return this;
            }

            public Builder dateRange(String dateRange) {
                this.dateRange = dateRange;
                return this;
            }

            public Builder fromDate(String fromDate) {
                this.fromDate = fromDate;
                return this;
            }

            public Builder toDate(String toDate) {
                this.toDate = toDate;
                return this;
            }

            public Filters build() {
                return new Filters(this);
            }
        }
    }

    /**
     * A sort directive: the field {@code name} and the {@code direction} ({@code ASC} or
     * {@code DESC}).
     */
    @Getter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Sort {

        @JsonProperty("name")
        private final String name;

        @JsonProperty("direction")
        private final String direction;

        /**
         * @param name      the field to sort by
         * @param direction the sort direction ({@code ASC} or {@code DESC})
         */
        public Sort(String name, String direction) {
            this.name = name;
            this.direction = direction;
        }
    }

    /**
     * Paging request: the zero-based page {@code number} and the page {@code size}.
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
