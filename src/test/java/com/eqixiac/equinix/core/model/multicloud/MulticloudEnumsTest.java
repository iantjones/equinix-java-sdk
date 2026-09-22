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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Tests for the wire enums. The expected value lists are copied from
 * {@code connection-coordinator/schemas/common.yaml} and {@code schemas/environment.yaml} of the
 * specification at commit {@code bbfc763}; a constant added to or removed from an enum without a
 * matching specification change fails {@link #constantsEqualTheSpecificationLists()}.
 */
@DisplayName("core.model.multicloud wire enums")
class MulticloudEnumsTest {

    /** Enum type to (the specification's enum list, the parser under test). */
    private record Case<E extends Enum<E>>(Class<E> type, List<String> specValues, Function<String, E> parser) {
    }

    private static final List<Case<?>> CASES = List.of(
            new Case<>(AdminState.class,
                    List.of("ADMIN_STATE_ENABLED", "ADMIN_STATE_DISABLED"), AdminState::fromString),
            new Case<>(VerificationState.class,
                    List.of("VERIFICATION_STATE_UNVERIFIED", "VERIFICATION_STATE_VERIFIED"),
                    VerificationState::fromString),
            new Case<>(ProvisioningState.class,
                    List.of("PROVISIONING_STATE_PENDING", "PROVISIONING_STATE_FINAL"), ProvisioningState::fromString),
            new Case<>(MulticloudConnectionType.class,
                    List.of("CONNECTION_TYPE_STANDARD", "CONNECTION_TYPE_TEST", "CONNECTION_TYPE_MONITORING"),
                    MulticloudConnectionType::fromString),
            new Case<>(EnvironmentVisibility.class,
                    List.of("ENVIRONMENT_VISIBILITY_PRIVATE", "ENVIRONMENT_VISIBILITY_PUBLIC"),
                    EnvironmentVisibility::fromString));

    @Test
    @DisplayName("constants are exactly the specification's values plus UNKNOWN, in specification order")
    void constantsEqualTheSpecificationLists() {
        for (Case<?> c : CASES) {
            List<String> expected = new java.util.ArrayList<>(c.specValues());
            expected.add("UNKNOWN");
            List<String> actual = Arrays.stream(c.type().getEnumConstants()).map(Enum::name).collect(Collectors.toList());
            assertEquals(expected, actual, c.type().getSimpleName());
        }
    }

    @Test
    @DisplayName("fromString maps every wire value to the constant of the same name")
    void parsesWireValues() {
        for (Case<?> c : CASES) {
            for (String wire : c.specValues()) {
                assertEquals(wire, c.parser().apply(wire).name(), c.type().getSimpleName());
            }
        }
    }

    @Test
    @DisplayName("fromString trims and ignores case")
    void tolerantParsing() {
        assertSame(AdminState.ADMIN_STATE_ENABLED, AdminState.fromString(" admin_state_enabled\n"));
        assertSame(VerificationState.VERIFICATION_STATE_VERIFIED,
                VerificationState.fromString("Verification_State_Verified"));
        assertSame(ProvisioningState.PROVISIONING_STATE_FINAL, ProvisioningState.fromString("provisioning_state_final "));
        assertSame(MulticloudConnectionType.CONNECTION_TYPE_TEST, MulticloudConnectionType.fromString("connection_type_test"));
        assertSame(EnvironmentVisibility.ENVIRONMENT_VISIBILITY_PRIVATE,
                EnvironmentVisibility.fromString("\tenvironment_visibility_private"));
    }

    @Test
    @DisplayName("fromString returns UNKNOWN for null, blank, the unprefixed form and an unlisted value")
    void unknownFallback() {
        for (Case<?> c : CASES) {
            for (String input : Arrays.asList(null, "", "   ", "ENABLED", "STATE_FROM_A_LATER_SPEC_REVISION", "unknown")) {
                assertEquals("UNKNOWN", c.parser().apply(input).name(),
                        c.type().getSimpleName() + " <- " + input);
            }
        }
    }

    /** The shape of the specification's Connection resource, reduced to its enum members. */
    private static final class ConnectionEnums {
        public AdminState adminState;
        public VerificationState verificationState;
        public MulticloudConnectionType connectionType;
    }

    @Test
    @DisplayName("the SDK wire mapper deserializes an unlisted value to UNKNOWN instead of failing the payload")
    void jacksonUnknownTolerance() throws Exception {
        ConnectionEnums read = Constants.mapper().readValue(
                "{\"adminState\":\"ADMIN_STATE_DRAINING\","
                        + "\"verificationState\":\"VERIFICATION_STATE_VERIFIED\","
                        + "\"connectionType\":\"CONNECTION_TYPE_CANARY\"}",
                ConnectionEnums.class);
        assertSame(AdminState.UNKNOWN, read.adminState);
        assertSame(VerificationState.VERIFICATION_STATE_VERIFIED, read.verificationState);
        assertSame(MulticloudConnectionType.UNKNOWN, read.connectionType);

        assertSame(ProvisioningState.UNKNOWN,
                Constants.mapper().readValue("\"PROVISIONING_STATE_ROLLED_BACK\"", ProvisioningState.class));
        assertSame(EnvironmentVisibility.UNKNOWN,
                Constants.mapper().readValue("\"ENVIRONMENT_VISIBILITY_PARTNER\"", EnvironmentVisibility.class));
    }

    @Test
    @DisplayName("doc contract: 'Jackson leaves an absent property null'; SPEC_DEFAULT is CONNECTION_TYPE_STANDARD")
    void absentConnectionType() throws Exception {
        ConnectionEnums read = Constants.mapper().readValue("{\"adminState\":\"ADMIN_STATE_ENABLED\"}",
                ConnectionEnums.class);
        assertNull(read.connectionType);
        assertSame(MulticloudConnectionType.CONNECTION_TYPE_STANDARD, MulticloudConnectionType.SPEC_DEFAULT);
    }

    @Test
    @DisplayName("serialization writes the constant name, which equals the wire value")
    void serializesWireValue() throws Exception {
        Map<Enum<?>, String> expected = new LinkedHashMap<>();
        expected.put(AdminState.ADMIN_STATE_DISABLED, "\"ADMIN_STATE_DISABLED\"");
        expected.put(VerificationState.VERIFICATION_STATE_UNVERIFIED, "\"VERIFICATION_STATE_UNVERIFIED\"");
        expected.put(ProvisioningState.PROVISIONING_STATE_PENDING, "\"PROVISIONING_STATE_PENDING\"");
        expected.put(MulticloudConnectionType.CONNECTION_TYPE_MONITORING, "\"CONNECTION_TYPE_MONITORING\"");
        expected.put(EnvironmentVisibility.ENVIRONMENT_VISIBILITY_PUBLIC, "\"ENVIRONMENT_VISIBILITY_PUBLIC\"");
        for (Map.Entry<Enum<?>, String> e : expected.entrySet()) {
            assertEquals(e.getValue(), Constants.mapper().writeValueAsString(e.getKey()));
        }
    }
}
