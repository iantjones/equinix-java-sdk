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

package com.eqixiac.equinix.customerportal.model.json.creators;

import com.eqixiac.equinix.customerportal.enums.NotificationCategory;
import com.eqixiac.equinix.customerportal.enums.NotificationSortBy;
import com.eqixiac.equinix.customerportal.enums.NotificationSortDirection;
import com.eqixiac.equinix.customerportal.enums.NotificationType;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

/**
 * Search criteria body for the unified notifications search endpoint
 * ({@code POST /notifications/v2/events/findAll}). All blocks are optional: {@link Filter} narrows
 * by category/type/notification number/account number/created date, {@link SortCriteria} controls
 * ordering and {@link PaginationRequest} controls paging.
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UnifiedNotificationSearchRequest {

    @JsonProperty("filter")
    private final Filter filter;

    @JsonProperty("sort")
    private final List<SortCriteria> sort;

    @JsonProperty("pagination")
    private final PaginationRequest pagination;

    private UnifiedNotificationSearchRequest(Builder builder) {
        this.filter = builder.filter;
        this.sort = builder.sort;
        this.pagination = builder.pagination;
    }

    /**
     * Returns a new builder for a unified notifications search request body.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Filter filter;
        private List<SortCriteria> sort;
        private PaginationRequest pagination;

        private Builder() {
        }

        public Builder filter(Filter filter) {
            this.filter = filter;
            return this;
        }

        public Builder sort(List<SortCriteria> sort) {
            this.sort = sort;
            return this;
        }

        public Builder pagination(PaginationRequest pagination) {
            this.pagination = pagination;
            return this;
        }

        public UnifiedNotificationSearchRequest build() {
            return new UnifiedNotificationSearchRequest(this);
        }
    }

    /**
     * Filter criteria for a unified notifications search. All criteria are optional and are
     * combined with AND semantics; each is a list of accepted values.
     */
    @Getter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Filter {

        @JsonProperty("category")
        private final List<NotificationCategory> category;

        @JsonProperty("type")
        private final List<NotificationType> type;

        @JsonProperty("notificationNumber")
        private final List<String> notificationNumber;

        @JsonProperty("accountNumber")
        private final List<String> accountNumber;

        @JsonProperty("createdDateTime")
        private final List<String> createdDateTime;

        private Filter(Builder builder) {
            this.category = builder.category;
            this.type = builder.type;
            this.notificationNumber = builder.notificationNumber;
            this.accountNumber = builder.accountNumber;
            this.createdDateTime = builder.createdDateTime;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private List<NotificationCategory> category;
            private List<NotificationType> type;
            private List<String> notificationNumber;
            private List<String> accountNumber;
            private List<String> createdDateTime;

            private Builder() {
            }

            public Builder category(List<NotificationCategory> category) {
                this.category = category;
                return this;
            }

            public Builder type(List<NotificationType> type) {
                this.type = type;
                return this;
            }

            public Builder notificationNumber(List<String> notificationNumber) {
                this.notificationNumber = notificationNumber;
                return this;
            }

            public Builder accountNumber(List<String> accountNumber) {
                this.accountNumber = accountNumber;
                return this;
            }

            public Builder createdDateTime(List<String> createdDateTime) {
                this.createdDateTime = createdDateTime;
                return this;
            }

            public Filter build() {
                return new Filter(this);
            }
        }
    }

    /**
     * A single sort criterion for a unified notifications search.
     */
    @Getter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SortCriteria {

        @JsonProperty("direction")
        private final NotificationSortDirection direction;

        @JsonProperty("property")
        private final NotificationSortBy property;

        public SortCriteria(NotificationSortDirection direction, NotificationSortBy property) {
            this.direction = direction;
            this.property = property;
        }
    }

    /**
     * Pagination request information for a unified notifications search.
     */
    @Getter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PaginationRequest {

        @JsonProperty("offset")
        private final Integer offset;

        @JsonProperty("limit")
        private final Integer limit;

        public PaginationRequest(Integer offset, Integer limit) {
            this.offset = offset;
            this.limit = limit;
        }
    }
}
