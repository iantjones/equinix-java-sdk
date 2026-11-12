package com.eqixiac.equinix.core;

import com.eqixiac.equinix.core.exception.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the exception class hierarchy and structure.
 * Verifies inheritance chains, constructors, and field accessors.
 */
class ExceptionMappingTest {

    // Subtype relationships are compile-time facts (an instanceOf assertion on them cannot fail),
    // so only the deliberate NON-subtype relationship is pinned here.
    @Test
    void serviceExceptionsAreSiblingsOfClientException() {
        // Service (API) errors must NOT be a kind of client (SDK/network) error: they are siblings
        // under BaseException so catch blocks don't conflate them.
        assertFalse(EquinixClientException.class.isAssignableFrom(EquinixServiceException.class));
        assertFalse(EquinixServiceException.class.isAssignableFrom(EquinixClientException.class));
    }

    @Test
    void serviceException_statusCodeAndPath() {
        EquinixServiceException ex = new EquinixServiceException(
                "test error", 404, "/fabric/v4/connections/invalid-uuid", null, null);

        assertEquals(404, ex.getStatusCode());
        assertEquals("/fabric/v4/connections/invalid-uuid", ex.getPath());
    }

    @Test
    void serviceException_exceptionDetails() {
        EquinixServiceException ex = new EquinixServiceException("test error");
        assertNotNull(ex.getExceptionDetails());
        assertTrue(ex.getExceptionDetails().isEmpty());
    }

    @Test
    void serviceException_messageContainsStatusAndPath() {
        EquinixServiceException ex = new EquinixServiceException(
                "test error", 401, "/fabric/v4/ports", null, null);

        String message = ex.getMessage();
        assertTrue(message.contains("401"));
        assertTrue(message.contains("/fabric/v4/ports"));
    }

    @Test
    void serviceException_messageOnlyCtor_preservesConstructorMessage() {
        EquinixServiceException ex = new EquinixServiceException("Could not determine deserialization target.");

        // The constructor-supplied summary must survive the getMessage() override, and the
        // null status/path must not surface as literal "null" lines.
        assertEquals("Could not determine deserialization target.", ex.getMessage());
        assertFalse(ex.getMessage().contains("null"));
    }

    @Test
    void serviceException_fullCtor_messageStartsWithSummary() {
        EquinixServiceException ex = new EquinixServiceException(
                "Rate limit exceeded (HTTP 429).", 429, "/fabric/v4/ports", null, null);

        assertTrue(ex.getMessage().startsWith("Rate limit exceeded (HTTP 429)."));
    }

    @Test
    void serviceException_correlationId_carriedAndRendered() {
        EquinixServiceException ex = new EquinixServiceException(
                "Server error (HTTP 503).", 503, "/fabric/v4/ports", null, null, "cid-1234");

        assertEquals("cid-1234", ex.getCorrelationId());
        assertTrue(ex.getMessage().contains("Correlation Id: cid-1234"));
    }

    @Test
    void serviceException_collectionsAreImmutable() {
        java.util.List<ExceptionDetail> details = new java.util.ArrayList<>();
        details.add(ExceptionDetail.builder().errorCode("EQ-1").build());
        java.util.Map<String, String> headers = new java.util.HashMap<>();
        headers.put("Retry-After", "30");

        EquinixServiceException ex = new EquinixServiceException(
                "test error", 429, "/fabric/v4/ports", headers, details);

        // The exception snapshots its inputs: later caller-side mutation must not leak in...
        details.clear();
        headers.clear();
        assertEquals(1, ex.getExceptionDetails().size());
        assertEquals("30", ex.getHttpHeaders().get("Retry-After"));

        // ...and the exposed collections themselves reject mutation.
        assertThrows(UnsupportedOperationException.class, () -> ex.getExceptionDetails().clear());
        assertThrows(UnsupportedOperationException.class, () -> ex.getHttpHeaders().put("x", "y"));
    }

    @Test
    void serviceException_isJavaSerializable() throws Exception {
        EquinixServiceException ex = new EquinixServiceException(
                "test error", 503, "/fabric/v4/ports", java.util.Map.of("Retry-After", "30"),
                java.util.List.of(ExceptionDetail.builder().errorCode("EQ-3142501")
                        .additionalInfo(List.of(new ExceptionAdditionalInfo("$.x", "bad", null))).build()),
                "cid-1234");

        java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
        try (java.io.ObjectOutputStream oos = new java.io.ObjectOutputStream(bos)) {
            oos.writeObject(ex);
        }
        try (java.io.ObjectInputStream ois = new java.io.ObjectInputStream(
                new java.io.ByteArrayInputStream(bos.toByteArray()))) {
            EquinixServiceException roundTripped = (EquinixServiceException) ois.readObject();
            assertEquals(503, roundTripped.getStatusCode());
            assertEquals("cid-1234", roundTripped.getCorrelationId());
            assertEquals(1, roundTripped.getExceptionDetails().size());
            assertEquals("EQ-3142501", roundTripped.getExceptionDetails().get(0).getErrorCode());
            assertEquals("$.x", roundTripped.getExceptionDetails().get(0).getAdditionalInfo().get(0).getProperty());
        }
    }

    @Test
    void serviceException_causeConstructor() {
        Exception cause = new RuntimeException("root cause");
        EquinixServiceException ex = new EquinixServiceException("wrapper", cause);
        assertEquals(cause, ex.getCause());
    }

    @Test
    void serviceException_responseBodySnippetRenderedOnlyWithoutDetails() {
        EquinixServiceException withoutDetails = new EquinixServiceException(
                "Server error (HTTP 504).", 504, "/fabric/v4/ports", null, null, null, "<html>Gateway Timeout</html>");
        assertEquals("<html>Gateway Timeout</html>", withoutDetails.getResponseBodySnippet());
        assertTrue(withoutDetails.getMessage().contains("Response Body: <html>Gateway Timeout</html>"));

        EquinixServiceException withDetails = new EquinixServiceException(
                "Server error (HTTP 500).", 500, "/fabric/v4/ports", null,
                List.of(ExceptionDetail.builder().errorCode("EQ-1").errorMessage("boom").build()), null, "raw");
        assertFalse(withDetails.getMessage().contains("Response Body"));
        assertTrue(withDetails.getMessage().contains("Error: EQ-1: boom"));
    }

    @Test
    void exceptionSubclasses_causePropagation() {
        Exception rootCause = new IllegalArgumentException("bad arg");

        EquinixAuthenticationException authEx = new EquinixAuthenticationException("auth failed", rootCause);
        assertEquals(rootCause, authEx.getCause());

        EquinixNotFoundException notFoundEx = new EquinixNotFoundException("not found", rootCause);
        assertEquals(rootCause, notFoundEx.getCause());

        EquinixRateLimitException rateEx = new EquinixRateLimitException("rate limited", rootCause);
        assertEquals(rootCause, rateEx.getCause());

        EquinixServerException serverEx = new EquinixServerException("server error", rootCause);
        assertEquals(rootCause, serverEx.getCause());

        EquinixConflictException conflictEx = new EquinixConflictException("conflict", rootCause);
        assertEquals(rootCause, conflictEx.getCause());

        EquinixAuthorizationException authzEx = new EquinixAuthorizationException("forbidden", rootCause);
        assertEquals(rootCause, authzEx.getCause());
    }
}
