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

package com.eqixiac.equinix.fabric.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Notification preference type of a port notification contact (the Fabric v4
 * {@code PortNotification.type} attribute: {@code NOTIFICATION}, {@code TECHNICAL},
 * {@code PEERING}, {@code ESCALATION}). Unrecognized values deserialize to
 * {@link #UNKNOWN} rather than failing the whole response.
 *
 * @author ianjones
 */
public enum PortNotificationType {
    NOTIFICATION,
    TECHNICAL,
    PEERING,
    ESCALATION,
    UNKNOWN;

    /**
     * Deserializes a port notification type leniently: an unrecognized value maps to
     * {@link #UNKNOWN} instead of failing the enclosing response.
     *
     * @param value the raw API value
     * @return the matching constant, or {@link #UNKNOWN}
     */
    @JsonCreator
    public static PortNotificationType fromString(String value) {
        try { return PortNotificationType.valueOf(value); }
        catch (Exception e) { return UNKNOWN; }
    }
}
