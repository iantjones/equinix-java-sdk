package com.eqixiac.equinix.core;

import com.eqixiac.equinix.core.internal.Constants;
import com.eqixiac.equinix.core.model.OAuthToken;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link OAuthToken}: validity is judged on the machine-independent
 * {@link Instant} timeline (not zone-local wall-clock time), the type is immutable
 * (no setters), and timeout parsing tolerates the spec's annotated-prose values.
 */
class OAuthTokenTest {

    @Test
    void freshTokenIsValid() {
        OAuthToken token = new OAuthToken("tok", "bearer", "3600", Instant.now());
        assertTrue(token.validSession());
    }

    @Test
    void elapsedTokenIsInvalid() {
        OAuthToken token = new OAuthToken("tok", "bearer", "3600", Instant.now().minusSeconds(4000));
        assertFalse(token.validSession());
    }

    @Test
    void tokenInsideSafetyMarginIsAlreadyInvalid() {
        // 3600s window, 3590s elapsed: nominally 10s left, but the 30s safety margin
        // treats it as expired so in-flight requests don't race the server-side expiry.
        OAuthToken token = new OAuthToken("tok", "bearer", "3600", Instant.now().minusSeconds(3590));
        assertFalse(token.validSession());
    }

    @Test
    void missingPiecesMakeTheSessionInvalidRatherThanThrowing() {
        assertFalse(new OAuthToken(null, "bearer", "3600", Instant.now()).validSession());
        assertFalse(new OAuthToken("tok", "bearer", null, Instant.now()).validSession());
        assertFalse(new OAuthToken("tok", "bearer", "3600", null).validSession());
        assertFalse(new OAuthToken().validSession());
    }

    @Test
    void timeoutProseAndGarbageAreTolerated() {
        // The spec documents token_timeout as a string like "3599 (60 minutes)" — leading digits win.
        assertTrue(new OAuthToken("tok", "bearer", "3599 (60 minutes)", Instant.now()).validSession());
        // No leading digits: invalid session, no exception.
        assertFalse(new OAuthToken("tok", "bearer", "soon", Instant.now()).validSession());
        // A digit run exceeding Long range must not throw NumberFormatException.
        assertDoesNotThrow(() ->
                new OAuthToken("tok", "bearer", "99999999999999999999999999", Instant.now()).validSession());
        assertFalse(new OAuthToken("tok", "bearer", "99999999999999999999999999", Instant.now()).validSession());
    }

    @Test
    void deserializesFromTokenEndpointShapeIgnoringUnknownProperties() throws Exception {
        // refresh_token is not modeled (the client_credentials grant never issues one) and any
        // future additions to the token response must not break deserialization.
        String body = "{\"access_token\":\"abc\",\"token_timeout\":\"3600\",\"token_type\":\"bearer\","
                + "\"user_name\":\"user\",\"refresh_token\":\"r\",\"refresh_token_timeout\":\"86400\","
                + "\"some_future_field\":true}";
        OAuthToken token = Constants.mapper().readValue(body, OAuthToken.class);
        assertEquals("abc", token.getSessionToken());
        assertEquals("bearer", token.getTokenType());
        assertEquals("3600", token.getTokenTimeout());
        assertNotNull(token.getSessionStart());
        assertTrue(token.validSession());
    }
}
