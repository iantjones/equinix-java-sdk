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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

/**
 * Request body for searching IBX or network notifications
 * ({@code POST /v1/notifications/ibx/search}, {@code POST /v1/notifications/network/search}).
 *
 * <p>The body carries a typed {@link Filter} ({@code ibxs}, {@code types}, {@code statuses}, a
 * {@code dateRange} and — for network notifications — {@code productTypes}). {@code sorts} is a
 * query parameter (not part of the body) and is exposed here so the client can forward it.</p>
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NotificationSearchRequest {

    @JsonProperty("filter")
    private final Filter filter;

    /** Sort fields (forwarded as the {@code sorts} query parameter); not serialized in the body. */
    @JsonIgnore
    private final List<String> sorts;

    private NotificationSearchRequest(Builder builder) {
        this.filter = builder.filter;
        this.sorts = builder.sorts;
    }

    /**
     * Returns a new builder for a notification search request.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Filter filter;
        private List<String> sorts;

        public Builder filter(Filter filter) {
            this.filter = filter;
            return this;
        }

        public Builder sorts(List<String> sorts) {
            this.sorts = sorts;
            return this;
        }

        public NotificationSearchRequest build() {
            return new NotificationSearchRequest(this);
        }
    }

    /**
     * Typed notification filter. {@code productTypes} applies only to network notification searches;
     * it is omitted from the serialized body when {@code null}.
     */
    @Getter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Filter {

        @JsonProperty("ibxs")
        private final List<String> ibxs;

        @JsonProperty("types")
        private final List<String> types;

        @JsonProperty("statuses")
        private final List<String> statuses;

        @JsonProperty("productTypes")
        private final List<String> productTypes;

        @JsonProperty("dateRange")
        private final DateRange dateRange;

        private Filter(Builder builder) {
            this.ibxs = builder.ibxs;
            this.types = builder.types;
            this.statuses = builder.statuses;
            this.productTypes = builder.productTypes;
            this.dateRange = builder.dateRange;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private List<String> ibxs;
            private List<String> types;
            private List<String> statuses;
            private List<String> productTypes;
            private DateRange dateRange;

            public Builder ibxs(List<String> ibxs) {
                this.ibxs = ibxs;
                return this;
            }

            public Builder types(List<String> types) {
                this.types = types;
                return this;
            }

            public Builder statuses(List<String> statuses) {
                this.statuses = statuses;
                return this;
            }

            public Builder productTypes(List<String> productTypes) {
                this.productTypes = productTypes;
                return this;
            }

            public Builder dateRange(DateRange dateRange) {
                this.dateRange = dateRange;
                return this;
            }

            public Filter build() {
                return new Filter(this);
            }
        }
    }

    /**
     * Date-range filter for notification events. Both dates are ISO-8601 date-time strings.
     */
    @Getter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class DateRange {

        @JsonProperty("fromDate")
        private final String fromDate;

        @JsonProperty("toDate")
        private final String toDate;

        public DateRange(String fromDate, String toDate) {
            this.fromDate = fromDate;
            this.toDate = toDate;
        }
    }
}
