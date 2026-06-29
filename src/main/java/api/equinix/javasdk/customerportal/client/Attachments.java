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

package api.equinix.javasdk.customerportal.client;

import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.customerportal.enums.AttachmentPurpose;
import api.equinix.javasdk.customerportal.model.Attachment;

import java.util.List;

/**
 * Client interface for managing file attachments in the Equinix Customer Portal.
 *
 * <p>Backed by the Attachments v1 API at {@code /v1/attachments}. Provides listing, retrieval,
 * upload, download and deletion of attachments referenced by orders, tickets and cases.</p>
 */
public interface Attachments {

    /**
     * Lists all attachments for the current account.
     *
     * <p>Maps to {@code GET /v1/attachments} ({@code getAttachments}).</p>
     *
     * @return a paginated list of attachments
     */
    PaginatedList<Attachment> list();

    /**
     * Lists attachments for the current account filtered to the supplied attachment ids.
     *
     * <p>Maps to {@code GET /v1/attachments} ({@code getAttachments}) with the
     * {@code attachmentIds} query parameter.</p>
     *
     * @param attachmentIds the attachment ids to filter by, or {@code null}/empty for all
     * @return a paginated list of matching attachments
     */
    PaginatedList<Attachment> list(List<String> attachmentIds);

    /**
     * Retrieves a specific attachment's metadata by its identifier.
     *
     * <p>Maps to {@code GET /v1/attachments/{attachmentId}} ({@code getAttachment}).</p>
     *
     * @param attachmentId the identifier of the attachment
     * @return the matching attachment
     */
    Attachment getByUuid(String attachmentId);

    /**
     * Downloads an attachment's file bytes.
     *
     * <p>Maps to {@code GET /v1/attachments/{attachmentId}/file} ({@code getAttachedFile}).</p>
     *
     * @param attachmentId the identifier of the attachment
     * @return the file bytes
     */
    byte[] download(String attachmentId);

    /**
     * Uploads a file as a new attachment.
     *
     * <p>Maps to {@code POST /v1/attachments/file} ({@code createAttachment}). The file is sent as
     * a {@code multipart/form-data} {@code uploadFile} part and the {@code purpose} is supplied as
     * a query parameter.</p>
     *
     * @param fileBytes the raw file content (sent as the required {@code uploadFile} part)
     * @param fileName  the file name to advertise for the uploaded file
     * @param purpose   the purpose of the upload ({@code LOA} or {@code PO_DOCUMENT})
     * @return the created attachment
     */
    Attachment upload(byte[] fileBytes, String fileName, AttachmentPurpose purpose);
}
