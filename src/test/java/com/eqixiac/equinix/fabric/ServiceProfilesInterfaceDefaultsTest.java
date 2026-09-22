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

package com.eqixiac.equinix.fabric;

import com.eqixiac.equinix.fabric.client.ServiceProfiles;
import com.eqixiac.equinix.fabric.model.implementation.ActivationKeyDetails;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Doc-contract tests for the {@code default} methods added to {@link ServiceProfiles} with the
 * Beta environment operations. The defaults exist so that implementations of the interface
 * written before those methods (test fakes, caller-side decorators) still compile.
 */
class ServiceProfilesInterfaceDefaultsTest {

    /** An implementation that overrides none of the default methods. */
    private static ServiceProfiles legacyImplementation(InvocationHandler abstractCalls) {
        return (ServiceProfiles) Proxy.newProxyInstance(ServiceProfiles.class.getClassLoader(),
                new Class<?>[]{ServiceProfiles.class},
                (proxy, method, args) -> method.isDefault()
                        ? InvocationHandler.invokeDefault(proxy, method, args)
                        : abstractCalls.invoke(proxy, method, args));
    }

    private static final InvocationHandler NO_ABSTRACT_CALLS = (proxy, method, args) -> {
        throw new AssertionError("unexpected call to " + method);
    };

    @Test
    @DisplayName("getEnvironments default throws UnsupportedOperationException naming the implementation")
    void getEnvironmentsDefaultThrows() {
        ServiceProfiles legacy = legacyImplementation(NO_ABSTRACT_CALLS);

        UnsupportedOperationException e = assertThrows(UnsupportedOperationException.class,
                () -> legacy.getEnvironments("23ad37d0-6b1c-4d1f-a4eb-9f3d68fbe4fd"));
        assertTrue(e.getMessage().contains("getEnvironments"));
    }

    @Test
    @DisplayName("validateActivationKey(ActivationKeyDetails) default throws UnsupportedOperationException")
    void validateDefaultThrows() {
        ServiceProfiles legacy = legacyImplementation(NO_ABSTRACT_CALLS);

        assertThrows(UnsupportedOperationException.class,
                () -> legacy.validateActivationKey("sp", "env", ActivationKeyDetails.ofValue("key_here")));
    }

    @Test
    @DisplayName("validateActivationKey(String) default wraps the key with ofValue and delegates to the details overload")
    void stringOverloadDelegates() {
        AtomicReference<Object[]> captured = new AtomicReference<>();
        ServiceProfiles overridingDetailsOnly = (ServiceProfiles) Proxy.newProxyInstance(
                ServiceProfiles.class.getClassLoader(), new Class<?>[]{ServiceProfiles.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("validateActivationKey")
                            && method.getParameterTypes()[2] == ActivationKeyDetails.class) {
                        captured.set(args);
                        return null;
                    }
                    return InvocationHandler.invokeDefault(proxy, method, args);
                });

        overridingDetailsOnly.validateActivationKey("sp", "env", "key_here");

        assertEquals("sp", captured.get()[0]);
        assertEquals("env", captured.get()[1]);
        ActivationKeyDetails details = (ActivationKeyDetails) captured.get()[2];
        assertEquals("key_here", details.getValue());
        assertNull(details.getProviderId());
        assertNull(details.getAccountId());
        assertNull(details.getBandwidth());
        assertNull(details.getRegion());
    }

    @Test
    @DisplayName("validateActivationKey(String) default rejects a null or blank key before delegating")
    void stringOverloadRejectsBlankKey() {
        ServiceProfiles legacy = legacyImplementation(NO_ABSTRACT_CALLS);

        assertThrows(IllegalArgumentException.class, () -> legacy.validateActivationKey("sp", "env", (String) null));
        assertThrows(IllegalArgumentException.class, () -> legacy.validateActivationKey("sp", "env", " "));
    }
}
