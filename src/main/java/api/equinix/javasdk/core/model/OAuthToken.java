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

package api.equinix.javasdk.core.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.time.Instant;

/**
 * An OAuth2 access token as returned by the Equinix token endpoint, plus the local
 * session-start instant used to judge expiry.
 *
 * <h3>Concurrency contract</h3>
 * <p>An {@code OAuthToken} instance is immutable once populated by deserialization —
 * there are no setters, so the documented contract is enforced rather than merely
 * promised. The SDK publishes a freshly authenticated instance to multiple threads
 * (see {@code EquinixClient#getOAuthToken()} / {@code setOAuthToken}); the fields that
 * participate in the validity check ({@link #sessionStart}, {@link #sessionToken} and
 * {@link #tokenTimeout}) are declared {@code volatile} so a thread reading the token
 * always observes the fully-written values even when the instance is handed over
 * without further synchronization.</p>
 *
 * <h3>Expiry clock</h3>
 * <p>Validity is measured on the machine-independent {@link Instant} timeline, not local
 * wall-clock time, so DST transitions and system-clock adjustments cannot make an expired
 * token appear valid. A small safety margin (30s) treats the token as expired slightly
 * early so in-flight requests do not race the server-side expiry.</p>
 *
 * @author ianjones
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class OAuthToken {

    /** Seconds before the nominal expiry at which the token is already treated as expired. */
    static final long EXPIRY_SAFETY_MARGIN_SECONDS = 30;

    @JsonProperty("access_token")
    private volatile String sessionToken;

    @JsonProperty("token_timeout")
    private volatile String tokenTimeout;

    @JsonProperty("user_name")
    private String userName;

    @JsonProperty("token_type")
    private String tokenType;

    @JsonIgnore
    private volatile Instant sessionStart = Instant.now();

    /**
     * For Jackson: field-bound deserialization of the token endpoint response.
     */
    public OAuthToken() {
    }

    /**
     * Constructs a fully populated token — intended for tests and for advanced callers that
     * inject a token obtained out-of-band via {@code EquinixClient#setOAuthToken}.
     *
     * @param sessionToken the bearer access token
     * @param tokenType the token type (e.g. {@code "bearer"})
     * @param tokenTimeout the validity window in seconds, as returned by the token endpoint
     * @param sessionStart the instant the validity window started
     */
    public OAuthToken(String sessionToken, String tokenType, String tokenTimeout, Instant sessionStart) {
        this.sessionToken = sessionToken;
        this.tokenType = tokenType;
        this.tokenTimeout = tokenTimeout;
        this.sessionStart = sessionStart;
    }

    /**
     * Whether this token is still usable: it has a bearer value and its validity window
     * (less the safety margin) has not elapsed on the {@link Instant} timeline.
     *
     * @return {@code true} if the token can still sign requests
     */
    public boolean validSession() {
        Long timeoutSeconds = parseTimeoutSeconds(getTokenTimeout());
        return getSessionToken() != null
                && getSessionStart() != null
                && timeoutSeconds != null
                && Instant.now().isBefore(
                        getSessionStart().plusSeconds(timeoutSeconds - EXPIRY_SAFETY_MARGIN_SECONDS));
    }

    /**
     * Parses the numeric seconds out of a timeout value. The spec ({@code Oauth2TokenResponse})
     * declares {@code token_timeout} as a string whose documented default is annotated prose
     * ("3599 (60 minutes)"), so only the leading digits are read; a value with no leading digits
     * (or a digit run that does not fit in a {@code long}) yields {@code null}.
     */
    private static Long parseTimeoutSeconds(String timeout) {
        if (timeout == null) {
            return null;
        }
        String trimmed = timeout.trim();
        int digits = 0;
        while (digits < trimmed.length() && Character.isDigit(trimmed.charAt(digits))) {
            digits++;
        }
        if (digits == 0) {
            return null;
        }
        try {
            return Long.parseLong(trimmed.substring(0, digits));
        } catch (NumberFormatException overflow) {
            return null;
        }
    }
}
