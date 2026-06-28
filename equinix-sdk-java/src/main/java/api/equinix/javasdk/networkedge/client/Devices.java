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

package api.equinix.javasdk.networkedge.client;

import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.core.enums.MetroCode;
import api.equinix.javasdk.networkedge.enums.LicenseType;
import api.equinix.javasdk.networkedge.model.Device;
import api.equinix.javasdk.networkedge.model.DeviceType;
import api.equinix.javasdk.networkedge.model.implementation.DeviceACL;
import api.equinix.javasdk.networkedge.model.implementation.NetworkInterface;
import api.equinix.javasdk.networkedge.model.json.creators.DeviceACLRequest;
import api.equinix.javasdk.networkedge.model.json.creators.DeviceOperator;
import api.equinix.javasdk.networkedge.model.json.creators.DeviceRMARequest;

import java.util.List;

/**
 * Client interface for managing Network Edge virtual devices. Provides operations
 * to list, retrieve, and create devices, as well as manage device types, network
 * interfaces, and license files.
 *
 * @author ianjones
 * @version $Id: $Id
 */
public interface Devices {

    /**
     * Lists available Network Edge devices.
     *
     * @return {@link api.equinix.javasdk.core.http.response.PaginatedList}
     */
    PaginatedList<Device> list();

    /**
     * Lists Network Edge devices based on the parameters provided.
     *
     * @param requestBuilder the desired query parameters.
     * @return {@link api.equinix.javasdk.core.http.response.PaginatedList}
     */
    PaginatedList<Device> list(RequestBuilder.Device requestBuilder);

    /**
     * Gets the specified device.
     *
     * @param uuid the unique identifier of the device.
     * @return {@link api.equinix.javasdk.networkedge.model.Device}
     */
    Device getByUuid(String uuid);

    /**
     * Lists the available device types.
     *
     * @return {@link api.equinix.javasdk.core.http.response.PaginatedList}
     */
    PaginatedList<DeviceType> listDeviceTypes();

    /**
     * Lists the interfaces for the specified device.
     *
     * @param uuid the unique identifier of the device.
     * @return {@link java.util.List}
     */
    List<NetworkInterface> listInterfaces(String uuid);

    /**
     * Returns an instance of DeviceBuilder for defining a new device.
     *
     * @return {@link api.equinix.javasdk.networkedge.model.json.creators.DeviceOperator.DeviceBuilder}
     * @param deviceName a {@link java.lang.String} object.
     */
    DeviceOperator.DeviceBuilder define(String deviceName);

    /**
     * Returns an instance of DeviceBuilderSecondary for defining a secondary device.
     *
     * @return {@link api.equinix.javasdk.networkedge.model.json.creators.DeviceOperator.DeviceBuilderSecondary}
     * @param secondaryDeviceName a {@link java.lang.String} object.
     * @param primaryDeviceUuid a {@link java.lang.String} object.
     */
    DeviceOperator.DeviceBuilderSecondary defineSecondary(String secondaryDeviceName, String primaryDeviceUuid);

    /**
     * Posts a new license file.
     *
     * @param metroCode the {@link api.equinix.javasdk.core.enums.MetroCode} to associate the license file with.
     * @param deviceTypeCode the device type code to associate the license file with.
     * @param licenseType the {@link api.equinix.javasdk.networkedge.enums.LicenseType} to associate the license file with.
     * @param fileContents the contents of the license file.
     * @return {@link java.lang.String} the fileId of the uploaded license file.
     */
    String postLicenseFile(MetroCode metroCode, String deviceTypeCode, LicenseType licenseType, String fileContents);

    /**
     * Triggers a soft reboot of the specified device.
     *
     * @param uuid the unique identifier of the device.
     * @return {@link java.lang.Boolean} {@code true} if the reboot request was accepted.
     */
    Boolean softReboot(String uuid);

    /**
     * Triggers an RMA (Return Merchandise Authorization) of the specified device. The required payload
     * fields vary by vendor; refer to the API reference for the exact payload per device type.
     *
     * @param uuid the unique identifier of the device.
     * @param deviceRMARequest the {@link api.equinix.javasdk.networkedge.model.json.creators.DeviceRMARequest} describing the RMA.
     * @return {@link java.lang.Boolean} {@code true} if the RMA request was accepted.
     */
    Boolean createRMA(String uuid, DeviceRMARequest deviceRMARequest);

    /**
     * Returns the ACL templates currently associated with the specified device, including each
     * template's provisioning status on the device.
     *
     * @param uuid the unique identifier of the device.
     * @return {@link api.equinix.javasdk.networkedge.model.implementation.DeviceACL}
     */
    DeviceACL getDeviceAcl(String uuid);

    /**
     * Associates ACL templates with the specified device.
     *
     * @param uuid the unique identifier of the device.
     * @param deviceACLRequest the {@link api.equinix.javasdk.networkedge.model.json.creators.DeviceACLRequest} describing the ACLs to apply.
     * @return {@link api.equinix.javasdk.networkedge.model.implementation.DeviceACL} the updated device ACL state.
     */
    DeviceACL addDeviceAcl(String uuid, DeviceACLRequest deviceACLRequest);

    /**
     * Associates ACL templates with the specified device on behalf of a customer account (reseller flow).
     *
     * @param uuid the unique identifier of the device.
     * @param deviceACLRequest the {@link api.equinix.javasdk.networkedge.model.json.creators.DeviceACLRequest} describing the ACLs to apply.
     * @param accountUcmId the customer account UCM id.
     * @return {@link api.equinix.javasdk.networkedge.model.implementation.DeviceACL} the updated device ACL state.
     */
    DeviceACL addDeviceAcl(String uuid, DeviceACLRequest deviceACLRequest, String accountUcmId);

    /**
     * Updates or removes the ACL templates associated with the specified device.
     *
     * @param uuid the unique identifier of the device.
     * @param deviceACLRequest the {@link api.equinix.javasdk.networkedge.model.json.creators.DeviceACLRequest} describing the desired ACLs.
     * @return {@link api.equinix.javasdk.networkedge.model.implementation.DeviceACL} the updated device ACL state.
     */
    DeviceACL updateDeviceAcl(String uuid, DeviceACLRequest deviceACLRequest);

    /**
     * Updates or removes the ACL templates associated with the specified device on behalf of a
     * customer account (reseller flow).
     *
     * @param uuid the unique identifier of the device.
     * @param deviceACLRequest the {@link api.equinix.javasdk.networkedge.model.json.creators.DeviceACLRequest} describing the desired ACLs.
     * @param accountUcmId the customer account UCM id.
     * @return {@link api.equinix.javasdk.networkedge.model.implementation.DeviceACL} the updated device ACL state.
     */
    DeviceACL updateDeviceAcl(String uuid, DeviceACLRequest deviceACLRequest, String accountUcmId);
}
