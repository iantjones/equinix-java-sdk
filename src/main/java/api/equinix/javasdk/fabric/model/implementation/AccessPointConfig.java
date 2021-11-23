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

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

/**
 * <p>AccessPointConfig class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
@Getter
public class AccessPointConfig {

    @JsonProperty("supportedBandwidths")
    List<Integer> supportedBandwidths;

    @JsonProperty("allowCustomBandwidth")
    private Boolean allowCustomBandwidth;

    @JsonProperty("allowBandwidthAutoApproval")
    private Boolean allowBandwidthAutoApproval;

    @JsonProperty("allowRemoteConnections")
    private Boolean allowRemoteConnections;

    @JsonProperty("bandwidthAlertThreshold")
    private Integer bandwidthAlertThreshold;

    @JsonProperty("apiConfig")
    private APIConfig apiConfig;

    @JsonProperty("linkProtocolConfig")
    private LinkProtocolConfig linkProtocolConfig;

    @JsonProperty("authenticationKey")
    private AuthenticationKey authenticationKey;

    @JsonProperty("redundancyRequired")
    private Boolean redundancyRequired;

    @JsonProperty("reuseVlanSTag")
    private Boolean reuseVlanSTag;

    @JsonProperty("connectionLabel")
    private String connectionLabel;

    @JsonProperty("metadata")
    private AccessPointMetadata metadata;
}
