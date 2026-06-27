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
import api.equinix.javasdk.customerportal.model.Attachment;
import api.equinix.javasdk.customerportal.model.json.AttachmentJson;
import api.equinix.javasdk.customerportal.model.json.creators.AttachmentOperator;
import api.equinix.javasdk.customerportal.model.wrappers.AttachmentWrapper;

public class AttachmentsImpl implements Attachments {

    private final CustomerPortal serviceManager;

    private final AttachmentClient<Attachment> serviceClient;

    public AttachmentsImpl(AttachmentClient<Attachment> serviceClient, CustomerPortal serviceManager) {
        this.serviceManager = serviceManager;
        this.serviceClient = serviceClient;
    }

    public PaginatedList<Attachment> list() {
        Page<Attachment, AttachmentJson> responsePage = this.serviceClient.list();
        PaginatedList<Attachment> attachmentList = Utils.mapPaginatedList(responsePage.getItems(), this.serviceClient, AttachmentWrapper::new);
        return new PaginatedList<>(attachmentList, this.serviceClient, responsePage.getAssociatedRequest(), responsePage.getAssociatedResponse(), responsePage.getPagination());
    }

    public Attachment getByUuid(String uuid) {
        AttachmentJson attachmentJson = this.serviceClient.getByUuid(uuid);
        return new AttachmentWrapper(attachmentJson, this.serviceClient);
    }

    public AttachmentOperator.AttachmentBuilder define() {
        return new AttachmentOperator(this.serviceClient).create();
    }
}
