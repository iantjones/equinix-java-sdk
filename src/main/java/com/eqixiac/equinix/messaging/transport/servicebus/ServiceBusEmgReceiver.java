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

package com.eqixiac.equinix.messaging.transport.servicebus;

import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.azure.messaging.servicebus.ServiceBusReceiverClient;
import com.azure.messaging.servicebus.models.DeadLetterOptions;
import com.azure.messaging.servicebus.models.ServiceBusReceiveMode;
import com.eqixiac.equinix.messaging.transport.EmgDelivery;
import com.eqixiac.equinix.messaging.transport.EmgReceiver;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * {@link EmgReceiver} backed by the Azure Service Bus {@link ServiceBusReceiverClient}, the
 * transport of the Equinix Messaging Gateway (AMQP 1.0; production namespace
 * {@code equinixemg.servicebus.windows.net} per the AsyncAPI spec). Receives in
 * {@link ServiceBusReceiveMode#PEEK_LOCK} with auto-complete disabled, so settlement is always
 * explicit.
 *
 * <p>Requires {@code com.azure:azure-messaging-servicebus} on the classpath; the SDK declares it
 * {@code <optional>}, so add it to your own build to use this class.
 *
 * <p>The connection string is a secret: it is passed straight to the Azure builder, never stored
 * in a field, never included in exception messages, logs or {@link #toString()}.
 */
public final class ServiceBusEmgReceiver implements EmgReceiver {

    private final ServiceBusReceiverClient client;
    private final String entity;

    private ServiceBusEmgReceiver(ServiceBusReceiverClient client, String entity) {
        this.client = Objects.requireNonNull(client, "client");
        this.entity = entity;
    }

    /**
     * Connects to an EMG queue with the connection string and queue name from your EMG subscription
     * (Equinix portal &gt; Developer Settings &gt; Messaging Gateway &gt; subscription &gt;
     * Configuration &gt; Copy YAML). No network I/O happens until the first receive.
     *
     * @param connectionString the Service Bus connection string (secret)
     * @param queueName        the queue name, e.g. {@code prod_<id>_queue}
     * @return the receiver
     * @throws IllegalArgumentException if the connection string is rejected (the value is not echoed)
     */
    public static ServiceBusEmgReceiver forQueue(String connectionString, String queueName) {
        Objects.requireNonNull(connectionString, "connectionString");
        Objects.requireNonNull(queueName, "queueName");
        ServiceBusClientBuilder.ServiceBusReceiverClientBuilder builder = builder(connectionString)
                .queueName(queueName);
        return new ServiceBusEmgReceiver(build(builder), "queue " + queueName);
    }

    /**
     * Connects to a topic subscription, for subscriptions provisioned as topic + subscription rather
     * than a queue.
     *
     * @param connectionString the Service Bus connection string (secret)
     * @param topicName        the topic name
     * @param subscriptionName the subscription name
     * @return the receiver
     * @throws IllegalArgumentException if the connection string is rejected (the value is not echoed)
     */
    public static ServiceBusEmgReceiver forSubscription(String connectionString, String topicName,
                                                        String subscriptionName) {
        Objects.requireNonNull(connectionString, "connectionString");
        Objects.requireNonNull(topicName, "topicName");
        Objects.requireNonNull(subscriptionName, "subscriptionName");
        ServiceBusClientBuilder.ServiceBusReceiverClientBuilder builder = builder(connectionString)
                .topicName(topicName).subscriptionName(subscriptionName);
        return new ServiceBusEmgReceiver(build(builder), "subscription " + topicName + "/" + subscriptionName);
    }

    /**
     * Wraps a receiver client you built yourself, e.g. with an {@code AzureSasCredential} SAS token
     * or a custom retry policy. It must use {@link ServiceBusReceiveMode#PEEK_LOCK} with
     * {@code disableAutoComplete()}; the returned receiver takes ownership and closes it.
     *
     * @param client the receiver client
     * @return the receiver
     */
    public static ServiceBusEmgReceiver wrap(ServiceBusReceiverClient client) {
        return new ServiceBusEmgReceiver(client, client.getEntityPath());
    }

    private static ServiceBusClientBuilder.ServiceBusReceiverClientBuilder builder(String connectionString) {
        try {
            return new ServiceBusClientBuilder().connectionString(connectionString).receiver()
                    .receiveMode(ServiceBusReceiveMode.PEEK_LOCK).disableAutoComplete();
        } catch (RuntimeException e) {
            // Deliberately drop the cause: Azure's message may quote the connection string.
            throw new IllegalArgumentException("Service Bus connection string rejected ("
                    + e.getClass().getSimpleName() + "); value not shown");
        }
    }

    private static ServiceBusReceiverClient build(ServiceBusClientBuilder.ServiceBusReceiverClientBuilder builder) {
        try {
            return builder.buildClient();
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Service Bus receiver could not be built ("
                    + e.getClass().getSimpleName() + "); connection details not shown");
        }
    }

    @Override
    public List<EmgDelivery> receive(int maxMessages, Duration maxWait) {
        List<EmgDelivery> out = new ArrayList<>();
        for (ServiceBusReceivedMessage message : client.receiveMessages(maxMessages, maxWait)) {
            out.add(new Delivery(client, message));
        }
        return out;
    }

    @Override
    public void close() {
        client.close();
    }

    @Override
    public String toString() {
        return "ServiceBusEmgReceiver[" + entity + "]";
    }

    private record Delivery(ServiceBusReceiverClient client, ServiceBusReceivedMessage message)
            implements EmgDelivery {

        @Override
        public byte[] body() {
            return message.getBody().toBytes();
        }

        @Override
        public String messageId() {
            return message.getMessageId();
        }

        @Override
        public long deliveryCount() {
            return message.getDeliveryCount();
        }

        @Override
        public void complete() {
            client.complete(message);
        }

        @Override
        public void abandon() {
            client.abandon(message);
        }

        @Override
        public void deadLetter(String reason, String description) {
            client.deadLetter(message, new DeadLetterOptions()
                    .setDeadLetterReason(reason).setDeadLetterErrorDescription(description));
        }

        @Override
        public String toString() {
            return "Delivery[" + message.getMessageId() + "]";
        }
    }
}
