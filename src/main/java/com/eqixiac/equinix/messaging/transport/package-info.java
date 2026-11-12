/**
 * Optional transport layer for the Equinix Messaging Gateway: {@link
 * com.eqixiac.equinix.messaging.transport.EmgListener} drives an {@link
 * com.eqixiac.equinix.messaging.transport.EmgReceiver} (peek-lock receive, explicit settlement) and
 * feeds the transport-agnostic {@link com.eqixiac.equinix.messaging.EmgMessageParser}. This package
 * has no third-party messaging dependency; the Azure Service Bus binding lives in
 * {@link com.eqixiac.equinix.messaging.transport.servicebus}.
 */
package com.eqixiac.equinix.messaging.transport;
