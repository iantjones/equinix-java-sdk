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

package com.eqixiac.equinix.customerportal.client.implementation;

import com.eqixiac.equinix.CustomerPortal;
import com.eqixiac.equinix.customerportal.client.SmartHandsRequests;
import com.eqixiac.equinix.customerportal.client.internal.SmartHandsClient;
import com.eqixiac.equinix.customerportal.model.SmartHandResponse;
import com.eqixiac.equinix.customerportal.model.SmartHandType;
import com.eqixiac.equinix.customerportal.model.SmartHandsLocation;
import com.eqixiac.equinix.customerportal.model.json.creators.SmartHandsRequestJson;

import java.util.List;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SmartHandsRequestsImpl implements SmartHandsRequests {

    private final SmartHandsClient serviceClient;

    private final CustomerPortal serviceManager;

    public SmartHandResponse createEquipmentInstall(SmartHandsRequestJson request) {
        return this.serviceClient.create("CreateEquipmentInstall", request);
    }

    public SmartHandResponse createShipmentUnpack(SmartHandsRequestJson request) {
        return this.serviceClient.create("CreateShipmentUnpack", request);
    }

    public SmartHandResponse createMoveJumperCable(SmartHandsRequestJson request) {
        return this.serviceClient.create("CreateMoveJumperCable", request);
    }

    public SmartHandResponse createCageEscort(SmartHandsRequestJson request) {
        return this.serviceClient.create("CreateCageEscort", request);
    }

    public SmartHandResponse createLocatePackage(SmartHandsRequestJson request) {
        return this.serviceClient.create("CreateLocatePackage", request);
    }

    public SmartHandResponse createPicturesDocument(SmartHandsRequestJson request) {
        return this.serviceClient.create("CreatePicturesDocument", request);
    }

    public SmartHandResponse createPatchCableInstall(SmartHandsRequestJson request) {
        return this.serviceClient.create("CreatePatchCableInstall", request);
    }

    public SmartHandResponse createPatchCableRemoval(SmartHandsRequestJson request) {
        return this.serviceClient.create("CreatePatchCableRemoval", request);
    }

    public SmartHandResponse createCageCleanup(SmartHandsRequestJson request) {
        return this.serviceClient.create("CreateCageCleanup", request);
    }

    public SmartHandResponse createCableRequest(SmartHandsRequestJson request) {
        return this.serviceClient.create("CreateCableRequest", request);
    }

    public SmartHandResponse createRunJumperCable(SmartHandsRequestJson request) {
        return this.serviceClient.create("CreateRunJumperCable", request);
    }

    public SmartHandResponse createOther(SmartHandsRequestJson request) {
        return this.serviceClient.create("CreateOther", request);
    }

    public List<? extends SmartHandsLocation> listLocations() {
        return this.serviceClient.listLocations(null, null, null);
    }

    public List<? extends SmartHandsLocation> listLocations(Boolean detail, String ibxs, String cages) {
        return this.serviceClient.listLocations(detail, ibxs, cages);
    }

    public List<? extends SmartHandType> listTypes() {
        return this.serviceClient.listTypes();
    }
}
