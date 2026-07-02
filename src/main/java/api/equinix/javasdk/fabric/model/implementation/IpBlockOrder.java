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

package api.equinix.javasdk.fabric.model.implementation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.NoArgsConstructor;

/**
 * Order details of an IP block. On submission the purchase-order / order-number / order-line fields
 * may be supplied; the response additionally carries the order {@code href} and resolved order
 * number.
 */
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class IpBlockOrder extends OrderRef {

    /**
     * Creates order details for an IP block submission (spec schema {@code IpBlockOrderRequest}).
     * All attributes are optional; pass {@code null} for those not required.
     *
     * @param purchaseOrderNumber the customer purchase-order number
     * @param orderNumber the order reference number
     * @param orderLine the order line
     */
    public IpBlockOrder(String purchaseOrderNumber, String orderNumber, String orderLine) {
        super(purchaseOrderNumber, orderNumber, orderLine);
    }
}
