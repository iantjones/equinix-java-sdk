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
import api.equinix.javasdk.customerportal.client.internal.implementation.CrossConnectClientImpl;
import api.equinix.javasdk.customerportal.enums.CrossConnectType;
import api.equinix.javasdk.customerportal.enums.MediaType;
import api.equinix.javasdk.customerportal.model.CrossConnect;
import api.equinix.javasdk.customerportal.model.json.CrossConnectJson;
import api.equinix.javasdk.customerportal.model.wrappers.CrossConnectWrapper;
import lombok.Getter;

public class CrossConnectOperator extends ResourceImpl<CrossConnect> {

    @Getter
    private final Pageable<CrossConnect> serviceClient;

    public CrossConnectOperator(Pageable<CrossConnect> serviceClient) {
        this.serviceClient = serviceClient;
    }

    public CrossConnectBuilder create() {
        return new CrossConnectBuilder();
    }

    @Getter
    public class CrossConnectBuilder {
        private String name;
        private CrossConnectType type;
        private MediaType mediaType;
        private String aEndIbx;
        private String zEndIbx;
        private String aEndCageId;
        private String zEndCageId;
        private String aEndPatchPanelId;
        private String zEndPatchPanelId;
        private String aEndPatchPanelPortId;
        private String zEndPatchPanelPortId;
        private String protocolType;
        private Integer bandwidth;
        private String connectionService;
        private String accountNumber;
        private String customerReferenceId;

        public CrossConnectBuilder name(String name) {
            this.name = name;
            return this;
        }

        public CrossConnectBuilder type(CrossConnectType type) {
            this.type = type;
            return this;
        }

        public CrossConnectBuilder mediaType(MediaType mediaType) {
            this.mediaType = mediaType;
            return this;
        }

        public CrossConnectBuilder aEndIbx(String aEndIbx) {
            this.aEndIbx = aEndIbx;
            return this;
        }

        public CrossConnectBuilder zEndIbx(String zEndIbx) {
            this.zEndIbx = zEndIbx;
            return this;
        }

        public CrossConnectBuilder aEndCageId(String aEndCageId) {
            this.aEndCageId = aEndCageId;
            return this;
        }

        public CrossConnectBuilder zEndCageId(String zEndCageId) {
            this.zEndCageId = zEndCageId;
            return this;
        }

        public CrossConnectBuilder aEndPatchPanelId(String aEndPatchPanelId) {
            this.aEndPatchPanelId = aEndPatchPanelId;
            return this;
        }

        public CrossConnectBuilder zEndPatchPanelId(String zEndPatchPanelId) {
            this.zEndPatchPanelId = zEndPatchPanelId;
            return this;
        }

        public CrossConnectBuilder aEndPatchPanelPortId(String aEndPatchPanelPortId) {
            this.aEndPatchPanelPortId = aEndPatchPanelPortId;
            return this;
        }

        public CrossConnectBuilder zEndPatchPanelPortId(String zEndPatchPanelPortId) {
            this.zEndPatchPanelPortId = zEndPatchPanelPortId;
            return this;
        }

        public CrossConnectBuilder protocolType(String protocolType) {
            this.protocolType = protocolType;
            return this;
        }

        public CrossConnectBuilder bandwidth(Integer bandwidth) {
            this.bandwidth = bandwidth;
            return this;
        }

        public CrossConnectBuilder connectionService(String connectionService) {
            this.connectionService = connectionService;
            return this;
        }

        public CrossConnectBuilder accountNumber(String accountNumber) {
            this.accountNumber = accountNumber;
            return this;
        }

        public CrossConnectBuilder customerReferenceId(String customerReferenceId) {
            this.customerReferenceId = customerReferenceId;
            return this;
        }

        public CrossConnect create() {
            CrossConnectCreatorJson creatorJson = new CrossConnectCreatorJson(this);
            CrossConnectJson crossConnectJson = ((CrossConnectClientImpl) CrossConnectOperator.this.getServiceClient()).create(creatorJson);
            return new CrossConnectWrapper(crossConnectJson, CrossConnectOperator.this.getServiceClient());
        }
    }
}
