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

import lombok.Builder;
import lombok.Getter;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

/**
 * One structured error entry parsed from an Equinix API error response body, normalised across the
 * error formats the different Equinix services use (see
 * {@link com.eqixiac.equinix.core.http.ResponseErrorMapper ResponseErrorMapper} for the formats and
 * how each maps onto these fields). Every field is optional: a service fills only what its format
 * carries. Immutable and {@link Serializable}, because it is carried by the (serializable)
 * {@link EquinixServiceException} family.
 *
 * @author ianjones
 */
@Getter
public final class ExceptionDetail implements Serializable {
    private static final long serialVersionUID = 2L;

    /**
     * The service's error code, for example {@code EQ-3142501} (Fabric), {@code EQX-171-1212}
     * (operations), {@code DCIM-ASSET-52724} (IBX SmartView), {@code invalid_scope} (STS) or
     * {@code not-found} (IAM).
     */
    private final String errorCode;

    /** The human-readable error message. */
    private final String errorMessage;

    /**
     * Supplementary detail text. Structured details (for example IAM's
     * {@code {"projectId":["..."]}}) are rendered as {@code key: value} text.
     */
    private final String details;

    /**
     * The request property or field the error refers to (Fabric {@code additionalInfo.property},
     * Operations {@code fields}, IBX SmartView {@code fieldName}, IAM {@code details} key);
     * several fields are joined with {@code ", "}.
     */
    private final String property;

    /** The service-side correlation id carried in the body, when the service returns one. */
    private final String correlationId;

    /** The error domain of the API gateway format ({@code errorDomain}). */
    private final String errorDomain;

    /** A short error title ({@code errorTitle} / {@code title}). */
    private final String errorTitle;

    /** A message addressed to the developer ({@code developerMessage}). */
    private final String developerMessage;

    /** A help text or link ({@code help}). */
    private final String help;

    /** Property-level annotations (Fabric / Network Edge {@code additionalInfo}); never {@code null}. */
    private final List<ExceptionAdditionalInfo> additionalInfo;

    @Builder
    private ExceptionDetail(String errorCode, String errorMessage, String details, String property,
                            String correlationId, String errorDomain, String errorTitle,
                            String developerMessage, String help, List<ExceptionAdditionalInfo> additionalInfo) {
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.details = details;
        this.property = property;
        this.correlationId = correlationId;
        this.errorDomain = errorDomain;
        this.errorTitle = errorTitle;
        this.developerMessage = developerMessage;
        this.help = help;
        this.additionalInfo = additionalInfo == null ? List.of()
                : additionalInfo.stream().filter(Objects::nonNull).toList();
    }

    /**
     * Renders this detail as one line, code first: {@code EQ-3142501: Invalid seller metroCode
     * location (field: /zSide/accessPoint/location/metroCode)}.
     *
     * @return the one-line rendering; empty when the detail carries nothing
     */
    public String toSummaryLine() {
        StringBuilder line = new StringBuilder();
        if (notBlank(errorCode)) {
            line.append(errorCode);
        }
        String text = notBlank(errorMessage) ? errorMessage : errorTitle;
        if (notBlank(text)) {
            if (line.length() > 0) {
                line.append(": ");
            }
            line.append(text);
        }
        if (notBlank(property)) {
            if (line.length() > 0) {
                line.append(' ');
            }
            line.append("(field: ").append(property).append(')');
        }
        return line.toString();
    }

    @Override
    public String toString() {
        return "ExceptionDetail[" + toSummaryLine() + "]";
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}
