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

import api.equinix.javasdk.core.client.PageableBase;
import api.equinix.javasdk.core.enums.RequestType;
import api.equinix.javasdk.core.http.Utils;
import api.equinix.javasdk.core.http.request.EquinixRequest;
import api.equinix.javasdk.core.http.request.PaginatedRequest;
import api.equinix.javasdk.core.http.response.EquinixResponse;
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.customerportal.client.implementation.CustomerPortalConfigImpl;
import api.equinix.javasdk.customerportal.client.internal.AttachmentClient;
import api.equinix.javasdk.customerportal.model.Attachment;
import api.equinix.javasdk.customerportal.model.json.AttachmentJson;
import api.equinix.javasdk.customerportal.model.json.creators.AttachmentCreatorJson;
import api.equinix.javasdk.customerportal.model.wrappers.AttachmentWrapper;

import java.util.Map;

public class AttachmentClientImpl extends PageableBase implements AttachmentClient<Attachment> {

    public AttachmentClientImpl(CustomerPortalConfigImpl configClient) {
        super(configClient, "CustomerPortal", "Attachments");
    }

    public Page<Attachment, AttachmentJson> list() {
        EquinixRequest<Attachment> equinixRequest = this.buildRequest("ListAttachments", RequestType.PAGINATED, AttachmentJson.getPagedTypeRef());
        EquinixResponse<Attachment> equinixResponse = this.invoke(equinixRequest);
        return Utils.handlePaginatedListResponse(equinixResponse, equinixRequest);
    }

    public AttachmentJson getByUuid(String uuid) {
        Map<String, String> pParams = Map.of("uuid", uuid);
        EquinixRequest<AttachmentJson> equinixRequest = this.buildRequestWithPathParams("GetAttachment", RequestType.SINGLE, pParams, AttachmentJson.getSingleTypeRef());
        EquinixResponse<AttachmentJson> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

    public AttachmentJson create(AttachmentCreatorJson attachmentCreatorJson) {
        EquinixRequest<AttachmentJson> equinixRequest = this.buildRequest("UploadAttachment", RequestType.SINGLE, AttachmentJson.getSingleTypeRef());
        Utils.serializeJson(equinixRequest, attachmentCreatorJson);
        EquinixResponse<AttachmentJson> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

    public AttachmentJson delete(String uuid) {
        Map<String, String> pParams = Map.of("uuid", uuid);
        EquinixRequest<Attachment> equinixRequest = this.buildRequestWithPathParams("DeleteAttachment", RequestType.SINGLE, pParams, AttachmentJson.getSingleTypeRef());
        EquinixResponse<Attachment> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

    public AttachmentJson refresh(String uuid) {
        return this.getByUuid(uuid);
    }

    public PaginatedList<Attachment> nextPage(PaginatedRequest<Attachment> equinixRequest) {
        EquinixResponse<Attachment> equinixResponse = this.invoke(equinixRequest);
        Page<Attachment, AttachmentJson> nextPage = Utils.handlePaginatedListResponse(equinixResponse, equinixRequest);
        PaginatedList<Attachment> newPaginatedList = Utils.mapPaginatedList(nextPage.getItems(), this, AttachmentWrapper::new);
        return new PaginatedList<>(newPaginatedList, this, equinixRequest, equinixResponse, nextPage.getPagination());
    }
}
