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

/**
 * What execution does when a create step fails: a Cloud Router, connection or routing-protocol
 * create that is rejected, a pre-flight dry-run that is rejected (HTTP 4xx validation), or a
 * resource that lands in a terminal failure state or does not reach {@code PROVISIONED} within the
 * timeout. The same policy applies to every step kind; nothing is silently skipped.
 */
public enum FailurePolicy {
    /**
     * The default. Stop at the first failed step and delete everything this run created, newest
     * first, waiting for each connection delete to finish before deleting the Cloud Router it
     * attaches to. Whatever cannot be deleted is reported as an orphan with its uuid. Resources that
     * existed before the run (adopted or recorded in a checkpoint) are never deleted.
     */
    ABORT_AND_ROLLBACK,
    /**
     * Record the failure and carry on with every step that does not depend on the failed one.
     * Dependents of a failed step are reported as {@code SKIPPED} with the reason; nothing is rolled
     * back, and the outcome reports the partial state so a later reconcile can finish it.
     */
    CONTINUE_ON_ERROR
}
