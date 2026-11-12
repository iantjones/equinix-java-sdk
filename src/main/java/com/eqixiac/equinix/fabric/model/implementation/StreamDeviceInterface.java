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

import com.eqixiac.equinix.fabric.enums.StreamAssetAttachmentStatus;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * A Network Edge device interface attached to a stream, as returned by
 * {@code /fabric/v4/streams/{streamId}/devices/{deviceId}} (spec 4.34.0 example
 * {@code StreamDeviceAssetResponse}).
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class StreamDeviceInterface {

    /** The interface URI under {@code /ne/v1/devices/{uuid}/interfaces/{id}}. */
    @JsonProperty("href")
    private String href;

    /** The interface id on the device. */
    @JsonProperty("id")
    private Integer id;

    /** The interface uuid. */
    @JsonProperty("uuid")
    private String uuid;

    /** The interface type, for example {@code DATA}. */
    @JsonProperty("type")
    private String type;

    /** The interface state, for example {@code ASSIGNED}. */
    @JsonProperty("state")
    private String state;

    /** The interface's attachment status on the stream. */
    @JsonProperty("attachmentStatus")
    private StreamAssetAttachmentStatus attachmentStatus;
}
