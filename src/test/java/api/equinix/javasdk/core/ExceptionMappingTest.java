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
    void exceptionHierarchy_serviceException_extendsClientException() {
        EquinixServiceException ex = new EquinixServiceException("test");
        assertInstanceOf(EquinixClientException.class, ex);
        assertInstanceOf(BaseException.class, ex);
    }

    @Test
    void exceptionHierarchy_authenticationException_extendsServiceException() {
        EquinixAuthenticationException ex = new EquinixAuthenticationException("Unauthorized");
        assertInstanceOf(EquinixServiceException.class, ex);
        assertInstanceOf(EquinixClientException.class, ex);
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
        EquinixServiceException ex = new EquinixServiceException("test error");
        ex.setStatusCode(404);
        ex.setPath("/fabric/v4/connections/invalid-uuid");

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
        EquinixServiceException ex = new EquinixServiceException("test error");
        ex.setStatusCode(401);
        ex.setPath("/fabric/v4/ports");

        String message = ex.getMessage();
        assertTrue(message.contains("401"));
        assertTrue(message.contains("/fabric/v4/ports"));
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
