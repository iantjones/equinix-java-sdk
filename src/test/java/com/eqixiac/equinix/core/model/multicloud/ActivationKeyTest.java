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

package com.eqixiac.equinix.core.model.multicloud;

import com.eqixiac.equinix.core.internal.Constants;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link ActivationKey}. The Connection Coordinator specification (commit
 * {@code bbfc763}) publishes no example key, so every fixture is a JSON object built from the
 * member list of {@code environment.yaml#/ActivationKeyV1} / {@code #/ActivationKeyV2}, base64
 * encoded with the RFC 4648 section 4 alphabet. The base64 literals were produced outside the SDK
 * ({@code printf '%s' "$json" | base64 -w0}) so the codec is checked against an independent
 * encoder.
 */
@DisplayName("core.model.multicloud.ActivationKey")
class ActivationKeyTest {

    private static final String ENV_URI = "providers/gcp/environments/aws-gcp-us-east";
    private static final String UUID = "3f2b8c1e-6d4a-4e7b-9a55-0c1d2e3f4a5b";
    private static final String ACCOUNT_ID = "example-project-123456";

    /** Canonical member order: envelope first, then the ActivationKeyV1 members in schema order. */
    private static final String V1_JSON = "{\"version\":1,"
            + "\"destinationEnvironmentUri\":\"" + ENV_URI + "\","
            + "\"sharedConnectionUuid\":\"" + UUID + "\","
            + "\"connectionSizeMbps\":1000,"
            + "\"destinationAccountId\":\"" + ACCOUNT_ID + "\"}";

    private static final String V1_BASE64 =
            "eyJ2ZXJzaW9uIjoxLCJkZXN0aW5hdGlvbkVudmlyb25tZW50VXJpIjoicHJvdmlkZXJzL2djcC9lbnZpcm9ubWVudHMv"
            + "YXdzLWdjcC11cy1lYXN0Iiwic2hhcmVkQ29ubmVjdGlvblV1aWQiOiIzZjJiOGMxZS02ZDRhLTRlN2ItOWE1NS0wYzFk"
            + "MmUzZjRhNWIiLCJjb25uZWN0aW9uU2l6ZU1icHMiOjEwMDAsImRlc3RpbmF0aW9uQWNjb3VudElkIjoiZXhhbXBsZS1w"
            + "cm9qZWN0LTEyMzQ1NiJ9";

    private static final String CIPHERTEXT = "Y2lwaGVydGV4dC1ieXRlcy1ub3QtcmVhbA==";

    private static final String V2_JSON = "{\"version\":2,"
            + "\"destinationEnvironmentUri\":\"" + ENV_URI + "\","
            + "\"encryptedContents\":\"" + CIPHERTEXT + "\"}";

    private static final String V2_BASE64 =
            "eyJ2ZXJzaW9uIjoyLCJkZXN0aW5hdGlvbkVudmlyb25tZW50VXJpIjoicHJvdmlkZXJzL2djcC9lbnZpcm9ubWVudHMv"
            + "YXdzLWdjcC11cy1lYXN0IiwiZW5jcnlwdGVkQ29udGVudHMiOiJZMmx3YUdWeWRHVjRkQzFpZVhSbGN5MXViM1F0Y21W"
            + "aGJBPT0ifQ==";

    private static String b64(String json) {
        return Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }

    private static ActivationKey.V1 builtV1() {
        return ActivationKey.V1.builder()
                .destinationEnvironmentUri(ENV_URI)
                .sharedConnectionUuid(UUID)
                .connectionSizeMbps(1000)
                .destinationAccountId(ACCOUNT_ID)
                .build();
    }

    @Test
    @DisplayName("fixtures: the independent base64 literals are the encoding of the JSON literals")
    void fixturesAreConsistent() {
        assertEquals(V1_BASE64, b64(V1_JSON));
        assertEquals(V2_BASE64, b64(V2_JSON));
    }

    // ══════════════════════════════════════════════
    //  Version 1
    // ══════════════════════════════════════════════

    @Nested
    @DisplayName("version 1")
    class Version1 {

        @Test
        @DisplayName("decode exposes the five ActivationKeyV1 members")
        void decodeExposesMembers() {
            ActivationKey.V1 v1 = assertInstanceOf(ActivationKey.V1.class, ActivationKey.decode(V1_BASE64));
            assertEquals(1, v1.version());
            assertEquals(ENV_URI, v1.destinationEnvironmentUri());
            assertEquals(UUID, v1.sharedConnectionUuid());
            assertEquals(1000, v1.connectionSizeMbps());
            assertEquals(ACCOUNT_ID, v1.destinationAccountId());
            assertEquals(OptionalInt.of(1), v1.envelopeVersion());
            assertEquals(Optional.of(ENV_URI), v1.envelopeDestinationEnvironmentUri());
            assertEquals(Optional.of("aws-gcp-us-east"), v1.destinationEnvironmentId());
        }

        @Test
        @DisplayName("builder encodes canonically: exact bytes, schema member order, padded standard alphabet")
        void builderEncodesCanonically() {
            String encoded = builtV1().encode();
            assertEquals(V1_BASE64, encoded);
            assertEquals(V1_JSON, new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8));
        }

        @Test
        @DisplayName("doc contract: 'Decoding a canonical string and encoding it again returns the identical string'")
        void canonicalRoundTripIsByteStable() {
            String once = builtV1().encode();
            ActivationKey decoded = ActivationKey.decode(once);
            assertEquals(once, decoded.encode());
            assertEquals(once, ActivationKey.decode(decoded.encode()).encode());
            assertEquals(builtV1(), decoded);
            assertEquals(builtV1().hashCode(), decoded.hashCode());
        }

        @Test
        @DisplayName("doc contract: a decoded key is 'not re-serialized' - member order, whitespace and unmodelled members survive")
        void decodedKeyIsReturnedAsIssued() {
            String nonCanonicalJson = "{ \"destinationAccountId\": \"" + ACCOUNT_ID + "\",\n"
                    + "  \"connectionSizeMbps\": 1000,\n"
                    + "  \"issuerHint\": \"a member this SDK does not model\",\n"
                    + "  \"sharedConnectionUuid\": \"" + UUID + "\",\n"
                    + "  \"destinationEnvironmentUri\": \"" + ENV_URI + "\",\n"
                    + "  \"version\": 1 }";
            String issued = b64(nonCanonicalJson);

            ActivationKey.V1 v1 = assertInstanceOf(ActivationKey.V1.class, ActivationKey.decode(issued));
            assertEquals(issued, v1.encode(), "the issuing provider must see the bytes it issued");
            assertNotEquals(builtV1().encode(), v1.encode());
            // Equality is on the five members, not on the encoded form.
            assertEquals(builtV1(), v1);
        }

        @Test
        @DisplayName("a key wrapped across lines decodes; encode() returns it without the whitespace")
        void whitespaceIsIgnored() {
            String wrapped = "  " + V1_BASE64.replaceAll("(.{20})", "$1\r\n") + " \t\n";
            ActivationKey key = ActivationKey.decode(wrapped);
            assertInstanceOf(ActivationKey.V1.class, key);
            assertEquals(V1_BASE64, key.encode());
        }

        @Test
        @DisplayName("URL-safe alphabet without padding decodes; encode() returns the issued form")
        void urlSafeAlphabetIsAccepted() {
            String json = "{\"version\":1,\"destinationEnvironmentUri\":\"providers/gcp/environments/e?>\","
                    + "\"sharedConnectionUuid\":\"u>?>?\",\"connectionSizeMbps\":50,"
                    + "\"destinationAccountId\":\"a?>?>~~\"}";
            String standard = b64(json);
            assertTrue(standard.contains("+") || standard.contains("/"),
                    "fixture must exercise characters that differ between the two alphabets");
            String urlSafe = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(json.getBytes(StandardCharsets.UTF_8));
            assertNotEquals(standard, urlSafe);

            ActivationKey.V1 v1 = assertInstanceOf(ActivationKey.V1.class, ActivationKey.decode(urlSafe));
            assertEquals(50, v1.connectionSizeMbps());
            assertEquals("a?>?>~~", v1.destinationAccountId());
            assertEquals(urlSafe, v1.encode());
        }

        @Test
        @DisplayName("builder rejects missing, blank and non-positive members")
        void builderValidates() {
            assertThrows(NullPointerException.class, () -> ActivationKey.V1.builder()
                    .sharedConnectionUuid(UUID).connectionSizeMbps(1000).destinationAccountId(ACCOUNT_ID).build());
            assertThrows(NullPointerException.class, () -> ActivationKey.V1.builder()
                    .destinationEnvironmentUri(ENV_URI).connectionSizeMbps(1000).destinationAccountId(ACCOUNT_ID).build());
            assertThrows(NullPointerException.class, () -> ActivationKey.V1.builder()
                    .destinationEnvironmentUri(ENV_URI).sharedConnectionUuid(UUID).connectionSizeMbps(1000).build());
            assertThrows(IllegalArgumentException.class, () -> ActivationKey.V1.builder()
                    .destinationEnvironmentUri(" ").sharedConnectionUuid(UUID).connectionSizeMbps(1000)
                    .destinationAccountId(ACCOUNT_ID).build());
            assertThrows(IllegalArgumentException.class, () -> ActivationKey.V1.builder()
                    .destinationEnvironmentUri(ENV_URI).sharedConnectionUuid(UUID)
                    .destinationAccountId(ACCOUNT_ID).build(), "connectionSizeMbps defaults to 0");
            assertThrows(IllegalArgumentException.class, () -> ActivationKey.V1.builder()
                    .destinationEnvironmentUri(ENV_URI).sharedConnectionUuid(UUID).connectionSizeMbps(-1)
                    .destinationAccountId(ACCOUNT_ID).build());
        }

        @Test
        @DisplayName("non-ASCII member values survive a canonical round trip")
        void nonAsciiRoundTrip() {
            ActivationKey.V1 built = ActivationKey.V1.builder()
                    .destinationEnvironmentUri(ENV_URI)
                    .sharedConnectionUuid(UUID)
                    .connectionSizeMbps(10000)
                    .destinationAccountId("konto-åäö-\"quoted\"\\")
                    .build();
            ActivationKey.V1 decoded = assertInstanceOf(ActivationKey.V1.class, ActivationKey.decode(built.encode()));
            assertEquals("konto-åäö-\"quoted\"\\", decoded.destinationAccountId());
            assertEquals(built.encode(), decoded.encode());
        }
    }

    // ══════════════════════════════════════════════
    //  Version 2
    // ══════════════════════════════════════════════

    @Nested
    @DisplayName("version 2 (encrypted)")
    class Version2 {

        @Test
        @DisplayName("decode exposes the cleartext envelope and returns the issued string")
        void envelopeOnly() {
            ActivationKey.V2Encrypted v2 =
                    assertInstanceOf(ActivationKey.V2Encrypted.class, ActivationKey.decode(V2_BASE64));
            assertEquals(2, v2.version());
            assertEquals(ENV_URI, v2.destinationEnvironmentUri());
            assertEquals(OptionalInt.of(2), v2.envelopeVersion());
            assertEquals(Optional.of(ENV_URI), v2.envelopeDestinationEnvironmentUri());
            assertEquals(Optional.of("aws-gcp-us-east"), v2.destinationEnvironmentId());
            assertEquals(V2_BASE64, v2.encode());
        }

        @Test
        @DisplayName("the public surface has no accessor for encryptedContents or any decrypted member")
        void noCiphertextAccessor() {
            Set<String> publicMethods = Arrays.stream(ActivationKey.V2Encrypted.class.getDeclaredMethods())
                    .filter(m -> Modifier.isPublic(m.getModifiers()) && !m.isSynthetic())
                    .map(Method::getName)
                    .collect(Collectors.toCollection(TreeSet::new));
            assertEquals(new TreeSet<>(Set.of("version", "destinationEnvironmentUri", "encode",
                    "envelopeVersion", "envelopeDestinationEnvironmentUri", "equals", "hashCode", "toString")),
                    publicMethods);
        }

        @Test
        @DisplayName("members inside the ciphertext are not read even when a cleartext look-alike is present")
        void cleartextLookAlikesAreNotExposed() {
            String json = "{\"version\":2,\"destinationEnvironmentUri\":\"" + ENV_URI + "\","
                    + "\"encryptedContents\":\"" + CIPHERTEXT + "\","
                    + "\"destinationAccountId\":\"" + ACCOUNT_ID + "\"}";
            ActivationKey key = ActivationKey.decode(b64(json));
            assertInstanceOf(ActivationKey.V2Encrypted.class, key);
            assertFalse(key.toString().contains(ACCOUNT_ID));
        }

        @Test
        @DisplayName("version 2 without encryptedContents is Opaque with a readable envelope")
        void missingCiphertextIsOpaque() {
            String json = "{\"version\":2,\"destinationEnvironmentUri\":\"" + ENV_URI + "\"}";
            ActivationKey.Opaque opaque = assertInstanceOf(ActivationKey.Opaque.class, ActivationKey.decode(b64(json)));
            assertEquals(OptionalInt.of(2), opaque.envelopeVersion());
            assertEquals(Optional.of(ENV_URI), opaque.envelopeDestinationEnvironmentUri());
        }
    }

    // ══════════════════════════════════════════════
    //  Opaque
    // ══════════════════════════════════════════════

    static Stream<String> uninterpretableInputs() {
        String v1MissingAccount = "{\"version\":1,\"destinationEnvironmentUri\":\"" + ENV_URI + "\","
                + "\"sharedConnectionUuid\":\"" + UUID + "\",\"connectionSizeMbps\":1000}";
        String v1SizeAsString = V1_JSON.replace(":1000,", ":\"1000\",");
        String v1SizeZero = V1_JSON.replace(":1000,", ":0,");
        String v1SizeFractional = V1_JSON.replace(":1000,", ":1000.5,");
        String v1BlankUuid = V1_JSON.replace(UUID, " ");
        String versionAsString = V1_JSON.replace("\"version\":1", "\"version\":\"1\"");
        String duplicateMember = V1_JSON.replace("}", ",\"destinationAccountId\":\"another-account\"}");
        return Stream.of(
                "not base64 ### %%%",
                "garbage",
                b64("not json"),
                b64("[1,2,3]"),
                b64("\"a json string\""),
                b64("42"),
                b64("null"),
                b64("{}"),
                b64("{\"version\":3,\"destinationEnvironmentUri\":\"" + ENV_URI + "\"}"),
                b64("{\"version\":1}"),
                b64(v1MissingAccount),
                b64(v1SizeAsString),
                b64(v1SizeZero),
                b64(v1SizeFractional),
                b64(v1BlankUuid),
                b64(versionAsString),
                b64(duplicateMember),
                b64(V1_JSON + " {\"trailing\":true}"),
                b64(V1_JSON).substring(0, 40),
                "äöü-provider-specific-key",
                "AQIDBAUGBwgJCgsMDQ4P");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("uninterpretableInputs")
    @DisplayName("doc contract: 'an input this SDK release cannot interpret yields Opaque' and is carried unchanged")
    void uninterpretableInputsBecomeOpaque(String input) {
        ActivationKey key = ActivationKey.decode(input);
        ActivationKey.Opaque opaque = assertInstanceOf(ActivationKey.Opaque.class, key);
        assertEquals(input, opaque.raw());
        assertEquals(input, opaque.encode());
        assertEquals(key, ActivationKey.decode(input));
    }

    @Test
    @DisplayName("Opaque exposes the envelope of an unlisted version and nothing of an unreadable payload")
    void opaqueEnvelope() {
        ActivationKey future = ActivationKey.decode(
                b64("{\"version\":3,\"destinationEnvironmentUri\":\"" + ENV_URI + "\",\"somethingNew\":true}"));
        assertInstanceOf(ActivationKey.Opaque.class, future);
        assertEquals(OptionalInt.of(3), future.envelopeVersion());
        assertEquals(Optional.of(ENV_URI), future.envelopeDestinationEnvironmentUri());
        assertEquals(Optional.of("aws-gcp-us-east"), future.destinationEnvironmentId());

        ActivationKey unreadable = ActivationKey.decode("not base64 ### %%%");
        assertEquals(OptionalInt.empty(), unreadable.envelopeVersion());
        assertEquals(Optional.empty(), unreadable.envelopeDestinationEnvironmentUri());
        assertEquals(Optional.empty(), unreadable.destinationEnvironmentId());
    }

    @Test
    @DisplayName("Opaque.raw() trims the ends and preserves interior characters")
    void opaqueRawTrimsEndsOnly() {
        ActivationKey.Opaque opaque =
                assertInstanceOf(ActivationKey.Opaque.class, ActivationKey.decode("  key with  interior spaces %\n"));
        assertEquals("key with  interior spaces %", opaque.raw());
    }

    @Test
    @DisplayName("doc contract: decode 'throws only for a null or blank argument'")
    void decodeRejectsNullAndBlankOnly() {
        assertThrows(NullPointerException.class, () -> ActivationKey.decode(null));
        assertThrows(IllegalArgumentException.class, () -> ActivationKey.decode(""));
        assertThrows(IllegalArgumentException.class, () -> ActivationKey.decode(" \t\r\n"));
    }

    // ══════════════════════════════════════════════
    //  Keys are credentials
    // ══════════════════════════════════════════════

    @Nested
    @DisplayName("redaction")
    class Redaction {

        private void assertLeaksNothing(String rendered, String encodedForm) {
            assertFalse(rendered.contains(ACCOUNT_ID), "destinationAccountId leaked: " + rendered);
            assertFalse(rendered.contains(UUID), "sharedConnectionUuid leaked: " + rendered);
            assertFalse(rendered.contains(CIPHERTEXT), "ciphertext leaked: " + rendered);
            assertFalse(rendered.contains(encodedForm), "encoded form leaked: " + rendered);
            // No 24-character window of the encoded form may appear either (partial leak).
            for (int i = 0; i + 24 <= encodedForm.length(); i += 8) {
                assertFalse(rendered.contains(encodedForm.substring(i, i + 24)),
                        "fragment of the encoded form leaked: " + rendered);
            }
        }

        @Test
        @DisplayName("V1.toString() never contains destinationAccountId, sharedConnectionUuid or the encoded form")
        void v1ToString() {
            for (ActivationKey key : new ActivationKey[] {builtV1(), ActivationKey.decode(V1_BASE64)}) {
                String rendered = key.toString();
                assertLeaksNothing(rendered, V1_BASE64);
                assertTrue(rendered.contains("destinationAccountId=<redacted>"), rendered);
                assertTrue(rendered.contains("sharedConnectionUuid=<redacted>"), rendered);
                // The non-sensitive members stay readable for diagnostics.
                assertTrue(rendered.contains(ENV_URI), rendered);
                assertTrue(rendered.contains("connectionSizeMbps=1000"), rendered);
                // String concatenation and String.valueOf go through toString as well.
                assertLeaksNothing("key=" + key, V1_BASE64);
                assertLeaksNothing(String.valueOf(key), V1_BASE64);
            }
        }

        @Test
        @DisplayName("V1.Builder.toString() contains no member value")
        void v1BuilderToString() {
            String rendered = ActivationKey.V1.builder()
                    .destinationEnvironmentUri(ENV_URI)
                    .sharedConnectionUuid(UUID)
                    .connectionSizeMbps(1000)
                    .destinationAccountId(ACCOUNT_ID)
                    .toString();
            assertLeaksNothing(rendered, V1_BASE64);
        }

        @Test
        @DisplayName("V2Encrypted.toString() never contains the ciphertext or the encoded form")
        void v2ToString() {
            String rendered = ActivationKey.decode(V2_BASE64).toString();
            assertLeaksNothing(rendered, V2_BASE64);
            assertTrue(rendered.contains("encryptedContents=<redacted>"), rendered);
            assertTrue(rendered.contains(ENV_URI), rendered);
        }

        @Test
        @DisplayName("Opaque.toString() reports the length of the raw string, never its content")
        void opaqueToString() {
            String secret = "provider-specific-SECRET-key-material-0123456789";
            String rendered = ActivationKey.decode(secret).toString();
            assertFalse(rendered.contains("SECRET"), rendered);
            assertTrue(rendered.contains(secret.length() + " chars"), rendered);

            // A version 1 payload that failed validation is Opaque; its members must not leak either.
            String v1MissingSize = "{\"version\":1,\"destinationEnvironmentUri\":\"" + ENV_URI + "\","
                    + "\"sharedConnectionUuid\":\"" + UUID + "\",\"destinationAccountId\":\"" + ACCOUNT_ID + "\"}";
            String encoded = b64(v1MissingSize);
            assertLeaksNothing(ActivationKey.decode(encoded).toString(), encoded);
        }

        @Test
        @DisplayName("doc contract: 'Jackson serializes them as an empty object' - no key material in JSON")
        void jacksonDoesNotSerializeKeyMaterial() throws Exception {
            for (ActivationKey key : new ActivationKey[] {
                    builtV1(), ActivationKey.decode(V2_BASE64), ActivationKey.decode("opaque-SECRET-key")}) {
                String json = Constants.mapper().writeValueAsString(key);
                assertEquals("{}", json);
            }
        }

        /** A consumer DTO serialized by the consumer's own mapper (Spring MVC, a logging appender). */
        record KeyEnvelope(String label, ActivationKey key) {
        }

        @Test
        @DisplayName("doc contract holds under a default ObjectMapper (FAIL_ON_EMPTY_BEANS enabled), "
                + "at the root and nested in a DTO")
        void jacksonDefaultMapperSerializesEmptyObject() throws Exception {
            ObjectMapper defaultMapper = new ObjectMapper();
            assertTrue(defaultMapper.isEnabled(SerializationFeature.FAIL_ON_EMPTY_BEANS));
            for (ActivationKey key : new ActivationKey[] {
                    builtV1(), ActivationKey.decode(V2_BASE64), ActivationKey.decode("opaque-SECRET-key")}) {
                assertEquals("{}", defaultMapper.writeValueAsString(key));
                String nested = defaultMapper.writeValueAsString(new KeyEnvelope("k", key));
                assertEquals("{\"label\":\"k\",\"key\":{}}", nested);
                assertLeaksNothing(nested, key.encode());
            }
        }
    }

    @Test
    @DisplayName("the sealed hierarchy permits exactly V1, V2Encrypted and Opaque")
    void sealedHierarchy() {
        assertTrue(ActivationKey.class.isSealed());
        Set<Class<?>> permitted = Set.of(ActivationKey.class.getPermittedSubclasses());
        assertEquals(Set.of(ActivationKey.V1.class, ActivationKey.V2Encrypted.class, ActivationKey.Opaque.class),
                permitted);
    }

    @Test
    @DisplayName("an exhaustive switch over the variants compiles without a default branch")
    void exhaustiveSwitch() {
        ActivationKey key = ActivationKey.decode(V1_BASE64);
        String summary = switch (key) {
            case ActivationKey.V1 v1 -> v1.connectionSizeMbps() + " Mbps to " + v1.destinationEnvironmentUri();
            case ActivationKey.V2Encrypted v2 -> "encrypted, for " + v2.destinationEnvironmentUri();
            case ActivationKey.Opaque opaque -> "format not recognized";
        };
        assertEquals("1000 Mbps to " + ENV_URI, summary);
    }
}
