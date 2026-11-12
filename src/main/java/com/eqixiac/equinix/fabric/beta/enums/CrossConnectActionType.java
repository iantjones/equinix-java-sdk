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

package com.eqixiac.equinix.fabric.beta.enums;

import com.eqixiac.equinix.fabric.beta.Beta;
import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Action to perform on the Cross Connect. The applicable action types are listed in the allowedActions of the Cross Connect. &lt;br&gt; XC_SIGNATURE_SUBMISSION - resend the quote to the customer email for signature. &lt;br&gt; XC_PROPOSED_DATE_TIME_ACCEPTANCE - accept the date and time proposed by Equinix. &lt;br&gt; XC_PROPOSED_DATE_TIME_REJECTION - reject the date and time proposed by Equinix. The expedite is cancelled and the Cross Connect is scheduled as STANDARD. &lt;br&gt; XC_CANCEL - cancel the Cross Connect before it is provisioned. &lt;br&gt; XC_DEINSTALL - remove a provisioned Cross ...
 *
 * <p>Values from the spec; {@link #UNKNOWN} is the read-side fallback for values added later.</p>
 * <p><strong>Beta</strong>: Fabric v4 4.34.0 (spec read 2026-11-11).</p>
 * <p>Generated from the Fabric v4 OpenAPI spec by the fabric-beta generator; regenerate rather than hand-edit.</p>
 */
@Beta
public enum CrossConnectActionType {

    /** Wire value {@code XC_SIGNATURE_SUBMISSION}. */
    @JsonProperty("XC_SIGNATURE_SUBMISSION")
    XC_SIGNATURE_SUBMISSION("XC_SIGNATURE_SUBMISSION"),

    /** Wire value {@code XC_PROPOSED_DATE_TIME_ACCEPTANCE}. */
    @JsonProperty("XC_PROPOSED_DATE_TIME_ACCEPTANCE")
    XC_PROPOSED_DATE_TIME_ACCEPTANCE("XC_PROPOSED_DATE_TIME_ACCEPTANCE"),

    /** Wire value {@code XC_PROPOSED_DATE_TIME_REJECTION}. */
    @JsonProperty("XC_PROPOSED_DATE_TIME_REJECTION")
    XC_PROPOSED_DATE_TIME_REJECTION("XC_PROPOSED_DATE_TIME_REJECTION"),

    /** Wire value {@code XC_CANCEL}. */
    @JsonProperty("XC_CANCEL")
    XC_CANCEL("XC_CANCEL"),

    /** Wire value {@code XC_DEINSTALL}. */
    @JsonProperty("XC_DEINSTALL")
    XC_DEINSTALL("XC_DEINSTALL"),

    /** Wire value {@code XC_INFORMATION_UPDATE}. */
    @JsonProperty("XC_INFORMATION_UPDATE")
    XC_INFORMATION_UPDATE("XC_INFORMATION_UPDATE"),

    /** Read-side fallback for any value this SDK version does not know. */
    @JsonEnumDefaultValue
    UNKNOWN("UNKNOWN");

    private final String value;

    CrossConnectActionType(String value) {
        this.value = value;
    }

    /**
     * Returns the wire value of this constant.
     *
     * @return the value exactly as the API sends it
     */
    public String getValue() {
        return value;
    }
}
