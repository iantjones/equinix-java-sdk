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

package com.eqixiac.equinix.customerportal.model;

import com.eqixiac.equinix.customerportal.enums.ReportScheduleStatus;
import com.eqixiac.equinix.customerportal.enums.ReportPeriod;
import com.eqixiac.equinix.customerportal.enums.ReportScheduleType;
import java.util.List;

/**
 * A scheduled report definition in the Report Center (Reports v1 {@code scheduledReport}).
 */
public interface ScheduledReport {

    /**
     * Returns the scheduled report id.
     *
     * @return the scheduled report id (UUID)
     */
    String getScheduledId();

    /**
     * Returns the report name.
     *
     * @return the report name
     */
    String getReportName();

    /**
     * Returns the schedule type (e.g. {@code DAILY}, {@code WEEKLY}, {@code MONTHLY}).
     *
     * @return the schedule type, or {@code null} if not provided
     */
    ReportScheduleType getScheduleType();

    /**
     * Returns the period (e.g. {@code 30_DAYS}, {@code 1_DAY}).
     *
     * @return the period, or {@code null} if not provided
     */
    ReportPeriod getPeriod();

    /**
     * Returns the person/entity who created this schedule.
     *
     * @return the creator, or {@code null} if not provided
     */
    String getCreatedBy();

    /**
     * Returns the time this schedule was first created.
     *
     * @return the created date, or {@code null} if not provided
     */
    String getCreatedDate();

    /**
     * Returns the last attempted date to trigger report generation.
     *
     * @return the last attempted date, or {@code null} if not provided
     */
    String getLastAttemptedDate();

    /**
     * Returns the customer organization id.
     *
     * @return the customer organization id, or {@code null} if not provided
     */
    Integer getCustomerOrganizationId();

    /**
     * Returns the customer org id the report is generated on behalf of.
     *
     * @return the forOrg value, or {@code null} if not provided
     */
    Integer getForOrg();

    /**
     * Returns the user key the report is generated on behalf of.
     *
     * @return the forUser value, or {@code null} if not provided
     */
    String getForUser();

    /**
     * Returns the person/entity who last modified this schedule.
     *
     * @return the last modifier, or {@code null} if not provided
     */
    String getLastModifiedBy();

    /**
     * Returns the last modified date of this schedule.
     *
     * @return the last modified date, or {@code null} if not provided
     */
    String getLastModifiedDate();

    /**
     * Returns the number of times report generation has failed.
     *
     * @return the failed-attempt count, or {@code null} if not provided
     */
    Integer getNumberOfFailedAttempts();

    /**
     * Returns the schedule status (e.g. {@code ACTIVE}, {@code INACTIVE}).
     *
     * @return the status, or {@code null} if not provided
     */
    ReportScheduleStatus getStatus();

    /**
     * Returns the parameters used to generate the reports.
     *
     * @return the parameters, or {@code null} if not provided
     */
    List<? extends ReportParameter> getParameters();

    /**
     * Returns the reports generated from this schedule.
     *
     * @return the generated reports, or {@code null} if not provided
     */
    List<? extends Report> getReports();
}
