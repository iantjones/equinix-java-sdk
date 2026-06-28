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

/**
 * A single cross-connect entry ({@code Layer1_Details} in the cross-connects v2 spec). {@code aSide}
 * and {@code zSide} are required; {@code diverseConnections}, {@code verifyLink},
 * {@code circuitDeliveryDate} and {@code submarineEngineerRequired} are optional.
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Layer1Detail {

    @JsonProperty("aSide")
    private final Layer1ASide aSide;

    @JsonProperty("zSide")
    private final Layer1ZSide zSide;

    @JsonProperty("diverseConnections")
    private Layer1DiverseConnections diverseConnections;

    @JsonProperty("verifyLink")
    private Boolean verifyLink;

    @JsonProperty("circuitDeliveryDate")
    private String circuitDeliveryDate;

    @JsonProperty("submarineEngineerRequired")
    private Boolean submarineEngineerRequired;

    private Layer1Detail(Builder builder) {
        this.aSide = builder.aSide;
        this.zSide = builder.zSide;
        this.diverseConnections = builder.diverseConnections;
        this.verifyLink = builder.verifyLink;
        this.circuitDeliveryDate = builder.circuitDeliveryDate;
        this.submarineEngineerRequired = builder.submarineEngineerRequired;
    }

    /**
     * Returns a new builder for a cross-connect detail.
     *
     * @param aSide the A-Side (required)
     * @param zSide the Z-Side (required)
     * @return a new builder
     */
    public static Builder builder(Layer1ASide aSide, Layer1ZSide zSide) {
        return new Builder(aSide, zSide);
    }

    public static class Builder {
        private final Layer1ASide aSide;
        private final Layer1ZSide zSide;
        private Layer1DiverseConnections diverseConnections;
        private Boolean verifyLink;
        private String circuitDeliveryDate;
        private Boolean submarineEngineerRequired;

        private Builder(Layer1ASide aSide, Layer1ZSide zSide) {
            this.aSide = aSide;
            this.zSide = zSide;
        }

        public Builder diverseConnections(Layer1DiverseConnections diverseConnections) {
            this.diverseConnections = diverseConnections;
            return this;
        }

        public Builder verifyLink(Boolean verifyLink) {
            this.verifyLink = verifyLink;
            return this;
        }

        public Builder circuitDeliveryDate(String circuitDeliveryDate) {
            this.circuitDeliveryDate = circuitDeliveryDate;
            return this;
        }

        public Builder submarineEngineerRequired(Boolean submarineEngineerRequired) {
            this.submarineEngineerRequired = submarineEngineerRequired;
            return this;
        }

        public Layer1Detail build() {
            return new Layer1Detail(this);
        }
    }
}
