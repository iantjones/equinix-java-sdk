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

/**
 * Request body for requesting Digital LOA private beta access
 * ({@code POST /diloa/v1/privateBetaAccess}). Both {@code email} and {@code companyName} are
 * required.
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PrivateBetaAccessRequest {

    @JsonProperty("email")
    private final String email;

    @JsonProperty("companyName")
    private final String companyName;

    private PrivateBetaAccessRequest(Builder builder) {
        this.email = builder.email;
        this.companyName = builder.companyName;
    }

    /**
     * Returns a new builder for a private beta access request.
     *
     * @param email       the email of the user submitting the request (required)
     * @param companyName the company name of the user submitting the request (required)
     * @return a new builder
     */
    public static Builder builder(String email, String companyName) {
        return new Builder(email, companyName);
    }

    public static class Builder {
        private final String email;
        private final String companyName;

        private Builder(String email, String companyName) {
            this.email = email;
            this.companyName = companyName;
        }

        public PrivateBetaAccessRequest build() {
            return new PrivateBetaAccessRequest(this);
        }
    }
}
