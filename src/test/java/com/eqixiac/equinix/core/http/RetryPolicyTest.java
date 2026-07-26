package com.eqixiac.equinix.core.http;

import com.eqixiac.equinix.core.enums.HttpMethod;
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
    @DisplayName("429 is retryable for every method — the server did not process the request")
    void status429_isRetryableRegardlessOfMethod() {
        RetryPolicy policy = RetryPolicy.defaultPolicy();

        assertTrue(policy.isRetryable(429, HttpMethod.POST));
        assertTrue(policy.isRetryable(429, HttpMethod.PATCH));
        assertTrue(policy.isRetryable(429, HttpMethod.GET));
        assertTrue(policy.isRetryable(429, null));
    }

    @Test
    @DisplayName("5xx retries keep the idempotency gate: POST/PATCH blocked, GET/PUT/DELETE allowed")
    void status5xx_keepsIdempotencyGate() {
        RetryPolicy policy = RetryPolicy.defaultPolicy();

        assertFalse(policy.isRetryable(503, HttpMethod.POST));
        assertFalse(policy.isRetryable(500, HttpMethod.PATCH));
        assertTrue(policy.isRetryable(503, HttpMethod.GET));
        assertTrue(policy.isRetryable(502, HttpMethod.PUT));
        assertTrue(policy.isRetryable(504, HttpMethod.DELETE));
    }

    @Test
    @DisplayName("isRetryable still requires the status to be in the retryable set (even 429)")
    void isRetryable_requiresRetryableStatus() {
        RetryPolicy no429 = new RetryPolicy(3, 1, 5, Set.of(503), true, true);

        assertFalse(no429.isRetryable(429, HttpMethod.GET));
        assertFalse(RetryPolicy.defaultPolicy().isRetryable(404, HttpMethod.GET));
        assertFalse(RetryPolicy.none().isRetryable(429, HttpMethod.GET));
    }

    @Test
    @DisplayName("none() permits no retries")
    void nonePolicy_disablesRetries() {
        RetryPolicy policy = RetryPolicy.none();

        assertEquals(0, policy.getMaxRetries());
        assertFalse(policy.isRetryOnIoException());
    }
}
