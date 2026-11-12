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
 * Callback interface for typed EMG notifications. Override the callbacks for the types you care
 * about; the rest default to no-ops (the message is then treated as handled). {@link #handle}
 * dispatches with an exhaustive {@code switch} over the sealed {@link EmgMessage} hierarchy.
 *
 * <p><b>Handlers must be idempotent.</b> EMG (Azure Service Bus) delivers at least once: a message
 * is redelivered if its lock expires or settlement fails, so the same {@link EmgMessage#id()} can
 * arrive more than once. Throwing from a callback signals failure; the transport adapter then
 * abandons the message so it is redelivered.
 */
public interface EmgMessageHandler {

    /**
     * IBX (data center) maintenance, incident, security incident or advisory.
     *
     * @param message the notification
     * @throws Exception to have the message redelivered
     */
    default void onIbxNotification(IbxNotification message) throws Exception {
    }

    /**
     * Network maintenance or incident.
     *
     * @param message the notification
     * @throws Exception to have the message redelivered
     */
    default void onNetworkNotification(NetworkNotification message) throws Exception {
    }

    /**
     * Fabric port or virtual connection up/down.
     *
     * @param message the notification
     * @throws Exception to have the message redelivered
     */
    default void onFabricNotification(FabricNotification message) throws Exception {
    }

    /**
     * Billing started for a closed order.
     *
     * @param message the notification
     * @throws Exception to have the message redelivered
     */
    default void onBillingNotification(BillingNotification message) throws Exception {
    }

    /**
     * Order status update.
     *
     * @param message the notification
     * @throws Exception to have the message redelivered
     */
    default void onOrderNotification(OrderNotification message) throws Exception {
    }

    /**
     * Managed Services trouble ticket or service request.
     *
     * @param message the notification
     * @throws Exception to have the message redelivered
     */
    default void onManagedServiceNotification(ManagedServiceNotification message) throws Exception {
    }

    /**
     * A well-formed envelope of a type this SDK version does not model.
     *
     * @param message the notification, with its body preserved verbatim
     * @throws Exception to have the message redelivered
     */
    default void onUnknownNotification(UnknownNotification message) throws Exception {
    }

    /**
     * Dispatches {@code message} to the matching callback.
     *
     * @param message the parsed message
     * @throws Exception whatever the callback throws
     */
    default void handle(EmgMessage message) throws Exception {
        switch (message) {
            case IbxNotification m -> onIbxNotification(m);
            case NetworkNotification m -> onNetworkNotification(m);
            case FabricNotification m -> onFabricNotification(m);
            case BillingNotification m -> onBillingNotification(m);
            case OrderNotification m -> onOrderNotification(m);
            case ManagedServiceNotification m -> onManagedServiceNotification(m);
            case UnknownNotification m -> onUnknownNotification(m);
        }
    }
}
