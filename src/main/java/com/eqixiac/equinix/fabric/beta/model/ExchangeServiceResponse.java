/*
 * Copyright 2026 Ian Jones. All Rights Reserved.
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

package com.eqixiac.equinix.fabric.beta.model;

import com.eqixiac.equinix.fabric.beta.Beta;
import com.eqixiac.equinix.fabric.beta.enums.ExchangeServiceResponseState;
import com.eqixiac.equinix.fabric.beta.enums.ExchangeServiceResponseType;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * ExchangeServiceResponse model.
 *
 * <p>Maps spec schema {@code ExchangeServiceResponse}. Unknown properties are ignored on read.</p>
 * <p><strong>Beta</strong>: Fabric v4 4.34.0 (spec read 2026-11-11).</p>
 * <p>Generated from the Fabric v4 OpenAPI spec by the fabric-beta generator; regenerate rather than hand-edit.</p>
 */
@Beta
@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE)
public class ExchangeServiceResponse {

    /** Internet Exchange Service URI */
    @JsonProperty("href")
    private String href;

    /** Internet Exchange Service identifier */
    @JsonProperty("uuid")
    private String uuid;

    /** Internet Exchange Service Type */
    @JsonProperty("type")
    private ExchangeServiceResponseType type;

    /** Name */
    @JsonProperty("name")
    private String name;

    /** bandwidth in Mbps */
    @JsonProperty("bandwidth")
    private Integer bandwidth;

    /** Description */
    @JsonProperty("description")
    private String description;

    /** State */
    @JsonProperty("state")
    private ExchangeServiceResponseState state;

    /** The {@code location} property. */
    @JsonProperty("location")
    private SimplifiedLocation location;

    /** The {@code publicPeeringConnection} property. */
    @JsonProperty("publicPeeringConnection")
    private PublicPeeringConnectionResponse publicPeeringConnection;

    /** The {@code routingProtocol} property. */
    @JsonProperty("routingProtocol")
    private RoutingProtocolResponse routingProtocol;

    /** Ordering and billing reference details for this Exchange Service. */
    @JsonProperty("order")
    private ExchangeServiceOrder order;

    /** The {@code project} property. */
    @JsonProperty("project")
    private Project project;

    /** The {@code account} property. */
    @JsonProperty("account")
    private SimplifiedAccount account;

    /** Preferences for notifications on Internet Exchange Service configuration or status changes */
    @JsonProperty("notifications")
    private List<ExchangeServiceNotification> notifications;

    /** Change log */
    @JsonProperty("changelog")
    private PlatformChangelog changelog;

    /** Additional information regarding the specified Exchange Service */
    @JsonProperty("additionalInfo")
    private ExchangeServiceAdditionalInfo additionalInfo;
}
