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
 * <p>DeviceClient interface.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
public interface DeviceClient<T> extends Pageable<T> {

    /**
     * <p>list.</p>
     *
     * @param requestBuilder a {@link api.equinix.javasdk.networkedge.client.RequestBuilder.Device} object.
     * @return a {@link api.equinix.javasdk.core.http.response.Page} object.
     */
    Page<Device, DeviceJson> list(RequestBuilder.Device requestBuilder);

    /**
     * <p>getByUuid.</p>
     *
     * @param uuid a {@link java.lang.String} object.
     * @return a {@link api.equinix.javasdk.networkedge.model.json.DeviceJson} object.
     */
    DeviceJson getByUuid(String uuid);

    /**
     * <p>listInterfaces.</p>
     *
     * @param uuid a {@link java.lang.String} object.
     * @return a {@link java.util.List} object.
     */
    List<NetworkInterface> listInterfaces(String uuid);

    /**
     * <p>getAllowedInterfaces.</p>
     *
     * @param deviceType the device type code.
     * @param queryParams the configuration query parameters.
     * @return a {@link api.equinix.javasdk.networkedge.model.implementation.AllowedInterfaceResponse} object.
     */
    AllowedInterfaceResponse getAllowedInterfaces(String deviceType, Map<String, List<String>> queryParams);

    /**
     * <p>listReloadHistory.</p>
     *
     * @param uuid the device uuid.
     * @return a {@link java.util.List} object.
     */
    List<DeviceReboot> listReloadHistory(String uuid);

    /**
     * <p>listUpgradeHistory.</p>
     *
     * @param uuid the device uuid.
     * @return a {@link java.util.List} object.
     */
    List<DeviceUpgrade> listUpgradeHistory(String uuid);

    /**
     * <p>getInterfaceStatistics.</p>
     *
     * @param uuid the device uuid.
     * @param interfaceId the interface id.
     * @param startDateTime the start of the stats window (ISO-8601), or {@code null}.
     * @param endDateTime the end of the stats window (ISO-8601), or {@code null}.
     * @return a {@link api.equinix.javasdk.networkedge.model.implementation.InterfaceStats} object.
     */
    InterfaceStats getInterfaceStatistics(String uuid, String interfaceId, String startDateTime, String endDateTime);

    /**
     * <p>listDownloadableImages.</p>
     *
     * @param deviceType the device type code.
     * @return a {@link java.util.List} object.
     */
    List<DownloadableImage> listDownloadableImages(String deviceType);

    /**
     * <p>requestImageDownload.</p>
     *
     * @param deviceType the device type code.
     * @param version the device version.
     * @return a {@link api.equinix.javasdk.networkedge.model.implementation.ImageDownload} object.
     */
    ImageDownload requestImageDownload(String deviceType, String version);

    /**
     * <p>restore. Restores the backup identified by {@code backupUuid}. Per the spec the backup
     * name is required in the request body.</p>
     *
     * @param backupUuid the unique identifier of the backup to restore.
     * @param backupName the name of the backup ({@code DeviceBackupUpdateRequest.name}).
     * @return a {@link java.lang.Boolean} object.
     */
    Boolean restore(String backupUuid, String backupName);

    /**
     * <p>updateAdditionalBandwidth.</p>
     *
     * @param uuid a {@link java.lang.String} object.
     * @param additionalBandwidth a {@link java.lang.Integer} object.
     * @return a {@link api.equinix.javasdk.networkedge.model.json.DeviceJson} object.
     */
    DeviceJson updateAdditionalBandwidth(String uuid, Integer additionalBandwidth);

    /**
     * <p>softReboot.</p>
     *
     * @param uuid a {@link java.lang.String} object.
     * @return a {@link java.lang.Boolean} object.
     */
    Boolean softReboot(String uuid);

    /**
     * <p>createRMA.</p>
     *
     * @param uuid a {@link java.lang.String} object.
     * @param deviceRMARequest a {@link api.equinix.javasdk.networkedge.model.json.creators.DeviceRMARequest} object.
     * @return a {@link java.lang.Boolean} object.
     */
    Boolean createRMA(String uuid, DeviceRMARequest deviceRMARequest);

    /**
     * <p>getACL.</p>
     *
     * @param uuid a {@link java.lang.String} object.
     * @return a {@link api.equinix.javasdk.networkedge.model.implementation.DeviceACL} object.
     */
    DeviceACL getACL(String uuid);

    /**
     * <p>addACL.</p>
     *
     * @param uuid a {@link java.lang.String} object.
     * @param deviceACLRequest a {@link api.equinix.javasdk.networkedge.model.json.creators.DeviceACLRequest} object.
     * @param accountUcmId a {@link java.lang.String} object.
     * @return a {@link api.equinix.javasdk.networkedge.model.implementation.DeviceACL} object.
     */
    DeviceACL addACL(String uuid, DeviceACLRequest deviceACLRequest, String accountUcmId);

    /**
     * <p>updateACL.</p>
     *
     * @param uuid a {@link java.lang.String} object.
     * @param deviceACLRequest a {@link api.equinix.javasdk.networkedge.model.json.creators.DeviceACLRequest} object.
     * @param accountUcmId a {@link java.lang.String} object.
     * @return a {@link api.equinix.javasdk.networkedge.model.implementation.DeviceACL} object.
     */
    DeviceACL updateACL(String uuid, DeviceACLRequest deviceACLRequest, String accountUcmId);

    /**
     * <p>ping.</p>
     *
     * @param uuid a {@link java.lang.String} object.
     * @return a {@link java.lang.Boolean} object.
     */
    Boolean ping(String uuid);

    /**
     * <p>postLicenseFile.</p>
     *
     * @param metroCode a {@link api.equinix.javasdk.core.enums.MetroCode} object.
     * @param deviceTypeCode a {@link java.lang.String} object.
     * @param licenseType a {@link api.equinix.javasdk.networkedge.enums.LicenseType} object.
     * @param fileContents a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    String postLicenseFile(MetroCode metroCode, String deviceTypeCode, LicenseType licenseType, String fileContents);

    /**
     * <p>postLicenseFile.</p>
     *
     * @param deviceUuid a {@link java.lang.String} object.
     * @param fileContents a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    String postLicenseFile(String deviceUuid, String fileContents);

    /**
     * <p>updateLicenseToken.</p>
     *
     * @param deviceUuid a {@link java.lang.String} object.
     * @param licenseToken a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    String updateLicenseToken(String deviceUuid, String licenseToken);

    /**
     * <p>create.</p>
     *
     * @param deviceCreatorJson a {@link api.equinix.javasdk.networkedge.model.json.creators.DeviceCreatorJson} object.
     * @param draft a {@link java.lang.Boolean} object.
     * @return a {@link api.equinix.javasdk.networkedge.model.json.DeviceJson} object.
     */
    DeviceJson create(DeviceCreatorJson deviceCreatorJson, Boolean draft);

    /**
     * <p>update.</p>
     *
     * @param uuid a {@link java.lang.String} object.
     * @param deviceUpdaterJson a {@link api.equinix.javasdk.networkedge.model.json.creators.DeviceUpdaterJson} object.
     * @return a {@link api.equinix.javasdk.networkedge.model.json.DeviceJson} object.
     */
    DeviceJson update(String uuid, DeviceUpdaterJson deviceUpdaterJson);

    /**
     * <p>delete.</p>
     *
     * @param uuid a {@link java.lang.String} object.
     * @return a {@link java.lang.Boolean} object.
     */
    Boolean delete(String uuid);

    /**
     * <p>refresh.</p>
     *
     * @param uuid a {@link java.lang.String} object.
     * @return a {@link api.equinix.javasdk.networkedge.model.json.DeviceJson} object.
     */
    DeviceJson refresh(String uuid);
}
