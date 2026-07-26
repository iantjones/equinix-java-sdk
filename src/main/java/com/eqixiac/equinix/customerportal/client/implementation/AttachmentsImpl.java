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

package com.eqixiac.equinix.customerportal.client.implementation;

import com.eqixiac.equinix.CustomerPortal;
import com.eqixiac.equinix.core.http.ResponseHandler;
import com.eqixiac.equinix.core.http.response.Page;
import com.eqixiac.equinix.core.http.response.PaginatedList;
import com.eqixiac.equinix.customerportal.client.Attachments;
import com.eqixiac.equinix.customerportal.client.internal.AttachmentClient;
import com.eqixiac.equinix.customerportal.enums.AttachmentPurpose;
import com.eqixiac.equinix.customerportal.model.Attachment;
import com.eqixiac.equinix.customerportal.model.json.AttachmentJson;
import com.eqixiac.equinix.customerportal.model.wrappers.AttachmentWrapper;

import java.util.List;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class AttachmentsImpl implements Attachments {

    private final AttachmentClient<Attachment> serviceClient;

    private final CustomerPortal serviceManager;

    public PaginatedList<Attachment> list() {
        return list(null);
    }

    public PaginatedList<Attachment> list(List<String> attachmentIds) {
        Page<AttachmentJson> responsePage = this.serviceClient.list(attachmentIds);
        PaginatedList<Attachment> attachmentList = ResponseHandler.mapPaginatedList(responsePage.getItems(), this.serviceClient, AttachmentWrapper::new);
        return new PaginatedList<>(attachmentList, this.serviceClient, responsePage.getAssociatedRequest(), responsePage.getAssociatedResponse(), responsePage.getPagination());
    }

    public Attachment getByUuid(String attachmentId) {
        AttachmentJson attachmentJson = this.serviceClient.getByUuid(attachmentId);
        return new AttachmentWrapper(attachmentJson, this.serviceClient);
    }

    public byte[] download(String attachmentId) {
        return this.serviceClient.download(attachmentId);
    }

    public Attachment upload(byte[] fileBytes, String fileName, AttachmentPurpose purpose) {
        AttachmentJson attachmentJson = this.serviceClient.upload(fileBytes, fileName, purpose);
        return new AttachmentWrapper(attachmentJson, this.serviceClient);
    }
}
