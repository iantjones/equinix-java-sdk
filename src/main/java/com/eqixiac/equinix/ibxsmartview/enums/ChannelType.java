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

package com.eqixiac.equinix.ibxsmartview.enums;

import com.eqixiac.equinix.core.model.APIParam;
import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * The delivery channel type for a streaming subscription. Per the smartviewv2 spec the
 * supported values are {@code AWS_IOT_CORE}, {@code WEBHOOK} and {@code AZURE}.
 */
public enum ChannelType implements APIParam {
    AWS_IOT_CORE,
    WEBHOOK,
    AZURE,
    UNKNOWN;

    @JsonCreator
    public static ChannelType fromString(String value) {
        try {
            return ChannelType.valueOf(value.toUpperCase());
        } catch (Exception e) {
            return UNKNOWN;
        }
    }
}
