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
import com.eqixiac.opencapacity.model.Event;
import com.eqixiac.opencapacity.model.NotifyOn;
import com.eqixiac.opencapacity.model.SubscriptionState;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * A subscription that a provider holds. The object keeps a reference to its client:
 * {@link #update()}, {@link #pause()}, {@link #delete()}, {@link #refresh()}, {@link #events}, and
 * {@link #stream} act on the subscription it represents.
 *
 * <p>The object is not thread-safe.
 */
public final class Subscription {

    private final Transport transport;
    private Wire.SubscriptionBody body;
    private String etag;
    private final String signingSecret;
    private boolean deleted;

    Subscription(Transport transport, Wire.SubscriptionBody body, String etag, String signingSecret) {
        this.transport = transport;
        this.body = body;
        this.etag = etag;
        this.signingSecret = signingSecret;
    }

    private Map<String, String> self() {
        return Map.of("subscriptionId", body.subscriptionId());
    }

    void replace(Transport.Reply reply) {
        this.body = transport.read(reply, Transport.type(Wire.SubscriptionBody.class));
        this.etag = reply.etag();
    }

    Transport transport() {
        return transport;
    }

    String currentEtag() {
        if (etag == null) {
            refresh();
        }
        return etag;
    }

    /**
     * Returns the identifier.
     *
     * @return the provider's identifier of the subscription
     */
    public String id() {
        return body.subscriptionId();
    }

    /**
     * Returns the state as of the last read.
     *
     * @return the state
     */
    public SubscriptionState state() {
        return body.state() == null ? SubscriptionState.UNKNOWN : body.state();
    }

    /**
     * Returns the display name.
     *
     * @return the name, or {@code null}
     */
    public String name() {
        return body.name();
    }

    /**
     * Returns the requirement the subscription follows.
     *
     * @return the requirement, or empty if the subscription follows a signal
     */
    public Optional<CapacityRequirement> requirement() {
        return Optional.ofNullable(body.requirement());
    }

    /**
     * Returns the signal the subscription follows.
     *
     * @return the signal identifier, or empty if the subscription follows a requirement
     */
    public Optional<String> signalId() {
        return Optional.ofNullable(body.signalId());
    }

    /**
     * Returns which changes produce an event.
     *
     * @return the trigger, or {@code GRADE_CHANGE} if the provider returned none
     */
    public NotifyOn notifyOn() {
        return body.notifyOn() == null ? NotifyOn.GRADE_CHANGE : body.notifyOn();
    }

    /**
     * Returns how events are delivered.
     *
     * @return the delivery settings
     */
    public Delivery delivery() {
        return body.delivery();
    }

    /**
     * Says whether you paused the subscription.
     *
     * @return {@code true} if it is paused
     */
    public boolean isPaused() {
        return Boolean.TRUE.equals(body.paused());
    }

    /**
     * Returns when the subscription was created.
     *
     * @return the creation time
     */
    public Instant createdAt() {
        return body.createdAt();
    }

    /**
     * Returns when the subscription last changed.
     *
     * @return the time of the last change
     */
    public Instant updatedAt() {
        return body.updatedAt();
    }

    /**
     * Returns when the subscription expires.
     *
     * @return the expiry time, or {@code null}
     */
    public Instant expiresAt() {
        return body.expiresAt();
    }

    /**
     * Returns the sequence of the most recent event as of the last read.
     *
     * @return the sequence, or 0 if there is no event yet
     */
    public long lastSequence() {
        return body.lastSequence() == null ? 0 : body.lastSequence();
    }

    /**
     * Returns the secret that signs webhook deliveries. The provider returns it once, in the
     * response to the create. Store it; it cannot be read again.
     *
     * @return the secret, or empty on any object that did not come from
     *     {@link SubscriptionBuilder#create()}
     */
    public Optional<String> signingSecret() {
        return Optional.ofNullable(signingSecret);
    }

    /**
     * Says whether {@link #delete()} was called on this object.
     *
     * @return {@code true} after a delete
     */
    public boolean isDeleted() {
        return deleted;
    }

    /**
     * Reads the subscription again.
     *
     * @return this subscription with its current state
     * @throws OpenCapacityException with status 404 if the subscription no longer exists
     */
    public Subscription refresh() {
        replace(transport.call("GetSubscription", self(), null, null, Map.of()));
        return this;
    }

    /**
     * Starts an amend. Nothing is sent until you call {@link SubscriptionUpdater#save()}.
     *
     * @return an updater that starts from the current values
     */
    public SubscriptionUpdater update() {
        return new SubscriptionUpdater(this, body);
    }

    /**
     * Stops delivery without deleting the subscription.
     *
     * @return this subscription in its new state
     * @throws OpenCapacityException if the request fails
     */
    public Subscription pause() {
        return update().paused(true).save();
    }

    /**
     * Starts delivery again after a pause.
     *
     * @return this subscription in its new state
     * @throws OpenCapacityException if the request fails
     */
    public Subscription resume() {
        return update().paused(false).save();
    }

    /**
     * Deletes the subscription. The provider sends no further events. The object keeps its data.
     *
     * @throws OpenCapacityException if the request fails
     */
    public void delete() {
        transport.call("DeleteSubscription", self(), null, null, Map.of());
        deleted = true;
    }

    /**
     * Reads one page of events in sequence order. Available for every delivery type.
     *
     * @param afterSequence return events with a sequence greater than this value; 0 for the first
     * @param limit the most events to return, 1 to 200
     * @return the events, which is empty when there is none after {@code afterSequence}
     * @throws OpenCapacityException if the request fails
     */
    public List<Event> events(long afterSequence, int limit) {
        String query = "after=" + afterSequence + "&limit=" + limit;
        Transport.Reply reply = transport.call("ListSubscriptionEvents", self(), query, null, Map.of());
        Wire.Page<Event> page = transport.read(reply, Transport.type(Wire.Page.class, Event.class));
        return List.copyOf(page.items());
    }

    /**
     * Reads events as the provider sends them, over one open connection. The call blocks. It
     * returns when {@code handler} returns {@code false} or when the provider closes the stream. To
     * continue after either, call it again with the sequence of the last event you handled.
     *
     * @param afterSequence handle events with a sequence greater than this value; 0 for the first
     * @param handler receives each event in sequence order, and returns {@code false} to stop
     * @return the sequence of the last event handled, or {@code afterSequence} if there was none
     * @throws OpenCapacityException if the request fails or the connection breaks
     */
    public long stream(long afterSequence, Predicate<Event> handler) {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Accept", "text/event-stream");
        if (afterSequence > 0) {
            headers.put("Last-Event-ID", Long.toString(afterSequence));
        }
        long last = afterSequence;
        try (InputStream in = transport.stream("ListSubscriptionEvents", self(), headers);
                BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            StringBuilder data = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("data:")) {
                    data.append(line.substring(5).stripLeading());
                } else if (line.isEmpty() && data.length() > 0) {
                    Event event = Events.parse(data.toString());
                    data.setLength(0);
                    last = Math.max(last, event.sequence());
                    if (!handler.test(event)) {
                        return last;
                    }
                }
            }
            return last;
        } catch (IOException broken) {
            throw new OpenCapacityException(
                    "The event stream broke after sequence " + last + ". Call stream(" + last + ", handler) to continue.",
                    broken);
        }
    }
}
