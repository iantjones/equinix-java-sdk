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

package com.eqixiac.equinix.ibxsmartview.model;

import com.eqixiac.equinix.ibxsmartview.enums.AlertConfigurationState;
import com.eqixiac.equinix.ibxsmartview.model.implementation.PowerAlertCondition;
import com.eqixiac.equinix.ibxsmartview.model.implementation.PowerAlertConfigurationAsset;
import com.eqixiac.equinix.ibxsmartview.model.implementation.PowerAlertCreator;
import com.eqixiac.equinix.ibxsmartview.model.implementation.PowerAlertRecipient;

import java.util.List;
import java.util.Map;

/**
 * Read-only model representing a power alert configuration returned by the IBX SmartView
 * power-events API. A power alert configuration defines the condition (event type and
 * threshold) that triggers an alert, the recipients to notify, and the assets being
 * monitored, scoped by level type ({@code CAGE}, {@code CABINET}, or {@code CIRCUIT}).
 */
public interface PowerAlertConfiguration {

    /**
     * Returns the unique identifier of the alert configuration.
     *
     * @return the alert configuration UID
     */
    String getAlertConfigurationUid();

    /**
     * Returns the IBX code associated with the alert configuration.
     *
     * @return the IBX code
     */
    String getIbx();

    /**
     * Returns the current state of the alert configuration (e.g. {@code ACTIVE}, {@code PAUSED},
     * {@code DELETED}).
     *
     * @return the configuration state
     */
    AlertConfigurationState getState();

    /**
     * Returns the section type of the alert configuration (e.g. {@code POWER}).
     *
     * @return the section type
     */
    String getSection();

    /**
     * Returns the source system that created the configuration.
     *
     * @return the source
     */
    String getSource();

    /**
     * Returns the condition that triggers the alert.
     *
     * @return the alert condition
     */
    PowerAlertCondition getCondition();

    /**
     * Returns the list of configured notification recipients.
     *
     * @return the recipients
     */
    List<PowerAlertRecipient> getRecipients();

    /**
     * Returns information about the user who created the configuration.
     *
     * @return the creator information
     */
    PowerAlertCreator getCreator();

    /**
     * Returns the timestamp at which the configuration was created.
     *
     * @return the creation timestamp
     */
    String getCreatedOn();

    /**
     * Returns the timestamp at which the configuration was last updated.
     *
     * @return the last-updated timestamp
     */
    String getUpdatedOn();

    /**
     * Returns the assets being monitored, keyed by level type ({@code CAGE}, {@code CABINET},
     * {@code CIRCUIT}).
     *
     * @return the monitored assets keyed by level type
     */
    Map<String, List<PowerAlertConfigurationAsset>> getAssets();
}
