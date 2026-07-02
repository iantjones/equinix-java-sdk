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

package api.equinix.javasdk.networkedge.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Billing-account status of a Network Edge metro account (spec {@code MetroAccountResponse.accountStatus}).
 * The live API returns mixed-case forms (e.g. {@code Active}), so parsing is case-insensitive.
 * {@link #UNKNOWN} is a read-side fallback for values added after this SDK release — never send it.
 */
public enum AccountStatus {
    ACTIVE,
    PENDING,
    SUSPENDED,
    UNKNOWN;

    @JsonCreator
    public static AccountStatus fromString(String value) {
        try { return AccountStatus.valueOf(value.toUpperCase()); }
        catch (Exception e) { return UNKNOWN; }
    }
}
