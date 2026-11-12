/**
 * Client for the <b>Equinix Messaging Gateway (EMG)</b>, Equinix's push channel for notifications:
 * IBX (data center) and network maintenance and incidents (flagged {@code SERVICE_IMPACTING} or
 * {@code NON_SERVICE_IMPACTING}), Fabric port/virtual-connection up/down, billing start, order status
 * and Managed Services tickets. EMG publishes JSON envelopes ({@code {"Task": {...header, "Body": {...}},
 * "Signature": "..."}}) to a queue dedicated to your organisation on Azure Service Bus, over AMQP 1.0
 * (AsyncAPI {@code emgv1}, production namespace {@code equinixemg.servicebus.windows.net}).
 *
 * <h2>Layers</h2>
 * <ul>
 *   <li>This package is transport agnostic: immutable message records (a sealed
 *       {@link com.eqixiac.equinix.messaging.EmgMessage} hierarchy), the tolerant
 *       {@link com.eqixiac.equinix.messaging.EmgMessageParser} and the typed
 *       {@link com.eqixiac.equinix.messaging.EmgMessageHandler}. It has no messaging-client dependency;
 *       use it with any AMQP client by passing the message body bytes to the parser.</li>
 *   <li>{@link com.eqixiac.equinix.messaging.transport} adds a receive/settle loop, and
 *       {@link com.eqixiac.equinix.messaging.transport.servicebus} binds it to Azure Service Bus. That
 *       binding needs {@code com.azure:azure-messaging-servicebus}, which the SDK declares
 *       {@code <optional>}: add it to your own build.</li>
 * </ul>
 *
 * <h2>Connection details</h2>
 * Create a subscription in the Equinix portal (portal.equinix.com) under <i>Developer Settings &gt; Messaging
 * Gateway</i> (one active subscription per environment; your user needs the notification permissions
 * for the relevant IBXs), choose the notification types, then open the subscription's
 * <i>Configuration</i> tab and <i>Copy YAML</i>: it holds the queue connection string and queue name.
 * Treat the connection string as a secret (environment variable or secret store). The legacy
 * client-id/client-secret ({@code client_credentials}) authentication for EMG was deprecated on
 * 2024-03-18 and is not supported here. EMG Order Management (placing orders through EMG) was
 * deprecated on 2024-09-30 and is not implemented; inbound Order <em>Notifications</em> are.
 *
 * <h2>Delivery semantics</h2>
 * Service Bus delivers <b>at least once</b>. A message whose lock expires, whose handler fails, or
 * whose settlement is lost is delivered again, so <b>handlers must be idempotent</b>: de-duplicate on
 * {@link com.eqixiac.equinix.messaging.EmgMessage#id()} (unique per notification) and, for ticket
 * updates, order by {@code body().sequence()}. Ordering across tickets is not guaranteed.
 *
 * <h2>Example</h2>
 * <pre>{@code
 * EmgMessageHandler handler = new EmgMessageHandler() {
 *     @Override
 *     public void onIbxNotification(IbxNotification n) {
 *         if (n.body().isServiceImpacting()) {
 *             alert(n.category(), n.body().servicerId(), n.body().startDateTime());
 *         }
 *     }
 * };
 * EmgReceiver receiver = ServiceBusEmgReceiver.forQueue(
 *         System.getenv("EMG_CONNECTION_STRING"), System.getenv("EMG_QUEUE"));
 * try (EmgListener listener = new EmgListener(receiver, handler)) {
 *     Runtime.getRuntime().addShutdownHook(new Thread(listener::close));
 *     listener.run();   // blocks: complete on success, abandon on handler error,
 * }                     // dead-letter bodies that are not EMG envelopes
 * }</pre>
 */
package com.eqixiac.equinix.messaging;
