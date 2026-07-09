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
import lombok.Getter;

/**
 * An account summary (the Fabric v4 {@code SimplifiedAccount} schema).
 *
 * @author ianjones
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class AccountSummary {

    @JsonProperty("accountNumber")
    private Long accountNumber;

    @JsonProperty("accountName")
    private String accountName;

    @JsonProperty("orgId")
    private Long orgId;

    @JsonProperty("organizationName")
    private String organizationName;

    @JsonProperty("globalOrgId")
    private String globalOrgId;

    @JsonProperty("globalOrganizationName")
    private String globalOrganizationName;

    @JsonProperty("ucmId")
    private String ucmId;

    @JsonProperty("globalCustId")
    private String globalCustId;

    @JsonProperty("resellerAccountNumber")
    private Long resellerAccountNumber;

    @JsonProperty("resellerAccountName")
    private String resellerAccountName;

    @JsonProperty("resellerUcmId")
    private String resellerUcmId;

    @JsonProperty("resellerOrgId")
    private Long resellerOrgId;
}
