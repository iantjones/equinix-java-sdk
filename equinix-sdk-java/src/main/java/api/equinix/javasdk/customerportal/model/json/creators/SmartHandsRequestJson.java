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
     * Because the API exposes around a dozen distinct order types, each with its own
     * {@code serviceDetails} schema, this is intentionally left as a free-form
     * {@code Map<String, Object>} rather than modelled as typed classes; a map is an acceptable
     * escape hatch here.
     *
     * <p>The required keys for each type are defined by the corresponding
     * {@code *Request.serviceDetails} schema in the smarthands v1 spec. The order type maps to a
     * spec schema (and to the {@code createXxx} client method / typed POST path) as follows:</p>
     *
     * <ul>
     *     <li>{@code equipmentInstall} — {@code equipmentInstallRequest.serviceDetails} (required:
     *     {@code deviceLocation}, {@code elevationDrawingAttached}, {@code installationPoint},
     *     {@code installedEquipmentPhotoRequired}, {@code mountHardwareIncluded},
     *     {@code patchDevices}, {@code powerItOn}, {@code scopeOfWork})</li>
     *     <li>{@code cageCleanup} — {@code cageCleanupRequest.serviceDetails}</li>
     *     <li>{@code cageEscort} — {@code cageEscortRequest.serviceDetails}</li>
     *     <li>{@code shipmentUnpack} — {@code shipmentUnpackRequest.serviceDetails}</li>
     *     <li>{@code cableRequest} — {@code cableRequestRequest.serviceDetails}</li>
     *     <li>{@code locatePackage} — {@code locatePackageRequest.serviceDetails}</li>
     *     <li>{@code moveJumperCable} — {@code moveJumperCableRequest.serviceDetails}</li>
     *     <li>{@code runJumperCable} — {@code runJumperCableRequest.serviceDetails}</li>
     *     <li>{@code patchCableInstall} — {@code patchCableInstallRequest.serviceDetails}</li>
     *     <li>{@code patchCableRemoval} — {@code patchCableRemovalRequest.serviceDetails}</li>
     *     <li>{@code picturesDocument} — {@code picturesDocumentRequest.serviceDetails}</li>
     *     <li>{@code other} — {@code otherRequest.serviceDetails}</li>
     * </ul>
     *
     * <p>Consult the smarthands v1 spec for the full set of properties and which are required for
     * the specific type you are ordering.</p>
     */
    @JsonProperty("serviceDetails")
    private final Map<String, Object> serviceDetails;

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
     * Returns a new builder for a smart hands request body.
     *
     * @param ibxLocation    the IBX/cage location (required)
     * @param contacts       the ordering, technical and notification contacts (required)
     * @param schedule       the scheduling details (required)
     * @param serviceDetails the per-type service details (required)
     * @return a new builder
     */
    public static Builder builder(IbxLocation ibxLocation, List<ContactInfo> contacts, ScheduleInfo schedule,
                                  Map<String, Object> serviceDetails) {
        return new Builder(ibxLocation, contacts, schedule, serviceDetails);
    }

    public static class Builder {
        private final IbxLocation ibxLocation;
        private final List<ContactInfo> contacts;
        private final ScheduleInfo schedule;
        private final Map<String, Object> serviceDetails;
        private String customerReferenceNumber;
        private PurchaseOrderInfo purchaseOrder;
        private List<SmartHandsAttachment> attachments;

        private Builder(IbxLocation ibxLocation, List<ContactInfo> contacts, ScheduleInfo schedule,
                        Map<String, Object> serviceDetails) {
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
