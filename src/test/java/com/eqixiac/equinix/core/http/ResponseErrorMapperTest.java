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
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link ResponseErrorMapper}: the status-to-typed-exception selection and the
 * per-service error-body parsing. The {@code /json/core/errors/*} fixtures carry the error SHAPES
 * observed live on 2026-11-11 (or documented by each service's spec); message texts are
 * illustrative where the live text was not recorded.
 */
class ResponseErrorMapperTest {

    private static final String PATH = "/fabric/v4/connections/abc";

    @Test
    void mapsKnownStatusCodesToTypedSubclasses() {
        assertInstanceOf(EquinixAuthenticationException.class,
                ResponseErrorMapper.toException(401, PATH, null, null, null));
        assertInstanceOf(EquinixAuthorizationException.class,
                ResponseErrorMapper.toException(403, PATH, null, null, null));
        assertInstanceOf(EquinixNotFoundException.class,
                ResponseErrorMapper.toException(404, PATH, null, null, null));
        assertInstanceOf(EquinixConflictException.class,
                ResponseErrorMapper.toException(409, PATH, null, null, null));
        assertInstanceOf(EquinixRateLimitException.class,
                ResponseErrorMapper.toException(429, PATH, null, null, null));
        assertInstanceOf(EquinixServerException.class,
                ResponseErrorMapper.toException(503, PATH, null, null, null));
    }

    @Test
    void mapsUnclassified4xxToBaseServiceException() {
        EquinixServiceException ex = ResponseErrorMapper.toException(422, PATH, null, null, null);
        assertEquals(EquinixServiceException.class, ex.getClass());
        assertEquals(422, ex.getStatusCode());
        assertEquals(PATH, ex.getPath());
    }

    @Test
    void carriesStatusPathAndHeaders() {
        Map<String, String> headers = Map.of("Retry-After", "30");
        EquinixRateLimitException ex = (EquinixRateLimitException)
                ResponseErrorMapper.toException(429, PATH, headers, null, null);

        assertEquals(429, ex.getStatusCode());
        assertEquals(PATH, ex.getPath());
        assertEquals("30", ex.getHttpHeaders().get("Retry-After"));
    }

    @Test
    void parsesJsonArrayErrorBody() {
        String body = "[{\"errorCode\":\"ERR-404\",\"errorMessage\":\"Resource not found\","
                + "\"correlationId\":\"abc-123\"}]";
        EquinixServiceException ex = ResponseErrorMapper.toException(404, PATH, null, body, null);

        assertEquals(1, ex.getExceptionDetails().size());
        assertEquals("ERR-404", ex.getExceptionDetails().get(0).getErrorCode());
        assertEquals("Resource not found", ex.getExceptionDetails().get(0).getErrorMessage());
        assertEquals("abc-123", ex.getExceptionDetails().get(0).getCorrelationId());
    }

    @Test
    void parsesSingleObjectErrorBody() {
        String body = "{\"errorCode\":\"ERR-409\",\"errorMessage\":\"Conflict\"}";
        EquinixServiceException ex = ResponseErrorMapper.toException(409, PATH, null, body, null);

        assertEquals(1, ex.getExceptionDetails().size());
        assertEquals("ERR-409", ex.getExceptionDetails().get(0).getErrorCode());
    }

    @Test
    void unparseableBodyYieldsEmptyDetails() {
        EquinixServiceException ex = ResponseErrorMapper.toException(500, PATH, null, "not json at all", null);

        // No blank placeholder entry - an unparseable body leaves the details empty.
        assertTrue(ex.getExceptionDetails().isEmpty());
        assertEquals(500, ex.getStatusCode());
    }

    @Test
    void blankOrNullBodyYieldsEmptyDetails() {
        assertTrue(ResponseErrorMapper.toException(404, PATH, null, null, null).getExceptionDetails().isEmpty());
        assertTrue(ResponseErrorMapper.toException(404, PATH, null, "   ", null).getExceptionDetails().isEmpty());
    }

    @Test
    void nullElementsInDetailsArrayAreFiltered() {
        // Some gateways return a details array containing JSON nulls; List.copyOf would NPE on
        // them and abort exception construction. They must be dropped instead.
        EquinixServiceException ex = ResponseErrorMapper.toException(502, PATH, null, "[null]", null);

        assertInstanceOf(EquinixServerException.class, ex);
        assertEquals(502, ex.getStatusCode());
        assertEquals(PATH, ex.getPath());
        assertTrue(ex.getExceptionDetails().isEmpty());
        assertDoesNotThrow(ex::getMessage);
    }

    @Test
    void nullElementsAlongsideRealDetailsAreFiltered() {
        String body = "[null,{\"errorCode\":\"ERR-500\",\"errorMessage\":\"Server error\"},null]";
        EquinixServiceException ex = ResponseErrorMapper.toException(500, PATH, null, body, null);

        assertEquals(1, ex.getExceptionDetails().size());
        assertEquals("ERR-500", ex.getExceptionDetails().get(0).getErrorCode());
        assertDoesNotThrow(ex::getMessage);
    }

    @Test
    void nullHeaderEntriesAreFiltered() {
        // Map.copyOf would NPE on a null header value; the null entry must be dropped while the
        // real ones are kept.
        Map<String, String> headers = new HashMap<>();
        headers.put("Retry-After", "30");
        headers.put("X-Broken", null);
        headers.put(null, "orphan-value");

        EquinixServiceException ex = ResponseErrorMapper.toException(429, PATH, headers, null, null);

        assertEquals(429, ex.getStatusCode());
        assertEquals("30", ex.getHttpHeaders().get("Retry-After"));
        assertFalse(ex.getHttpHeaders().containsKey("X-Broken"));
        assertEquals(1, ex.getHttpHeaders().size());
    }

    @Test
    void messageIncludesSummaryStatusAndPath() {
        EquinixServiceException ex = ResponseErrorMapper.toException(404, PATH, null, null, null);
        String message = ex.getMessage();
        assertNotNull(message);
        // The constructor-supplied summary must not be discarded by the getMessage() override.
        assertTrue(message.contains("Resource not found (HTTP 404)."));
        assertTrue(message.contains("404"));
        assertTrue(message.contains(PATH));
    }

    @Test
    void carriesCorrelationIdInFieldAndMessage() {
        EquinixServiceException ex = ResponseErrorMapper.toException(
                503, PATH, null, null, "11111111-2222-3333-4444-555555555555");

        assertEquals("11111111-2222-3333-4444-555555555555", ex.getCorrelationId());
        assertTrue(ex.getMessage().contains("Correlation Id: 11111111-2222-3333-4444-555555555555"));
    }

    @Test
    void nullCorrelationIdIsOmittedFromMessage() {
        EquinixServiceException ex = ResponseErrorMapper.toException(503, PATH, null, null, null);

        assertNull(ex.getCorrelationId());
        assertFalse(ex.getMessage().contains("Correlation Id"));
    }

    @Test
    void exceptionDetailsAreImmutable() {
        String body = "[{\"errorCode\":\"ERR-404\",\"errorMessage\":\"Resource not found\"}]";
        EquinixServiceException ex = ResponseErrorMapper.toException(404, PATH, null, body, null);

        assertThrows(UnsupportedOperationException.class, () -> ex.getExceptionDetails().clear());
    }

    // ------------------------------------------------------------------------------------------
    // Per-service error formats
    // ------------------------------------------------------------------------------------------

    private static String fixture(String name) {
        try (InputStream in = ResponseErrorMapperTest.class.getResourceAsStream("/json/core/errors/" + name)) {
            assertNotNull(in, "missing fixture " + name);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static ExceptionDetail single(EquinixServiceException ex) {
        assertEquals(1, ex.getExceptionDetails().size(), () -> "details: " + ex.getExceptionDetails());
        assertNull(ex.getResponseBodySnippet(), "a parsed body must not also be kept as a snippet");
        return ex.getExceptionDetails().get(0);
    }

    @Test
    void fabricBareArray_codeMessageDetailsCorrelationAndProperty() {
        EquinixServiceException ex = ResponseErrorMapper.toException(400, PATH, null, fixture("fabric_400.json"), null);
        ExceptionDetail d = single(ex);

        assertEquals("EQ-3142501", d.getErrorCode());
        assertEquals("Invalid seller metroCode location", d.getErrorMessage());
        assertEquals("zSide metroCode must match a seller location", d.getDetails());
        assertEquals("f3d0c1a2-1b2c-4d5e-8f90-0a1b2c3d4e5f", d.getCorrelationId());
        assertEquals("/zSide/accessPoint/location/metroCode", d.getProperty());
        assertEquals(1, d.getAdditionalInfo().size());
        assertEquals("Invalid seller metroCode location", d.getAdditionalInfo().get(0).getReason());
        assertTrue(ex.getMessage().contains(
                "EQ-3142501: Invalid seller metroCode location (field: /zSide/accessPoint/location/metroCode)"),
                ex.getMessage());
    }

    @Test
    void iamErrorObject_nestedCodeMessageAndDetailsKeyAsField() {
        EquinixServiceException ex = ResponseErrorMapper.toException(400, PATH, null, fixture("iam_400.json"), null);
        ExceptionDetail d = single(ex);

        assertEquals("invalid-argument", d.getErrorCode());
        assertEquals("Project arg only supported when using an access-policy auth token", d.getErrorMessage());
        assertEquals("projectId", d.getProperty());
        assertEquals("projectId: Project arg only supported when using an access-policy auth token", d.getDetails());
        assertTrue(ex.getMessage().startsWith("Error returned by Equinix API (HTTP 400). invalid-argument: "
                + "Project arg only supported when using an access-policy auth token (field: projectId)"), ex.getMessage());
    }

    @Test
    void iamSpecMinimalErrorResponse() {
        // accessv1.yaml ErrorResponse: {"error":{"errorCode":"not-found"}} is the documented minimum.
        ExceptionDetail d = single(ResponseErrorMapper.toException(404, PATH, null,
                "{\"error\":{\"errorCode\":\"not-found\"}}", null));
        assertEquals("not-found", d.getErrorCode());
    }

    @Test
    void stsOauthError_snakeCaseAndCamelCaseDescription() {
        ExceptionDetail snake = single(ResponseErrorMapper.toException(400, PATH, null, fixture("sts_400.json"), null));
        assertEquals("invalid_scope", snake.getErrorCode());
        assertEquals("The requested scope is invalid, unknown, or malformed.", snake.getErrorMessage());

        ExceptionDetail camel = single(ResponseErrorMapper.toException(400, PATH, null, fixture("sts_400_camel.json"), null));
        assertEquals("invalid_request", camel.getErrorCode());
        assertEquals("subject_token is required", camel.getErrorMessage());
    }

    @Test
    void operationsErrors_codeFieldsAndMessage() {
        EquinixServiceException ex = ResponseErrorMapper.toException(400, PATH, null, fixture("operations_400.json"), null);
        ExceptionDetail d = single(ex);

        assertEquals("EQX-171-1212", d.getErrorCode());
        assertEquals("Invalid value for productTypes", d.getErrorMessage());
        assertEquals("/filters/productTypes/0", d.getProperty());
        assertTrue(ex.getMessage().contains("EQX-171-1212: Invalid value for productTypes (field: /filters/productTypes/0)"));
    }

    @Test
    void smartHandsErrors_stringEntriesAndTicketId() {
        EquinixServiceException ex = ResponseErrorMapper.toException(400, PATH, null, fixture("smarthands_400.json"), null);

        List<ExceptionDetail> details = ex.getExceptionDetails();
        assertEquals(2, details.size());
        assertEquals("Requested date must be in the future", details.get(0).getErrorMessage());
        assertEquals("Location is not valid for this account", details.get(1).getErrorMessage());
        assertEquals("ticketId: 1-234567890123", details.get(0).getDetails());
        assertTrue(ex.getMessage().contains("Location is not valid for this account"));
        assertTrue(ex.getMessage().contains("ticketId: 1-234567890123"));
    }

    @Test
    void smartViewV2Array_titleMessageDeveloperMessage() {
        EquinixServiceException ex = ResponseErrorMapper.toException(400, PATH, null, fixture("smartview_v2_400.json"), null);
        ExceptionDetail d = single(ex);

        assertEquals("DCIM-ASSET-52724", d.getErrorCode());
        assertEquals("Invalid request", d.getErrorMessage());
        assertEquals("Bad Request", d.getErrorTitle());
        assertEquals("accountNo is a mandatory parameter/header", d.getDeveloperMessage());
        assertNull(d.getProperty(), "fieldName:null must not become a field");
        assertTrue(ex.getMessage().contains("Developer Message: accountNo is a mandatory parameter/header"));
    }

    @Test
    void smartViewV1Legacy_statusErrorAndPayload() {
        EquinixServiceException ex = ResponseErrorMapper.toException(400, PATH, null, fixture("smartview_v1_400.json"), null);
        ExceptionDetail d = single(ex);

        assertEquals("3001", d.getErrorCode());
        assertEquals("INVALID_INPUT", d.getErrorTitle());
        assertEquals("Required parameter 'accountNo'", d.getErrorMessage());
        assertTrue(ex.getMessage().contains("3001: Required parameter 'accountNo'"));
    }

    @Test
    void gatewaySingleObject_allFields() {
        EquinixServiceException ex = ResponseErrorMapper.toException(404, PATH, null, fixture("gateway_404.json"), null);
        ExceptionDetail d = single(ex);

        assertEquals("IC-LG-01", d.getErrorCode());
        assertEquals("Resource not found", d.getErrorMessage());
        assertEquals("apigateway", d.getErrorDomain());
        assertEquals("Not Found", d.getErrorTitle());
        assertEquals("No resource matched the requested path", d.getDeveloperMessage());
    }

    @Test
    void apigeeFault_faultstringAndErrorcode() {
        EquinixServiceException ex = ResponseErrorMapper.toException(401, PATH, null, fixture("apigee_401.json"), null);
        ExceptionDetail d = single(ex);

        assertInstanceOf(EquinixAuthenticationException.class, ex);
        assertEquals("oauth.v2.InvalidAccessToken", d.getErrorCode());
        assertEquals("Invalid access token", d.getErrorMessage());
        assertTrue(ex.getMessage().contains("oauth.v2.InvalidAccessToken: Invalid access token"));
    }

    @Test
    void htmlBody_keptAsTruncatedCollapsedSnippet() {
        EquinixServiceException ex = ResponseErrorMapper.toException(504, PATH, null, fixture("gateway_504.html"), null);

        assertTrue(ex.getExceptionDetails().isEmpty());
        assertEquals("<html> <head><title>504 Gateway Time-out</title></head> <body> "
                + "<center><h1>504 Gateway Time-out</h1></center> </body> </html>", ex.getResponseBodySnippet());
        assertTrue(ex.getMessage().contains("Response Body: <html>"));
    }

    @Test
    void longUnparseableBody_isTruncated() {
        String body = "x".repeat(ErrorBodyParser.SNIPPET_MAX_CHARS + 100);
        EquinixServiceException ex = ResponseErrorMapper.toException(502, PATH, null, body, null);

        assertEquals(ErrorBodyParser.SNIPPET_MAX_CHARS + 3, ex.getResponseBodySnippet().length());
        assertTrue(ex.getResponseBodySnippet().endsWith("..."));
    }

    @Test
    void jsonOfUnknownShape_keptAsSnippet() {
        EquinixServiceException ex = ResponseErrorMapper.toException(500, PATH, null, "{\"foo\":{\"bar\":1}}", null);

        assertTrue(ex.getExceptionDetails().isEmpty());
        assertEquals("{\"foo\":{\"bar\":1}}", ex.getResponseBodySnippet());
    }

    @Test
    void unknownKeysAndNonScalarValuesAreTolerated() {
        String body = "[{\"errorCode\":\"EQ-1\",\"errorMessage\":{\"nested\":true},\"brandNew\":[1,2],"
                + "\"message\":\"fallback text\",\"additionalInfo\":[\"not-an-object\",{\"property\":\"p\"}]}]";
        ExceptionDetail d = single(ResponseErrorMapper.toException(400, PATH, null, body, null));

        assertEquals("EQ-1", d.getErrorCode());
        assertEquals("fallback text", d.getErrorMessage(), "a non-scalar errorMessage falls through to message");
        assertEquals("p", d.getProperty());
    }
}
