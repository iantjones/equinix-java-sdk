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

import api.equinix.javasdk.customerportal.enums.ConnectorType;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * Z-Side for a cross-connect ({@code zSide} in the cross-connects v2 spec). The spec models the
 * Z-Side as a {@code oneOf} of {@code zSideWithLOA} and {@code zSideWithPatchPanel}; this class
 * carries the union of both variants' fields (each serialised only when set), with static
 * factories for each shape:
 *
 * <ul>
 *     <li>{@link #withLOA(String, String, String)} — a new customer/patch panel: {@code loaAttachmentId},
 *     {@code ibx} and {@code providerName} are required.</li>
 *     <li>{@link #withPatchPanel(Layer1PatchPanel, ConnectorType)} — an existing customer: only
 *     {@code patchPanel} and {@code connectorType} are required.</li>
 * </ul>
 *
 * <p>{@code notificationEmail} is common to both variants and is optional.</p>
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Layer1ZSide {

    @JsonProperty("notificationEmail")
    private String notificationEmail;

    // zSideWithLOA variant
    @JsonProperty("loaAttachmentId")
    private String loaAttachmentId;

    @JsonProperty("ibx")
    private String ibx;

    @JsonProperty("providerName")
    private String providerName;

    // zSideWithPatchPanel variant
    @JsonProperty("patchPanel")
    private Layer1PatchPanel patchPanel;

    @JsonProperty("connectorType")
    private ConnectorType connectorType;

    @JsonProperty("circuitId")
    private String circuitId;

    private Layer1ZSide() {
    }

    /**
     * Z-Side for a new customer or patch panel, identified by a Letter of Authorization (LOA).
     *
     * @param loaAttachmentId attachment id of the uploaded LOA (required)
     * @param ibx             IBX location code (required)
     * @param providerName    account name of the service provider (required)
     * @return the Z-Side
     */
    public static Layer1ZSide withLOA(String loaAttachmentId, String ibx, String providerName) {
        Layer1ZSide zSide = new Layer1ZSide();
        zSide.loaAttachmentId = loaAttachmentId;
        zSide.ibx = ibx;
        zSide.providerName = providerName;
        return zSide;
    }

    /**
     * Z-Side for an existing customer, identified by patch panel and connector type.
     *
     * @param patchPanel    the destination patch panel (required)
     * @param connectorType the connector type (required)
     * @return the Z-Side
     */
    public static Layer1ZSide withPatchPanel(Layer1PatchPanel patchPanel, ConnectorType connectorType) {
        Layer1ZSide zSide = new Layer1ZSide();
        zSide.patchPanel = patchPanel;
        zSide.connectorType = connectorType;
        return zSide;
    }

    /**
     * Sets the customer/carrier circuit id (applies to the patch-panel variant).
     *
     * @param circuitId the circuit id cable reference number for verification
     * @return this Z-Side
     */
    public Layer1ZSide circuitId(String circuitId) {
        this.circuitId = circuitId;
        return this;
    }

    /**
     * Sets the notification email; the Z-Side customer is notified when the cross connect completes.
     *
     * @param notificationEmail the notification email
     * @return this Z-Side
     */
    public Layer1ZSide notificationEmail(String notificationEmail) {
        this.notificationEmail = notificationEmail;
        return this;
    }
}
