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

package api.equinix.javasdk.networkedge.model.implementation;

import api.equinix.javasdk.networkedge.enums.DeviceManagementType;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * <p>DeviceManagementTypes class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
@Getter
public class DeviceManagementTypes {

    @JsonProperty("EQUINIX-CONFIGURED")
    DeviceManagement equinixConfigured;

    @JsonProperty("SELF-CONFIGURED")
    DeviceManagement selfConfigured;

    /**
     * <p>byValue.</p>
     *
     * @param deviceManagementType a {@link api.equinix.javasdk.networkedge.enums.DeviceManagementType} object.
     * @return a {@link api.equinix.javasdk.networkedge.model.implementation.DeviceManagement} object.
     */
    public DeviceManagement byValue(DeviceManagementType deviceManagementType) {
        DeviceManagement deviceManagement = null;

        switch (deviceManagementType) {
            case EQUINIX_CONFIGURED: deviceManagement = equinixConfigured;
            case SELF_CONFIGURED: deviceManagement = selfConfigured;
        }

        return deviceManagement;
    }
}
