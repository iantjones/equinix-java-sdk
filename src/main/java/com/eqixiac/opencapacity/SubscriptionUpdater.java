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

/**
 * Amends a subscription. Returned by {@link Subscription#update()}. The updater starts from the
 * current values and {@link #save()} sends the whole subscription.
 */
public final class SubscriptionUpdater {

    private final Subscription subscription;
    private String name;
    private CapacityRequirement requirement;
    private final String signalId;
    private NotifyOn notifyOn;
    private Delivery delivery;
    private Boolean paused;
    private Instant expiresAt;

    SubscriptionUpdater(Subscription subscription, Wire.SubscriptionBody current) {
        this.subscription = subscription;
        this.name = current.name();
        this.requirement = current.requirement();
        this.signalId = current.signalId();
        this.notifyOn = current.notifyOn();
        Delivery now = current.delivery();
        this.delivery = now == null ? null : new Delivery(now.type(), now.url(), null);
        this.paused = current.paused();
        this.expiresAt = current.expiresAt();
    }

    /**
     * Sets the display name.
     *
     * @param value the name
     * @return this updater
     */
    public SubscriptionUpdater name(String value) {
        this.name = value;
        return this;
    }

    /**
     * Replaces the requirement of a subscription that follows a requirement.
     *
     * @param value the new requirement
     * @return this updater
     * @throws IllegalStateException if the subscription follows a signal
     */
    public SubscriptionUpdater requirement(CapacityRequirement value) {
        if (signalId != null) {
            throw new IllegalStateException("This subscription follows a signal. Amend the signal instead.");
        }
        this.requirement = Objects.requireNonNull(value, "value");
        return this;
    }

    /**
     * Sets which changes produce an event.
     *
     * @param value the trigger
     * @return this updater
     */
    public SubscriptionUpdater notifyOn(NotifyOn value) {
        this.notifyOn = value;
        return this;
    }

    /**
     * Delivers events to an HTTPS endpoint. A change of URL returns the subscription to
     * {@code PENDING_VERIFICATION}.
     *
     * @param url the HTTPS URL
     * @return this updater
     */
    public SubscriptionUpdater webhook(String url) {
        this.delivery = new Delivery(DeliveryType.WEBHOOK, Objects.requireNonNull(url, "url"), null);
        return this;
    }

    /**
     * Keeps events at the provider for you to read.
     *
     * @return this updater
     */
    public SubscriptionUpdater pull() {
        this.delivery = new Delivery(DeliveryType.PULL, null, null);
        return this;
    }

    /**
     * Pauses or resumes delivery.
     *
     * @param value {@code true} to pause, {@code false} to resume
     * @return this updater
     */
    public SubscriptionUpdater paused(boolean value) {
        this.paused = value;
        return this;
    }

    /**
     * Sets when the subscription expires.
     *
     * @param value the expiry time
     * @return this updater
     */
    public SubscriptionUpdater expiresAt(Instant value) {
        this.expiresAt = value;
        return this;
    }

    /**
     * Sends the amended subscription.
     *
     * @return the subscription in its new state
     * @throws OpenCapacityException with status 412 if the subscription changed since this object
     *     last read it
     */
    public Subscription save() {
        Wire.SubscriptionBody body = Wire.SubscriptionBody.request(
                name, requirement, signalId, notifyOn, delivery, paused, expiresAt);
        String etag = subscription.currentEtag();
        Map<String, String> headers = etag == null ? Map.of() : Map.of("If-Match", etag);
        subscription.replace(subscription.transport().call(
                "ReplaceSubscription", Map.of("subscriptionId", subscription.id()), null, body, headers));
        return subscription;
    }
}
