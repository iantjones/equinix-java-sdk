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
 * Request body for searching billing accounts ({@code POST /billing/v2/billingAccounts/search},
 * {@code searchBillingAccount}).
 *
 * <p>All criteria are optional: {@code ibxCode} and {@code metroCode} narrow by where the account
 * may place orders, {@code accountStatus} filters by one or more statuses, {@code projectId} scopes
 * to an associated project, and {@code limit}/{@code offset} control paging.</p>
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BillingAccountSearchRequest {

    @JsonProperty("ibxCode")
    private final String ibxCode;

    @JsonProperty("metroCode")
    private final String metroCode;

    @JsonProperty("accountStatus")
    private final List<String> accountStatus;

    @JsonProperty("projectId")
    private final String projectId;

    @JsonProperty("limit")
    private final Integer limit;

    @JsonProperty("offset")
    private final Integer offset;

    private BillingAccountSearchRequest(Builder builder) {
        this.ibxCode = builder.ibxCode;
        this.metroCode = builder.metroCode;
        this.accountStatus = builder.accountStatus;
        this.projectId = builder.projectId;
        this.limit = builder.limit;
        this.offset = builder.offset;
    }

    /**
     * Returns a new builder for a billing account search body. All criteria are optional.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String ibxCode;
        private String metroCode;
        private List<String> accountStatus;
        private String projectId;
        private Integer limit;
        private Integer offset;

        private Builder() {
        }

        public Builder ibxCode(String ibxCode) {
            this.ibxCode = ibxCode;
            return this;
        }

        public Builder metroCode(String metroCode) {
            this.metroCode = metroCode;
            return this;
        }

        public Builder accountStatus(List<String> accountStatus) {
            this.accountStatus = accountStatus;
            return this;
        }

        public Builder projectId(String projectId) {
            this.projectId = projectId;
            return this;
        }

        public Builder limit(Integer limit) {
            this.limit = limit;
            return this;
        }

        public Builder offset(Integer offset) {
            this.offset = offset;
            return this;
        }

        public BillingAccountSearchRequest build() {
            return new BillingAccountSearchRequest(this);
        }
    }
}
