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
import lombok.Getter;

import java.util.List;

/**
 * Body of {@code PUT /fabric/v4/streams/{streamId}/devices/{deviceId}} (spec
 * {@code StreamDevicePutRequest}): the complete desired set of device interfaces to stream. Interfaces
 * not listed are detached; an empty list detaches all of them.
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StreamDevicePutRequest {

    @JsonProperty("metricsEnabled")
    private final Boolean metricsEnabled;

    @JsonProperty("interfaces")
    private final List<InterfaceRef> interfaces;

    /**
     * Creates the request.
     *
     * @param metricsEnabled whether metrics are streamed; the spec requires {@code true} when interfaces are given
     * @param interfaceIds the device interface ids to attach
     */
    public StreamDevicePutRequest(Boolean metricsEnabled, List<Integer> interfaceIds) {
        if (interfaceIds != null && !interfaceIds.isEmpty() && !Boolean.TRUE.equals(metricsEnabled)) {
            throw new IllegalArgumentException("metricsEnabled must be true when interfaces are provided (spec StreamDevicePutRequest)");
        }
        this.metricsEnabled = metricsEnabled;
        this.interfaces = interfaceIds == null ? null : interfaceIds.stream().map(InterfaceRef::new).toList();
    }

    /** Interface reference: {@code {"id": 3}}. */
    @Getter
    public static final class InterfaceRef {
        @JsonProperty("id")
        private final Integer id;

        InterfaceRef(Integer id) {
            this.id = id;
        }
    }
}
