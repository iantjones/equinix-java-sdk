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
import com.eqixiac.opencapacity.model.SignalState;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Standing signals of need. A signal tells the provider about a requirement so that the provider
 * can plan for it. You can amend, withdraw, and delete it.
 */
public final class Signals {

    private final Transport transport;

    Signals(Transport transport) {
        this.transport = transport;
    }

    /**
     * Starts a new signal. Nothing is sent until you call {@link SignalBuilder#create()}.
     *
     * @param requirement the requirement to record
     * @return a builder
     */
    public SignalBuilder define(CapacityRequirement requirement) {
        return new SignalBuilder(transport, Objects.requireNonNull(requirement, "requirement"));
    }

    /**
     * Reads one signal.
     *
     * @param signalId the identifier
     * @return the signal
     * @throws OpenCapacityException with status 404 if no such signal exists
     */
    public Signal get(String signalId) {
        Objects.requireNonNull(signalId, "signalId");
        Transport.Reply reply = transport.send("GET", "/signals/" + Transport.encode(signalId), null, Map.of(), true);
        return new Signal(transport, transport.read(reply, Transport.type(Wire.SignalBody.class)), reply.etag());
    }

    /**
     * Lists your signals in every state.
     *
     * @return the signals, across all pages
     * @throws OpenCapacityException if a request fails
     */
    public List<Signal> list() {
        return list(null);
    }

    /**
     * Lists your signals in one state.
     *
     * @param state the state, or {@code null} for every state
     * @return the signals, across all pages
     * @throws OpenCapacityException if a request fails
     */
    public List<Signal> list(SignalState state) {
        String query = "/signals?limit=200" + (state == null ? "" : "&state=" + state.name());
        List<Signal> all = new ArrayList<>();
        String cursor = null;
        do {
            String path = cursor == null ? query : query + "&cursor=" + Transport.encode(cursor);
            Transport.Reply reply = transport.send("GET", path, null, Map.of(), true);
            Wire.Page<Wire.SignalBody> page =
                    transport.read(reply, Transport.type(Wire.Page.class, Wire.SignalBody.class));
            for (Wire.SignalBody body : page.items()) {
                all.add(new Signal(transport, body, null));
            }
            cursor = page.next();
        } while (cursor != null);
        return List.copyOf(all);
    }
}
