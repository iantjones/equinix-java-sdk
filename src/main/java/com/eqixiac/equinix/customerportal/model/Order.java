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

import com.eqixiac.equinix.customerportal.enums.Channel;
import com.eqixiac.equinix.customerportal.enums.OrderStatus;
import com.eqixiac.equinix.customerportal.enums.QuoteRequestType;
import com.eqixiac.equinix.customerportal.enums.SubChannel;
import com.eqixiac.equinix.customerportal.model.implementation.AdditionalInfo;
import com.eqixiac.equinix.customerportal.model.implementation.OrderContactInfo;
import com.eqixiac.equinix.customerportal.model.implementation.OrderLine;
import com.eqixiac.equinix.customerportal.model.implementation.OrderNote;

import java.util.List;

/**
 * An order retrieved from the Equinix Customer Portal (Orders v2). Reflects the spec
 * {@code Orders} ({@code ordersBase} plus {@code details[]}).
 */
public interface Order {

    /**
     * Returns the unique identifier of the order.
     *
     * @return the order id
     */
    String getOrderId();

    /**
     * Returns the customer account name.
     *
     * @return the account name
     */
    String getAccountName();

    /**
     * Returns the customer account number.
     *
     * @return the account number
     */
    String getAccountNumber();

    /**
     * Returns the request type of the originating quote
     * ({@code NEW}, {@code AMENDMENT}, {@code PAPERWORK}, {@code REPLACEMENT_RENEWAL},
     * {@code TERMINATION} or {@code MIGRATION}).
     *
     * @return the quote request type
     */
    QuoteRequestType getQuoteRequestType();

    /**
     * Returns the related parties associated with the order.
     *
     * @return the contacts
     */
    List<OrderContactInfo> getContacts();

    /**
     * Returns the current status of the order
     * ({@code RECEIVED}, {@code IN_PROGRESS}, {@code ON_HOLD}, {@code CLOSED} or {@code CANCELLED}).
     *
     * @return the status
     */
    OrderStatus getStatus();

    /**
     * Returns the order created date and time (UTC).
     *
     * @return the created date and time
     */
    String getCreatedDateTime();

    /**
     * Returns the order updated date and time (UTC).
     *
     * @return the updated date and time
     */
    String getUpdatedDateTime();

    /**
     * Returns the order closed date and time (UTC).
     *
     * @return the closed date and time
     */
    String getClosedDateTime();

    /**
     * Returns the estimated completion date and time (UTC).
     *
     * @return the estimated completion date and time
     */
    String getEstimatedCompletionDateTime();

    /**
     * Returns the order currency code in ISO-4217 format.
     *
     * @return the currency code
     */
    String getCurrencyCode();

    /**
     * Returns the channel the order was placed through.
     *
     * @return the channel
     */
    Channel getChannel();

    /**
     * Returns the sub-channel the order was placed through.
     *
     * @return the sub-channel
     */
    SubChannel getSubChannel();

    /**
     * Returns the notes associated with the order.
     *
     * @return the notes
     */
    List<OrderNote> getNotes();

    /**
     * Returns the product-specific additional information.
     *
     * @return the additional info entries
     */
    List<AdditionalInfo> getAdditionalInfo();

    /**
     * Returns the customer / external reference id.
     *
     * @return the customer reference id
     */
    String getCustomerReferenceId();

    /**
     * Returns whether the order can be cancelled.
     *
     * @return {@code true} when the order is cancellable
     */
    Boolean getCancellable();

    /**
     * Returns whether the order can be modified.
     *
     * @return {@code true} when the order is modifiable
     */
    Boolean getModifiable();

    /**
     * Returns the order line items.
     *
     * @return the order details
     */
    List<OrderLine> getDetails();

    /**
     * Refreshes this order from the API.
     */
    void refresh();
}
