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

package api.equinix.javasdk.customerportal.client.internal.implementation;

import api.equinix.javasdk.core.client.ResourceClientBase;
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.customerportal.client.implementation.CustomerPortalConfigImpl;
import api.equinix.javasdk.customerportal.client.internal.AttachmentClient;
import api.equinix.javasdk.customerportal.model.Attachment;
import api.equinix.javasdk.customerportal.model.json.AttachmentJson;
import api.equinix.javasdk.customerportal.model.json.creators.AttachmentCreatorJson;
import api.equinix.javasdk.customerportal.model.wrappers.AttachmentWrapper;

import java.util.Map;

public class AttachmentClientImpl extends ResourceClientBase<Attachment, AttachmentJson> implements AttachmentClient<Attachment> {

    public AttachmentClientImpl(CustomerPortalConfigImpl configClient) {
        super(configClient, "CustomerPortal", "Attachments", AttachmentJson.class);
    }

    @Override
    protected Attachment wrap(AttachmentJson json) {
        return new AttachmentWrapper(json, this);
    }

    public Page<Attachment, AttachmentJson> list() {
        return listPage("ListAttachments");
    }

    public AttachmentJson getByUuid(String uuid) {
        return getOne("GetAttachment", Map.of("attachmentId", uuid));
    }

    public AttachmentJson create(AttachmentCreatorJson attachmentCreatorJson) {
        return postOne("UploadAttachment", attachmentCreatorJson);
    }

    public byte[] download(String attachmentId) {
        return bytesOp("DownloadAttachment", Map.of("attachmentId", attachmentId), null);
    }

    public AttachmentJson delete(String uuid) {
        return deleteOne("DeleteAttachment", Map.of("attachmentId", uuid));
    }

    public AttachmentJson refresh(String uuid) {
        return this.getByUuid(uuid);
    }
}
