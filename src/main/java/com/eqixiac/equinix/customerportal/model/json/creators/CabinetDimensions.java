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

package com.eqixiac.equinix.customerportal.model.json.creators;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * Cabinet dimensions ({@code width}, {@code depth}, {@code height}) on a secure cabinet order
 * ({@code Dimensions} schema). All three are required.
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CabinetDimensions {

    @JsonProperty("width")
    private final CabinetDimension width;

    @JsonProperty("depth")
    private final CabinetDimension depth;

    @JsonProperty("height")
    private final CabinetDimension height;

    /**
     * Creates a set of cabinet dimensions.
     *
     * @param width  the cabinet width (required)
     * @param depth  the cabinet depth (required)
     * @param height the cabinet height (required)
     */
    public CabinetDimensions(CabinetDimension width, CabinetDimension depth, CabinetDimension height) {
        this.width = width;
        this.depth = depth;
        this.height = height;
    }
}
