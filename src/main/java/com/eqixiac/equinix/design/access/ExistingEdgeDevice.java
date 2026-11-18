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
package com.eqixiac.equinix.design.access;

/**
 * A Network Edge device that exists in the account.
 *
 * @param uuid          the device uuid
 * @param name          the device name
 * @param metro         the metro code
 * @param typeCode      the device type code, or {@code null}
 * @param status        the device status as the API names it
 * @param redundancy    {@code PRIMARY}, {@code SECONDARY} or {@code CLUSTER}, or {@code null}
 * @param redundantUuid the uuid of the device it is paired with, or {@code null}
 */
public record ExistingEdgeDevice(String uuid, String name, String metro, String typeCode, String status,
                                 String redundancy, String redundantUuid) {

    /** @return {@code true} when the device is provisioned */
    public boolean usable() {
        return "PROVISIONED".equals(status);
    }
}
