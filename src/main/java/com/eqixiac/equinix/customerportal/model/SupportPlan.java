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

import com.eqixiac.equinix.customerportal.enums.PlanFrequency;
import com.eqixiac.equinix.customerportal.enums.SupportPlanStatus;
import com.eqixiac.equinix.customerportal.model.implementation.SupportPlanAssignment;

import java.util.List;

/**
 * A Smart Hands support plan retrieved from the Equinix Customer Portal Support Plans v2 API
 * ({@code GET /colocations/v2/supportPlans}).
 */
public interface SupportPlan {

    /**
     * Returns the unique identifier of the plan. May repeat across entries for rollover plans or
     * plan assignment.
     *
     * @return the plan id
     */
    String getId();

    /**
     * Returns the account number that owns the support plan.
     *
     * @return the account number
     */
    String getAccountNumber();

    /**
     * Returns the name of the support plan.
     *
     * @return the plan name
     */
    String getPlanName();

    /**
     * Returns the Equinix product catalog code for the plan.
     *
     * @return the product code
     */
    String getProductCode();

    /**
     * Returns the IBXs where the support plan can be used.
     *
     * @return the list of IBX codes, or {@code null} if not provided
     */
    List<String> getIbxs();

    /**
     * Indicates whether the plan can only be used on a specific IBX.
     *
     * @return {@code true} when IBX-specific
     */
    Boolean getIbxSpecific();

    /**
     * Returns the billing frequency of the plan.
     *
     * @return the plan frequency
     */
    PlanFrequency getPlanFrequency();

    /**
     * Returns the number of minutes purchased for the plan.
     *
     * @return the purchased minutes, or {@code null} if not provided
     */
    Integer getPurchasedMinutes();

    /**
     * Returns the number of minutes assigned for a pro-rated plan.
     *
     * @return the assigned minutes, or {@code null} if not provided
     */
    Integer getAssignedMinutes();

    /**
     * Returns the number of minutes consumed for the plan.
     *
     * @return the consumed minutes, or {@code null} if not provided
     */
    Integer getConsumedMinutes();

    /**
     * Returns the number of minutes remaining for the plan.
     *
     * @return the remaining minutes, or {@code null} if not provided
     */
    Integer getRemainingMinutes();

    /**
     * Returns the number of minutes consumed in the previous month (MONTHLY/MONTHLY_ROLLOVER only).
     *
     * @return the previous consumed minutes, or {@code null} if not provided
     */
    Integer getPreviousConsumedMinutes();

    /**
     * Returns the number of minutes consumed in the current month (MONTHLY/MONTHLY_ROLLOVER only).
     *
     * @return the current consumed minutes, or {@code null} if not provided
     */
    Integer getCurrentConsumedMinutes();

    /**
     * Returns the number of minutes consumed for prepaid (ANNUAL) plans.
     *
     * @return the prepaid consumed minutes, or {@code null} if not provided
     */
    Integer getPrepaidConsumedMinutes();

    /**
     * Returns the number of minutes assigned to an end customer (resellers only).
     *
     * @return the transition minutes, or {@code null} if not provided
     */
    Integer getTransitionMinutes();

    /**
     * Returns the plan start date in {@code YYYY-MM-DD} format.
     *
     * @return the start date
     */
    String getStartDate();

    /**
     * Returns the plan end date in {@code YYYY-MM-DD} format.
     *
     * @return the end date
     */
    String getEndDate();

    /**
     * Returns the status of the support plan.
     *
     * @return the plan status
     */
    SupportPlanStatus getStatus();

    /**
     * Returns the date and time the support plan was created, in ISO 8601 format.
     *
     * @return the created date-time
     */
    String getCreatedDateTime();

    /**
     * Returns the date and time the support plan was last updated, in ISO 8601 format.
     *
     * @return the updated date-time
     */
    String getUpdatedDateTime();

    /**
     * Returns the customer assignment details (resellers only).
     *
     * @return the assignment, or {@code null} if not assigned
     */
    SupportPlanAssignment getAssignment();
}
