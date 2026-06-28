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
import java.util.Map;

/**
 * Search criteria body for the unified notifications search endpoint. All fields are optional;
 * the {@code filter}, {@code sort} and {@code pagination} blocks are modelled as free-form maps to
 * accommodate the full, deeply nested search grammar.
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UnifiedNotificationSearchRequest {

    @JsonProperty("filter")
    private final Map<String, Object> filter;

    @JsonProperty("sort")
    private final List<Map<String, Object>> sort;

    @JsonProperty("pagination")
    private final Map<String, Object> pagination;

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
        private Map<String, Object> filter;
        private List<Map<String, Object>> sort;
        private Map<String, Object> pagination;

        private Builder() {
        }

        public Builder filter(Map<String, Object> filter) {
            this.filter = filter;
            return this;
        }

        public Builder sort(List<Map<String, Object>> sort) {
            this.sort = sort;
            return this;
        }

        public Builder pagination(Map<String, Object> pagination) {
            this.pagination = pagination;
            return this;
        }

        public UnifiedNotificationSearchRequest build() {
            return new UnifiedNotificationSearchRequest(this);
        }
    }
}
