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

package com.eqixiac.equinix.operations.model;

import com.eqixiac.equinix.operations.enums.OrderHistoryStatus;
import com.eqixiac.equinix.operations.model.implementation.OrderHistoryAccount;
import com.eqixiac.equinix.operations.model.implementation.OrderHistoryContact;
import com.eqixiac.equinix.operations.model.implementation.OrderHistoryLink;

import java.util.List;

/**
 * A single order history record ({@code order-header}) returned by an order history search.
 */
public interface OrderHistoryItem {

    /**
     * Returns the order number.
     *
     * @return the order number
     */
    String getOrderNumber();

    /**
     * Returns the list of products ordered in the order.
     *
     * @return the product types
     */
    List<String> getType();

    /**
     * Returns the order status (see {@link OrderHistoryStatus} for the values the live service
     * uses; unrecognised values read as {@code UNKNOWN}).
     *
     * @return the order status
     */
    OrderHistoryStatus getOrderStatus();

    /**
     * Returns the order created date and time in ISO date format.
     *
     * @return the created date and time
     */
    String getCreatedAt();

    /**
     * Returns the submitted date.
     *
     * @return the submitted date
     */
    String getSubmittedDate();

    /**
     * Returns the account associated with the order.
     *
     * @return the account
     */
    OrderHistoryAccount getAccount();

    /**
     * Returns the ordering contact.
     *
     * @return the ordering contact
     */
    OrderHistoryContact getOrderingContact();

    /**
     * Returns the notification contact.
     *
     * @return the notification contact
     */
    OrderHistoryContact getNotificationContact();

    /**
     * Returns the notification contacts, as the live service returns them ({@code notificationContacts}
     * array). Not in the orderhistoryv1 spec.
     *
     * @return the notification contacts, or {@code null}
     */
    List<OrderHistoryContact> getNotificationContacts();

    /**
     * Returns the technical contacts ({@code technicalContacts}). Not in the orderhistoryv1 spec.
     *
     * @return the technical contacts, or {@code null}
     */
    List<OrderHistoryContact> getTechnicalContacts();

    /**
     * Returns where the order was placed, for example {@code Portal} ({@code orderSource}). Not in
     * the orderhistoryv1 spec.
     *
     * @return the order source, or {@code null}
     */
    String getOrderSource();

    /**
     * Returns the list of IBXs for the order lines.
     *
     * @return the IBXs
     */
    List<String> getIbx();

    /**
     * Returns the list of customer reference numbers for the order lines.
     *
     * @return the customer reference numbers
     */
    List<String> getCustomerReferenceNumbers();

    /**
     * Returns the HATEOAS links for the order.
     *
     * @return the links
     */
    List<OrderHistoryLink> getLinks();
}
