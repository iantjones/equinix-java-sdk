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

package api.equinix.javasdk.customerportal.model;

import api.equinix.javasdk.customerportal.enums.SubChannel;
import api.equinix.javasdk.customerportal.enums.Channel;
import api.equinix.javasdk.customerportal.enums.QuoteRequestType;
import api.equinix.javasdk.customerportal.enums.QuoteStatus;
import api.equinix.javasdk.customerportal.model.implementation.QuoteContact;
import api.equinix.javasdk.customerportal.model.implementation.QuoteDetail;
import api.equinix.javasdk.customerportal.model.implementation.QuotePricing;
import api.equinix.javasdk.customerportal.model.implementation.QuoteTermsOfUse;

import java.util.List;

/**
 * A quote retrieved from the Equinix Customer Portal (Quotes v2). Reflects the spec
 * {@code quote_response} ({@code quote_response_base} plus {@code details[]}).
 */
public interface Quote {

    /**
     * Returns the Equinix quote id.
     *
     * @return the quote id
     */
    String getQuoteId();

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
     * Returns the quote request type
     * ({@code NEW}, {@code AMENDMENT}, {@code PAPERWORK}, {@code REPLACEMENT_RENEWAL},
     * {@code TERMINATION} or {@code MIGRATION}).
     *
     * @return the quote request type
     */
    QuoteRequestType getQuoteRequestType();

    /**
     * Returns the related parties associated with the quote.
     *
     * @return the contacts
     */
    List<QuoteContact> getContacts();

    /**
     * Returns the current status of the quote ({@code SUBMITTED} or {@code APPROVED}).
     *
     * @return the status
     */
    QuoteStatus getStatus();

    /**
     * Returns the quote created date and time (UTC).
     *
     * @return the created date and time
     */
    String getCreatedDateTime();

    /**
     * Returns the quote updated date and time (UTC).
     *
     * @return the updated date and time
     */
    String getUpdatedDateTime();

    /**
     * Returns the quote expiration date and time (UTC).
     *
     * @return the expiration date and time
     */
    String getExpirationDateTime();

    /**
     * Returns the channel the quote was requested through.
     *
     * @return the channel
     */
    Channel getChannel();

    /**
     * Returns the sub-channel the quote was requested through.
     *
     * @return the sub-channel
     */
    SubChannel getSubChannel();

    /**
     * Returns the customer / external reference id.
     *
     * @return the customer reference id
     */
    String getCustomerReferenceId();

    /**
     * Returns the quote agreement number.
     *
     * @return the agreement number
     */
    String getAgreementNumber();

    /**
     * Returns the quote currency code in ISO-4217 format.
     *
     * @return the currency code
     */
    String getCurrencyCode();

    /**
     * Returns the terms and conditions of the quote (initial term, renewal period, non-renewal
     * notice).
     *
     * @return the terms of use
     */
    List<QuoteTermsOfUse> getTermsOfUse();

    /**
     * Returns the grand-total pricing details.
     *
     * @return the total pricing
     */
    List<QuotePricing> getTotalPricing();

    /**
     * Returns the version number, updated when the quote is revised.
     *
     * @return the version number
     */
    String getVersionNumber();

    /**
     * Returns the quote line items.
     *
     * @return the quote details
     */
    List<QuoteDetail> getDetails();

    /**
     * Refreshes this quote from the API.
     */
    void refresh();
}
