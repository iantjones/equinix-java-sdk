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

package com.eqixiac.equinix.fabric.model.implementation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * The Fabric v4 {@code ActivationKeyDetails} schema. It is the {@code keyDetails} property of both
 * {@code EnvironmentActionRequest} and {@code EnvironmentActionResponse}.
 *
 * <p><b>Beta</b>: the schema carries the catalog's Beta marker (catalog fetched 2026-09-21).</p>
 *
 * <table>
 *   <caption>Properties</caption>
 *   <tr><th>Property</th><th>Type</th><th>Catalog description</th></tr>
 *   <tr><td>{@code value}</td><td>string</td><td>"Provider Encoded activation key"</td></tr>
 *   <tr><td>{@code providerId}</td><td>string</td><td>"AWS Connection identifier"</td></tr>
 *   <tr><td>{@code accountId}</td><td>string</td><td>"Account identifier"</td></tr>
 *   <tr><td>{@code bandwidth}</td><td>integer, Mbps</td><td>"Bandwidth in Mbps"</td></tr>
 *   <tr><td>{@code region}</td><td>string</td><td>"Cloud provider region identifier"</td></tr>
 * </table>
 *
 * <p>The schema marks no property required or read-only. The catalog's only request example sends
 * {@code value} alone, so {@link #ofValue(String)} builds that body. The catalog publishes no
 * response example; which of the other four properties the service fills in on a response is
 * unverified. Null properties are omitted from a serialized request.</p>
 *
 * <p>{@code value} is opaque to this class. It is sent as given and is not decoded, and it is
 * distinct from an access point's {@code authenticationKey}.</p>
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ActivationKeyDetails {

    @JsonProperty("value")
    private String value;

    @JsonProperty("providerId")
    private String providerId;

    @JsonProperty("accountId")
    private String accountId;

    /** Bandwidth in Mbps. */
    @JsonProperty("bandwidth")
    private Integer bandwidth;

    @JsonProperty("region")
    private String region;

    /**
     * Explicit, order-pinned constructor behind {@code builder()}. Four of the five parameters
     * are {@code String}, so construction is by named builder setters only.
     */
    @Builder
    private ActivationKeyDetails(String value, String providerId, String accountId, Integer bandwidth,
                                 String region) {
        this.value = value;
        this.providerId = providerId;
        this.accountId = accountId;
        this.bandwidth = bandwidth;
        this.region = region;
    }

    /**
     * Builds the body of the catalog's {@code ValidateActivationKey} request example: a
     * {@code keyDetails} object with {@code value} set and every other property absent.
     *
     * @param value the provider-encoded activation key, sent unmodified
     * @return key details carrying only {@code value}
     * @throws IllegalArgumentException if {@code value} is {@code null} or blank
     */
    public static ActivationKeyDetails ofValue(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("activation key value must not be null or blank");
        }
        return ActivationKeyDetails.builder().value(value).build();
    }
}
