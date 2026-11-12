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

package com.eqixiac.equinix.messaging;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EmgMessageHandlerTest {

    private final EmgMessageParser parser = new EmgMessageParser();

    /** Records which callback received which message. */
    private static final class Recorder implements EmgMessageHandler {
        final List<String> calls = new ArrayList<>();
        final List<EmgMessage> messages = new ArrayList<>();

        private void record(String name, EmgMessage m) {
            calls.add(name);
            messages.add(m);
        }

        @Override public void onIbxNotification(IbxNotification m) { record("ibx", m); }
        @Override public void onNetworkNotification(NetworkNotification m) { record("network", m); }
        @Override public void onFabricNotification(FabricNotification m) { record("fabric", m); }
        @Override public void onBillingNotification(BillingNotification m) { record("billing", m); }
        @Override public void onOrderNotification(OrderNotification m) { record("order", m); }
        @Override public void onManagedServiceNotification(ManagedServiceNotification m) { record("managed", m); }
        @Override public void onUnknownNotification(UnknownNotification m) { record("unknown", m); }
    }

    @Test
    void eachTypeReachesItsOwnCallback() throws Exception {
        List<String> inputs = List.of(
                Fixtures.read("docs-ibx-sample.json"),
                Fixtures.read("docs-network-sample.json"),
                Fixtures.read("docs-fabric-sample.json"),
                Fixtures.read("docs-billing-sample.json"),
                Fixtures.read("docs-order-sample.json"),
                Fixtures.envelope("TroubleTicket.ManagedService", "1.0", "spec-managed-services-body.json"),
                "{\"Task\":{\"Resource\":\"Something.New\",\"Body\":{}}}");
        Recorder recorder = new Recorder();
        for (String input : inputs) {
            EmgMessage message = parser.parse(input);
            recorder.handle(message);
            assertSame(message, recorder.messages.get(recorder.messages.size() - 1));
        }
        assertEquals(List.of("ibx", "network", "fabric", "billing", "order", "managed", "unknown"), recorder.calls);
    }

    @Test
    void callbacksDefaultToNoOps() {
        EmgMessageHandler onlyIbx = new EmgMessageHandler() {
        };
        assertDoesNotThrow(() -> onlyIbx.handle(parser.parse(Fixtures.read("docs-order-sample.json"))));
    }

    @Test
    void callbackExceptionsPropagate() {
        EmgMessageHandler failing = new EmgMessageHandler() {
            @Override
            public void onFabricNotification(FabricNotification m) throws Exception {
                throw new java.io.IOException("downstream unavailable");
            }
        };
        EmgMessage fabric = parser.parse(Fixtures.read("docs-fabric-sample.json"));
        assertThrows(java.io.IOException.class, () -> failing.handle(fabric));
    }

    @Test
    void sealedSwitchIsExhaustiveWithoutDefault() {
        EmgMessage m = parser.parse(Fixtures.read("docs-ibx-sample.json"));
        String kind = switch (m) {
            case IbxNotification n -> "ibx:" + n.category();
            case NetworkNotification n -> "network";
            case FabricNotification n -> "fabric";
            case BillingNotification n -> "billing";
            case OrderNotification n -> "order";
            case ManagedServiceNotification n -> "managed";
            case UnknownNotification n -> "unknown";
        };
        assertEquals("ibx:MAINTENANCE", kind);
    }
}
