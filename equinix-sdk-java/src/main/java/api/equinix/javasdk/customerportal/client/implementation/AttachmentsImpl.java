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

package api.equinix.javasdk.customerportal.client.implementation;

import api.equinix.javasdk.CustomerPortal;
import api.equinix.javasdk.core.http.Utils;
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.customerportal.client.Attachments;
import api.equinix.javasdk.customerportal.client.internal.AttachmentClient;
import api.equinix.javasdk.customerportal.enums.AttachmentPurpose;
import api.equinix.javasdk.customerportal.model.Attachment;
import api.equinix.javasdk.customerportal.model.json.AttachmentJson;
import api.equinix.javasdk.customerportal.model.wrappers.AttachmentWrapper;

import java.util.List;

public class AttachmentsImpl implements Attachments {

    private final CustomerPortal serviceManager;

    private final AttachmentClient<Attachment> serviceClient;

    public AttachmentsImpl(AttachmentClient<Attachment> serviceClient, CustomerPortal serviceManager) {
        this.serviceManager = serviceManager;
        this.serviceClient = serviceClient;
    }

    public PaginatedList<Attachment> list() {
        return list(null);
    }

    public PaginatedList<Attachment> list(List<String> attachmentIds) {
        Page<Attachment, AttachmentJson> responsePage = this.serviceClient.list(attachmentIds);
        PaginatedList<Attachment> attachmentList = Utils.mapPaginatedList(responsePage.getItems(), this.serviceClient, AttachmentWrapper::new);
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
