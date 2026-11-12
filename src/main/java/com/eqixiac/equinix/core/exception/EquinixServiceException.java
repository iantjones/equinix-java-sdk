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

import com.eqixiac.equinix.core.util.StringUtils;
import lombok.Getter;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Thrown when the Equinix API returns an error HTTP response.
 *
 * <p>This is the base class for all API error exceptions. It carries the HTTP status code,
 * response headers, request path, and structured error details from the API response body.
 * It is a <em>sibling</em> of {@link EquinixClientException} - both extend {@link BaseException} - 
 * so catching {@code EquinixClientException} (client/SDK/network errors) does not also catch
 * API errors, and vice versa. Catch {@link BaseException} to handle either.
 * Specific HTTP status codes are mapped to typed subclasses:</p>
 *
 * <table>
 *   <tr><th>HTTP Status</th><th>Exception Class</th></tr>
 *   <tr><td>401 Unauthorized</td><td>{@link EquinixAuthenticationException}</td></tr>
 *   <tr><td>403 Forbidden</td><td>{@link EquinixAuthorizationException}</td></tr>
 *   <tr><td>404 Not Found</td><td>{@link EquinixNotFoundException}</td></tr>
 *   <tr><td>409 Conflict</td><td>{@link EquinixConflictException}</td></tr>
 *   <tr><td>429 Too Many Requests</td><td>{@link EquinixRateLimitException}</td></tr>
 *   <tr><td>5xx Server Error</td><td>{@link EquinixServerException}</td></tr>
 * </table>
 *
 * <h3>Error Handling</h3>
 * <pre>{@code
 * try {
 *     Connection conn = fabric.connections().getByUuid("uuid");
 * } catch (EquinixNotFoundException e) {
 *     System.err.println("Not found: " + e.getStatusCode());
 * } catch (EquinixServiceException e) {
 *     System.err.println("API error: " + e.getMessage());
 * }
 * }</pre>
 *
 * @author ianjones
 */
@Getter
public class EquinixServiceException extends BaseException {
    private static final long serialVersionUID = 1L;

    private final List<ExceptionDetail> exceptionDetails;
    private final Integer statusCode;
    private final Map<String, String> httpHeaders;
    private final String path;

    /**
     * The SDK-generated request correlation id ({@code X-Correlation-Id} header value) of the
     * request that produced this error, or {@code null} for errors constructed outside the HTTP
     * layer. Quote this id when raising the failure with Equinix support - the same id was sent
     * to the API and appears in the SDK's request/retry logs.
     */
    private final String correlationId;

    /**
     * A whitespace-collapsed, truncated snippet of the raw error body, kept when the body could not
     * be parsed into any {@link ExceptionDetail} (an HTML gateway page, plain text, or JSON of an
     * unknown shape); {@code null} when details were parsed or there was no body.
     */
    private final String responseBodySnippet;

    /**
     * Creates an exception carrying only a message (no HTTP metadata).
     *
     * @param errorMessage a human-readable message
     */
    public EquinixServiceException(String errorMessage) {
        this(errorMessage, null, null, null, null, null, null);
    }

    /**
     * Creates an exception carrying a message and a cause (no HTTP metadata).
     *
     * @param errorMessage a human-readable message
     * @param cause        the underlying cause
     */
    public EquinixServiceException(String errorMessage, Exception cause) {
        super(errorMessage, cause);
        this.exceptionDetails = List.of();
        this.statusCode = null;
        this.httpHeaders = null;
        this.path = null;
        this.correlationId = null;
        this.responseBodySnippet = null;
    }

    /**
     * Convenience constructor without a correlation id; see the full constructor.
     *
     * @param errorMessage     a human-readable summary message.
     * @param statusCode       the HTTP status code returned by the API.
     * @param path             the request URI that produced the error.
     * @param httpHeaders      relevant response headers (e.g. {@code Retry-After}); may be {@code null}.
     * @param exceptionDetails structured error details parsed from the response body; may be {@code null}.
     */
    public EquinixServiceException(String errorMessage, Integer statusCode, String path,
                                   Map<String, String> httpHeaders, List<ExceptionDetail> exceptionDetails) {
        this(errorMessage, statusCode, path, httpHeaders, exceptionDetails, null);
    }

    /**
     * Full constructor used by {@link com.eqixiac.equinix.core.http.ResponseErrorMapper ResponseErrorMapper} when mapping an HTTP error response
     * into a typed exception. All API-error metadata is supplied at construction time and stored
     * as immutable copies, so the exception's state cannot change after construction. Null entries
     * are dropped while copying: a gateway can return a details array containing JSON {@code null}
     * elements (e.g. a {@code [null]} body) and header maps can carry {@code null} values - neither
     * may abort exception construction with a {@code NullPointerException}.
     *
     * @param errorMessage     a human-readable summary message.
     * @param statusCode       the HTTP status code returned by the API.
     * @param path             the request URI that produced the error.
     * @param httpHeaders      relevant response headers (e.g. {@code Retry-After}); may be {@code null}.
     * @param exceptionDetails structured error details parsed from the response body; may be {@code null}.
     * @param correlationId    the SDK-generated {@code X-Correlation-Id} of the failed request; may be {@code null}.
     */
    public EquinixServiceException(String errorMessage, Integer statusCode, String path,
                                   Map<String, String> httpHeaders, List<ExceptionDetail> exceptionDetails,
                                   String correlationId) {
        this(errorMessage, statusCode, path, httpHeaders, exceptionDetails, correlationId, null);
    }

    /**
     * Full constructor additionally carrying the raw-body snippet of an error body that could not
     * be parsed into details.
     *
     * @param errorMessage        a human-readable summary message.
     * @param statusCode          the HTTP status code returned by the API.
     * @param path                the request URI that produced the error.
     * @param httpHeaders         relevant response headers (e.g. {@code Retry-After}); may be {@code null}.
     * @param exceptionDetails    structured error details parsed from the response body; may be {@code null}.
     * @param correlationId       the SDK-generated {@code X-Correlation-Id} of the failed request; may be {@code null}.
     * @param responseBodySnippet truncated raw body when no details could be parsed; may be {@code null}.
     */
    public EquinixServiceException(String errorMessage, Integer statusCode, String path,
                                   Map<String, String> httpHeaders, List<ExceptionDetail> exceptionDetails,
                                   String correlationId, String responseBodySnippet) {
        super(errorMessage);
        this.statusCode = statusCode;
        this.path = path;
        this.httpHeaders = copyOfHeaders(httpHeaders);
        this.exceptionDetails = copyOfDetails(exceptionDetails);
        this.correlationId = correlationId;
        this.responseBodySnippet = responseBodySnippet;
    }

    /**
     * Immutable copy of the headers with null keys/values dropped ({@code Map.copyOf} would throw
     * a {@code NullPointerException} on them).
     */
    private static Map<String, String> copyOfHeaders(Map<String, String> httpHeaders) {
        if (httpHeaders == null) {
            return null;
        }
        return httpHeaders.entrySet().stream()
                .filter(entry -> entry.getKey() != null && entry.getValue() != null)
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /**
     * Immutable copy of the details with null elements dropped ({@code List.copyOf} would throw a
     * {@code NullPointerException} on a parsed {@code [null]} body).
     */
    private static List<ExceptionDetail> copyOfDetails(List<ExceptionDetail> exceptionDetails) {
        if (exceptionDetails == null) {
            return List.of();
        }
        return exceptionDetails.stream().filter(Objects::nonNull).toList();
    }

    /**
     * The summary (which for a mapped HTTP error already ends with the first detail's
     * {@code <code>: <message> (field: <property>)} line), followed by the HTTP status, URI and correlation id, then one block per parsed
     * {@link ExceptionDetail}: {@code Error: <code>: <message> (field: <property>)} plus any
     * details, developer message, help and property annotations. When nothing could be parsed, the
     * raw-body snippet is appended instead.
     *
     * @return the full multi-line message
     */
    @Override
    public String getMessage() {
        StringBuilder errorString = new StringBuilder();

        String summary = super.getMessage();
        if (!StringUtils.isNullOrEmpty(summary)) {
            errorString.append(summary);
        }
        if (statusCode != null) {
            appendLine(errorString, "Status Code: " + statusCode);
        }
        if (path != null) {
            appendLine(errorString, "URI: " + path);
        }
        if (correlationId != null) {
            appendLine(errorString, "Correlation Id: " + correlationId);
        }

        boolean first = true;
        for (ExceptionDetail detail : exceptionDetails) {
            String line = detail.toSummaryLine();
            // ResponseErrorMapper already appends the first detail's line to the summary.
            boolean inSummary = first && summary != null && summary.endsWith(line);
            first = false;
            if (!line.isEmpty() && !inSummary) {
                appendLine(errorString, "Error: " + line);
            }
            appendIfPresent(errorString, "  Title: ", detail.getErrorMessage() != null ? detail.getErrorTitle() : null);
            appendIfPresent(errorString, "  Details: ", detail.getDetails());
            appendIfPresent(errorString, "  Developer Message: ", detail.getDeveloperMessage());
            appendIfPresent(errorString, "  Error Domain: ", detail.getErrorDomain());
            appendIfPresent(errorString, "  Help: ", detail.getHelp());
            if (detail.getCorrelationId() != null && !detail.getCorrelationId().equals(correlationId)) {
                appendIfPresent(errorString, "  Service Correlation Id: ", detail.getCorrelationId());
            }
            for (ExceptionAdditionalInfo info : detail.getAdditionalInfo()) {
                StringBuilder infoLine = new StringBuilder();
                if (!StringUtils.isNullOrEmpty(info.getProperty())) {
                    infoLine.append("Property: ").append(info.getProperty());
                }
                if (!StringUtils.isNullOrEmpty(info.getReason())) {
                    infoLine.append(infoLine.length() > 0 ? " | " : "").append("Reason: ").append(info.getReason());
                }
                if (!StringUtils.isNullOrEmpty(info.getValue())) {
                    infoLine.append(infoLine.length() > 0 ? " | " : "").append("Value: ").append(info.getValue());
                }
                if (infoLine.length() > 0) {
                    appendLine(errorString, "  " + infoLine);
                }
            }
        }
        if (exceptionDetails.isEmpty() && responseBodySnippet != null) {
            appendLine(errorString, "Response Body: " + responseBodySnippet);
        }

        return errorString.toString();
    }

    private static void appendIfPresent(StringBuilder errorString, String label, String value) {
        if (!StringUtils.isNullOrEmpty(value)) {
            appendLine(errorString, label + value);
        }
    }

    private static void appendLine(StringBuilder errorString, String line) {
        if (errorString.length() > 0) {
            errorString.append('\n');
        }
        errorString.append(line);
    }
}
