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

package com.eqixiac.equinix.customerportal.model.json.creators;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * A single cross-connect deinstallation entry ({@code Layer1_Deinstall_Details} in the
 * cross-connects v2 spec). {@code assetId} is required; {@code proceedWithLiveTraffic} and
 * {@code patchEquipment} are optional.
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Layer1DeinstallDetail {

    @JsonProperty("assetId")
    private final String assetId;

    @JsonProperty("proceedWithLiveTraffic")
    private Boolean proceedWithLiveTraffic;

    @JsonProperty("patchEquipment")
    private PatchEquipment patchEquipment;

    private Layer1DeinstallDetail(String assetId) {
        this.assetId = assetId;
    }

    /**
     * Creates a deinstall detail for the given cross-connect asset.
     *
     * @param assetId asset id of the cross connect to de-install (required)
     * @return the deinstall detail
     */
    public static Layer1DeinstallDetail of(String assetId) {
        return new Layer1DeinstallDetail(assetId);
    }

    /**
     * When {@code true}, Equinix proceeds with deinstallation despite detecting live traffic.
     *
     * @param proceedWithLiveTraffic whether to proceed with live traffic
     * @return this deinstall detail
     */
    public Layer1DeinstallDetail proceedWithLiveTraffic(Boolean proceedWithLiveTraffic) {
        this.proceedWithLiveTraffic = proceedWithLiveTraffic;
        return this;
    }

    /**
     * Sets the patch equipment to remove during deinstallation.
     *
     * @param patchEquipment the patch equipment to remove
     * @return this deinstall detail
     */
    public Layer1DeinstallDetail patchEquipment(PatchEquipment patchEquipment) {
        this.patchEquipment = patchEquipment;
        return this;
    }

    /**
     * Patch equipment to remove during a cross-connect deinstallation. Unlike the install-time
     * patch equipment, no connector type is supplied; {@code cabinetId}, {@code details} and
     * {@code port} are all required by the spec.
     */
    @Getter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PatchEquipment {

        @JsonProperty("cabinetId")
        private final String cabinetId;

        @JsonProperty("details")
        private final String details;

        @JsonProperty("port")
        private final Integer port;

        /**
         * Creates patch equipment to remove.
         *
         * @param cabinetId the cabinet to remove the patch cable from (required)
         * @param details   additional information to facilitate removal (required)
         * @param port      the device port number to be removed (required)
         */
        public PatchEquipment(String cabinetId, String details, Integer port) {
            this.cabinetId = cabinetId;
            this.details = details;
            this.port = port;
        }
    }
}
