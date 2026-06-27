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
import api.equinix.javasdk.networkedge.model.implementation.NetworkInterface;
import api.equinix.javasdk.networkedge.model.json.DeviceJson;
import api.equinix.javasdk.networkedge.model.json.creators.DeviceCreatorJson;
import api.equinix.javasdk.networkedge.model.json.creators.DeviceUpdaterJson;

import java.util.List;

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
     * <p>restore.</p>
     *
     * @param uuid a {@link java.lang.String} object.
     * @param backupUuid a {@link java.lang.String} object.
     * @return a {@link java.lang.Boolean} object.
     */
    Boolean restore(String uuid, String backupUuid);

    /**
     * <p>updateAdditionalBandwidth.</p>
     *
     * @param uuid a {@link java.lang.String} object.
     * @param additionalBandwidth a {@link java.lang.Integer} object.
     * @return a {@link api.equinix.javasdk.networkedge.model.json.DeviceJson} object.
     */
    DeviceJson updateAdditionalBandwidth(String uuid, Integer additionalBandwidth);

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
