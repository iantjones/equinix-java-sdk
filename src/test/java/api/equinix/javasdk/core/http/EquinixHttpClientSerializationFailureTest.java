package api.equinix.javasdk.core.http;

import api.equinix.javasdk.core.enums.HttpMethod;
import api.equinix.javasdk.core.exception.EquinixClientException;
import api.equinix.javasdk.core.http.request.EquinixRequest;
import api.equinix.javasdk.core.http.request.RequestBody;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Regression tests for request-body serialization failures in
 * {@link EquinixHttpClient#executeWithRetries}. A {@code JsonProcessingException} thrown while
 * serializing the request body extends {@code IOException}, but it is a deterministic client-side
 * bug — not a transport failure: it must be thrown after exactly one attempt (no retries, which
 * could never succeed) and must not be recorded on the circuit breaker (the service was never
 * contacted, so it says nothing about service health).
 *
 * <p>No HTTP server is involved: the serialization failure happens while building the wire entity,
 * before any network I/O, so the endpoint below is never contacted.</p>
 */
class EquinixHttpClientSerializationFailureTest {

    /** Incremented once per serialization attempt — Jackson calls the getter each time it serializes. */
    private final AtomicInteger serializationAttempts = new AtomicInteger();

    /** A payload that deterministically fails Jackson serialization on every attempt. */
    public class UnserializablePayload {
        public String getValue() {
            serializationAttempts.incrementAndGet();
            throw new IllegalStateException("deterministically unserializable");
        }
    }

    @Test
    @DisplayName("a body that always fails serialization -> exactly one attempt, thrown immediately, breaker untouched")
    void serializationFailure_isNotRetried_andDoesNotTouchBreaker() throws Exception {
        try (EquinixHttpClient client = new EquinixHttpClient()) {
            client.setRetryPolicy(new RetryPolicy(3, 1, 5, Set.of(429, 500, 502, 503, 504), true, true));
            // Threshold 1: a single recordFailure() would open the breaker, so CLOSED afterwards
            // proves the failure was never recorded.
            CircuitBreaker breaker = new CircuitBreaker(1, 60_000);
            client.setCircuitBreaker(breaker);

            EquinixClientException ex = assertThrows(EquinixClientException.class,
                    () -> client.executeWithRetries(putRequestWithBody(new UnserializablePayload())));

            assertInstanceOf(JsonProcessingException.class, ex.getCause());
            // Deterministic client bug: exactly one serialization attempt, no retries.
            assertEquals(1, serializationAttempts.get());
            // The breaker was untouched.
            assertEquals(CircuitBreaker.State.CLOSED, breaker.getState());
            assertDoesNotThrow(breaker::acquire);
        }
    }

    /**
     * A PUT (idempotent — eligible for IO-exception retries under the policy above) so that only
     * the serialization-failure classification, not the method-idempotency gate, prevents retries.
     */
    private EquinixRequest<Object> putRequestWithBody(Object payload) {
        EquinixRequest<Object> request = new EquinixRequest<>();
        request.setHttpMethod(HttpMethod.PUT);
        // Never contacted: serialization fails before any connection is opened.
        request.setEndPoint(URI.create("http://localhost:1"));
        request.setResourcePath("widgets");
        request.setBody(RequestBody.json(payload));
        return request;
    }
}
