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

package api.equinix.javasdk.networkedge.client.internal.implementation;

import api.equinix.javasdk.core.client.ClientBase;
import api.equinix.javasdk.core.enums.MetroCode;
import api.equinix.javasdk.core.enums.RequestType;
import api.equinix.javasdk.networkedge.client.implementation.NetworkEdgeConfigImpl;
import api.equinix.javasdk.networkedge.client.internal.FilesClient;
import api.equinix.javasdk.networkedge.enums.DeviceManagementType;
import api.equinix.javasdk.networkedge.enums.FileProcessType;
import api.equinix.javasdk.networkedge.enums.LicenseType;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author ianjones
 */
public class FilesClientImpl extends ClientBase implements FilesClient {

    public FilesClientImpl(NetworkEdgeConfigImpl configClient) {
        super(configClient, "NetworkEdge", "Files");
    }

    public String uploadFile(MetroCode metroCode, String deviceTypeCode, FileProcessType processType,
                             DeviceManagementType deviceManagementType, LicenseType licenseType, String fileContents) {
        Map<String, Object> body = new HashMap<>();
        if (metroCode != null) {
            body.put("metroCode", metroCode);
        }
        if (deviceTypeCode != null) {
            body.put("deviceTypeCode", deviceTypeCode);
        }
        if (processType != null) {
            body.put("processType", processType);
        }
        if (deviceManagementType != null) {
            body.put("deviceManagementType", deviceManagementType);
        }
        if (licenseType != null) {
            body.put("licenseType", licenseType);
        }
        if (fileContents != null) {
            body.put("file", fileContents);
        }
        return mapOp("UploadFile", RequestType.SINGLE, null, null, body).get("fileUuid");
    }
}
