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

/**
 * A parsed Equinix Messaging Gateway notification. The hierarchy is sealed, so a {@code switch}
 * over it is checked for exhaustiveness by the compiler:
 *
 * <pre>{@code
 * switch (message) {
 *     case IbxNotification m -> ...;
 *     case NetworkNotification m -> ...;
 *     case FabricNotification m -> ...;
 *     case BillingNotification m -> ...;
 *     case OrderNotification m -> ...;
 *     case ManagedServiceNotification m -> ...;
 *     case UnknownNotification m -> ...;
 * }
 * }</pre>
 *
 * <p>Every implementation is an immutable record carrying the common {@link EmgHeader} and the
 * exact JSON text it was parsed from ({@link #rawJson()}).
 */
public sealed interface EmgMessage permits IbxNotification, NetworkNotification, FabricNotification,
        BillingNotification, OrderNotification, ManagedServiceNotification, UnknownNotification {

    /**
     * The header attributes shared by every notification.
     *
     * @return the header, never {@code null}
     */
    EmgHeader header();

    /**
     * The complete envelope exactly as received (UTF-8 decoded, BOM removed), for auditing or for
     * fields this SDK version does not model.
     *
     * @return the raw JSON text
     */
    String rawJson();

    /**
     * Shortcut for {@code header().id()}: EMG delivers at least once, so use this as the key to
     * de-duplicate redelivered notifications.
     *
     * @return the message id, or {@code null} if the notification carried none
     */
    default String id() {
        return header().id();
    }
}
