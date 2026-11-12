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

package com.eqixiac.equinix.networkedge.model.json.creators;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * Options for {@code DELETE /ne/v1/devices/{uuid}}: the {@code deleteRedundantDevice} query
 * parameter and the optional {@code deletionInfo} body ({@code VirtualDeviceDeleteRequest}).
 *
 * <p>Per the spec, a redundant pair must be deleted with {@code deleteRedundantDevice=true} so
 * both devices go together, and a Palo Alto BYOL device needs its deactivation key (for the
 * secondary too) or the licence must be released through the vendor.</p>
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DeviceDeleteRequest {

    /** Sent as the {@code deleteRedundantDevice} query parameter, not in the body. */
    @JsonIgnore
    private Boolean deleteRedundantDevice;

    @JsonProperty("deactivationKey")
    private String deactivationKey;

    @JsonProperty("secondary")
    private Secondary secondary;

    /**
     * Sets {@code deleteRedundantDevice}: delete the primary and secondary of a pair together.
     *
     * @param deleteRedundantDevice {@code true} for a redundant pair
     * @return this request
     */
    public DeviceDeleteRequest deleteRedundantDevice(boolean deleteRedundantDevice) {
        this.deleteRedundantDevice = deleteRedundantDevice;
        return this;
    }

    /**
     * Sets the (primary) device's licence deactivation key.
     *
     * @param deactivationKey the deactivation key
     * @return this request
     */
    public DeviceDeleteRequest deactivationKey(String deactivationKey) {
        this.deactivationKey = deactivationKey;
        return this;
    }

    /**
     * Sets the secondary device's licence deactivation key ({@code secondary.deactivationKey}).
     *
     * @param deactivationKey the secondary device's deactivation key
     * @return this request
     */
    public DeviceDeleteRequest secondaryDeactivationKey(String deactivationKey) {
        this.secondary = new Secondary(deactivationKey);
        return this;
    }

    /**
     * Whether a body needs to be sent.
     *
     * @return {@code true} when a deactivation key is set
     */
    @JsonIgnore
    public boolean hasBody() {
        return deactivationKey != null || secondary != null;
    }

    /** {@code SecondaryDeviceDeleteRequest}. */
    @Getter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Secondary {

        @JsonProperty("deactivationKey")
        private final String deactivationKey;

        Secondary(String deactivationKey) {
            this.deactivationKey = deactivationKey;
        }
    }
}
