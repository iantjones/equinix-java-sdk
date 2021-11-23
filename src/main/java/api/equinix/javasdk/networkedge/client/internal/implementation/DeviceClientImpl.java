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

import api.equinix.javasdk.core.client.PageableBase;
import api.equinix.javasdk.core.http.Utils;
import api.equinix.javasdk.core.http.request.EquinixRequest;
import api.equinix.javasdk.core.http.request.PaginatedRequest;
import api.equinix.javasdk.core.http.response.EquinixResponse;
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.core.enums.MetroCode;
import api.equinix.javasdk.core.enums.RequestType;
import api.equinix.javasdk.networkedge.client.RequestBuilder;
import api.equinix.javasdk.networkedge.client.implementation.NetworkEdgeConfigImpl;
import api.equinix.javasdk.networkedge.client.internal.DeviceClient;
import api.equinix.javasdk.networkedge.enums.LicenseType;
import api.equinix.javasdk.networkedge.model.Device;
import api.equinix.javasdk.networkedge.model.implementation.NetworkInterface;
import api.equinix.javasdk.networkedge.model.implementation.UUIDResult;
import api.equinix.javasdk.networkedge.model.json.DeviceJson;
import api.equinix.javasdk.networkedge.model.json.Pricing;
import api.equinix.javasdk.networkedge.model.json.VPNJson;
import api.equinix.javasdk.networkedge.model.json.creators.DeviceCreatorJson;
import api.equinix.javasdk.networkedge.model.json.creators.DeviceUpdaterJson;
import api.equinix.javasdk.networkedge.model.wrappers.DeviceWrapper;
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.List;
import java.util.Map;

/**
 * <p>DeviceClientImpl class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
public class DeviceClientImpl extends PageableBase implements DeviceClient<Device> {

    /**
     * <p>Constructor for DeviceClientImpl.</p>
     *
     * @param configClient a {@link api.equinix.javasdk.networkedge.client.implementation.NetworkEdgeConfigImpl} object.
     */
    public DeviceClientImpl(NetworkEdgeConfigImpl configClient) {
        super(configClient, "NetworkEdge", "Devices");
    }

    /**
     * {@inheritDoc}
     *
     * <p>list.</p>
     */
    public Page<Device, DeviceJson> list(RequestBuilder.Device requestBuilder) {
        Map<String, List<String>> qParams = Utils.newMap(requestBuilder);

        EquinixRequest<Device> equinixRequest = this.buildRequest("ListDevices", RequestType.PAGINATED, null, qParams, DeviceJson.getPagedTypeRef());
        EquinixResponse<Device> equinixResponse = this.invoke(equinixRequest);
        return Utils.handlePaginatedListResponse(equinixResponse, equinixRequest);
    }

    /** {@inheritDoc} */
    public DeviceJson getByUuid(String uuid) {
        Map<String, String> pParams = Map.of("uuid", uuid);

        EquinixRequest<DeviceJson> equinixRequest = this.buildRequestWithPathParams("GetDevice", RequestType.SINGLE, pParams, DeviceJson.getSingleTypeRef());
        EquinixResponse<DeviceJson> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

    /** {@inheritDoc} */
    public List<NetworkInterface> listInterfaces(String uuid) {
        Map<String, String> pParams = Map.of("uuid", uuid);

        EquinixRequest<NetworkInterface> equinixRequest = this.buildRequestWithPathParams("GetNetworkInterfaces", RequestType.LIST, pParams, NetworkInterface.getListTypeRef());
        EquinixResponse<NetworkInterface> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleListResponse(equinixResponse, equinixRequest);
    }

    /** {@inheritDoc} */
    public Boolean restore(String uuid, String backupUuid) {
        Map<String, List<String>> qParams = Map.of("backupUuid", Utils.singleParamList(backupUuid));
        Map<String, String> pParams = Map.of("uuid", uuid);

        EquinixRequest<DeviceJson> equinixRequest = this.buildRequest("RestoreBackup", RequestType.SINGLE, pParams, qParams);
        EquinixResponse<DeviceJson> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleBooleanResponse(equinixResponse, equinixRequest);
    }

    /** {@inheritDoc} */
    public DeviceJson updateAdditionalBandwidth(String uuid, Integer additionalBandwidth) {
        Map<String, String> pParams = Map.of("uuid", uuid);

        Map<String, Integer> requestBody = Utils.singlePropertyBody("additionalBandwidth", additionalBandwidth);
        EquinixRequest<DeviceJson> equinixRequest = this.buildRequestWithPathParams("UpdateAdditionalBandwidth", RequestType.SINGLE, pParams);
        Utils.serializeJson(equinixRequest, requestBody);
        EquinixResponse<DeviceJson> equinixResponse = this.invoke(equinixRequest);
        return getByUuid(uuid);
    }

    /** {@inheritDoc} */
    public Boolean ping(String uuid) {
        Map<String, String> pParams = Map.of("uuid", uuid);

        EquinixRequest<DeviceJson> equinixRequest = this.buildRequestWithPathParams("PingDevice", RequestType.SINGLE, pParams, DeviceJson.getCreateTypeRef());
        EquinixResponse<DeviceJson> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleBooleanResponse(equinixResponse, equinixRequest);
    }

    /** {@inheritDoc} */
    public String postLicenseFile(MetroCode metroCode, String deviceTypeCode, LicenseType licenseType, String fileContents) {
        Map<String, List<String>> qParams = Map.of("metroCode", Utils.singleParamList(metroCode),
                "deviceTypeCode", Utils.singleParamList(deviceTypeCode),
                "licenseType", Utils.singleParamList(licenseType));
        Map<String, String> payload = Map.of("file", fileContents);

        EquinixRequest<Device> equinixRequest = this.buildRequestWithQueryParams("PostLicense", RequestType.SINGLE, qParams, new TypeReference<Map<String, String>>() {});
        Utils.serializeJson(equinixRequest, payload);
        EquinixResponse<Device> equinixResponse = this.invoke(equinixRequest);
        Map<String, String> responseMap = Utils.handleMapResponse(equinixRequest, equinixResponse);
        return responseMap.get("fileId");
    }

    /** {@inheritDoc} */
    public String postLicenseFile(String deviceUuid, String fileContents) {
        Map<String, String> pParams = Map.of("uuid", deviceUuid);
        Map<String, String> payload = Map.of("file", fileContents);

        EquinixRequest<Device> equinixRequest = this.buildRequestWithPathParams("PostLicenseToDevice", RequestType.SINGLE, pParams, new TypeReference<Map<String, String>>() {});
        Utils.serializeJson(equinixRequest, payload);
        EquinixResponse<Device> equinixResponse = this.invoke(equinixRequest);
        Map<String, String> responseMap = Utils.handleMapResponse(equinixRequest, equinixResponse);
        return responseMap.get("fileId");
    }

    /** {@inheritDoc} */
    public String updateLicenseToken(String deviceUuid, String licenseToken) {
        Map<String, String> pParams = Map.of("uuid", deviceUuid);
        Map<String, String> payload = Map.of("token", licenseToken);

        EquinixRequest<Device> equinixRequest = this.buildRequestWithPathParams("UpdateLicenseToken", RequestType.SINGLE, pParams, new TypeReference<Map<String, String>>() {});
        Utils.serializeJson(equinixRequest, payload);
        EquinixResponse<Device> equinixResponse = this.invoke(equinixRequest);
        Map<String, String> responseMap = Utils.handleMapResponse(equinixRequest, equinixResponse);
        return responseMap.get("fileId");
    }

    /**
     * <p>getPricing.</p>
     *
     * @param deviceUuid a {@link java.lang.String} object.
     * @return a {@link api.equinix.javasdk.networkedge.model.json.Pricing} object.
     */
    public Pricing getPricing(String deviceUuid) {
        Map<String, List<String>> qParams = Utils.singleParamMap("virtualDeviceUuid", deviceUuid);
        EquinixRequest<VPNJson> equinixRequest = this.buildRequestWithQueryParams("GetPricing", RequestType.SINGLE, qParams, Pricing.getSingleTypeRef());
        EquinixResponse<VPNJson> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

    /** {@inheritDoc} */
    public DeviceJson create(DeviceCreatorJson deviceCreatorJson, Boolean draft) {
        Map<String, List<String>> qParams = Utils.singleParamMap("draft", draft);

        EquinixRequest<DeviceJson> equinixRequest = this.buildRequestWithQueryParams("CreateDevice", RequestType.SINGLE, qParams, DeviceJson.getCreateTypeRef());
        Utils.serializeJson(equinixRequest, deviceCreatorJson);

        EquinixResponse<DeviceJson> equinixResponse = this.invoke(equinixRequest);
        UUIDResult uuidResult = Utils.handleSingletonResponse(equinixResponse, equinixRequest);
        return getByUuid(uuidResult.getUuid());
    }

    /** {@inheritDoc} */
    public DeviceJson update(String uuid, DeviceUpdaterJson deviceUpdaterJson) {
        Map<String, String> pParams = Map.of("uuid", uuid);

        EquinixRequest<DeviceUpdaterJson> equinixRequest = this.buildRequestWithPathParams("UpdateDevice", RequestType.SINGLE, pParams);
        Utils.serializeJson(equinixRequest, deviceUpdaterJson);
        EquinixResponse<DeviceUpdaterJson> equinixResponse = this.invoke(equinixRequest);
        return getByUuid(uuid);
    }

    /** {@inheritDoc} */
    public Boolean delete(String uuid) {
        Map<String, String> pParams = Map.of("uuid", uuid);
        EquinixRequest<Device> equinixRequest = this.buildRequestWithPathParams("DeleteDevice", RequestType.SINGLE, pParams);
        EquinixResponse<Device> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleBooleanResponse(equinixResponse, equinixRequest);
    }

    /** {@inheritDoc} */
    public DeviceJson refresh(String uuid) {
        return this.getByUuid(uuid);
    }

    /** {@inheritDoc} */
    @Override
    public PaginatedList<Device> nextPage(PaginatedRequest<Device> equinixRequest) {
        EquinixResponse<Device> equinixResponse = this.invoke(equinixRequest);
        Page<Device, DeviceJson> nextPage = Utils.handlePaginatedListResponse(equinixResponse, equinixRequest);
        PaginatedList<Device> newPaginatedList = Utils.mapPaginatedList(nextPage.getItems(), this, DeviceWrapper::new);
        return new PaginatedList<>(newPaginatedList, this, equinixRequest, equinixResponse, nextPage.getPagination());
    }
}
