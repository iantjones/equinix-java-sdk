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

import api.equinix.javasdk.customerportal.enums.ReportStatus;
import api.equinix.javasdk.customerportal.enums.FileType;

import java.util.List;
import java.util.Map;

/**
 * A generated report in the Report Center (Reports v1 {@code downloadable-report} for the list view,
 * {@code report} for the detail view).
 *
 * <p>The list view ({@code getReports}) populates the {@code downloadable-report} fields; the detail
 * view ({@code getReportById}) additionally populates the richer {@code report} fields
 * ({@code createdBy}, {@code createdDate}, {@code startTime}, {@code endTime}, {@code errorMessage},
 * {@code location}, {@code numberOfAttempts}, {@code parameters}). Fields not present in a given
 * response are {@code null}.</p>
 */
public interface Report {

    /**
     * Returns the unique identifier of the report.
     *
     * @return the report id (UUID)
     */
    String getReportId();

    /**
     * Returns the scheduled report id this report was generated from.
     *
     * @return the scheduled id, or {@code null} if not provided
     */
    String getScheduledId();

    /**
     * Returns the report name.
     *
     * @return the report name, or {@code null} if not provided
     */
    String getReportName();

    /**
     * Returns the file name of the report.
     *
     * @return the file name, or {@code null} if not provided
     */
    String getFileName();

    /**
     * Returns the file type of the report.
     *
     * @return the file type, or {@code null} if not provided
     */
    FileType getFileType();

    /**
     * Returns the size of the report in bytes.
     *
     * @return the file size, or {@code null} if not provided
     */
    Long getFileSize();

    /**
     * Returns the person/entity this report was created for.
     *
     * @return the createdFor value, or {@code null} if not provided
     */
    String getCreatedFor();

    /**
     * Returns the time this report was first requested.
     *
     * @return the requested date, or {@code null} if not provided
     */
    String getRequestedDate();

    /**
     * Returns the time this report was generated.
     *
     * @return the generated date, or {@code null} if not provided
     */
    String getGeneratedDate();

    /**
     * Returns the report status.
     *
     * @return the status
     */
    ReportStatus getStatus();

    /**
     * Returns the number of times this report has been downloaded.
     *
     * @return the download count, or {@code null} if not provided
     */
    Integer getNumberOfDownloads();

    /**
     * Returns the person/entity who created this report (detail view only).
     *
     * @return the creator, or {@code null} if not provided
     */
    String getCreatedBy();

    /**
     * Returns the time this report was first created (detail view only).
     *
     * @return the created date, or {@code null} if not provided
     */
    String getCreatedDate();

    /**
     * Returns the processing start time of the report (detail view only).
     *
     * @return the start time, or {@code null} if not provided
     */
    String getStartTime();

    /**
     * Returns the processing end time of the report (detail view only).
     *
     * @return the end time, or {@code null} if not provided
     */
    String getEndTime();

    /**
     * Returns an error message if report generation failed (detail view only).
     *
     * @return the error message, or {@code null} if not provided
     */
    String getErrorMessage();

    /**
     * Returns the location of the report once generated (detail view only).
     *
     * @return the location, or {@code null} if not provided
     */
    String getLocation();

    /**
     * Returns the number of attempts made to generate the report (detail view only).
     *
     * @return the attempt count, or {@code null} if not provided
     */
    Integer getNumberOfAttempts();

    /**
     * Returns the person/entity who accessed this report last (detail view only).
     *
     * @return the last accessor, or {@code null} if not provided
     */
    String getLastAccessedBy();

    /**
     * Returns the time this report was last accessed (detail view only).
     *
     * @return the last accessed date, or {@code null} if not provided
     */
    String getLastAccessedDate();

    /**
     * Returns the report publish data as a free-form JSON object (detail view only). The Reports
     * v1 spec declares no concrete properties for this object.
     *
     * @return the publisher info, or {@code null} if not provided
     */
    Map<String, Object> getPublisherInfo();

    /**
     * Returns the parameters used to generate the report (detail view only).
     *
     * @return the parameters, or {@code null} if not provided
     */
    List<? extends ReportParameter> getParameters();
}
