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

package api.equinix.javasdk.customerportal.model.wrappers;

import api.equinix.javasdk.core.http.response.Pageable;
import api.equinix.javasdk.core.model.ResourceImpl;
import api.equinix.javasdk.customerportal.client.internal.implementation.AttachmentClientImpl;
import api.equinix.javasdk.customerportal.model.Attachment;
import api.equinix.javasdk.customerportal.model.json.AttachmentJson;
import lombok.Getter;
import lombok.experimental.Delegate;

public class AttachmentWrapper extends ResourceImpl<Attachment> implements Attachment {

    @Delegate(excludes = AttachmentMutability.class)
    private AttachmentJson jsonObject;
    @Getter
    private final Pageable<Attachment> serviceClient;

    public AttachmentWrapper(AttachmentJson attachmentJson, Pageable<Attachment> serviceClient) {
        this.jsonObject = attachmentJson;
        this.serviceClient = serviceClient;
    }

    public Boolean delete() {
        this.jsonObject = ((AttachmentClientImpl)this.serviceClient).delete(this.getUuid());
        return true;
    }

    public void refresh() {
        this.jsonObject = ((AttachmentClientImpl)this.serviceClient).refresh(this.getUuid());
    }

    private interface AttachmentMutability {
        Boolean delete();
        void refresh();
    }
}
