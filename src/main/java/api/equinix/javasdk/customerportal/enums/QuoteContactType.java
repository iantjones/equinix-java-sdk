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

package api.equinix.javasdk.customerportal.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * The role of a quote contact ({@code Contacts.type} in the Quotes v2 spec).
 */
public enum QuoteContactType {
    QUOTATION,
    SUPPLIER,
    UNKNOWN;

    @JsonCreator
    public static QuoteContactType fromString(String value) {
        try { return QuoteContactType.valueOf(value); }
        catch (Exception e) { return UNKNOWN; }
    }
}
