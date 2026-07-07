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

import api.equinix.javasdk.networkedge.enums.DeviceStatus;
import api.equinix.javasdk.networkedge.enums.LicenseStatus;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;

/**
 *
 * @author ianjones
 */
@Getter(AccessLevel.PACKAGE)
@Setter(AccessLevel.PACKAGE)
public class DeviceUpdaterJson {

    @JsonAlias("name")
    @JsonProperty("virtualDeviceName")
    private String virtualDeviceName;

    // Spec models the PATCH (and create) termLength as a string (e.g. "1, 12, 24, 36"); the
    // device-detail response also returns it as a string, so it is modelled as String here to
    // match the documented contract and keep create/update aligned.
    @JsonProperty("termLength")
    private String termLength;

    @JsonProperty("clusterName")
    private String clusterName;

    // READ_ONLY (serialize-only): DeviceJson.core is a DeviceCore object, so it must not be
    // mapped into this Integer when an updater is built from an existing device via convertValue.
    @JsonProperty(value = "core", access = JsonProperty.Access.READ_ONLY)
    private Integer core;

    @JsonProperty("termLengthEffectiveImmediate")
    private Boolean termLengthEffectiveImmediate;

    @JsonProperty("autoRenewalOptOut")
    private Boolean autoRenewalOptOut;

    @JsonProperty("vendorConfig")
    private VendorConfigPatch vendorConfig;

    // WRITE_ONLY (deserialize-only): captured from the device's licenseStatus when an updater is
    // built via convertValue; it is never part of the PATCH body.
    @JsonProperty(value = "licenseStatus", access = JsonProperty.Access.WRITE_ONLY)
    private LicenseStatus licenseStatus;

    // READ_ONLY (serialize-only): the spec's VirtualDeviceInternalPatchRequest.status field
    // ("Use this field to update the license status of a device", enum PROVISIONED/PROVISIONING/
    // DEPROVISIONED/DEPROVISIONING/FAILED). Must not be populated from the device's current status
    // when an updater is built via convertValue, so it is only sent when set explicitly.
    @JsonProperty(value = "status", access = JsonProperty.Access.READ_ONLY)
    private DeviceStatus status;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    static class VendorConfigPatch {
        @JsonProperty("disablePassword")
        private Boolean disablePassword;

        VendorConfigPatch() {
        }

        VendorConfigPatch(Boolean disablePassword) {
            this.disablePassword = disablePassword;
        }
    }

    @JsonProperty("notifications")
    private ArrayList<String> notifications;
}
