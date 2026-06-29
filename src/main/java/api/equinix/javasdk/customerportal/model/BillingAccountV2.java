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

import api.equinix.javasdk.customerportal.enums.BillingAccountStatus;

/**
 * A billing account, as returned by the Platform Billing Account v2 (BAS) API.
 */
public interface BillingAccountV2 {

    String getAccountId();

    String getAccountNumber();

    String getAccountName();

    String getCurrency();

    String getBillingCountry();

    BillingAccountStatus getAccountStatus();

    Boolean getIsGlobal();

    Boolean getIsPoRequired();

    Boolean getIsSignatureRequired();

    Boolean getIsReseller();

    Boolean getIsSubCustomer();

    String getCreatedDatetime();

    String getLastModifiedDatetime();
}
