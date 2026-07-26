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

package com.eqixiac.equinix.networkedge.client.internal.implementation;

import com.eqixiac.equinix.core.client.ClientBase;
import com.eqixiac.equinix.core.enums.MetroCode;
import com.eqixiac.equinix.core.enums.RequestType;
import com.eqixiac.equinix.core.exception.EquinixClientException;
import com.eqixiac.equinix.core.http.ResponseHandler;
import com.eqixiac.equinix.core.http.request.EquinixRequest;
import com.eqixiac.equinix.networkedge.client.implementation.NetworkEdgeConfigImpl;
import com.eqixiac.equinix.networkedge.client.internal.FilesClient;
import com.eqixiac.equinix.networkedge.enums.DeviceManagementType;
import com.eqixiac.equinix.networkedge.enums.FileProcessType;
import com.eqixiac.equinix.networkedge.enums.LicenseType;
import com.eqixiac.equinix.core.http.request.RequestBody;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 *
 * @author ianjones
 */
public class FilesClientImpl extends ClientBase implements FilesClient {

    public FilesClientImpl(NetworkEdgeConfigImpl configClient) {
        super(configClient, "NetworkEdge", "Files");
    }

    /**
     * Uploads a license / cloud-init file via a {@code multipart/form-data} request, as the spec's
     * {@code FileUploadRequest} for {@code POST /ne/v1/files} requires ({@code file} is declared
     * {@code type: string, format: binary}).
     *
     * <p>The scalar fields are sent as plain form parts and the file contents as a binary
     * {@code file} part. The multipart body is assembled by hand (httpmime /
     * {@code MultipartEntityBuilder} is not a dependency) and attached as a raw-bytes
     * {@code RequestBody}.</p>
     */
    public String uploadFile(MetroCode metroCode, String deviceTypeCode, FileProcessType processType,
                             DeviceManagementType deviceManagementType, LicenseType licenseType, String fileContents) {
        EquinixRequest<Object> request = buildRequest("UploadFile", RequestType.SINGLE, Object.class);

        Map<String, String> formFields = new LinkedHashMap<>();
        if (metroCode != null) {
            formFields.put("metroCode", metroCode.name());
        }
        if (deviceTypeCode != null) {
            formFields.put("deviceTypeCode", deviceTypeCode);
        }
        if (processType != null) {
            formFields.put("processType", processType.name());
        }
        if (deviceManagementType != null) {
            formFields.put("deviceManagementType", deviceManagementType.getJsonValue());
        }
        if (licenseType != null) {
            formFields.put("licenseType", licenseType.name());
        }

        String boundary = "----EquinixSdkBoundary" + UUID.randomUUID().toString().replace("-", "");
        byte[] body = buildMultipartBody(boundary, formFields, "file", "file",
                fileContents != null ? fileContents.getBytes(StandardCharsets.UTF_8) : null);
        request.setContentType("multipart/form-data; boundary=" + boundary);
        request.setBody(RequestBody.bytes(body, "multipart/form-data"));

        return ResponseHandler.handleMapResponse(request, invoke(request)).get("fileUuid");
    }

    /**
     * Builds a {@code multipart/form-data} body with plain-text form fields followed by a single
     * binary file part, using an explicit boundary.
     *
     * @param boundary      the multipart boundary string
     * @param formFields    the plain form fields (name → value), in order
     * @param fileFieldName the form field name of the file part (e.g. {@code file})
     * @param fileName      the file name to advertise in the file part's Content-Disposition
     * @param fileBytes     the raw file content
     * @return the encoded multipart body
     */
    private byte[] buildMultipartBody(String boundary, Map<String, String> formFields,
                                      String fileFieldName, String fileName, byte[] fileBytes) {
        StringBuilder fields = new StringBuilder();
        for (Map.Entry<String, String> field : formFields.entrySet()) {
            fields.append("--").append(boundary).append("\r\n")
                    .append("Content-Disposition: form-data; name=\"").append(field.getKey()).append("\"\r\n\r\n")
                    .append(field.getValue()).append("\r\n");
        }
        String fileHeader = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"" + fileFieldName + "\"; filename=\"" + fileName + "\"\r\n"
                + "Content-Type: application/octet-stream\r\n\r\n";
        String footer = "\r\n--" + boundary + "--\r\n";
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            out.write(fields.toString().getBytes(StandardCharsets.UTF_8));
            out.write(fileHeader.getBytes(StandardCharsets.UTF_8));
            if (fileBytes != null) {
                out.write(fileBytes);
            }
            out.write(footer.getBytes(StandardCharsets.UTF_8));
            return out.toByteArray();
        } catch (IOException e) {
            throw new EquinixClientException("Failed to assemble multipart upload body.", e);
        }
    }
}
