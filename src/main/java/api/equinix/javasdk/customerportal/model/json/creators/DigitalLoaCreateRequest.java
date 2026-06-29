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
 * Request body for creating a Digital Letter of Authorization (Digital LOA) document. The
 * {@code products} the LOA is valid for, the {@code requestor} party and the {@code provider} party
 * are required; {@code notes} and an {@code expiryDateTime} are optional. The deeply nested party
 * and product blocks are modelled as free-form maps.
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DigitalLoaCreateRequest {

    @JsonProperty("products")
    private final List<Map<String, Object>> products;

    @JsonProperty("requestor")
    private final Map<String, Object> requestor;

    @JsonProperty("provider")
    private final Map<String, Object> provider;

    @JsonProperty("notes")
    private final String notes;

    @JsonProperty("expiryDateTime")
    private final String expiryDateTime;

    private DigitalLoaCreateRequest(Builder builder) {
        this.products = builder.products;
        this.requestor = builder.requestor;
        this.provider = builder.provider;
        this.notes = builder.notes;
        this.expiryDateTime = builder.expiryDateTime;
    }

    /**
     * Returns a new builder for a Digital LOA create request body.
     *
     * @param products  the services the LOA is valid for (required)
     * @param requestor the requestor party (required)
     * @param provider  the provider party (required)
     * @return a new builder
     */
    public static Builder builder(List<Map<String, Object>> products, Map<String, Object> requestor,
                                  Map<String, Object> provider) {
        return new Builder(products, requestor, provider);
    }

    public static class Builder {
        private final List<Map<String, Object>> products;
        private final Map<String, Object> requestor;
        private final Map<String, Object> provider;
        private String notes;
        private String expiryDateTime;

        private Builder(List<Map<String, Object>> products, Map<String, Object> requestor,
                        Map<String, Object> provider) {
            this.products = products;
            this.requestor = requestor;
            this.provider = provider;
        }

        public Builder notes(String notes) {
            this.notes = notes;
            return this;
        }

        public Builder expiryDateTime(String expiryDateTime) {
            this.expiryDateTime = expiryDateTime;
            return this;
        }

        public DigitalLoaCreateRequest build() {
            return new DigitalLoaCreateRequest(this);
        }
    }
}
