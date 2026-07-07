package api.equinix.javasdk.core.http;

import api.equinix.javasdk.core.enums.HttpMethod;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link RetryPolicy}, in particular the HTTP-method idempotency gating:
 * POST and PATCH are non-idempotent (RFC 5789 — the SDK sends RFC 6902 'add' operations that
 * must not be applied twice) and are not retried unless explicitly opted in.
 */
class RetryPolicyTest {

    @Test
    @DisplayName("default policy does not retry POST or PATCH (non-idempotent)")
    void defaultPolicy_doesNotRetryNonIdempotentMethods() {
        RetryPolicy policy = RetryPolicy.defaultPolicy();

        assertFalse(policy.isRetryableMethod(HttpMethod.POST));
        assertFalse(policy.isRetryableMethod(HttpMethod.PATCH));
    }

    @Test
    @DisplayName("default policy retries idempotent methods (GET/PUT/DELETE)")
    void defaultPolicy_retriesIdempotentMethods() {
        RetryPolicy policy = RetryPolicy.defaultPolicy();

        assertTrue(policy.isRetryableMethod(HttpMethod.GET));
        assertTrue(policy.isRetryableMethod(HttpMethod.PUT));
        assertTrue(policy.isRetryableMethod(HttpMethod.DELETE));
    }

    @Test
    @DisplayName("retryNonIdempotentMethods opt-in makes POST and PATCH retryable")
    void optIn_makesNonIdempotentMethodsRetryable() {
        RetryPolicy policy = new RetryPolicy(3, 1, 5, Set.of(503), true, true, true);

        assertTrue(policy.isRetryableMethod(HttpMethod.POST));
        assertTrue(policy.isRetryableMethod(HttpMethod.PATCH));
        assertTrue(policy.isRetryableMethod(HttpMethod.GET));
    }

    @Test
    @DisplayName("default policy retries 429 and the transient 5xx statuses only")
    void defaultPolicy_statusGating() {
        RetryPolicy policy = RetryPolicy.defaultPolicy();

        assertTrue(policy.isRetryableStatus(429));
        assertTrue(policy.isRetryableStatus(500));
        assertTrue(policy.isRetryableStatus(502));
        assertTrue(policy.isRetryableStatus(503));
        assertTrue(policy.isRetryableStatus(504));
        assertFalse(policy.isRetryableStatus(404));
        assertFalse(policy.isRetryableStatus(400));
    }

    @Test
    @DisplayName("none() permits no retries")
    void nonePolicy_disablesRetries() {
        RetryPolicy policy = RetryPolicy.none();

        assertEquals(0, policy.getMaxRetries());
        assertFalse(policy.isRetryOnIoException());
    }
}
