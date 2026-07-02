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

package api.equinix.javasdk.internetaccess.enums;

import api.equinix.javasdk.core.model.APIParam;
import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Type of change applied to an Equinix Internet Access (EIA) v2 service (spec schema
 * {@code ChangeType}). {@link #UNKNOWN} is a read-side fallback for values added after this SDK
 * release — never send it.
 */
public enum ChangeType implements APIParam {
    SERVICE_CREATION,
    SERVICE_UPDATE,
    SERVICE_DELETION,
    UNKNOWN;

    @JsonCreator
    public static ChangeType fromString(String value) {
        try {
            return ChangeType.valueOf(value.toUpperCase());
        } catch (Exception e) {
            return UNKNOWN;
        }
    }
}
