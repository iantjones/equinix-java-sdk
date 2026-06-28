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

package api.equinix.javasdk.customerportal.model;

import java.util.List;
import java.util.Map;

/**
 * A scheduled report definition in the Report Center.
 */
public interface ScheduledReport {

    /**
     * Returns the scheduled report id.
     *
     * @return the scheduled report id
     */
    String getScheduledId();

    /**
     * Returns the report name.
     *
     * @return the report name
     */
    String getName();

    /**
     * Returns the schedule type (e.g. {@code ONE_TIME}, {@code DAILY}, {@code WEEKLY}).
     *
     * @return the schedule type, or {@code null} if not provided
     */
    String getScheduleType();

    /**
     * Returns the report parameters.
     *
     * @return the parameters, or {@code null} if not provided
     */
    List<Map<String, Object>> getParameters();
}
