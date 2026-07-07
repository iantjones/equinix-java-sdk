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

package api.equinix.javasdk.core.exception;

/**
 * Thrown when the Equinix API returns HTTP 409 Conflict.
 * This indicates the request conflicts with the current state of the resource,
 * such as attempting to create a resource that already exists or modifying a
 * resource that has been concurrently updated.
 *
 * @author ianjones
 */
public class EquinixConflictException extends EquinixServiceException {
    private static final long serialVersionUID = 1L;

    public EquinixConflictException(String errorMessage) {
        super(errorMessage);
    }

    public EquinixConflictException(String errorMessage, Exception cause) {
        super(errorMessage, cause);
    }

    public EquinixConflictException(String errorMessage, Integer statusCode, String path,
                                    java.util.Map<String, String> httpHeaders,
                                    java.util.List<ExceptionDetail> exceptionDetails) {
        super(errorMessage, statusCode, path, httpHeaders, exceptionDetails);
    }

    /**
     * Full constructor carrying the SDK-generated request correlation id; used by
     * {@link api.equinix.javasdk.core.http.ResponseErrorMapper ResponseErrorMapper}.
     *
     * @param errorMessage     a human-readable summary message.
     * @param statusCode       the HTTP status code returned by the API.
     * @param path             the request URI that produced the error.
     * @param httpHeaders      relevant response headers; may be {@code null}.
     * @param exceptionDetails structured error details from the response body; may be {@code null}.
     * @param correlationId    the SDK-generated {@code X-Correlation-Id} of the failed request; may be {@code null}.
     */
    public EquinixConflictException(String errorMessage, Integer statusCode, String path,
            java.util.Map<String, String> httpHeaders,
            java.util.List<ExceptionDetail> exceptionDetails,
            String correlationId) {
        super(errorMessage, statusCode, path, httpHeaders, exceptionDetails, correlationId);
    }
}
