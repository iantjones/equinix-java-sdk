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
import java.util.UUID;

/** Builds a new signal. Returned by {@link Signals#define(CapacityRequirement)}. */
public final class SignalBuilder {

    private final Transport transport;
    private final CapacityRequirement requirement;
    private String reference;
    private Confidence confidence;
    private Sharing sharing;
    private Contact contact;
    private Instant expiresAt;

    SignalBuilder(Transport transport, CapacityRequirement requirement) {
        this.transport = transport;
        this.requirement = requirement;
    }

    /**
     * Sets your own reference for the signal.
     *
     * @param value the reference
     * @return this builder
     */
    public SignalBuilder reference(String value) {
        this.reference = value;
        return this;
    }

    /**
     * Says how firm the need is.
     *
     * @param value the confidence
     * @return this builder
     */
    public SignalBuilder confidence(Confidence value) {
        this.confidence = value;
        return this;
    }

    /**
     * Says whether the provider can count the signal in totals that do not identify you. Without
     * this call the provider uses the signal for its own planning only.
     *
     * @param value the sharing setting
     * @return this builder
     */
    public SignalBuilder sharing(Sharing value) {
        this.sharing = value;
        return this;
    }

    /**
     * Permits the provider to contact you about the signal, and says whom. Without this call the
     * provider is not permitted to contact you.
     *
     * @param name the name of the person or team, or {@code null}
     * @param email the email address
     * @return this builder
     */
    public SignalBuilder contact(String name, String email) {
        this.contact = new Contact(name, email);
        return this;
    }

    /**
     * Sets when the signal expires. Without this call the provider's maximum lifetime applies.
     *
     * @param value the expiry time
     * @return this builder
     */
    public SignalBuilder expiresAt(Instant value) {
        this.expiresAt = value;
        return this;
    }

    /**
     * Sends the signal to the provider.
     *
     * @return the stored signal, in state {@code RECEIVED}
     * @throws OpenCapacityException if the request fails
     */
    public Signal create() {
        Confidentiality confidentiality = sharing == null && contact == null
                ? null
                : new Confidentiality(sharing, contact != null ? Boolean.TRUE : null);
        Wire.SignalBody body = Wire.SignalBody.request(
                reference, confidence, confidentiality, contact, requirement, expiresAt);
        Transport.Reply reply = transport.send(
                "POST", "/signals", body, Map.of("Idempotency-Key", UUID.randomUUID().toString()), true);
        return new Signal(transport, transport.read(reply, Transport.type(Wire.SignalBody.class)), reply.etag());
    }
}
