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

package com.eqixiac.equinix.customerportal.model;

/**
 * Result of submitting a colocation order, carrying the generated order identifier.
 *
 * <p>The colocation v2 order APIs (cross-connects, shipments, work visits) return the new order
 * id in the {@code Location} response header as {@code /orders/{orderId}}; the Secure Cabinet v1
 * order API returns it as an {@code orderNumber} JSON field. In both cases the identifier can be
 * used with {@link com.eqixiac.equinix.customerportal.client.Orders} to retrieve, negotiate,
 * note or cancel the order.</p>
 */
public interface OrderResponse {

    /**
     * Returns the generated order identifier.
     *
     * @return the order identifier
     */
    String getOrderId();
}
