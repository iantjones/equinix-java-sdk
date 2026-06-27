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

package api.equinix.javasdk.core.http;

import api.equinix.javasdk.core.exception.EquinixAuthenticationException;
import api.equinix.javasdk.core.exception.EquinixAuthorizationException;
import api.equinix.javasdk.core.exception.EquinixConflictException;
import api.equinix.javasdk.core.exception.EquinixNotFoundException;
import api.equinix.javasdk.core.exception.EquinixRateLimitException;
import api.equinix.javasdk.core.exception.EquinixServerException;
import api.equinix.javasdk.core.exception.EquinixServiceException;
import api.equinix.javasdk.core.exception.ExceptionDetail;
import api.equinix.javasdk.core.internal.Constants;
import com.fasterxml.jackson.core.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Maps an Equinix API error response (status code + headers + body) into the appropriate typed
 * {@link EquinixServiceException}. Centralizes the status-to-subclass selection and the
 * response-body parsing ladder that was previously inlined in {@link EquinixHttpClient}, producing
 * a fully-constructed, immutable exception.
 *
 * <p>The body parser tolerates the three shapes the API uses for errors — a JSON array of
 * {@link ExceptionDetail}, a single {@link ExceptionDetail} object, or an unparseable/empty body —
 * never throwing while interpreting the body.</p>
 *
 * @author ianjones
 */
public final class ResponseErrorMapper {

    private static final Logger logger = LoggerFactory.getLogger(ResponseErrorMapper.class);

    private ResponseErrorMapper() {
    }

    /**
     * Builds the typed exception for an error response.
     *
     * @param statusCode  the HTTP status code.
     * @param path        the request URI that produced the error.
     * @param httpHeaders relevant response headers (e.g. {@code Retry-After}); may be {@code null}.
     * @param errorBody   the raw response body, or {@code null}/blank when none was returned.
     * @return the typed {@link EquinixServiceException} for the status code.
     */
    public static EquinixServiceException toException(int statusCode, String path,
                                                      Map<String, String> httpHeaders, String errorBody) {
        List<ExceptionDetail> details = parseDetails(errorBody);
        String message = summaryMessage(statusCode);

        switch (statusCode) {
            case 401:
                return new EquinixAuthenticationException(message, statusCode, path, httpHeaders, details);
            case 403:
                return new EquinixAuthorizationException(message, statusCode, path, httpHeaders, details);
            case 404:
                return new EquinixNotFoundException(message, statusCode, path, httpHeaders, details);
            case 409:
                return new EquinixConflictException(message, statusCode, path, httpHeaders, details);
            case 429:
                return new EquinixRateLimitException(message, statusCode, path, httpHeaders, details);
            default:
                if (statusCode >= 500) {
                    return new EquinixServerException(message, statusCode, path, httpHeaders, details);
                }
                return new EquinixServiceException(message, statusCode, path, httpHeaders, details);
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

    private static List<ExceptionDetail> parseDetails(String errorBody) {
        if (errorBody == null || errorBody.isBlank()) {
            return new ArrayList<>();
        }

        try {
            return Constants.objectMapper.readValue(errorBody, new TypeReference<ArrayList<ExceptionDetail>>() {});
        }
        catch (Exception arrayEx) {
            try {
                ExceptionDetail singleDetail = Constants.objectMapper.readValue(
                        errorBody, new TypeReference<ExceptionDetail>() {});
                List<ExceptionDetail> details = new ArrayList<>();
                details.add(singleDetail);
                return details;
            }
            catch (Exception singleEx) {
                logger.warn("Could not parse error response body: {}", errorBody);
                List<ExceptionDetail> details = new ArrayList<>();
                details.add(new ExceptionDetail());
                return details;
            }
        }
    }
}
