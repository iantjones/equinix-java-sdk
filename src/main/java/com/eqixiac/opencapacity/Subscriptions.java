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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Subscriptions to the answer for a requirement or a signal. The provider sends an event when the
 * answer changes, to a webhook or for you to read.
 */
public final class Subscriptions {

    private final Transport transport;

    Subscriptions(Transport transport) {
        this.transport = transport;
    }

    /**
     * Starts a subscription to the answer for a requirement. Nothing is sent until you call
     * {@link SubscriptionBuilder#create()}.
     *
     * @param requirement the requirement to follow
     * @return a builder
     */
    public SubscriptionBuilder define(CapacityRequirement requirement) {
        return new SubscriptionBuilder(transport, Objects.requireNonNull(requirement, "requirement"), null);
    }

    /**
     * Starts a subscription that follows the answer and the state of one of your signals.
     *
     * @param signal the signal to follow
     * @return a builder
     */
    public SubscriptionBuilder define(Signal signal) {
        return new SubscriptionBuilder(transport, null, Objects.requireNonNull(signal, "signal").id());
    }

    /**
     * Reads one subscription.
     *
     * @param subscriptionId the identifier
     * @return the subscription
     * @throws OpenCapacityException with status 404 if no such subscription exists
     */
    public Subscription get(String subscriptionId) {
        Objects.requireNonNull(subscriptionId, "subscriptionId");
        Transport.Reply reply = transport.send(
                "GET", "/subscriptions/" + Transport.encode(subscriptionId), null, Map.of(), true);
        return new Subscription(
                transport, transport.read(reply, Transport.type(Wire.SubscriptionBody.class)), reply.etag(), null);
    }

    /**
     * Lists your subscriptions.
     *
     * @return the subscriptions, across all pages
     * @throws OpenCapacityException if a request fails
     */
    public List<Subscription> list() {
        List<Subscription> all = new ArrayList<>();
        String cursor = null;
        do {
            String path = "/subscriptions?limit=200" + (cursor == null ? "" : "&cursor=" + Transport.encode(cursor));
            Transport.Reply reply = transport.send("GET", path, null, Map.of(), true);
            Wire.Page<Wire.SubscriptionBody> page =
                    transport.read(reply, Transport.type(Wire.Page.class, Wire.SubscriptionBody.class));
            for (Wire.SubscriptionBody body : page.items()) {
                all.add(new Subscription(transport, body, null, null));
            }
            cursor = page.next();
        } while (cursor != null);
        return List.copyOf(all);
    }
}
