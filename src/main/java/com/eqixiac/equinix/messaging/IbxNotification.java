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
 * IBX (data center) maintenance, incident, security-incident or advisory notification:
 * {@code Task.Resource} {@code DataCenter.Maintenance}, {@code DataCenter.Incident},
 * {@code DataCenter.SecurityIncident} or {@code DataCenter.Advisory}, schema version 1.0 or 2.0.
 *
 * @param header  common header
 * @param body    the ticket details
 * @param rawJson the envelope as received
 */
public record IbxNotification(EmgHeader header, TicketBody body, String rawJson) implements EmgMessage {

    /**
     * The event kind, derived from {@code Task.Resource}.
     *
     * @return the category, never {@code null}
     */
    public TicketCategory category() {
        return TicketCategory.fromResource(header.resource());
    }
}
