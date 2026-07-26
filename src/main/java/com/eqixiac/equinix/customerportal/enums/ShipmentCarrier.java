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

import com.eqixiac.equinix.core.model.APIParam;

/**
 * The carrier service delivering a shipment ({@code carrier} in the shipments v2 spec and
 * {@code carrierName} in the shipments v1 spec). {@code CUSTOMER_CARRIER} is a v2-only value,
 * valid when creating a shipment but not when updating one; the v1 {@code carrierName} enum is
 * limited to {@code FEDEX}/{@code DHL}/{@code UPS}/{@code OTHER}.
 */
public enum ShipmentCarrier implements APIParam {
    DHL,
    FEDEX,
    UPS,
    OTHER,
    CUSTOMER_CARRIER
}
