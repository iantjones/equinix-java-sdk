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

package com.eqixiac.equinix.fabric.model.implementation.filter;

/**
 * The connection search filter properties of the Fabric v4 {@code SearchFieldName} schema
 * (spec 4.34.0), for {@code fabric.connections().search(...)}. Includes
 * {@link #A_SIDE_INTERCONNECT_UUID} ({@code /aSide/accessPoint/interconnect/uuid}), which finds the
 * connections of a Fabric Interconnect ({@code IC} access point).
 *
 * <pre>{@code
 * fabric.connections().search(Filter.filter().and()
 *     .equals(ConnectionSearchField.A_SIDE_INTERCONNECT_UUID.path(), interconnectUuid));
 * }</pre>
 */
public enum ConnectionSearchField {
    /** {@code /isRemote}. */
    IS_REMOTE("/isRemote"),
    /** {@code /name}. */
    NAME("/name"),
    /** {@code /uuid}. */
    UUID("/uuid"),
    /** {@code /type}. */
    TYPE("/type"),
    /** {@code /geoScope}. */
    GEO_SCOPE("/geoScope"),
    /** {@code /account/orgId}. */
    ACCOUNT_ORG_ID("/account/orgId"),
    /** {@code /aSide/accessPoint/account/accountName}. */
    A_SIDE_ACCOUNT_ACCOUNT_NAME("/aSide/accessPoint/account/accountName"),
    /** {@code /aSide/accessPoint/account/accountNumber}. */
    A_SIDE_ACCOUNT_ACCOUNT_NUMBER("/aSide/accessPoint/account/accountNumber"),
    /** {@code /aSide/accessPoint/router/uuid}. */
    A_SIDE_ROUTER_UUID("/aSide/accessPoint/router/uuid"),
    /** {@code /aSide/accessPoint/interconnect/uuid}. */
    A_SIDE_INTERCONNECT_UUID("/aSide/accessPoint/interconnect/uuid"),
    /** {@code /aSide/accessPoint/linkProtocol/vlanTagMin}. */
    A_SIDE_LINK_PROTOCOL_VLAN_TAG_MIN("/aSide/accessPoint/linkProtocol/vlanTagMin"),
    /** {@code /aSide/accessPoint/linkProtocol/vlanTagMax}. */
    A_SIDE_LINK_PROTOCOL_VLAN_TAG_MAX("/aSide/accessPoint/linkProtocol/vlanTagMax"),
    /** {@code /aSide/accessPoint/location/metroCode}. */
    A_SIDE_LOCATION_METRO_CODE("/aSide/accessPoint/location/metroCode"),
    /** {@code /aSide/accessPoint/location/metroName}. */
    A_SIDE_LOCATION_METRO_NAME("/aSide/accessPoint/location/metroName"),
    /** {@code /aSide/accessPoint/name}. */
    A_SIDE_NAME("/aSide/accessPoint/name"),
    /** {@code /aSide/accessPoint/port/uuid}. */
    A_SIDE_PORT_UUID("/aSide/accessPoint/port/uuid"),
    /** {@code /aSide/accessPoint/port/name}. */
    A_SIDE_PORT_NAME("/aSide/accessPoint/port/name"),
    /** {@code /aSide/accessPoint/type}. */
    A_SIDE_TYPE("/aSide/accessPoint/type"),
    /** {@code /aSide/accessPoint/virtualDevice/name}. */
    A_SIDE_VIRTUAL_DEVICE_NAME("/aSide/accessPoint/virtualDevice/name"),
    /** {@code /aSide/accessPoint/virtualDevice/uuid}. */
    A_SIDE_VIRTUAL_DEVICE_UUID("/aSide/accessPoint/virtualDevice/uuid"),
    /** {@code /aSide/serviceToken/uuid}. */
    A_SIDE_SERVICE_TOKEN_UUID("/aSide/serviceToken/uuid"),
    /** {@code /bandwidth}. */
    BANDWIDTH("/bandwidth"),
    /** {@code /change/status}. */
    CHANGE_STATUS("/change/status"),
    /** {@code /changeLog/createdBy}. */
    CHANGE_LOG_CREATED_BY("/changeLog/createdBy"),
    /** {@code /changeLog/createdDateTime}. */
    CHANGE_LOG_CREATED_DATE_TIME("/changeLog/createdDateTime"),
    /** {@code /changeLog/deletedBy}. */
    CHANGE_LOG_DELETED_BY("/changeLog/deletedBy"),
    /** {@code /changeLog/deletedDateTime}. */
    CHANGE_LOG_DELETED_DATE_TIME("/changeLog/deletedDateTime"),
    /** {@code /changeLog/lastUpdatedBy}. */
    CHANGE_LOG_LAST_UPDATED_BY("/changeLog/lastUpdatedBy"),
    /** {@code /operation/equinixStatus}. */
    OPERATION_EQUINIX_STATUS("/operation/equinixStatus"),
    /** {@code /operation/providerStatus}. */
    OPERATION_PROVIDER_STATUS("/operation/providerStatus"),
    /** {@code /operation/maintenanceStatus}. */
    OPERATION_MAINTENANCE_STATUS("/operation/maintenanceStatus"),
    /** {@code /operation/lockEnabled}. */
    OPERATION_LOCK_ENABLED("/operation/lockEnabled"),
    /** {@code /project/projectId}. */
    PROJECT_PROJECT_ID("/project/projectId"),
    /** {@code /redundancy/group}. */
    REDUNDANCY_GROUP("/redundancy/group"),
    /** {@code /redundancy/priority}. */
    REDUNDANCY_PRIORITY("/redundancy/priority"),
    /** {@code /redundancy/name}. */
    REDUNDANCY_NAME("/redundancy/name"),
    /** {@code /redundancy/description}. */
    REDUNDANCY_DESCRIPTION("/redundancy/description"),
    /** {@code /zSide/accessPoint/account/accountName}. */
    Z_SIDE_ACCOUNT_ACCOUNT_NAME("/zSide/accessPoint/account/accountName"),
    /** {@code /zSide/accessPoint/authenticationKey}. */
    Z_SIDE_AUTHENTICATION_KEY("/zSide/accessPoint/authenticationKey"),
    /** {@code /zSide/accessPoint/linkProtocol/vlanTagMin}. */
    Z_SIDE_LINK_PROTOCOL_VLAN_TAG_MIN("/zSide/accessPoint/linkProtocol/vlanTagMin"),
    /** {@code /zSide/accessPoint/linkProtocol/vlanTagMax}. */
    Z_SIDE_LINK_PROTOCOL_VLAN_TAG_MAX("/zSide/accessPoint/linkProtocol/vlanTagMax"),
    /** {@code /zSide/accessPoint/location/metroCode}. */
    Z_SIDE_LOCATION_METRO_CODE("/zSide/accessPoint/location/metroCode"),
    /** {@code /zSide/accessPoint/location/metroName}. */
    Z_SIDE_LOCATION_METRO_NAME("/zSide/accessPoint/location/metroName"),
    /** {@code /zSide/accessPoint/sellerRegion}. */
    Z_SIDE_SELLER_REGION("/zSide/accessPoint/sellerRegion"),
    /** {@code /zSide/accessPoint/name}. */
    Z_SIDE_NAME("/zSide/accessPoint/name"),
    /** {@code /zSide/accessPoint/port/uuid}. */
    Z_SIDE_PORT_UUID("/zSide/accessPoint/port/uuid"),
    /** {@code /zSide/accessPoint/network/uuid}. */
    Z_SIDE_NETWORK_UUID("/zSide/accessPoint/network/uuid"),
    /** {@code /zSide/accessPoint/port/name}. */
    Z_SIDE_PORT_NAME("/zSide/accessPoint/port/name"),
    /** {@code /zSide/accessPoint/profile/uuid}. */
    Z_SIDE_PROFILE_UUID("/zSide/accessPoint/profile/uuid"),
    /** {@code /zSide/accessPoint/type}. */
    Z_SIDE_TYPE("/zSide/accessPoint/type"),
    /** {@code /zSide/accessPoint/role}. */
    Z_SIDE_ROLE("/zSide/accessPoint/role"),
    /** {@code /zSide/accessPoint/virtualDevice/name}. */
    Z_SIDE_VIRTUAL_DEVICE_NAME("/zSide/accessPoint/virtualDevice/name"),
    /** {@code /zSide/accessPoint/virtualDevice/uuid}. */
    Z_SIDE_VIRTUAL_DEVICE_UUID("/zSide/accessPoint/virtualDevice/uuid"),
    /** {@code /zSide/serviceToken/uuid}. */
    Z_SIDE_SERVICE_TOKEN_UUID("/zSide/serviceToken/uuid"),
    /** {@code /zSide/internetAccess/uuid}. */
    Z_SIDE_INTERNET_ACCESS_UUID("/zSide/internetAccess/uuid"),
    /** {@code /state}. */
    STATE("/state");

    private final String path;

    ConnectionSearchField(String path) {
        this.path = path;
    }

    /**
     * Returns the filter property path.
     *
     * @return the path, for example {@code /aSide/accessPoint/interconnect/uuid}
     */
    public String path() {
        return path;
    }
}
