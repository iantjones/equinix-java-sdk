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
 * Charge type of an order line price (Orders v2 API; also {@code pricingDetails.type} in the Quotes v2 spec,
 * which adds the {@code NET_*} and {@code TAX} values). {@link #UNKNOWN} is a read-side fallback for values
 * added after this SDK release — never send it.
 */
public enum PricingChargeType {
    ONE_TIME_CHARGE,
    MONTHLY_CHARGE,
    MONTHLY_DISCOUNT,
    ONE_TIME_DISCOUNT,
    NET_MONTHLY_CHARGE,
    NET_ONE_TIME_CHARGE,
    TAX,
    UNKNOWN;

    @JsonCreator
    public static PricingChargeType fromString(String value) {
        try { return PricingChargeType.valueOf(value); }
        catch (Exception e) { return UNKNOWN; }
    }
}
