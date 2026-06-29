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

package api.equinix.javasdk.customerportal.model.json.creators;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;
import java.util.Map;

/**
 * Shared request body for all smart hands order types. Every typed create
 * ({@code equipmentInstall}, {@code shipmentUnpack}, {@code cageEscort}, etc.) uses these
 * common fields ({@code ibxLocation}, {@code contacts}, {@code schedule}, optional
 * {@code purchaseOrder}, {@code customerReferenceNumber} and {@code attachments}) together
 * with a per-type {@code serviceDetails} object whose shape varies by smart hands type.
 *
 * <p>{@code ibxLocation}, {@code contacts}, {@code schedule} and {@code serviceDetails} are
 * required by the API.</p>
 *
 * <p>For each order type a typed {@code serviceDetails} creator is available (e.g.
 * {@link EquipmentInstallDetails}, {@link ShipmentUnpackDetails}, {@link CageEscortDetails},
 * {@link MoveJumperCableDetails}, {@link RunJumperCableDetails}, {@link CableRequestDetails},
 * {@link LocatePackageDetails}, {@link PicturesDocumentDetails}, {@link PatchCableInstallDetails},
 * {@link PatchCableRemovalDetails}, {@link CageCleanupDetails}, {@link OtherSmartHandsDetails}).
 * Pass one to {@link #builder(IbxLocation, List, ScheduleInfo, Object)}. A free-form
 * {@code Map<String, Object>} remains available as an escape hatch via
 * {@link #builder(IbxLocation, List, ScheduleInfo, Map)}.</p>
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SmartHandsRequestJson {

    @JsonProperty("ibxLocation")
    private final IbxLocation ibxLocation;

    @JsonProperty("contacts")
    private final List<ContactInfo> contacts;

    @JsonProperty("schedule")
    private final ScheduleInfo schedule;

    /**
     * The per-type {@code serviceDetails} object whose shape varies by smart hands order type.
     * Typed as {@code Object} so it may be either a typed {@code serviceDetails} creator or a
     * free-form {@code Map<String, Object>} escape hatch; both serialize to the same JSON object.
     *
     * <p>The required keys for each type are defined by the corresponding
     * {@code *Request.serviceDetails} schema in the smarthands v1 spec. The order type maps to a
     * spec schema, typed creator and {@code createXxx} client method / typed POST path as
     * follows:</p>
     *
     * <ul>
     *     <li>{@code equipmentInstall} — {@link EquipmentInstallDetails}</li>
     *     <li>{@code cageCleanup} — {@link CageCleanupDetails}</li>
     *     <li>{@code cageEscort} — {@link CageEscortDetails}</li>
     *     <li>{@code shipmentUnpack} — {@link ShipmentUnpackDetails}</li>
     *     <li>{@code cableRequest} — {@link CableRequestDetails}</li>
     *     <li>{@code locatePackage} — {@link LocatePackageDetails}</li>
     *     <li>{@code moveJumperCable} — {@link MoveJumperCableDetails}</li>
     *     <li>{@code runJumperCable} — {@link RunJumperCableDetails}</li>
     *     <li>{@code patchCableInstall} — {@link PatchCableInstallDetails}</li>
     *     <li>{@code patchCableRemoval} — {@link PatchCableRemovalDetails}</li>
     *     <li>{@code picturesDocument} — {@link PicturesDocumentDetails}</li>
     *     <li>{@code other} — {@link OtherSmartHandsDetails}</li>
     * </ul>
     *
     * <p>Consult the smarthands v1 spec for the full set of properties and which are required for
     * the specific type you are ordering.</p>
     */
    @JsonProperty("serviceDetails")
    private final Object serviceDetails;

    @JsonProperty("customerReferenceNumber")
    private final String customerReferenceNumber;

    @JsonProperty("purchaseOrder")
    private final PurchaseOrderInfo purchaseOrder;

    @JsonProperty("attachments")
    private final List<SmartHandsAttachment> attachments;

    private SmartHandsRequestJson(Builder builder) {
        this.ibxLocation = builder.ibxLocation;
        this.contacts = builder.contacts;
        this.schedule = builder.schedule;
        this.serviceDetails = builder.serviceDetails;
        this.customerReferenceNumber = builder.customerReferenceNumber;
        this.purchaseOrder = builder.purchaseOrder;
        this.attachments = builder.attachments;
    }

    /**
     * Returns a new builder for a smart hands request body using a typed per-type
     * {@code serviceDetails} creator (e.g. {@link EquipmentInstallDetails}).
     *
     * @param ibxLocation    the IBX/cage location (required)
     * @param contacts       the ordering, technical and notification contacts (required)
     * @param schedule       the scheduling details (required)
     * @param serviceDetails the typed per-type service details (required)
     * @return a new builder
     */
    public static Builder builder(IbxLocation ibxLocation, List<ContactInfo> contacts, ScheduleInfo schedule,
                                  Object serviceDetails) {
        return new Builder(ibxLocation, contacts, schedule, serviceDetails);
    }

    /**
     * Returns a new builder for a smart hands request body using a free-form
     * {@code Map<String, Object>} {@code serviceDetails} escape hatch.
     *
     * @param ibxLocation    the IBX/cage location (required)
     * @param contacts       the ordering, technical and notification contacts (required)
     * @param schedule       the scheduling details (required)
     * @param serviceDetails the per-type service details as a free-form map (required)
     * @return a new builder
     */
    public static Builder builder(IbxLocation ibxLocation, List<ContactInfo> contacts, ScheduleInfo schedule,
                                  Map<String, Object> serviceDetails) {
        return new Builder(ibxLocation, contacts, schedule, (Object) serviceDetails);
    }

    public static class Builder {
        private final IbxLocation ibxLocation;
        private final List<ContactInfo> contacts;
        private final ScheduleInfo schedule;
        private final Object serviceDetails;
        private String customerReferenceNumber;
        private PurchaseOrderInfo purchaseOrder;
        private List<SmartHandsAttachment> attachments;

        private Builder(IbxLocation ibxLocation, List<ContactInfo> contacts, ScheduleInfo schedule,
                        Object serviceDetails) {
            this.ibxLocation = ibxLocation;
            this.contacts = contacts;
            this.schedule = schedule;
            this.serviceDetails = serviceDetails;
        }

        public Builder customerReferenceNumber(String customerReferenceNumber) {
            this.customerReferenceNumber = customerReferenceNumber;
            return this;
        }

        public Builder purchaseOrder(PurchaseOrderInfo purchaseOrder) {
            this.purchaseOrder = purchaseOrder;
            return this;
        }

        public Builder attachments(List<SmartHandsAttachment> attachments) {
            this.attachments = attachments;
            return this;
        }

        public SmartHandsRequestJson build() {
            return new SmartHandsRequestJson(this);
        }
    }
}
