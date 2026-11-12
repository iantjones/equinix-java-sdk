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

import lombok.Getter;

import java.io.Serializable;

/**
 * A property-level error annotation nested inside an {@link ExceptionDetail} (the Fabric / Network
 * Edge {@code additionalInfo[{property, reason}]} entries). Immutable and {@link Serializable}
 * because it is carried by the (serializable) {@link EquinixServiceException} family.
 *
 * @author ianjones
 */
@Getter
public final class ExceptionAdditionalInfo implements Serializable {
    private static final long serialVersionUID = 2L;

    /** The request property the annotation refers to, for example {@code $.notifications}. */
    private final String property;

    /** Why the property was rejected. */
    private final String reason;

    /** The rejected value, when the service echoes it. */
    private final String value;

    /**
     * Creates an annotation.
     *
     * @param property the request property; may be {@code null}
     * @param reason   why it was rejected; may be {@code null}
     * @param value    the rejected value; may be {@code null}
     */
    public ExceptionAdditionalInfo(String property, String reason, String value) {
        this.property = property;
        this.reason = reason;
        this.value = value;
    }
}
