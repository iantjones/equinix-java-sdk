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

import java.time.Instant;
import java.util.List;

/**
 * Body of an IBX (data center) or network maintenance/incident notification. One shape serves the
 * spec's {@code IBXNotificationV1Body}, {@code IBXNotificationV2Body},
 * {@code NetworkNotificationV1Body} and {@code NetworkNotificationV2Body}, which differ only in that
 * the network schemas omit {@code Assets} (the Notification Types guide documents {@code Assets} for
 * network notifications too, so it is read when present).
 *
 * @param requestorId   customer reference number (if any)
 * @param servicerId    the Equinix ticket id for the maintenance/incident
 * @param state         ticket state
 * @param description   summary from the Equinix technician
 * @param sequence      incrementing number across related notifications, starting at 0 (the spec
 *                      says integer; the documented samples send a string such as {@code "2"}, which
 *                      is accepted)
 * @param startDateTime start of the maintenance
 * @param endDateTime   end of the maintenance; for incidents and advisories only populated once the
 *                      state is {@link TicketState#COMPLETED}
 * @param accountNumber the customer account the activity relates to
 * @param impact        whether the activity is service-impacting
 * @param prodId        link to more information on the activity (wire {@code ProdID})
 * @param organizer     maintenance organizer
 * @param assets        affected customer assets, never {@code null}
 */
public record TicketBody(String requestorId, String servicerId, TicketState state, String description,
                         Integer sequence, Instant startDateTime, Instant endDateTime, String accountNumber,
                         ServiceImpact impact, String prodId, String organizer, List<Asset> assets) {

    /** Normalises {@code assets} to an unmodifiable list without {@code null} elements. */
    public TicketBody {
        assets = Wire.copy(assets);
    }

    /**
     * Whether Equinix flagged the activity as {@link ServiceImpact#SERVICE_IMPACTING}. Absent or
     * unrecognised impact values return {@code false}; inspect {@link #impact()} to tell them apart.
     *
     * @return {@code true} only for an explicit service-impacting flag
     */
    public boolean isServiceImpacting() {
        return impact == ServiceImpact.SERVICE_IMPACTING;
    }

    /**
     * An affected customer asset.
     *
     * @param ibx          IBX (data center) code
     * @param cage         cage
     * @param cabinet      cabinet
     * @param serialNumber serial number
     * @param circuitType  circuit type
     */
    public record Asset(String ibx, String cage, String cabinet, String serialNumber, String circuitType) {
    }
}
