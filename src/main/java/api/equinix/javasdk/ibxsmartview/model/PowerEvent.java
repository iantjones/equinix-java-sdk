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

package api.equinix.javasdk.ibxsmartview.model;

import api.equinix.javasdk.ibxsmartview.enums.AlertStatus;
import api.equinix.javasdk.ibxsmartview.model.implementation.PowerEventAsset;
import api.equinix.javasdk.ibxsmartview.model.implementation.PowerEventProcessing;

/**
 * Read-only model representing a single IBX SmartView power event. Power events are raised
 * when a monitored power asset (cage, cabinet, or circuit) crosses a configured threshold,
 * for example when a cage draw exceeds a percentage of usable kVA.
 */
public interface PowerEvent {

    /**
     * Returns the database identifier of the power event.
     *
     * @return the numeric identifier of the power event
     */
    Long getId();

    /**
     * Returns the unique identifier of the power event.
     *
     * @return the alert UID of the power event
     */
    String getAlertUid();

    /**
     * Returns the trace identifier related to this power event's status change.
     *
     * @return the trace UID of the power event
     */
    String getTraceUid();

    /**
     * Returns the current status of the power event (e.g. {@code ACTIVE} or {@code INACTIVE}).
     *
     * @return the status of the power event
     */
    AlertStatus getStatus();

    /**
     * Returns the asset associated with this power event.
     *
     * @return the power event asset
     */
    PowerEventAsset getAsset();

    /**
     * Returns the processing-time information associated with this power event.
     *
     * @return the power event processing information
     */
    PowerEventProcessing getActiveProcessing();

    /**
     * Returns the category of the power event (e.g. {@code POWER}).
     *
     * @return the category of the power event
     */
    String getCategory();

    /**
     * Returns the type of power event (e.g. {@code CAGE_DRAW}, {@code CABINET_DRAW_TO_USABLE_KVA}).
     *
     * @return the event type
     */
    String getEventType();

    /**
     * Returns the condition type that triggered the event (e.g. {@code EXCEEDS}, {@code FALLS_BELOW}).
     *
     * @return the condition type
     */
    String getConditionType();

    /**
     * Returns the configured threshold value that triggers the event.
     *
     * @return the trigger value
     */
    String getTriggerValue();

    /**
     * Returns the actual value observed at the time the event was triggered.
     *
     * @return the current value
     */
    String getCurrentValue();

    /**
     * Returns the customer account number associated with the event.
     *
     * @return the account number
     */
    String getAccountNo();
}
