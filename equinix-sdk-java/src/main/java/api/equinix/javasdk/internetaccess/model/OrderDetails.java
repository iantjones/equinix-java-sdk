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

package api.equinix.javasdk.internetaccess.model;

import api.equinix.javasdk.internetaccess.enums.OrderState;
import api.equinix.javasdk.internetaccess.enums.ServiceOrderType;
import api.equinix.javasdk.internetaccess.model.implementation.OrderChangeLog;
import api.equinix.javasdk.internetaccess.model.implementation.OrderContact;
import api.equinix.javasdk.internetaccess.model.implementation.OrderLink;
import api.equinix.javasdk.internetaccess.model.implementation.OrderPurchaseOrder;
import api.equinix.javasdk.internetaccess.model.implementation.OrderSignature;

import java.util.List;

/**
 * The full details of an Equinix Internet Access (EIA) v1 order, as returned by the single get
 * ({@code GET /internetAccess/v1/orders/{orderUUID}}).
 *
 * <p>This is a read-only response view.</p>
 */
public interface OrderDetails {

    /**
     * @return the URI of the order
     */
    String getHref();

    /**
     * @return the unique identifier of the order
     */
    String getUuid();

    /**
     * @return the order number
     */
    String getNumber();

    /**
     * @return the order type ({@code NEW} or {@code AMENDMENT})
     */
    ServiceOrderType getType();

    /**
     * @return the contacts associated with the order
     */
    List<OrderContact> getContacts();

    /**
     * @return whether the order is a draft
     */
    Boolean getDraft();

    /**
     * @return the HATEOAS links describing follow-on actions available on the order
     */
    List<OrderLink> getLinks();

    /**
     * @return the purchase order referenced by the order
     */
    OrderPurchaseOrder getPurchaseOrder();

    /**
     * @return the customer reference number of the order
     */
    String getReferenceNumber();

    /**
     * @return the signature configuration of the order
     */
    OrderSignature getSignature();

    /**
     * @return the processing status of the order
     */
    OrderState getStatus();

    /**
     * @return the audit trail of the order
     */
    OrderChangeLog getChangeLog();

    /**
     * @return the tags applied to the order
     */
    List<String> getTags();
}
