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

package com.eqixiac.equinix.core.http;

import com.eqixiac.equinix.core.exception.EquinixAuthenticationException;
import com.eqixiac.equinix.core.exception.EquinixAuthorizationException;
import com.eqixiac.equinix.core.exception.EquinixConflictException;
import com.eqixiac.equinix.core.exception.EquinixNotFoundException;
import com.eqixiac.equinix.core.exception.EquinixRateLimitException;
import com.eqixiac.equinix.core.exception.EquinixServerException;
import com.eqixiac.equinix.core.exception.EquinixServiceException;
import com.eqixiac.equinix.core.exception.ExceptionDetail;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

/**
 * Maps an Equinix API error response (status code + headers + body) into the appropriate typed
 * {@link EquinixServiceException}. Centralizes the status-to-subclass selection and the
 * response-body parsing ladder that was previously inlined in {@link EquinixHttpClient}, producing
 * a fully-constructed, immutable exception.
 *
 * <p>The body is parsed by {@link ErrorBodyParser}, which recognises the error format of every
 * Equinix service (Fabric / Network Edge / EIA / Resource Manager arrays, IAM {@code {"error":{...}}},
 * STS {@code {"error","error_description"}}, operations and Smart Hands {@code {"errors":[...]}},
 * IBX SmartView v1 and v2, the API gateway object and Apigee faults) and never throws. The first
 * parsed detail is appended to the summary message, so {@link Throwable#getMessage()} leads with the
 * service's code, message and field. A body that yields no details (an HTML gateway page, for
 * example) is kept as a truncated snippet on the exception.</p>
 *
 * @author ianjones
 */
@Slf4j
public final class ResponseErrorMapper {

    private ResponseErrorMapper() {
    }

    /**
     * Builds the typed exception for an error response.
     *
     * @param statusCode    the HTTP status code.
     * @param path          the request URI that produced the error.
     * @param httpHeaders   relevant response headers (e.g. {@code Retry-After}); may be {@code null}.
     * @param errorBody     the raw response body, or {@code null}/blank when none was returned.
     * @param correlationId the SDK-generated {@code X-Correlation-Id} sent with the failed request; may be {@code null}.
     * @return the typed {@link EquinixServiceException} for the status code.
     */
    public static EquinixServiceException toException(int statusCode, String path,
                                                      Map<String, String> httpHeaders, String errorBody,
                                                      String correlationId) {
        List<ExceptionDetail> details = ErrorBodyParser.parse(errorBody);
        String snippet = null;
        String message = summaryMessage(statusCode);
        if (details.isEmpty()) {
            snippet = ErrorBodyParser.snippet(errorBody);
            if (snippet != null) {
                log.debug("Error response body has no recognised error format: {}", snippet);
            }
        } else {
            String first = details.get(0).toSummaryLine();
            if (!first.isEmpty()) {
                message = message + " " + first;
            }
        }

        switch (statusCode) {
            case 401:
                return new EquinixAuthenticationException(message, statusCode, path, httpHeaders, details, correlationId, snippet);
            case 403:
                return new EquinixAuthorizationException(message, statusCode, path, httpHeaders, details, correlationId, snippet);
            case 404:
                return new EquinixNotFoundException(message, statusCode, path, httpHeaders, details, correlationId, snippet);
            case 409:
                return new EquinixConflictException(message, statusCode, path, httpHeaders, details, correlationId, snippet);
            case 429:
                return new EquinixRateLimitException(message, statusCode, path, httpHeaders, details, correlationId, snippet);
            default:
                if (statusCode >= 500) {
                    return new EquinixServerException(message, statusCode, path, httpHeaders, details, correlationId, snippet);
                }
                return new EquinixServiceException(message, statusCode, path, httpHeaders, details, correlationId, snippet);
        }
    }

    private static String summaryMessage(int statusCode) {
        switch (statusCode) {
            case 401:
                return "Authentication failed (HTTP 401).";
            case 403:
                return "Authorization denied (HTTP 403).";
            case 404:
                return "Resource not found (HTTP 404).";
            case 409:
                return "Resource conflict (HTTP 409).";
            case 429:
                return "Rate limit exceeded (HTTP 429).";
            default:
                return (statusCode >= 500)
                        ? "Server error (HTTP " + statusCode + ")."
                        : "Error returned by Equinix API (HTTP " + statusCode + ").";
        }
    }
}
