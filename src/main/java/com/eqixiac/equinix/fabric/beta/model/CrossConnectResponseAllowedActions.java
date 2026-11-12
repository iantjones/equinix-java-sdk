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
import com.eqixiac.equinix.fabric.beta.enums.CrossConnectActionType;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * CrossConnectResponseAllowedActions model.
 *
 * <p>Maps an inline spec object. Unknown properties are ignored on read.</p>
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
public class CrossConnectResponseAllowedActions {

    /** Action to perform on the Cross Connect. The applicable action types are listed in the allowedActions of the Cross Connect. &lt;br&gt; XC_SIGNATURE_SUBMISSION - resend the quote to the customer email for signature. &lt;br&gt; XC_PROPOSED_DATE_TIME_ACCEPTANCE - accept the date and time proposed by Equinix. &lt;br&gt; XC_PROPOSED_DATE_TIME_REJECTION - reject the date and time proposed by Equinix. The expedite is cancelled and the Cross Connect is scheduled as STANDARD. &lt;br&gt; XC_CANCEL - cancel the Cross Connect before it is provisioned. &lt;br&gt; XC_DEINSTALL - remove a provisioned Cross ... */
    @JsonProperty("type")
    private CrossConnectActionType type;
}
