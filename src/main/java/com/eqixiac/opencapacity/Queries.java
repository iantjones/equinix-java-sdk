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
import java.util.Map;
import java.util.Objects;

/**
 * One-time capacity questions. A query creates nothing at the provider. To record a need, use
 * {@link Signals}.
 */
public final class Queries {

    private final Transport transport;

    Queries(Transport transport) {
        this.transport = transport;
    }

    /**
     * Asks whether a requirement can be met.
     *
     * @param requirement the requirement
     * @return the provider's graded answer, with one result per location
     * @throws OpenCapacityException if the request fails, for example with code
     *     {@code INCONSISTENT_DEMAND} or {@code QUOTA_EXCEEDED}
     */
    public CapacityAnswer ask(CapacityRequirement requirement) {
        return ask(requirement, null);
    }

    /**
     * Asks whether a requirement can be met, with your own reference.
     *
     * @param requirement the requirement
     * @param reference your reference, which the answer echoes, or {@code null}
     * @return the provider's graded answer
     * @throws OpenCapacityException if the request fails
     */
    public CapacityAnswer ask(CapacityRequirement requirement, String reference) {
        Objects.requireNonNull(requirement, "requirement");
        Transport.Reply reply = transport.send(
                "POST", "/queries", new Wire.Query(requirement, reference), Map.of(), true);
        return transport.read(reply, Transport.type(CapacityAnswer.class));
    }
}
