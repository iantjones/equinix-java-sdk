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

package com.eqixiac.equinix.core.exception;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.io.Serializable;

/**
 * A property-level error annotation nested inside an {@link ExceptionDetail}. {@link Serializable}
 * because it is carried by the (serializable) {@link EquinixServiceException} family.
 *
 * @author ianjones
 */
@Getter
public class ExceptionAdditionalInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    @JsonProperty("property")
    private String property;

    @JsonProperty("reason")
    private String reason;

    @JsonProperty("value")
    private String value;
}
