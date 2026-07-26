package com.eqixiac.equinix.core.http;

import com.eqixiac.equinix.core.exception.CircuitOpenException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the {@link CircuitBreaker} state machine: consecutive-failure counting,
 * open-state rejection, cooldown-gated half-open probing, and probe outcomes.
 */
class CircuitBreakerTest {

    private static final long LONG_COOLDOWN_MS = 60_000;
    private static final long SHORT_COOLDOWN_MS = 50;

    @Test
    @DisplayName("stays closed below the failure threshold")
    void staysClosedBelowThreshold() {
        CircuitBreaker breaker = new CircuitBreaker(3, LONG_COOLDOWN_MS);

        breaker.acquire();
        breaker.recordFailure();
        breaker.acquire();
        breaker.recordFailure();

        assertEquals(CircuitBreaker.State.CLOSED, breaker.getState());
        assertDoesNotThrow(breaker::acquire);
    }

    @Test
    @DisplayName("opens after N consecutive failures and rejects with CircuitOpenException")
    void opensAfterConsecutiveFailures() {
        CircuitBreaker breaker = new CircuitBreaker(2, LONG_COOLDOWN_MS);

        breaker.acquire();
        breaker.recordFailure();
        breaker.acquire();
        breaker.recordFailure();

        assertEquals(CircuitBreaker.State.OPEN, breaker.getState());
        CircuitOpenException rejection = assertThrows(CircuitOpenException.class, breaker::acquire);
        assertTrue(rejection.getRemainingCooldownMillis() > 0);
        assertTrue(rejection.getMessage().contains("open"));
    }

    @Test
    @DisplayName("a success resets the consecutive-failure count")
    void successResetsFailureCount() {
        CircuitBreaker breaker = new CircuitBreaker(2, LONG_COOLDOWN_MS);

        breaker.acquire();
        breaker.recordFailure();
        breaker.acquire();
        breaker.recordSuccess();
        breaker.acquire();
        breaker.recordFailure();

        // 1 failure, success, 1 failure — never 2 consecutive, so still closed.
        assertEquals(CircuitBreaker.State.CLOSED, breaker.getState());
    }

    @Test
    @DisplayName("admits a half-open probe after the cooldown; a successful probe closes the circuit")
    void halfOpenProbeSuccessCloses() throws InterruptedException {
        CircuitBreaker breaker = new CircuitBreaker(1, SHORT_COOLDOWN_MS);

        breaker.acquire();
        breaker.recordFailure();
        assertEquals(CircuitBreaker.State.OPEN, breaker.getState());
        assertThrows(CircuitOpenException.class, breaker::acquire);

        Thread.sleep(SHORT_COOLDOWN_MS + 30);

        // Cooldown elapsed: the next acquire is the probe.
        assertDoesNotThrow(breaker::acquire);
        assertEquals(CircuitBreaker.State.HALF_OPEN, breaker.getState());
        breaker.recordSuccess();
        assertEquals(CircuitBreaker.State.CLOSED, breaker.getState());
        assertDoesNotThrow(breaker::acquire);
    }

    @Test
    @DisplayName("a failed half-open probe re-opens the circuit for another cooldown")
    void halfOpenProbeFailureReopens() throws InterruptedException {
        CircuitBreaker breaker = new CircuitBreaker(1, SHORT_COOLDOWN_MS);

        breaker.acquire();
        breaker.recordFailure();
        Thread.sleep(SHORT_COOLDOWN_MS + 30);

        breaker.acquire(); // the probe
        breaker.recordFailure();

        assertEquals(CircuitBreaker.State.OPEN, breaker.getState());
        assertThrows(CircuitOpenException.class, breaker::acquire);
    }

    @Test
    @DisplayName("while a probe is pending, other requests are rejected")
    void pendingProbeRejectsOtherRequests() throws InterruptedException {
        CircuitBreaker breaker = new CircuitBreaker(1, SHORT_COOLDOWN_MS);

        breaker.acquire();
        breaker.recordFailure();
        Thread.sleep(SHORT_COOLDOWN_MS + 30);

        breaker.acquire(); // probe slot taken, no outcome reported yet
        assertThrows(CircuitOpenException.class, breaker::acquire);
    }

    @Test
    @DisplayName("a probe that never reports back does not wedge the breaker: its slot expires after a cooldown")
    void abandonedProbeSlotExpires() throws InterruptedException {
        CircuitBreaker breaker = new CircuitBreaker(1, SHORT_COOLDOWN_MS);

        breaker.acquire();
        breaker.recordFailure();
        Thread.sleep(SHORT_COOLDOWN_MS + 30);
        breaker.acquire(); // probe acquired, then "dies" without recording an outcome

        Thread.sleep(SHORT_COOLDOWN_MS + 30);
        assertDoesNotThrow(breaker::acquire); // a new probe is admitted
    }

    @Test
    @DisplayName("a straggler success completing while OPEN is ignored (no cooldown/probe bypass)")
    void stragglerSuccessWhileOpenIsIgnored() {
        CircuitBreaker breaker = new CircuitBreaker(2, LONG_COOLDOWN_MS);

        breaker.acquire();
        breaker.acquire(); // straggler admitted while still closed
        breaker.recordFailure();
        breaker.recordFailure();
        assertEquals(CircuitBreaker.State.OPEN, breaker.getState());

        // The straggler's late 200/429 must not snap the circuit shut.
        breaker.recordSuccess();

        assertEquals(CircuitBreaker.State.OPEN, breaker.getState());
        assertThrows(CircuitOpenException.class, breaker::acquire);
    }

    @Test
    @DisplayName("a straggler failure completing while OPEN does not extend the cooldown")
    void stragglerFailureWhileOpenDoesNotExtendCooldown() throws InterruptedException {
        long cooldown = 200;
        CircuitBreaker breaker = new CircuitBreaker(1, cooldown);

        breaker.acquire();
        breaker.acquire(); // straggler admitted while still closed
        breaker.recordFailure();
        assertEquals(CircuitBreaker.State.OPEN, breaker.getState());

        Thread.sleep(150);
        breaker.recordFailure(); // straggler reports late, mid-cooldown
        Thread.sleep(100);

        // 250ms since opening: the original cooldown elapsed, so a probe is admitted —
        // the straggler failure must not have restarted the clock.
        assertDoesNotThrow(breaker::acquire);
        assertEquals(CircuitBreaker.State.HALF_OPEN, breaker.getState());
    }

    @Test
    @DisplayName("after a straggler success is ignored, recovery still goes through the half-open probe")
    void recoveryAfterIgnoredStragglerGoesThroughProbe() throws InterruptedException {
        CircuitBreaker breaker = new CircuitBreaker(1, SHORT_COOLDOWN_MS);

        breaker.acquire();
        breaker.acquire(); // straggler
        breaker.recordFailure();
        breaker.recordSuccess(); // straggler outcome, ignored
        assertEquals(CircuitBreaker.State.OPEN, breaker.getState());

        Thread.sleep(SHORT_COOLDOWN_MS + 30);

        assertDoesNotThrow(breaker::acquire); // the probe
        assertEquals(CircuitBreaker.State.HALF_OPEN, breaker.getState());
        breaker.recordSuccess(); // the probe's success closes the circuit
        assertEquals(CircuitBreaker.State.CLOSED, breaker.getState());
    }

    @Test
    @DisplayName("constructor validates its arguments")
    void constructorValidation() {
        assertThrows(IllegalArgumentException.class, () -> new CircuitBreaker(0, 1000));
        assertThrows(IllegalArgumentException.class, () -> new CircuitBreaker(3, 0));
    }
}
