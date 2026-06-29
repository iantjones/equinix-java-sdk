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

package api.equinix.javasdk.networkedge.model.json.creators;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>Request body for triggering an RMA (Return Merchandise Authorization) of a virtual device.
 * The {@code version} is required; the remaining fields are vendor-dependent. The {@code vendorConfig}
 * map carries vendor-specific values (e.g. {@code siteId}, {@code systemIpAddress}, {@code licenseKey},
 * {@code adminPassword}) whose required subset varies by device type — refer to the API reference for
 * the exact payload per vendor.</p>
 *
 * <pre>{@code
 * DeviceRMARequest request = new DeviceRMARequest("17.09.01a")
 *     .withLicenseFileId("329a0bcd-0b2f-4bc5-b875-b506aa4b9730")
 *     .withVendorConfigValue("siteId", "567");
 * }</pre>
 *
 * @author ianjones
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DeviceRMARequest {

    @JsonProperty("version")
    private final String version;

    @JsonProperty("cloudInitFileId")
    private String cloudInitFileId;

    @JsonProperty("licenseFileId")
    private String licenseFileId;

    @JsonProperty("token")
    private String token;

    @JsonProperty("vendorConfig")
    private Map<String, String> vendorConfig;

    @JsonProperty("userPublicKey")
    private UserPublicKeyRequest userPublicKey;

    /**
     *
     * @param version any version you want to associate with the RMA (required).
     */
    public DeviceRMARequest(String version) {
        this.version = version;
    }

    /**
     * <p>For a C8KV device, the Id of an uploaded bootstrap file (see
     * {@link api.equinix.javasdk.networkedge.client.Setup#uploadFile}).</p>
     *
     * @param cloudInitFileId the uploaded bootstrap file id.
     * @return this {@link api.equinix.javasdk.networkedge.model.json.creators.DeviceRMARequest} for chaining.
     */
    public DeviceRMARequest withCloudInitFileId(String cloudInitFileId) {
        this.cloudInitFileId = cloudInitFileId;
        return this;
    }

    /**
     * <p>The Id of an uploaded license file (see {@code Devices#postLicenseFile}).</p>
     *
     * @param licenseFileId the uploaded license file id.
     * @return this {@link api.equinix.javasdk.networkedge.model.json.creators.DeviceRMARequest} for chaining.
     */
    public DeviceRMARequest withLicenseFileId(String licenseFileId) {
        this.licenseFileId = licenseFileId;
        return this;
    }

    /**
     * <p>A license token. For a cluster, provide tokens for both node0 and node1.</p>
     *
     * @param token the license token.
     * @return this {@link api.equinix.javasdk.networkedge.model.json.creators.DeviceRMARequest} for chaining.
     */
    public DeviceRMARequest withToken(String token) {
        this.token = token;
        return this;
    }

    /**
     * <p>Sets a single vendor-specific configuration value (e.g. {@code siteId},
     * {@code systemIpAddress}, {@code licenseKey}, {@code adminPassword}).</p>
     *
     * @param key the vendor configuration field name.
     * @param value the vendor configuration value.
     * @return this {@link api.equinix.javasdk.networkedge.model.json.creators.DeviceRMARequest} for chaining.
     */
    public DeviceRMARequest withVendorConfigValue(String key, String value) {
        if (this.vendorConfig == null) {
            this.vendorConfig = new HashMap<>();
        }
        this.vendorConfig.put(key, value);
        return this;
    }

    /**
     * <p>Replaces the full vendor-specific configuration map.</p>
     *
     * @param vendorConfig the vendor configuration values.
     * @return this {@link api.equinix.javasdk.networkedge.model.json.creators.DeviceRMARequest} for chaining.
     */
    public DeviceRMARequest withVendorConfig(Map<String, String> vendorConfig) {
        this.vendorConfig = vendorConfig;
        return this;
    }

    /**
     * <p>Sets the user public key details for the RMA. The {@code keyName} must reference an
     * existing public key (see {@code PublicKeys#define}).</p>
     *
     * @param username the username.
     * @param keyName the name of an existing public key.
     * @return this {@link api.equinix.javasdk.networkedge.model.json.creators.DeviceRMARequest} for chaining.
     */
    public DeviceRMARequest withUserPublicKey(String username, String keyName) {
        this.userPublicKey = new UserPublicKeyRequest(username, keyName);
        return this;
    }

    /**
     * <p>The user public key details supplied with an RMA request.</p>
     */
    @Getter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class UserPublicKeyRequest {

        @JsonProperty("username")
        private final String username;

        @JsonProperty("keyName")
        private final String keyName;

        /**
         *
         * @param username the username.
         * @param keyName the name of an existing public key.
         */
        public UserPublicKeyRequest(String username, String keyName) {
            this.username = username;
            this.keyName = keyName;
        }
    }
}
