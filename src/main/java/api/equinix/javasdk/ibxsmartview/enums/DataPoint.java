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

package api.equinix.javasdk.ibxsmartview.enums;

import api.equinix.javasdk.core.model.APIParam;
import com.fasterxml.jackson.annotation.JsonCreator;

public enum DataPoint implements APIParam {
    TEMPERATURE,
    HUMIDITY,
    UNKNOWN;

    @JsonCreator
    public static DataPoint fromString(String value) {
        try {
            return DataPoint.valueOf(value.toUpperCase());
        } catch (Exception e) {
            return UNKNOWN;
        }
    }

    @Override
    public String toString() {
        return name().toLowerCase();
    }
}
