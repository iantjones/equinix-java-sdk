package api.equinix.javasdk.core;

import api.equinix.javasdk.core.exception.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the exception class hierarchy and structure.
 * Verifies inheritance chains, constructors, and field accessors.
 */
class ExceptionMappingTest {

    @Test
    void exceptionHierarchy_baseException_isRuntimeException() {
        BaseException ex = new BaseException("test") {};
        assertInstanceOf(RuntimeException.class, ex);
    }

    @Test
    void exceptionHierarchy_clientException_extendsBaseException() {
        EquinixClientException ex = new EquinixClientException("test");
        assertInstanceOf(BaseException.class, ex);
        assertInstanceOf(RuntimeException.class, ex);
    }

    @Test
    void exceptionHierarchy_serviceException_isSiblingOfClientException() {
        EquinixServiceException ex = new EquinixServiceException("test");
        assertInstanceOf(BaseException.class, ex);
        // Service (API) errors must NOT be a kind of client (SDK/network) error —
        // they are siblings under BaseException so catch blocks don't conflate them.
        assertFalse(EquinixClientException.class.isInstance(ex));
    }

    @Test
    void exceptionHierarchy_authenticationException_extendsServiceException() {
        EquinixAuthenticationException ex = new EquinixAuthenticationException("Unauthorized");
        assertInstanceOf(EquinixServiceException.class, ex);
        assertInstanceOf(BaseException.class, ex);
        assertFalse(EquinixClientException.class.isInstance(ex));
    }

    @Test
    void exceptionHierarchy_authorizationException_extendsServiceException() {
        EquinixAuthorizationException ex = new EquinixAuthorizationException("Forbidden");
        assertInstanceOf(EquinixServiceException.class, ex);
    }

    @Test
    void exceptionHierarchy_notFoundException_extendsServiceException() {
        EquinixNotFoundException ex = new EquinixNotFoundException("Not found");
        assertInstanceOf(EquinixServiceException.class, ex);
    }

    @Test
    void exceptionHierarchy_conflictException_extendsServiceException() {
        EquinixConflictException ex = new EquinixConflictException("Conflict");
        assertInstanceOf(EquinixServiceException.class, ex);
    }

    @Test
    void exceptionHierarchy_rateLimitException_extendsServiceException() {
        EquinixRateLimitException ex = new EquinixRateLimitException("Rate limited");
        assertInstanceOf(EquinixServiceException.class, ex);
    }

    @Test
    void exceptionHierarchy_serverException_extendsServiceException() {
        EquinixServerException ex = new EquinixServerException("Server error");
        assertInstanceOf(EquinixServiceException.class, ex);
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
        details.add(new ExceptionDetail());
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
                java.util.List.of(new ExceptionDetail()), "cid-1234");

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
        }
    }

    @Test
    void serviceException_causeConstructor() {
        Exception cause = new RuntimeException("root cause");
        EquinixServiceException ex = new EquinixServiceException("wrapper", cause);
        assertEquals(cause, ex.getCause());
    }

    @Test
    void exceptionDetail_deserializesFromJson() throws Exception {
        ObjectMapper mapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        InputStream is = getClass().getResourceAsStream("/json/core/error_401_response.json");
        assertNotNull(is, "error_401_response.json fixture not found on classpath");

        JsonNode root = mapper.readTree(is);
        JsonNode errorsNode = root.get("errors");
        assertNotNull(errorsNode, "JSON should contain 'errors' array");
        assertTrue(errorsNode.isArray());

        List<ExceptionDetail> details = mapper.readValue(
                errorsNode.traverse(mapper),
                new TypeReference<List<ExceptionDetail>>() {});

        assertEquals(1, details.size());
        assertEquals("ERR-401", details.get(0).getErrorCode());
        assertEquals("Unauthorized", details.get(0).getErrorMessage());
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
