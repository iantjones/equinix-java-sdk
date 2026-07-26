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

import com.eqixiac.equinix.customerportal.enums.ConnectorType;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * Diversified / redundant cross-connect configuration ({@code diverseConnections} in the
 * cross-connects v2 spec). For a new redundant connection ({@code type=NEW}), {@code aSide} and
 * {@code zSide} are mandatory; for an existing connection ({@code type=EXISTING}, the default),
 * the {@code serialNumber} and type are mandatory.
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Layer1DiverseConnections {

    /**
     * Redundant connection with existing connections or new connections.
     */
    public enum Type {
        NEW,
        EXISTING
    }

    @JsonProperty("type")
    private Type type;

    @JsonProperty("serialNumber")
    private String serialNumber;

    @JsonProperty("aSide")
    private DiverseASide aSide;

    @JsonProperty("zSide")
    private DiverseZSide zSide;

    private Layer1DiverseConnections() {
    }

    /**
     * Builds a diversified connection from an existing, previously provisioned connection.
     *
     * @param serialNumber the serial number of the existing cross connect
     * @return the diverse-connections configuration
     */
    public static Layer1DiverseConnections existing(String serialNumber) {
        Layer1DiverseConnections diverse = new Layer1DiverseConnections();
        diverse.type = Type.EXISTING;
        diverse.serialNumber = serialNumber;
        return diverse;
    }

    /**
     * Builds a diversified connection as a new redundant connection.
     *
     * @param aSide the A-Side of the redundant connection
     * @param zSide the Z-Side of the redundant connection
     * @return the diverse-connections configuration
     */
    public static Layer1DiverseConnections newConnection(DiverseASide aSide, DiverseZSide zSide) {
        Layer1DiverseConnections diverse = new Layer1DiverseConnections();
        diverse.type = Type.NEW;
        diverse.aSide = aSide;
        diverse.zSide = zSide;
        return diverse;
    }

    /**
     * A-Side of a diversified connection.
     */
    @Getter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class DiverseASide {

        @JsonProperty("patchPanel")
        private final Layer1PatchPanel patchPanel;

        @JsonProperty("connectorType")
        private final ConnectorType connectorType;

        @JsonProperty("mediaConverterRequired")
        private Boolean mediaConverterRequired;

        @JsonProperty("ifcCircuitCount")
        private Integer ifcCircuitCount;

        @JsonProperty("patchEquipment")
        private Layer1PatchEquipment patchEquipment;

        public DiverseASide(Layer1PatchPanel patchPanel, ConnectorType connectorType) {
            this.patchPanel = patchPanel;
            this.connectorType = connectorType;
        }

        public DiverseASide mediaConverterRequired(Boolean mediaConverterRequired) {
            this.mediaConverterRequired = mediaConverterRequired;
            return this;
        }

        public DiverseASide ifcCircuitCount(Integer ifcCircuitCount) {
            this.ifcCircuitCount = ifcCircuitCount;
            return this;
        }

        public DiverseASide patchEquipment(Layer1PatchEquipment patchEquipment) {
            this.patchEquipment = patchEquipment;
            return this;
        }
    }

    /**
     * Z-Side of a diversified connection.
     */
    @Getter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class DiverseZSide {

        @JsonProperty("patchPanel")
        private final Layer1PatchPanel patchPanel;

        @JsonProperty("connectorType")
        private final ConnectorType connectorType;

        @JsonProperty("circuitId")
        private String circuitId;

        public DiverseZSide(Layer1PatchPanel patchPanel, ConnectorType connectorType) {
            this.patchPanel = patchPanel;
            this.connectorType = connectorType;
        }

        public DiverseZSide circuitId(String circuitId) {
            this.circuitId = circuitId;
            return this;
        }
    }
}
