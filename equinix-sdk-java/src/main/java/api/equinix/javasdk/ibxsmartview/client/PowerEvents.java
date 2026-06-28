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

package api.equinix.javasdk.ibxsmartview.client;

import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.ibxsmartview.model.PowerAlertConfiguration;
import api.equinix.javasdk.ibxsmartview.model.PowerEvent;
import api.equinix.javasdk.ibxsmartview.model.json.creators.PowerAlertConfigurationOperator;

import java.util.List;

/**
 * Client interface for the IBX SmartView power-events API ({@code /dcim/v3/powerEvents}). Provides
 * methods to search power events raised against monitored power assets and to manage power alert
 * configurations, including create, search, update, pause, resume, and delete operations.
 */
public interface PowerEvents {

    /**
     * Searches power events matching the specified filters.
     *
     * @param ibx the list of IBX codes to filter by, or {@code null} for all
     * @param status the list of statuses to filter by (e.g. {@code ACTIVE}, {@code INACTIVE}), or {@code null} for all
     * @param edgeCollectedOn an ISO-8601 date to retrieve events collected after a specific date, or {@code null} for all
     * @param offset the zero-based record offset for pagination
     * @param limit the maximum number of records to retrieve per request
     * @return a paginated list of matching power events
     */
    PaginatedList<PowerEvent> search(List<String> ibx, List<String> status, String edgeCollectedOn, int offset, int limit);

    /**
     * Returns a builder for defining and creating a new power alert configuration.
     *
     * @return a power alert configuration builder
     */
    PowerAlertConfigurationOperator.PowerAlertConfigurationBuilder defineAlertConfiguration();

    /**
     * Searches power alert configurations matching the specified filters.
     *
     * @param ibx the list of IBX codes to filter by, or {@code null} for all
     * @param state the list of states to filter by (e.g. {@code ACTIVE}, {@code PAUSED}, {@code DELETED}), or {@code null} for all
     * @param offset the zero-based record offset for pagination
     * @param limit the maximum number of records to retrieve per request
     * @return a paginated list of matching power alert configurations
     */
    PaginatedList<PowerAlertConfiguration> searchAlertConfigurations(List<String> ibx, List<String> state, int offset, int limit);

    /**
     * Returns a builder for updating an existing power alert configuration. The
     * {@code alertConfigurationUid} identifies the configuration to update.
     *
     * @param alertConfigurationUid the unique identifier of the power alert configuration to update
     * @return a power alert configuration update builder
     */
    PowerAlertConfigurationOperator.PowerAlertConfigurationUpdateBuilder updateAlertConfiguration(String alertConfigurationUid);

    /**
     * Pauses an active power alert configuration so that it stops triggering new alerts until resumed.
     *
     * @param alertConfigurationUid the unique identifier of the power alert configuration to pause
     */
    void pauseAlertConfiguration(String alertConfigurationUid);

    /**
     * Resumes a previously paused power alert configuration, returning it to the {@code ACTIVE} state.
     *
     * @param alertConfigurationUid the unique identifier of the power alert configuration to resume
     */
    void resumeAlertConfiguration(String alertConfigurationUid);

    /**
     * Permanently deletes a power alert configuration. Deleted configurations cannot be recovered.
     *
     * @param alertConfigurationUid the unique identifier of the power alert configuration to delete
     */
    void deleteAlertConfiguration(String alertConfigurationUid);
}
