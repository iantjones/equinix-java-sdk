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
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link ResponseErrorMapper} — the status-to-typed-exception selection and the
 * tolerant response-body parsing ladder (JSON array / single object / unparseable / empty).
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

        // No blank placeholder entry — an unparseable body leaves the details empty.
        assertTrue(ex.getExceptionDetails().isEmpty());
        assertEquals(500, ex.getStatusCode());
    }

    @Test
    void blankOrNullBodyYieldsEmptyDetails() {
        assertTrue(ResponseErrorMapper.toException(404, PATH, null, null, null).getExceptionDetails().isEmpty());
        assertTrue(ResponseErrorMapper.toException(404, PATH, null, "   ", null).getExceptionDetails().isEmpty());
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
}
