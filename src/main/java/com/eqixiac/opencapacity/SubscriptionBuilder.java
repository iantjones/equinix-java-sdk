/*
 * Copyright 2026 Ian Jones. All Rights Reserved.
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

package com.eqixiac.opencapacity;

import com.eqixiac.opencapacity.model.CapacityRequirement;
import com.eqixiac.opencapacity.model.Delivery;
import com.eqixiac.opencapacity.model.DeliveryType;
import com.eqixiac.opencapacity.model.NotifyOn;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Builds a new subscription. Returned by {@link Subscriptions#define}. */
public final class SubscriptionBuilder {

    private final Transport transport;
    private final CapacityRequirement requirement;
    private final String signalId;
    private String name;
    private NotifyOn notifyOn;
    private Delivery delivery;
    private Instant expiresAt;

    SubscriptionBuilder(Transport transport, CapacityRequirement requirement, String signalId) {
        this.transport = transport;
        this.requirement = requirement;
        this.signalId = signalId;
    }

    /**
     * Sets a display name.
     *
     * @param value the name
     * @return this builder
     */
    public SubscriptionBuilder name(String value) {
        this.name = value;
        return this;
    }

    /**
     * Sets which changes produce an event. Without this call the provider sends an event when a
     * grade changes.
     *
     * @param value the trigger
     * @return this builder
     */
    public SubscriptionBuilder notifyOn(NotifyOn value) {
        this.notifyOn = value;
        return this;
    }

    /**
     * Delivers events to an HTTPS endpoint of yours. The provider verifies the endpoint before it
     * sends events, and signs each delivery.
     *
     * @param url the HTTPS URL
     * @return this builder
     */
    public SubscriptionBuilder webhook(String url) {
        this.delivery = new Delivery(DeliveryType.WEBHOOK, Objects.requireNonNull(url, "url"), null);
        return this;
    }

    /**
     * Keeps events at the provider for you to read with {@link Subscription#events} or
     * {@link Subscription#stream}.
     *
     * @return this builder
     */
    public SubscriptionBuilder pull() {
        this.delivery = new Delivery(DeliveryType.PULL, null, null);
        return this;
    }

    /**
     * Sets when the subscription expires.
     *
     * @param value the expiry time
     * @return this builder
     */
    public SubscriptionBuilder expiresAt(Instant value) {
        this.expiresAt = value;
        return this;
    }

    /**
     * Sends the subscription to the provider.
     *
     * @return the subscription. With webhook delivery its state is {@code PENDING_VERIFICATION} and
     *     {@link Subscription#signingSecret()} holds the secret, which the provider returns only
     *     this once
     * @throws IllegalStateException if neither {@link #webhook(String)} nor {@link #pull()} was called
     * @throws OpenCapacityException if the request fails
     */
    public Subscription create() {
        if (delivery == null) {
            throw new IllegalStateException("Choose a delivery type: call webhook(url) or pull().");
        }
        Wire.SubscriptionBody body = Wire.SubscriptionBody.request(
                name, requirement, signalId, notifyOn, delivery, null, expiresAt);
        Transport.Reply reply = transport.call(
                "CreateSubscription", Map.of(), null, body, Map.of("Idempotency-Key", UUID.randomUUID().toString()));
        Wire.SubscriptionBody created = transport.read(reply, Transport.type(Wire.SubscriptionBody.class));
        String secret = created.delivery() == null ? null : created.delivery().signingSecret();
        return new Subscription(transport, created, reply.etag(), secret);
    }
}
