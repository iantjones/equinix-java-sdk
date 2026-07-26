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

package com.eqixiac.equinix.customerportal.client;

import com.eqixiac.equinix.customerportal.model.SmartHandResponse;
import com.eqixiac.equinix.customerportal.model.SmartHandType;
import com.eqixiac.equinix.customerportal.model.SmartHandsLocation;
import com.eqixiac.equinix.customerportal.model.json.creators.SmartHandsRequestJson;

import java.util.List;

/**
 * Client interface for submitting Smart Hands orders in the Equinix Customer Portal.
 *
 * <p>Backed by the Smart Hands v1 API at {@code /v1/orders/smarthands/{type}}. Each Smart Hands
 * service has its own typed create operation; all share the common request envelope
 * ({@link SmartHandsRequestJson}: IBX location, contacts, schedule, optional purchase order and
 * attachments) plus a per-type {@code serviceDetails} object. A typed {@code serviceDetails}
 * creator exists for every order type (e.g.
 * {@link com.eqixiac.equinix.customerportal.model.json.creators.EquipmentInstallDetails},
 * {@link com.eqixiac.equinix.customerportal.model.json.creators.RunJumperCableDetails}); a
 * free-form {@code Map<String, Object>} remains available as an escape hatch. Reference data is
 * available via {@link #listLocations()} and {@link #listTypes()}.</p>
 */
public interface SmartHandsRequests {

    /**
     * Requests equipment installation per your specifications by an IBX technician.
     *
     * @param request the smart hands request body
     * @return the created order response
     */
    SmartHandResponse createEquipmentInstall(SmartHandsRequestJson request);

    /**
     * Requests inbound shipment unpacking and packaging disposal.
     *
     * @param request the smart hands request body
     * @return the created order response
     */
    SmartHandResponse createShipmentUnpack(SmartHandsRequestJson request);

    /**
     * Requests a jumper cable to be moved.
     *
     * @param request the smart hands request body
     * @return the created order response
     */
    SmartHandResponse createMoveJumperCable(SmartHandsRequestJson request);

    /**
     * Requests an escort into a cage.
     *
     * @param request the smart hands request body
     * @return the created order response
     */
    SmartHandResponse createCageEscort(SmartHandsRequestJson request);

    /**
     * Requests a package to be located.
     *
     * @param request the smart hands request body
     * @return the created order response
     */
    SmartHandResponse createLocatePackage(SmartHandsRequestJson request);

    /**
     * Requests pictures or documentation of equipment.
     *
     * @param request the smart hands request body
     * @return the created order response
     */
    SmartHandResponse createPicturesDocument(SmartHandsRequestJson request);

    /**
     * Requests a patch cable to be installed.
     *
     * @param request the smart hands request body
     * @return the created order response
     */
    SmartHandResponse createPatchCableInstall(SmartHandsRequestJson request);

    /**
     * Requests a patch cable to be removed.
     *
     * @param request the smart hands request body
     * @return the created order response
     */
    SmartHandResponse createPatchCableRemoval(SmartHandsRequestJson request);

    /**
     * Requests a cage cleanup.
     *
     * @param request the smart hands request body
     * @return the created order response
     */
    SmartHandResponse createCageCleanup(SmartHandsRequestJson request);

    /**
     * Requests a cable.
     *
     * @param request the smart hands request body
     * @return the created order response
     */
    SmartHandResponse createCableRequest(SmartHandsRequestJson request);

    /**
     * Requests a jumper cable to be run.
     *
     * @param request the smart hands request body
     * @return the created order response
     */
    SmartHandResponse createRunJumperCable(SmartHandsRequestJson request);

    /**
     * Requests another smart hands service not covered by a dedicated type.
     *
     * @param request the smart hands request body
     * @return the created order response
     */
    SmartHandResponse createOther(SmartHandsRequestJson request);

    /**
     * Lists the IBX locations, cages and cabinets where the current user may place smart
     * hands orders.
     *
     * @return the list of permitted locations
     */
    List<? extends SmartHandsLocation> listLocations();

    /**
     * Lists the IBX locations, cages and cabinets where the current user may place smart hands
     * orders, optionally filtered.
     *
     * <p>Maps to {@code GET /v1/orders/smarthands/locations} ({@code getLocation}).</p>
     *
     * @param detail when {@code true}, returns detailed permission with cages and cabinets, or {@code null} for the default
     * @param ibxs   a comma-separated list of IBX codes to filter by (e.g. {@code AM1,AM2}), or {@code null}
     * @param cages  a comma-separated list of cage ids to filter by (e.g. {@code AM1:02:002MC1}), or {@code null}
     * @return the list of permitted locations
     */
    List<? extends SmartHandsLocation> listLocations(Boolean detail, String ibxs, String cages);

    /**
     * Lists all supported smart hands order types.
     *
     * @return the list of supported types
     */
    List<? extends SmartHandType> listTypes();
}
