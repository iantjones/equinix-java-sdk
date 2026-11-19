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
import com.eqixiac.opencapacity.model.Delivery;
import com.eqixiac.opencapacity.model.NotifyOn;
import com.eqixiac.opencapacity.model.SignalState;
import com.eqixiac.opencapacity.model.SubscriptionState;
import java.time.Instant;
import java.util.List;

/** JSON bodies that the public model objects wrap. Not part of the public API. */
final class Wire {

    private Wire() {
    }

    record Query(CapacityRequirement requirement, String reference) {
    }

    record SignalBody(
            String signalId, SignalState state, Integer revision, Instant createdAt, Instant updatedAt,
            Instant expiresAt, String reference, Confidence confidence, Confidentiality confidentiality,
            Contact contact, CapacityRequirement requirement, CloseReason closeReason, CapacityAnswer answer) {

        static SignalBody request(String reference, Confidence confidence, Confidentiality confidentiality,
                Contact contact, CapacityRequirement requirement, Instant expiresAt) {
            return new SignalBody(null, null, null, null, null, expiresAt, reference, confidence,
                    confidentiality, contact, requirement, null, null);
        }
    }

    record SubscriptionBody(
            String subscriptionId, SubscriptionState state, Instant createdAt, Instant updatedAt,
            Long lastSequence, String name, CapacityRequirement requirement, String signalId,
            NotifyOn notifyOn, Delivery delivery, Boolean paused, Instant expiresAt) {

        static SubscriptionBody request(String name, CapacityRequirement requirement, String signalId,
                NotifyOn notifyOn, Delivery delivery, Boolean paused, Instant expiresAt) {
            return new SubscriptionBody(null, null, null, null, null, name, requirement, signalId, notifyOn,
                    delivery, paused, expiresAt);
        }
    }

    record Pagination(Integer limit, String next) {
    }

    record Page<T>(List<T> data, Pagination pagination) {
        String next() {
            return pagination == null ? null : pagination.next();
        }

        List<T> items() {
            return data == null ? List.of() : data;
        }
    }
}
