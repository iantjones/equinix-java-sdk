/*
 * Copyright 2026 Ian Jones. All Rights Reserved.
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

package com.eqixiac.opencapacity;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Objects;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Checks the {@code OpenCapacity-Signature} header of a webhook delivery. The header value is
 * {@code t=<unix seconds>,v1=<hex>}, where the hex string is the HMAC-SHA256 of
 * {@code <t>.<raw body>} keyed with the signing secret of the subscription.
 */
public final class WebhookSignature {

    /** The name of the header that carries the signature. */
    public static final String HEADER = "OpenCapacity-Signature";

    private WebhookSignature() {
    }

    /**
     * Says whether a delivery is authentic and recent. Pass the body exactly as it arrived, before
     * any JSON parsing.
     *
     * @param signingSecret the signing secret of the subscription
     * @param headerValue the value of the {@link #HEADER} header, or {@code null}
     * @param rawBody the request body as received
     * @param now the current time
     * @param tolerance how old a delivery may be, for example 5 minutes
     * @return {@code true} if the signature matches and the timestamp is within the tolerance
     */
    public static boolean verify(
            String signingSecret, String headerValue, String rawBody, Instant now, Duration tolerance) {
        Objects.requireNonNull(signingSecret, "signingSecret");
        Objects.requireNonNull(rawBody, "rawBody");
        if (headerValue == null) {
            return false;
        }
        Long timestamp = null;
        String signature = null;
        for (String part : headerValue.split(",")) {
            String trimmed = part.trim();
            if (trimmed.startsWith("t=")) {
                try {
                    timestamp = Long.parseLong(trimmed.substring(2));
                } catch (NumberFormatException malformed) {
                    return false;
                }
            } else if (trimmed.startsWith("v1=")) {
                signature = trimmed.substring(3);
            }
        }
        if (timestamp == null || signature == null) {
            return false;
        }
        if (Duration.between(Instant.ofEpochSecond(timestamp), now).abs().compareTo(tolerance) > 0) {
            return false;
        }
        byte[] expected = hmac(signingSecret, timestamp, rawBody).getBytes(StandardCharsets.US_ASCII);
        return MessageDigest.isEqual(expected, signature.getBytes(StandardCharsets.US_ASCII));
    }

    /**
     * Computes the header value for a body. A provider uses this to sign; a customer uses it in
     * tests.
     *
     * @param signingSecret the signing secret of the subscription
     * @param timestamp the time of signing in seconds since the epoch
     * @param rawBody the request body as sent
     * @return the header value, {@code t=<timestamp>,v1=<hex>}
     */
    public static String sign(String signingSecret, long timestamp, String rawBody) {
        return "t=" + timestamp + ",v1=" + hmac(signingSecret, timestamp, rawBody);
    }

    private static String hmac(String secret, long timestamp, String rawBody) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal((timestamp + "." + rawBody).getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException unavailable) {
            throw new IllegalStateException("HmacSHA256 is not available in this Java runtime", unavailable);
        }
    }
}
