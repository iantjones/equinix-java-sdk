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

import com.eqixiac.opencapacity.model.CapacityAnswer;
import com.eqixiac.opencapacity.model.CapacityRequirement;
import com.eqixiac.opencapacity.model.CloseReason;
import com.eqixiac.opencapacity.model.Confidence;
import com.eqixiac.opencapacity.model.Confidentiality;
import com.eqixiac.opencapacity.model.Contact;
import com.eqixiac.opencapacity.model.SignalState;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

/**
 * A standing signal of need that a provider holds. The object keeps a reference to its client:
 * {@link #update()}, {@link #withdraw()}, {@link #delete()}, and {@link #refresh()} act on the
 * signal it represents.
 *
 * <p>The object is not thread-safe.
 */
public final class Signal {

    private final Transport transport;
    private Wire.SignalBody body;
    private String etag;
    private boolean deleted;

    Signal(Transport transport, Wire.SignalBody body, String etag) {
        this.transport = transport;
        this.body = body;
        this.etag = etag;
    }

    private String path() {
        return "/signals/" + Transport.encode(body.signalId());
    }

    void replace(Transport.Reply reply) {
        this.body = transport.read(reply, Transport.type(Wire.SignalBody.class));
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
     * @return the provider's identifier of the signal
     */
    public String id() {
        return body.signalId();
    }

    /**
     * Returns the state as of the last read.
     *
     * @return the state
     */
    public SignalState state() {
        return body.state() == null ? SignalState.UNKNOWN : body.state();
    }

    /**
     * Says whether the signal is an active need.
     *
     * @return {@code true} in state {@code RECEIVED} or {@code ACKNOWLEDGED}
     */
    public boolean isActive() {
        return state() == SignalState.RECEIVED || state() == SignalState.ACKNOWLEDGED;
    }

    /**
     * Returns the revision, which increases by one on each amend.
     *
     * @return the revision
     */
    public int revision() {
        return body.revision() == null ? 0 : body.revision();
    }

    /**
     * Returns when the signal was created.
     *
     * @return the creation time
     */
    public Instant createdAt() {
        return body.createdAt();
    }

    /**
     * Returns when the signal last changed.
     *
     * @return the time of the last change
     */
    public Instant updatedAt() {
        return body.updatedAt();
    }

    /**
     * Returns when the signal expires.
     *
     * @return the expiry time
     */
    public Instant expiresAt() {
        return body.expiresAt();
    }

    /**
     * Returns your reference.
     *
     * @return the reference, or {@code null}
     */
    public String reference() {
        return body.reference();
    }

    /**
     * Returns how firm the need is.
     *
     * @return the confidence, or {@code null}
     */
    public Confidence confidence() {
        return body.confidence();
    }

    /**
     * Returns what the provider can do with the signal.
     *
     * @return the confidentiality settings, or {@code null} for the most restrictive defaults
     */
    public Confidentiality confidentiality() {
        return body.confidentiality();
    }

    /**
     * Returns whom the provider can contact.
     *
     * @return the contact, or {@code null}
     */
    public Contact contact() {
        return body.contact();
    }

    /**
     * Returns the requirement.
     *
     * @return the requirement
     */
    public CapacityRequirement requirement() {
        return body.requirement();
    }

    /**
     * Returns why the provider closed the signal.
     *
     * @return the reason, or empty if the signal is not closed
     */
    public Optional<CloseReason> closeReason() {
        return Optional.ofNullable(body.closeReason());
    }

    /**
     * Returns the provider's current answer to the signal.
     *
     * @return the answer, or empty if the provider gives none
     */
    public Optional<CapacityAnswer> answer() {
        return Optional.ofNullable(body.answer());
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
     * Reads the signal again.
     *
     * @return this signal with its current state
     * @throws OpenCapacityException with status 404 if the signal no longer exists
     */
    public Signal refresh() {
        replace(transport.send("GET", path(), null, Map.of(), true));
        return this;
    }

    /**
     * Starts an amend. Nothing is sent until you call {@link SignalUpdater#save()}.
     *
     * @return an updater that starts from the current values
     */
    public SignalUpdater update() {
        return new SignalUpdater(this, body);
    }

    /**
     * Withdraws the signal. It is no longer an active need and stays readable in state
     * {@code WITHDRAWN}. An amend makes it active again.
     *
     * @return this signal in its new state
     * @throws OpenCapacityException with status 409 if the signal is closed or expired
     */
    public Signal withdraw() {
        replace(transport.send("POST", path() + "/withdraw", null, Map.of(), true));
        return this;
    }

    /**
     * Deletes the signal from the provider's API. The object keeps its data.
     *
     * @throws OpenCapacityException if the request fails
     */
    public void delete() {
        transport.send("DELETE", path(), null, Map.of(), true);
        deleted = true;
    }
}
