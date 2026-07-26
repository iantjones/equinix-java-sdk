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

package com.eqixiac.equinix.fabric.model.implementation;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

/**
 * Request payload for a Fabric Cloud Router diagnostic command (ping / traceroute). Carries the
 * destination, source connection and the command-specific tuning parameters.
 */
@Getter
@Setter(AccessLevel.PRIVATE)
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CloudRouterCommandRequest {

    @JsonProperty("destination")
    private String destination;

    @JsonProperty("sourceConnection")
    private Map<String, String> sourceConnection;

    @JsonProperty("timeout")
    private Integer timeout;

    @JsonProperty("dataBytes")
    private Integer dataBytes;

    @JsonProperty("interval")
    private Integer interval;

    @JsonProperty("count")
    private Integer count;

    @JsonProperty("probes")
    private Integer probes;

    @JsonProperty("hopsMax")
    private Integer hopsMax;

    private CloudRouterCommandRequest(Builder builder) {
        this.destination = builder.destination;
        this.sourceConnection = builder.sourceConnectionUuid != null ? Map.of("uuid", builder.sourceConnectionUuid) : null;
        this.timeout = builder.timeout;
        this.dataBytes = builder.dataBytes;
        this.interval = builder.interval;
        this.count = builder.count;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String destination;
        private String sourceConnectionUuid;
        private Integer timeout;
        private Integer dataBytes;
        private Integer interval;
        private Integer count;

        public Builder destination(String destination) {
            this.destination = destination;
            return this;
        }

        public Builder sourceConnection(String connectionUuid) {
            this.sourceConnectionUuid = connectionUuid;
            return this;
        }

        public Builder timeout(Integer timeout) {
            this.timeout = timeout;
            return this;
        }

        public Builder dataBytes(Integer dataBytes) {
            this.dataBytes = dataBytes;
            return this;
        }

        public Builder interval(Integer interval) {
            this.interval = interval;
            return this;
        }

        public Builder count(Integer count) {
            this.count = count;
            return this;
        }

        public CloudRouterCommandRequest build() {
            return new CloudRouterCommandRequest(this);
        }
    }
}
