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

import com.eqixiac.equinix.core.internal.MonotonicClock;
import lombok.Builder;
import lombok.Value;

import java.time.Clock;
import java.time.Duration;

/**
 * How a plan is executed or reconciled.
 *
 * <pre>{@code
 * ExecutionOptions options = ExecutionOptions.builder()
 *     .failurePolicy(FailurePolicy.CONTINUE_ON_ERROR)
 *     .provisionTimeout(Duration.ofMinutes(15))
 *     .build();
 * }</pre>
 */
@Value
@Builder(toBuilder = true)
public class ExecutionOptions {

    /** What to do when a step fails. Default {@link FailurePolicy#ABORT_AND_ROLLBACK}. */
    @Builder.Default
    FailurePolicy failurePolicy = FailurePolicy.ABORT_AND_ROLLBACK;

    /**
     * Send every create as a {@code dryRun=true} request where the API offers one (Cloud Routers and
     * connections) and create nothing. A connection whose Cloud Router does not exist yet cannot be
     * dry-run and is reported {@code DRY_RUN_DEFERRED}; routing protocols have no dry-run in the API
     * and are reported the same way. No checkpoint is written, and the failure policy does not stop a
     * dry run: every step is attempted and a rejection is reported as {@code FAILED}. Default
     * {@code false}.
     */
    @Builder.Default
    boolean dryRun = false;

    /**
     * Reconcile only: patch resources that exist but differ from the plan, limited to the fields the
     * API can patch (Cloud Router package and notifications; connection name, bandwidth and
     * notifications). Every other difference is reported, never forced. Default {@code false}.
     */
    @Builder.Default
    boolean applyUpdates = false;

    /** How long to wait for a created resource to reach {@code PROVISIONED}. Default 5 minutes. */
    @Builder.Default
    Duration provisionTimeout = Duration.ofMinutes(5);

    /**
     * How long a rollback waits for each asynchronous delete to reach {@code DEPROVISIONED} or HTTP 404
     * before reporting the resource as an orphan. Default 10 minutes.
     */
    @Builder.Default
    Duration deleteTimeout = Duration.ofMinutes(10);

    /** Delay between state polls. Default 5 seconds; {@link Duration#ZERO} polls without sleeping. */
    @Builder.Default
    Duration pollInterval = Duration.ofSeconds(5);

    /** The clock timeouts are measured on. Default a monotonic clock; tests inject their own. */
    @Builder.Default
    Clock clock = MonotonicClock.get();

    /**
     * The defaults: abort and roll back on failure, real creates, no updates, 5-minute provisioning
     * and 10-minute delete timeouts, 5-second polls.
     *
     * @return default options
     */
    public static ExecutionOptions defaults() {
        return ExecutionOptions.builder().build();
    }
}
