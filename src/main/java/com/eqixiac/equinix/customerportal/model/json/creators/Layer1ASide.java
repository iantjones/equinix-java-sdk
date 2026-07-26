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

import com.eqixiac.equinix.customerportal.enums.ConnectionService;
import com.eqixiac.equinix.customerportal.enums.ConnectorType;
import com.eqixiac.equinix.customerportal.enums.CrossConnectMediaType;
import com.eqixiac.equinix.customerportal.enums.ProtocolType;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * A-Side source patch panel for a cross-connect ({@code aSide} in the cross-connects v2 spec).
 * {@code patchPanel}, {@code connectionService}, {@code mediaType}, {@code protocolType} and
 * {@code connectorType} are required to establish a new connection. {@code mediaConverterRequired},
 * {@code ifcCircuitCount} and {@code patchEquipment} are optional.
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Layer1ASide {

    @JsonProperty("patchPanel")
    private final Layer1PatchPanel patchPanel;

    @JsonProperty("connectionService")
    private final ConnectionService connectionService;

    @JsonProperty("mediaType")
    private final CrossConnectMediaType mediaType;

    @JsonProperty("protocolType")
    private final ProtocolType protocolType;

    @JsonProperty("connectorType")
    private final ConnectorType connectorType;

    @JsonProperty("mediaConverterRequired")
    private Boolean mediaConverterRequired;

    @JsonProperty("ifcCircuitCount")
    private Integer ifcCircuitCount;

    @JsonProperty("patchEquipment")
    private Layer1PatchEquipment patchEquipment;

    private Layer1ASide(Builder builder) {
        this.patchPanel = builder.patchPanel;
        this.connectionService = builder.connectionService;
        this.mediaType = builder.mediaType;
        this.protocolType = builder.protocolType;
        this.connectorType = builder.connectorType;
        this.mediaConverterRequired = builder.mediaConverterRequired;
        this.ifcCircuitCount = builder.ifcCircuitCount;
        this.patchEquipment = builder.patchEquipment;
    }

    /**
     * Returns a new builder for an A-Side, requiring the five mandatory fields.
     *
     * @param patchPanel        the source patch panel (required)
     * @param connectionService the connection service (required)
     * @param mediaType         the media type (required)
     * @param protocolType      the protocol type (required)
     * @param connectorType     the connector type (required)
     * @return a new builder
     */
    public static Builder builder(Layer1PatchPanel patchPanel, ConnectionService connectionService,
                                  CrossConnectMediaType mediaType, ProtocolType protocolType,
                                  ConnectorType connectorType) {
        return new Builder(patchPanel, connectionService, mediaType, protocolType, connectorType);
    }

    public static class Builder {
        private final Layer1PatchPanel patchPanel;
        private final ConnectionService connectionService;
        private final CrossConnectMediaType mediaType;
        private final ProtocolType protocolType;
        private final ConnectorType connectorType;
        private Boolean mediaConverterRequired;
        private Integer ifcCircuitCount;
        private Layer1PatchEquipment patchEquipment;

        private Builder(Layer1PatchPanel patchPanel, ConnectionService connectionService,
                        CrossConnectMediaType mediaType, ProtocolType protocolType, ConnectorType connectorType) {
            this.patchPanel = patchPanel;
            this.connectionService = connectionService;
            this.mediaType = mediaType;
            this.protocolType = protocolType;
            this.connectorType = connectorType;
        }

        public Builder mediaConverterRequired(Boolean mediaConverterRequired) {
            this.mediaConverterRequired = mediaConverterRequired;
            return this;
        }

        public Builder ifcCircuitCount(Integer ifcCircuitCount) {
            this.ifcCircuitCount = ifcCircuitCount;
            return this;
        }

        public Builder patchEquipment(Layer1PatchEquipment patchEquipment) {
            this.patchEquipment = patchEquipment;
            return this;
        }

        public Layer1ASide build() {
            return new Layer1ASide(this);
        }
    }
}
