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
import java.util.Map;

/**
 * A connection as read from Fabric: the fields drift detection compares.
 *
 * @param uuid               the Fabric uuid
 * @param name               the connection name
 * @param state              the state name; {@code null} when not returned
 * @param type               the connection type name (for example {@code IP_VC})
 * @param bandwidthMbps      the bandwidth, or {@code null}
 * @param aSideRouterUuid    the A-side Cloud Router uuid, or {@code null}
 * @param zSideRouterUuid    the Z-side Cloud Router uuid (backbone links), or {@code null}
 * @param zSideProfileUuid   the Z-side service-profile uuid (provider connections), or {@code null}
 * @param zSideSellerRegion  the Z-side seller region, or {@code null}
 * @param zSidePeeringType   the Z-side peering type name, or {@code null}
 * @param projectId          the project
 * @param additionalInfo     the top-level additionalInfo entries (the plan marker lives here)
 * @param notificationEmails every notification recipient
 */
public record LiveConnection(String uuid, String name, String state, String type, Integer bandwidthMbps,
                             String aSideRouterUuid, String zSideRouterUuid, String zSideProfileUuid,
                             String zSideSellerRegion, String zSidePeeringType, String projectId,
                             Map<String, String> additionalInfo, List<String> notificationEmails) {

    /** Makes the collections immutable and non-null. */
    public LiveConnection {
        additionalInfo = additionalInfo == null ? Map.of() : Map.copyOf(additionalInfo);
        notificationEmails = notificationEmails == null ? List.of() : List.copyOf(notificationEmails);
    }

    /**
     * Whether the connection is gone or going ({@code DEPROVISIONING}, {@code DEPROVISIONED},
     * {@code CANCELLED}).
     *
     * @return {@code true} when it no longer counts as present
     */
    public boolean isGone() {
        return "DEPROVISIONED".equals(state) || "DEPROVISIONING".equals(state) || "CANCELLED".equals(state);
    }
}
