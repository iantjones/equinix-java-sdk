/*
 * Copyright 2021 Ian Jones. All Rights Reserved.
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

package com.eqixiac.equinix.messaging;

/**
 * Network maintenance or incident notification: {@code Task.Resource} {@code Network.Maintenance}
 * or {@code Network.Incident} (schema 1.0), or a ticket-shaped {@code DataCenter.Network.Port} /
 * {@code DataCenter.Network.VirtualConnection} notification (the spec maps those 2.0 resources to
 * {@code NetworkNotificationV2Body}; when their body is instead Fabric-shaped, the parser produces a
 * {@link FabricNotification}).
 *
 * @param header  common header
 * @param body    the ticket details
 * @param rawJson the envelope as received
 */
public record NetworkNotification(EmgHeader header, TicketBody body, String rawJson) implements EmgMessage {

    /**
     * The event kind, derived from {@code Task.Resource}.
     *
     * @return the category, never {@code null}
     */
    public TicketCategory category() {
        return TicketCategory.fromResource(header.resource());
    }
}
