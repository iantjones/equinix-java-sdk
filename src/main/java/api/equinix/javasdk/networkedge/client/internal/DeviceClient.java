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

package api.equinix.javasdk.networkedge.client.internal;

import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.core.http.response.Pageable;
import api.equinix.javasdk.core.enums.MetroCode;
import api.equinix.javasdk.networkedge.client.RequestBuilder;
import api.equinix.javasdk.networkedge.enums.LicenseType;
import api.equinix.javasdk.networkedge.model.Device;
import api.equinix.javasdk.networkedge.model.implementation.AllowedInterfaceResponse;
import api.equinix.javasdk.networkedge.model.implementation.DeviceACL;
import api.equinix.javasdk.networkedge.model.implementation.DeviceReboot;
import api.equinix.javasdk.networkedge.model.implementation.DeviceUpgrade;
import api.equinix.javasdk.networkedge.model.implementation.DownloadableImage;
import api.equinix.javasdk.networkedge.model.implementation.ImageDownload;
import api.equinix.javasdk.networkedge.model.implementation.InterfaceStats;
import api.equinix.javasdk.networkedge.model.implementation.NetworkInterface;
import api.equinix.javasdk.networkedge.model.json.DeviceJson;
import api.equinix.javasdk.networkedge.model.json.creators.DeviceACLRequest;
import api.equinix.javasdk.networkedge.model.json.creators.DeviceCreatorJson;
import api.equinix.javasdk.networkedge.model.json.creators.DeviceRMARequest;
import api.equinix.javasdk.networkedge.model.json.creators.DeviceUpdaterJson;

import java.util.List;
import java.util.Map;

/**
 *
 * @author ianjones
 */
public interface DeviceClient<T> extends Pageable<T> {

    Page<DeviceJson> list(RequestBuilder.Device requestBuilder);

    DeviceJson getByUuid(String uuid);

    List<NetworkInterface> listInterfaces(String uuid);

    /**
     *
     * @param deviceType the device type code.
     * @param queryParams the configuration query parameters.
     */
    AllowedInterfaceResponse getAllowedInterfaces(String deviceType, Map<String, List<String>> queryParams);

    /**
     *
     * @param uuid the device uuid.
     */
    List<DeviceReboot> listReloadHistory(String uuid);

    /**
     *
     * @param uuid the device uuid.
     */
    List<DeviceUpgrade> listUpgradeHistory(String uuid);

    /**
     *
     * @param uuid the device uuid.
     * @param interfaceId the interface id.
     * @param startDateTime the start of the stats window (ISO-8601), or {@code null}.
     * @param endDateTime the end of the stats window (ISO-8601), or {@code null}.
     */
    InterfaceStats getInterfaceStatistics(String uuid, String interfaceId, String startDateTime, String endDateTime);

    /**
     *
     * @param deviceType the device type code.
     */
    List<DownloadableImage> listDownloadableImages(String deviceType);

    /**
     *
     * @param deviceType the device type code.
     * @param version the device version.
     */
    ImageDownload requestImageDownload(String deviceType, String version);

    /**
     * <p>restore. Restores the backup identified by {@code backupUuid}. Per the spec the backup
     * name is required in the request body.</p>
     *
     * @param backupUuid the unique identifier of the backup to restore.
     * @param backupName the name of the backup ({@code DeviceBackupUpdateRequest.name}).
     */
    Boolean restore(String backupUuid, String backupName);

    DeviceJson updateAdditionalBandwidth(String uuid, Integer additionalBandwidth);

    Boolean softReboot(String uuid);

    Boolean createRMA(String uuid, DeviceRMARequest deviceRMARequest);

    DeviceACL getACL(String uuid);

    DeviceACL addACL(String uuid, DeviceACLRequest deviceACLRequest, String accountUcmId);

    DeviceACL updateACL(String uuid, DeviceACLRequest deviceACLRequest, String accountUcmId);

    Boolean ping(String uuid);

    String postLicenseFile(MetroCode metroCode, String deviceTypeCode, LicenseType licenseType, String fileContents);

    String postLicenseFile(String deviceUuid, String fileContents);

    String updateLicenseToken(String deviceUuid, String licenseToken);

    DeviceJson create(DeviceCreatorJson deviceCreatorJson, Boolean draft);

    DeviceJson update(String uuid, DeviceUpdaterJson deviceUpdaterJson);

    Boolean delete(String uuid);

    DeviceJson refresh(String uuid);
}
