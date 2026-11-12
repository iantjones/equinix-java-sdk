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

/** What execution or reconcile did with one plan step. */
public enum StepAction {
    /** Created by this run. */
    CREATED,
    /** Already created by an earlier run of the same plan, according to the checkpoint. */
    ALREADY_DONE,
    /** Found live (by its marker or name) and taken over without creating anything. */
    ADOPTED,
    /** Present live and differing; the patchable fields were updated (opt-in). */
    UPDATED,
    /** Present live and differing; reported only (updates not enabled, or the field cannot be patched). */
    DRIFT_REPORTED,
    /** Dry-run mode: the API accepted the step's dry-run request. Nothing was created. */
    DRY_RUN_PASSED,
    /** Dry-run mode: the step could not be dry-run now (its endpoint does not exist yet, or the API has no dry-run). */
    DRY_RUN_DEFERRED,
    /** Not attempted because a step it depends on failed or was not created. */
    SKIPPED,
    /** The step failed; see the outcome's errors. */
    FAILED,
    /** The element cannot be created through the API (Interconnect, native multicloud link). */
    NOT_EXECUTABLE
}
