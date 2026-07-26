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

package com.eqixiac.equinix.customerportal.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * The type of a created ticket ({@code TicketResponse.type} in the Support v2 spec).
 */
public enum TicketType {
    CASE,
    TROUBLE,
    UNKNOWN;

    @JsonCreator
    public static TicketType fromString(String value) {
        try { return TicketType.valueOf(value); }
        catch (Exception e) { return UNKNOWN; }
    }
}
