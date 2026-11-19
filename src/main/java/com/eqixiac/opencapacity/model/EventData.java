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

package com.eqixiac.opencapacity.model;

/**
 * The payload of an {@link Event}. Which components are present depends on the event type.
 *
 * @param subscriptionId the subscription the event belongs to
 * @param sequence the position of the event within the subscription, starting at 1
 * @param reason why an answer event was sent, or {@code null}
 * @param answer the latest answer, for an answer event
 * @param signalId the signal, for a signal event
 * @param signalState the new state of the signal, for a signal event
 * @param subscriptionState the new state of the subscription, for a subscription event
 * @param challenge the value to echo, for a verification event
 */
public record EventData(
        String subscriptionId, Long sequence, EventReason reason, CapacityAnswer answer, String signalId,
        SignalState signalState, SubscriptionState subscriptionState, String challenge) {
}
