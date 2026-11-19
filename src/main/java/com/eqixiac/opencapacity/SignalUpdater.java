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
import com.eqixiac.opencapacity.model.Confidence;
import com.eqixiac.opencapacity.model.Confidentiality;
import com.eqixiac.opencapacity.model.Contact;
import com.eqixiac.opencapacity.model.Sharing;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * Amends a signal. Returned by {@link Signal#update()}. The updater starts from the current values
 * and {@link #save()} sends the whole signal, as the specification requires. The request carries
 * the version of the signal that this object last read, so a change made elsewhere in between is
 * not overwritten: the provider answers 412 and you call {@link Signal#refresh()} and repeat.
 */
public final class SignalUpdater {

    private final Signal signal;
    private CapacityRequirement requirement;
    private String reference;
    private Confidence confidence;
    private Sharing sharing;
    private Contact contact;
    private Instant expiresAt;

    SignalUpdater(Signal signal, Wire.SignalBody current) {
        this.signal = signal;
        this.requirement = current.requirement();
        this.reference = current.reference();
        this.confidence = current.confidence();
        this.sharing = current.confidentiality() == null ? null : current.confidentiality().sharing();
        this.contact = current.contact();
        this.expiresAt = current.expiresAt();
    }

    /**
     * Replaces the requirement. To change part of it, start from
     * {@code signal.requirement().toBuilder()}.
     *
     * @param value the new requirement
     * @return this updater
     */
    public SignalUpdater requirement(CapacityRequirement value) {
        this.requirement = Objects.requireNonNull(value, "value");
        return this;
    }

    /**
     * Sets your reference.
     *
     * @param value the reference, or {@code null} to remove it
     * @return this updater
     */
    public SignalUpdater reference(String value) {
        this.reference = value;
        return this;
    }

    /**
     * Says how firm the need is.
     *
     * @param value the confidence
     * @return this updater
     */
    public SignalUpdater confidence(Confidence value) {
        this.confidence = value;
        return this;
    }

    /**
     * Says whether the provider can count the signal in totals that do not identify you.
     *
     * @param value the sharing setting
     * @return this updater
     */
    public SignalUpdater sharing(Sharing value) {
        this.sharing = value;
        return this;
    }

    /**
     * Permits the provider to contact you about the signal, and says whom.
     *
     * @param name the name of the person or team, or {@code null}
     * @param email the email address
     * @return this updater
     */
    public SignalUpdater contact(String name, String email) {
        this.contact = new Contact(name, email);
        return this;
    }

    /**
     * Withdraws the permission to contact you and removes the contact.
     *
     * @return this updater
     */
    public SignalUpdater noContact() {
        this.contact = null;
        return this;
    }

    /**
     * Sets when the signal expires.
     *
     * @param value the expiry time
     * @return this updater
     */
    public SignalUpdater expiresAt(Instant value) {
        this.expiresAt = value;
        return this;
    }

    /**
     * Sends the amended signal. The revision increases by one and the state returns to
     * {@code RECEIVED}. An amend of a withdrawn signal makes it active again.
     *
     * @return the signal with its new revision
     * @throws OpenCapacityException with status 412 if the signal changed since this object last
     *     read it, or 409 if the signal is closed or expired
     */
    public Signal save() {
        Confidentiality confidentiality = sharing == null && contact == null
                ? null
                : new Confidentiality(sharing, contact != null ? Boolean.TRUE : null);
        Wire.SignalBody body = Wire.SignalBody.request(
                reference, confidence, confidentiality, contact, requirement, expiresAt);
        String etag = signal.currentEtag();
        Map<String, String> headers = etag == null ? Map.of() : Map.of("If-Match", etag);
        signal.replace(signal.transport().call(
                "ReplaceSignal", Map.of("signalId", signal.id()), null, body, headers));
        return signal;
    }
}
