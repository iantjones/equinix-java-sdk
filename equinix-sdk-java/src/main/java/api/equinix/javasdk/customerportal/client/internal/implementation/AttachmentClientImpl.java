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
import api.equinix.javasdk.core.enums.RequestType;
import api.equinix.javasdk.core.http.Utils;
import api.equinix.javasdk.core.http.request.EquinixRequest;
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.customerportal.client.implementation.CustomerPortalConfigImpl;
import api.equinix.javasdk.customerportal.client.internal.AttachmentClient;
import api.equinix.javasdk.customerportal.enums.AttachmentPurpose;
import api.equinix.javasdk.customerportal.model.Attachment;
import api.equinix.javasdk.customerportal.model.json.AttachmentJson;
import api.equinix.javasdk.customerportal.model.wrappers.AttachmentWrapper;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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

    public Page<Attachment, AttachmentJson> list(List<String> attachmentIds) {
        if (attachmentIds == null || attachmentIds.isEmpty()) {
            return listPage("ListAttachments");
        }
        return listPage("ListAttachments", Map.of("attachmentIds", attachmentIds));
    }

    public AttachmentJson getByUuid(String uuid) {
        return getOne("GetAttachment", Map.of("attachmentId", uuid));
    }

    /**
     * Uploads a file as a new attachment via a hand-built {@code multipart/form-data} request.
     *
     * <p>Maps to {@code POST /v1/attachments/file}. The file bytes are sent as the required
     * {@code uploadFile} form part and the {@code purpose} is passed as a query parameter. The
     * multipart entity is assembled by hand (httpmime/{@code MultipartEntityBuilder} is not a
     * dependency) using only httpcore classes and attached directly to the request.</p>
     */
    public AttachmentJson upload(byte[] fileBytes, String fileName, AttachmentPurpose purpose) {
        EquinixRequest<AttachmentJson> request = buildRequest("UploadAttachment", RequestType.SINGLE, AttachmentJson.class);
        if (purpose != null) {
            request.addSingleQueryParameter("purpose", purpose.name());
        }
        String boundary = "----EquinixSdkBoundary" + UUID.randomUUID().toString().replace("-", "");
        byte[] body = buildMultipartBody(boundary, "uploadFile", fileName, fileBytes);
        request.setContentType("multipart/form-data; boundary=" + boundary);
        request.setHttpEntity(new ByteArrayEntity(body, ContentType.create("multipart/form-data")));
        return Utils.handleSingletonResponse(invoke(request), request);
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

    /**
     * Builds a single-part {@code multipart/form-data} body with an explicit boundary.
     *
     * @param boundary  the multipart boundary string
     * @param fieldName the form field name (e.g. {@code uploadFile})
     * @param fileName  the file name to advertise in the part's Content-Disposition
     * @param fileBytes the raw file content
     * @return the encoded multipart body
     */
    private byte[] buildMultipartBody(String boundary, String fieldName, String fileName, byte[] fileBytes) {
        String safeFileName = fileName == null ? "file" : fileName;
        String header = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"" + fieldName + "\"; filename=\"" + safeFileName + "\"\r\n"
                + "Content-Type: application/octet-stream\r\n\r\n";
        String footer = "\r\n--" + boundary + "--\r\n";
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            out.write(header.getBytes(StandardCharsets.UTF_8));
            if (fileBytes != null) {
                out.write(fileBytes);
            }
            out.write(footer.getBytes(StandardCharsets.UTF_8));
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to assemble multipart upload body", e);
        }
    }
}
