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

import api.equinix.javasdk.core.util.StringUtils;
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
 * It is a <em>sibling</em> of {@link EquinixClientException} — both extend {@link BaseException} —
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
     * layer. Quote this id when raising the failure with Equinix support — the same id was sent
     * to the API and appears in the SDK's request/retry logs.
     */
    private final String correlationId;

    public EquinixServiceException(String errorMessage) {
        this(errorMessage, null, null, null, null, null);
    }

    public EquinixServiceException(String errorMessage, Exception cause) {
        super(errorMessage, cause);
        this.exceptionDetails = List.of();
        this.statusCode = null;
        this.httpHeaders = null;
        this.path = null;
        this.correlationId = null;
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
     * Full constructor used by {@link api.equinix.javasdk.core.http.ResponseErrorMapper ResponseErrorMapper} when mapping an HTTP error response
     * into a typed exception. All API-error metadata is supplied at construction time and stored
     * as immutable copies, so the exception's state cannot change after construction. Null entries
     * are dropped while copying: a gateway can return a details array containing JSON {@code null}
     * elements (e.g. a {@code [null]} body) and header maps can carry {@code null} values — neither
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
        super(errorMessage);
        this.statusCode = statusCode;
        this.path = path;
        this.httpHeaders = copyOfHeaders(httpHeaders);
        this.exceptionDetails = copyOfDetails(exceptionDetails);
        this.correlationId = correlationId;
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

        for(ExceptionDetail exceptionDetail : exceptionDetails) {
            errorString.append(!StringUtils.isNullOrEmpty(exceptionDetail.getErrorMessage()) ? "\nError Message: " + exceptionDetail.getErrorMessage() : "")
                    .append(!StringUtils.isNullOrEmpty(exceptionDetail.getFault()) ? "\nFault: " + exceptionDetail.getFault() : "")
                    .append(!StringUtils.isNullOrEmpty(exceptionDetail.getError()) ? "\nError: " + exceptionDetail.getError() : "")
                    .append(!StringUtils.isNullOrEmpty(exceptionDetail.getDetails()) ? "\nDetails: " + exceptionDetail.getDetails() : "")
                    .append(!StringUtils.isNullOrEmpty(exceptionDetail.getMoreInfo()) ? "\nMore Info: " + exceptionDetail.getMoreInfo() : "")
                    .append(!StringUtils.isNullOrEmpty(exceptionDetail.getErrorCode()) ? "\nError Code: " + exceptionDetail.getErrorCode() : "")
                    .append(!StringUtils.isNullOrEmpty(exceptionDetail.getCorrelationId()) ? "\nCorrelation Id: " + exceptionDetail.getCorrelationId() : "")
                    .append(!StringUtils.isNullOrEmpty(exceptionDetail.getPath()) ? "\nURI: " + exceptionDetail.getPath() : "");

            for(ExceptionAdditionalInfo exceptionAdditionalInfo : exceptionDetail.getAdditionalInfo()) {
                errorString.append(!StringUtils.isNullOrEmpty(exceptionAdditionalInfo.getProperty()) ? "\nProperty: " + exceptionAdditionalInfo.getProperty() : "")
                            .append(!StringUtils.isNullOrEmpty(exceptionAdditionalInfo.getReason()) ? "|Reason: " + exceptionAdditionalInfo.getReason() : "")
                            .append(!StringUtils.isNullOrEmpty(exceptionAdditionalInfo.getValue()) ? "|Value: " + exceptionAdditionalInfo.getValue() : "")
                            .append("\n");
            }

            errorString.append("\n");
        }

        return errorString.toString();
    }

    private static void appendLine(StringBuilder errorString, String line) {
        if (errorString.length() > 0) {
            errorString.append('\n');
        }
        errorString.append(line);
    }
}
