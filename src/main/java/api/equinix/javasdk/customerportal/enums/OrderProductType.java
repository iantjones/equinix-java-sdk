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
 * Product type of an order line (Orders v2 API; also {@code quote_details.productType} in the Quotes v2 spec,
 * which adds {@code CLOUD_EXCHANGE_PORTS}). {@link #UNKNOWN} is a read-side fallback for values added after this
 * SDK release — never send it.
 */
public enum OrderProductType {
    CROSS_CONNECT,
    SMART_HANDS,
    WORK_VISIT,
    SECURITY_ACCESS,
    CONFERENCE_ROOM,
    TROUBLE_TICKET,
    SHIPMENTS,
    NETWORK_PORTS,
    DEINSTALL_CROSS_CONNECT,
    CLOUD_EXCHANGE_PORTS,
    OTHER,
    UNKNOWN;

    @JsonCreator
    public static OrderProductType fromString(String value) {
        try { return OrderProductType.valueOf(value); }
        catch (Exception e) { return UNKNOWN; }
    }
}
