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

package api.equinix.javasdk.customerportal.model.json.creators;

import api.equinix.javasdk.core.http.response.Pageable;
import api.equinix.javasdk.core.model.ResourceImpl;
import api.equinix.javasdk.customerportal.client.internal.implementation.AttachmentClientImpl;
import api.equinix.javasdk.customerportal.model.Attachment;
import api.equinix.javasdk.customerportal.model.json.AttachmentJson;
import api.equinix.javasdk.customerportal.model.wrappers.AttachmentWrapper;
import lombok.Getter;

public class AttachmentOperator extends ResourceImpl<Attachment> {

    @Getter
    private final Pageable<Attachment> serviceClient;

    public AttachmentOperator(Pageable<Attachment> serviceClient) {
        this.serviceClient = serviceClient;
    }

    public AttachmentBuilder create() {
        return new AttachmentBuilder();
    }

    @Getter
    public class AttachmentBuilder {
        private String fileName;
        private String entityType;
        private String entityUuid;

        public AttachmentBuilder fileName(String fileName) {
            this.fileName = fileName;
            return this;
        }

        public AttachmentBuilder entityType(String entityType) {
            this.entityType = entityType;
            return this;
        }

        public AttachmentBuilder entityUuid(String entityUuid) {
            this.entityUuid = entityUuid;
            return this;
        }

        public Attachment create() {
            AttachmentCreatorJson creatorJson = new AttachmentCreatorJson(this);
            AttachmentJson attachmentJson = ((AttachmentClientImpl) AttachmentOperator.this.getServiceClient()).create(creatorJson);
            return new AttachmentWrapper(attachmentJson, AttachmentOperator.this.getServiceClient());
        }
    }
}
