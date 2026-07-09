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

package api.equinix.javasdk.fabric.model.implementation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * EPT (Precision Time) service network information (the Fabric v4 {@code ipv4} schema):
 * the primary and secondary timing-server addresses, network mask and default gateway.
 *
 * <p>Prefer {@code builder()} over the positional constructor — all four parameters are
 * {@code String}s, so builder construction is self-documenting and transposition-proof.</p>
 *
 * @author ianjones
 */
@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PrecisionTimeIpv4 {

    @JsonProperty("primary")
    private String primary;

    @JsonProperty("secondary")
    private String secondary;

    @JsonProperty("networkMask")
    private String networkMask;

    @JsonProperty("defaultGateway")
    private String defaultGateway;

    /**
     * Explicit constructor replacing the Lombok-generated {@code @AllArgsConstructor}: the
     * argument order is pinned here in code (four same-typed {@code String} parameters)
     * rather than by field declaration order.
     *
     * @param primary        the primary timing-server address
     * @param secondary      the secondary timing-server address
     * @param networkMask    the network mask
     * @param defaultGateway the default gateway
     */
    @Builder
    public PrecisionTimeIpv4(String primary, String secondary, String networkMask, String defaultGateway) {
        this.primary = primary;
        this.secondary = secondary;
        this.networkMask = networkMask;
        this.defaultGateway = defaultGateway;
    }
}
