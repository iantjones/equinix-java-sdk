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


package com.eqixiac.equinix.design.plan;

import java.util.List;

/**
 * A Cloud Router as read from Fabric: the fields drift detection compares.
 *
 * @param uuid               the Fabric uuid
 * @param name               the router name
 * @param state              the state name (for example {@code PROVISIONED}); {@code null} when not returned
 * @param metroCode          the metro code
 * @param packageCode        the package code name
 * @param projectId          the project
 * @param notificationEmails every notification recipient, in returned order
 */
public record LiveRouter(String uuid, String name, String state, String metroCode, String packageCode,
                         String projectId, List<String> notificationEmails) {

    /** Makes the e-mail list immutable and non-null. */
    public LiveRouter {
        notificationEmails = notificationEmails == null ? List.of() : List.copyOf(notificationEmails);
    }

    /**
     * Whether the router is gone or going ({@code DEPROVISIONING}, {@code DEPROVISIONED}).
     *
     * @return {@code true} when it no longer counts as present
     */
    public boolean isGone() {
        return "DEPROVISIONED".equals(state) || "DEPROVISIONING".equals(state);
    }
}
