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

import api.equinix.javasdk.customerportal.enums.SupportCaseStatus;
import api.equinix.javasdk.customerportal.model.implementation.SupportCaseAttachmentInfo;
import api.equinix.javasdk.customerportal.model.implementation.SupportCaseContact;
import api.equinix.javasdk.customerportal.model.implementation.SupportCaseEmail;
import api.equinix.javasdk.customerportal.model.implementation.SupportCaseLocation;
import api.equinix.javasdk.customerportal.model.implementation.SupportCaseNote;
import api.equinix.javasdk.customerportal.model.implementation.SupportCaseOtherDetails;

import java.util.List;

/**
 * A trouble ticket / support case retrieved by case or order number from the Equinix Customer
 * Portal support v2 API ({@code SingleCaseResponseV2}).
 */
public interface SupportCase {

    String getId();

    String getAccountNumber();

    String getAccountName();

    String getCustomerReferenceId();

    String getChannel();

    String getOrderId();

    SupportCaseStatus getStatus();

    String getCreatedDateTime();

    SupportCaseLocation getLocation();

    List<SupportCaseContact> getContacts();

    List<SupportCaseNote> getNotes();

    List<SupportCaseAttachmentInfo> getAttachments();

    List<SupportCaseEmail> getEmail();

    SupportCaseOtherDetails getOtherDetails();
}
