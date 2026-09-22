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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * The activation key of the Connection Coordinator specification
 * ({@code environment.yaml#/ActivationKey}): the token a customer obtains from one provider and
 * enters at the other provider to accept a connection.
 *
 * <p><b>Beta</b>: derived from the open specification at commit {@code bbfc763} (see the package
 * documentation). The versioned key format ({@code ActivationKeyV1} / {@code ActivationKeyV2})
 * was introduced between commits {@code 8173027} and {@code bbfc763}; the specification publishes
 * no example key, so the test fixtures are built from the schema's field list.</p>
 *
 * <h2>Wire format</h2>
 * <p>The specification states the key "is always transported as a base64-encoded JSON string".
 * The JSON object is discriminated on the integer {@code version}. Every version carries the
 * cleartext envelope {@code version} + {@code destinationEnvironmentUri}.</p>
 *
 * <table>
 *   <caption>Variants</caption>
 *   <tr><th>Type</th><th>Condition</th><th>Readable members</th></tr>
 *   <tr><td>{@link V1}</td>
 *       <td>{@code version == 1} and all five required fields present with the schema's types</td>
 *       <td>{@code version}, {@code destinationEnvironmentUri}, {@code sharedConnectionUuid},
 *       {@code connectionSizeMbps} (Mbps), {@code destinationAccountId}</td></tr>
 *   <tr><td>{@link V2Encrypted}</td>
 *       <td>{@code version == 2}, {@code destinationEnvironmentUri} and {@code encryptedContents}
 *       present</td>
 *       <td>{@code version}, {@code destinationEnvironmentUri}. {@code encryptedContents} is
 *       ciphertext under the destination environment's public key (the specification's example
 *       algorithm is {@code RSA-OAEP-256}); this SDK holds no private key, does not decrypt it
 *       and exposes no accessor for it</td></tr>
 *   <tr><td>{@link Opaque}</td>
 *       <td>anything else: not base64, not JSON, not a JSON object, duplicate JSON keys, trailing
 *       content, an unlisted version, or a listed version with missing or mistyped fields</td>
 *       <td>the raw string; the envelope when it could be read</td></tr>
 * </table>
 *
 * <h2>Decoding never rejects a key</h2>
 * <p>{@link #decode(String)} throws only for a {@code null} or blank argument. Every other input
 * yields a value; an input this SDK release cannot interpret yields {@link Opaque}, so a
 * provider-issued key in a newer or provider-specific format can still be carried to the other
 * provider unchanged.</p>
 *
 * <h2>{@code encode()} returns what the provider issued</h2>
 * <p>For a decoded {@link V1} or {@link V2Encrypted}, {@link #encode()} returns the input string
 * with ASCII whitespace removed (the base64 alphabet contains none, so the payload bytes are
 * unchanged). It is not re-serialized: JSON members this SDK does not model, member order and
 * padding survive, and the issuing provider sees the bytes it issued. For a {@link V1} created
 * with {@link V1#builder()}, {@code encode()} returns the canonical form: compact UTF-8 JSON with
 * members in the order {@code version}, {@code destinationEnvironmentUri},
 * {@code sharedConnectionUuid}, {@code connectionSizeMbps}, {@code destinationAccountId}, encoded
 * with the RFC 4648 section 4 alphabet, with padding. Decoding a canonical string and encoding it
 * again returns the identical string.</p>
 *
 * <h2>Keys are credentials</h2>
 * <p>The specification requires the receiving provider to verify that the key was presented by an
 * authorized user of {@code destinationAccountId}, and a version 1 key is fully cleartext: its
 * five fields are sufficient to rebuild it. {@code toString()} on every variant therefore omits
 * {@code destinationAccountId}, {@code sharedConnectionUuid}, the ciphertext and the encoded form.
 * Jackson serializes every variant as the empty object {@code {}} under any {@code ObjectMapper}
 * configuration: the variants expose no JavaBean getters, and each is annotated with a serializer
 * that writes {@code {}} (a default {@code ObjectMapper} would otherwise reject a bean with no
 * properties, {@code SerializationFeature.FAIL_ON_EMPTY_BEANS}). Write {@link #encode()}
 * explicitly where a key must be transported.</p>
 *
 * <h2>Not to be confused with</h2>
 * <ul>
 *   <li>{@code fabric.model.implementation.AccessPoint.activationKey} (Fabric v4, Beta): a plain
 *       string on an access point. Its value may be in this open format; pass it to
 *       {@link #decode(String)} to find out.</li>
 *   <li>{@code networkedge} {@code DeviceVendorConfig.activationKey}: a vendor licence key for a
 *       virtual device. Unrelated.</li>
 *   <li>The Fabric service-profile {@code authenticationKey} (an AWS account id, an Azure service
 *       key, a GCP pairing key, an OCI virtual-circuit OCID): an identifier, not a structured
 *       single-use intent. Fabric v4 keeps the two as sibling fields.</li>
 * </ul>
 *
 * @author ianjones
 */
@JsonSerialize(using = ActivationKey.RedactingSerializer.class)
public sealed interface ActivationKey permits ActivationKey.V1, ActivationKey.V2Encrypted, ActivationKey.Opaque {

    /**
     * The Jackson serializer of every variant: it writes the empty object {@code {}} and nothing
     * else, so no key material reaches JSON produced by any {@code ObjectMapper}, including one
     * with {@code SerializationFeature.FAIL_ON_EMPTY_BEANS} enabled (the default).
     */
    final class RedactingSerializer extends JsonSerializer<ActivationKey> {

        @Override
        public void serialize(ActivationKey value, JsonGenerator gen, SerializerProvider serializers)
                throws IOException {
            gen.writeStartObject();
            gen.writeEndObject();
        }

        @Override
        public Class<ActivationKey> handledType() {
            return ActivationKey.class;
        }
    }

    /**
     * Decodes a key as received from a provider.
     *
     * <p>ASCII whitespace (space, tab, CR, LF) anywhere in the input is ignored for decoding, so a
     * key wrapped across lines by a console or an e-mail client still decodes. Both the RFC 4648
     * section 4 alphabet and the URL-safe section 5 alphabet are accepted, with or without
     * padding.</p>
     *
     * @param encoded the key string
     * @return a {@link V1}, a {@link V2Encrypted}, or an {@link Opaque} carrying the input
     * @throws NullPointerException if {@code encoded} is null
     * @throws IllegalArgumentException if {@code encoded} is blank
     */
    static ActivationKey decode(String encoded) {
        return ActivationKeyCodec.decode(encoded);
    }

    /**
     * @return the string to hand to the receiving provider; see the class documentation for what
     *         each variant returns
     */
    String encode();

    /**
     * @return the envelope's {@code version}; empty only for an {@link Opaque} key whose payload
     *         carried no readable integer {@code version}
     */
    OptionalInt envelopeVersion();

    /**
     * @return the envelope's {@code destinationEnvironmentUri}; empty only for an {@link Opaque}
     *         key whose payload carried no readable {@code destinationEnvironmentUri}
     */
    Optional<String> envelopeDestinationEnvironmentUri();

    /**
     * The environment id named by the envelope's {@code destinationEnvironmentUri}, extracted by
     * {@link EnvironmentRef#environmentIdOf(String)}.
     *
     * @return the environment id, or empty when the envelope is unreadable or its URI contains no
     *         {@code environments/{id}} segment
     */
    default Optional<String> destinationEnvironmentId() {
        return envelopeDestinationEnvironmentUri().flatMap(EnvironmentRef::environmentIdOf);
    }

    /**
     * A version 1 key: all members in cleartext.
     *
     * <p>Equality compares the five members, not the encoded form: two keys with the same members
     * issued with different JSON member order are equal.</p>
     */
    final class V1 implements ActivationKey {

        /** The {@code version} value of this variant. */
        public static final int VERSION = 1;

        private final String destinationEnvironmentUri;
        private final String sharedConnectionUuid;
        private final int connectionSizeMbps;
        private final String destinationAccountId;
        private final String encoded;

        V1(String destinationEnvironmentUri, String sharedConnectionUuid, int connectionSizeMbps,
           String destinationAccountId, String encoded) {
            this.destinationEnvironmentUri = destinationEnvironmentUri;
            this.sharedConnectionUuid = sharedConnectionUuid;
            this.connectionSizeMbps = connectionSizeMbps;
            this.destinationAccountId = destinationAccountId;
            this.encoded = encoded;
        }

        /**
         * @return a builder for a version 1 key in canonical encoding
         */
        public static Builder builder() {
            return new Builder();
        }

        /** @return {@code 1} */
        public int version() {
            return VERSION;
        }

        /** @return the URI of the environment the key is intended for; never blank */
        public String destinationEnvironmentUri() {
            return destinationEnvironmentUri;
        }

        /** @return the UUID both providers use for the connection; never blank. Sensitive. */
        public String sharedConnectionUuid() {
            return sharedConnectionUuid;
        }

        /** @return the size of the connection to provision, in Mbps; always {@code > 0} */
        public int connectionSizeMbps() {
            return connectionSizeMbps;
        }

        /** @return the customer's account id at the destination provider; never blank. Sensitive. */
        public String destinationAccountId() {
            return destinationAccountId;
        }

        @Override
        public String encode() {
            return encoded;
        }

        @Override
        public OptionalInt envelopeVersion() {
            return OptionalInt.of(VERSION);
        }

        @Override
        public Optional<String> envelopeDestinationEnvironmentUri() {
            return Optional.of(destinationEnvironmentUri);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof V1 other)) {
                return false;
            }
            return connectionSizeMbps == other.connectionSizeMbps
                    && destinationEnvironmentUri.equals(other.destinationEnvironmentUri)
                    && sharedConnectionUuid.equals(other.sharedConnectionUuid)
                    && destinationAccountId.equals(other.destinationAccountId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(destinationEnvironmentUri, sharedConnectionUuid, connectionSizeMbps,
                    destinationAccountId);
        }

        /**
         * @return a description that omits {@code sharedConnectionUuid},
         *         {@code destinationAccountId} and the encoded form
         */
        @Override
        public String toString() {
            return "ActivationKey.V1{version=" + VERSION
                    + ", destinationEnvironmentUri=" + destinationEnvironmentUri
                    + ", sharedConnectionUuid=<redacted>"
                    + ", connectionSizeMbps=" + connectionSizeMbps
                    + ", destinationAccountId=<redacted>}";
        }

        /**
         * Builder for a version 1 key. All four members are required. A customer does not create
         * keys (the issuing provider does); the builder exists for fixtures and for code that
         * stands in for a provider.
         */
        public static final class Builder {

            private String destinationEnvironmentUri;
            private String sharedConnectionUuid;
            private int connectionSizeMbps;
            private String destinationAccountId;

            private Builder() {
            }

            /**
             * @param destinationEnvironmentUri the environment URI, for example
             *        {@code providers/aws/environments/aws-gcp-us-east}
             * @return this builder
             */
            public Builder destinationEnvironmentUri(String destinationEnvironmentUri) {
                this.destinationEnvironmentUri = destinationEnvironmentUri;
                return this;
            }

            /**
             * @param sharedConnectionUuid the connection UUID shared by both providers
             * @return this builder
             */
            public Builder sharedConnectionUuid(String sharedConnectionUuid) {
                this.sharedConnectionUuid = sharedConnectionUuid;
                return this;
            }

            /**
             * @param connectionSizeMbps the connection size in Mbps
             * @return this builder
             */
            public Builder connectionSizeMbps(int connectionSizeMbps) {
                this.connectionSizeMbps = connectionSizeMbps;
                return this;
            }

            /**
             * @param destinationAccountId the customer's account id at the destination provider
             * @return this builder
             */
            public Builder destinationAccountId(String destinationAccountId) {
                this.destinationAccountId = destinationAccountId;
                return this;
            }

            /**
             * @return the key, in canonical encoding
             * @throws NullPointerException if a string member was not set
             * @throws IllegalArgumentException if a string member is blank or
             *         {@code connectionSizeMbps <= 0}
             */
            public V1 build() {
                String uri = requireText(destinationEnvironmentUri, "destinationEnvironmentUri");
                String uuid = requireText(sharedConnectionUuid, "sharedConnectionUuid");
                String account = requireText(destinationAccountId, "destinationAccountId");
                if (connectionSizeMbps <= 0) {
                    throw new IllegalArgumentException("connectionSizeMbps must be > 0, got " + connectionSizeMbps);
                }
                return new V1(uri, uuid, connectionSizeMbps, account,
                        ActivationKeyCodec.canonicalV1(uri, uuid, connectionSizeMbps, account));
            }

            private static String requireText(String value, String name) {
                Objects.requireNonNull(value, name);
                if (value.isBlank()) {
                    throw new IllegalArgumentException(name + " must not be blank");
                }
                return value;
            }

            /** @return a description that contains no member values */
            @Override
            public String toString() {
                return "ActivationKey.V1.Builder{<redacted>}";
            }
        }
    }

    /**
     * A version 2 key: the envelope in cleartext, everything else encrypted for the destination
     * environment. Only the envelope is readable. Equality compares the encoded form.
     */
    final class V2Encrypted implements ActivationKey {

        /** The {@code version} value of this variant. */
        public static final int VERSION = 2;

        private final String destinationEnvironmentUri;
        private final String encoded;

        V2Encrypted(String destinationEnvironmentUri, String encoded) {
            this.destinationEnvironmentUri = destinationEnvironmentUri;
            this.encoded = encoded;
        }

        /** @return {@code 2} */
        public int version() {
            return VERSION;
        }

        /** @return the URI of the environment the key is intended for; never blank */
        public String destinationEnvironmentUri() {
            return destinationEnvironmentUri;
        }

        @Override
        public String encode() {
            return encoded;
        }

        @Override
        public OptionalInt envelopeVersion() {
            return OptionalInt.of(VERSION);
        }

        @Override
        public Optional<String> envelopeDestinationEnvironmentUri() {
            return Optional.of(destinationEnvironmentUri);
        }

        @Override
        public boolean equals(Object o) {
            return this == o || (o instanceof V2Encrypted other && encoded.equals(other.encoded));
        }

        @Override
        public int hashCode() {
            return encoded.hashCode();
        }

        /** @return a description that omits the ciphertext and the encoded form */
        @Override
        public String toString() {
            return "ActivationKey.V2Encrypted{version=" + VERSION
                    + ", destinationEnvironmentUri=" + destinationEnvironmentUri
                    + ", encryptedContents=<redacted>}";
        }
    }

    /**
     * A key this SDK release cannot interpret. It carries the input so the key can still be handed
     * to the receiving provider. Equality compares {@link #raw()}.
     */
    final class Opaque implements ActivationKey {

        private final String raw;
        private final Integer envelopeVersion;
        private final String envelopeDestinationEnvironmentUri;

        Opaque(String raw, Integer envelopeVersion, String envelopeDestinationEnvironmentUri) {
            this.raw = raw;
            this.envelopeVersion = envelopeVersion;
            this.envelopeDestinationEnvironmentUri = envelopeDestinationEnvironmentUri;
        }

        /**
         * @return the input string with leading and trailing whitespace removed; interior
         *         characters are preserved because the format is unknown. Sensitive.
         */
        public String raw() {
            return raw;
        }

        /** @return {@link #raw()} */
        @Override
        public String encode() {
            return raw;
        }

        @Override
        public OptionalInt envelopeVersion() {
            return envelopeVersion == null ? OptionalInt.empty() : OptionalInt.of(envelopeVersion);
        }

        @Override
        public Optional<String> envelopeDestinationEnvironmentUri() {
            return Optional.ofNullable(envelopeDestinationEnvironmentUri);
        }

        @Override
        public boolean equals(Object o) {
            return this == o || (o instanceof Opaque other && raw.equals(other.raw));
        }

        @Override
        public int hashCode() {
            return raw.hashCode();
        }

        /** @return a description that reports the length of {@link #raw()} and not its content */
        @Override
        public String toString() {
            return "ActivationKey.Opaque{"
                    + (envelopeVersion == null ? "" : "version=" + envelopeVersion + ", ")
                    + "raw=<redacted, " + raw.length() + " chars>}";
        }
    }
}
