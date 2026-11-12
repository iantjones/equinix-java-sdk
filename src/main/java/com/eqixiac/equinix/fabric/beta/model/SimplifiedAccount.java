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
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * SimplifiedAccount model.
 *
 * <p>Maps spec schema {@code SimplifiedAccount}. Unknown properties are ignored on read.</p>
 * <p>Request-capable: every property has a chained setter ({@code setX(...)} returns this), so an
 * instance doubles as its own builder.</p>
 * <p><strong>Beta</strong>: Fabric v4 4.34.0 (spec read 2026-11-11).</p>
 * <p>Generated from the Fabric v4 OpenAPI spec by the fabric-beta generator; regenerate rather than hand-edit.</p>
 */
@Beta
@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE)
public class SimplifiedAccount {

    /** Account number */
    @JsonProperty("accountNumber")
    private Long accountNumber;

    /** Account name */
    @JsonProperty("accountName")
    private String accountName;

    /** Customer organization identifier */
    @JsonProperty("orgId")
    private Long orgId;

    /** Customer organization name */
    @JsonProperty("organizationName")
    private String organizationName;

    /** Global organization identifier */
    @JsonProperty("globalOrgId")
    private String globalOrgId;

    /** Global organization name */
    @JsonProperty("globalOrganizationName")
    private String globalOrganizationName;

    /** Account ucmId */
    @JsonProperty("ucmId")
    private String ucmId;

    /** Account name */
    @JsonProperty("globalCustId")
    private String globalCustId;

    /** Reseller account number */
    @JsonProperty("resellerAccountNumber")
    private Long resellerAccountNumber;

    /** Reseller account name */
    @JsonProperty("resellerAccountName")
    private String resellerAccountName;

    /** Reseller account ucmId */
    @JsonProperty("resellerUcmId")
    private String resellerUcmId;

    /** Reseller customer organization identifier */
    @JsonProperty("resellerOrgId")
    private Long resellerOrgId;
}
