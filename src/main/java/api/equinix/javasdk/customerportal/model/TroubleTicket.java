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

import api.equinix.javasdk.customerportal.enums.TicketStatus;
import api.equinix.javasdk.customerportal.model.implementation.TicketAttachment;
import api.equinix.javasdk.customerportal.model.implementation.TicketContact;
import api.equinix.javasdk.customerportal.model.implementation.TicketNote;
import api.equinix.javasdk.customerportal.model.implementation.TicketResolution;

import java.util.List;

/**
 * A trouble ticket retrieved from the Equinix Customer Portal Tickets v2 API.
 */
public interface TroubleTicket {

    String getId();

    String getCategory();

    String getSubCategory();

    String getDescription();

    String getPrimaryId();

    String getSecondaryId();

    String getCustomerReferenceId();

    String getOccurredDateTime();

    String getResolutionDateTime();

    TicketStatus getStatus();

    List<TicketResolution> getResolutions();

    List<TicketNote> getNotes();

    List<TicketAttachment> getAttachments();

    List<TicketContact> getContacts();

    void refresh();
}
