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

package com.eqixiac.equinix.internetaccess.enums;

import com.eqixiac.equinix.core.model.APIParam;
import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * RIR justification question asked for a provider-assigned IP block (spec schema
 * {@code IpBlockQuestions}, property {@code type}). {@link #UNKNOWN} is a read-side fallback for
 * values added after this SDK release — never send it.
 */
public enum IpBlockQuestionType implements APIParam {
    PRIVATE_SPACE_CONSIDERED,
    REFUSED_PREVIOUSLY,
    RETURNING_ADDRESS_SPACE,
    UNKNOWN;

    @JsonCreator
    public static IpBlockQuestionType fromString(String value) {
        try {
            return IpBlockQuestionType.valueOf(value.toUpperCase());
        } catch (Exception e) {
            return UNKNOWN;
        }
    }
}
