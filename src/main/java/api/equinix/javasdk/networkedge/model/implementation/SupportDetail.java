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

package api.equinix.javasdk.networkedge.model.implementation;

import api.equinix.javasdk.networkedge.enums.DevicePlane;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * <p>SupportDetail class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
@Getter
public class SupportDetail {

    @JsonProperty("instanceName")
    private String instanceName;

    @JsonProperty("serviceId")
    private String serviceId;

    @JsonProperty("deviceOrderReferenceId")
    private String deviceOrderReferenceId;

    @JsonProperty("licenseOrderNumber")
    private String licenseOrderNumber;

    @JsonProperty("licenseOrderReferenceId")
    private String licenseOrderReferenceId;

    @JsonProperty("message")
    private String message;

    @JsonProperty("tenant")
    private String tenant;

    @JsonProperty("computeHostName")
    private String computeHostName;

    @JsonProperty("ibx")
    private String ibx;

    @JsonProperty("podId")
    private String podId;

    @JsonProperty("createdByUserCustOrgName")
    private String createdByUserCustOrgName;

    @JsonProperty("createdByUserCustOrgId")
    private Integer createdByUserCustOrgId;

    @JsonProperty("accountCustOrgName")
    private String accountCustOrgName;

    @JsonProperty("accountCustOrgId")
    private Integer accountCustOrgId;

    @JsonProperty("plane")
    private DevicePlane plane;

    @JsonProperty("deviceLinkSupportUrl")
    private String deviceLinkSupportUrl;

    @JsonProperty("l2SupportUrl")
    private String l2SupportUrl;

    @JsonProperty("l3SupportUrl")
    private String l3SupportUrl;

    @JsonProperty("cspSupportUrl")
    private String cspSupportUrl;
}
