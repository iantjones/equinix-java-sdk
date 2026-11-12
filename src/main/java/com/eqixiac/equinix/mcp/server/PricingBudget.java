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


package com.eqixiac.equinix.mcp.server;

/**
 * The live-pricing time budget of one tool call: {@link ServerContext#pricingTimeoutMillis()}
 * milliseconds, shared by every pricing lookup the call makes (cloud provider price lists and the
 * Equinix rate card alike). The clock starts at the call's first pricing lookup, not at the start of
 * the call, so work that precedes pricing (an optimizer reading the Fabric catalogue for several
 * seconds) does not spend it. A lookup waits at most for what is left; once the budget is spent,
 * further live lookups are skipped and the rate-card chain falls through to reference figures. A
 * cancelled call has no budget left.
 */
public final class PricingBudget {

    private final long budgetMillis;
    private final CancellationSignal cancellation;
    /** Set at the first {@link #remainingMillis()} call; {@code null} until then. */
    private Long deadlineNanos;

    PricingBudget(long budgetMillis, CancellationSignal cancellation) {
        this.budgetMillis = budgetMillis;
        this.cancellation = cancellation == null ? CancellationSignal.none() : cancellation;
    }

    /**
     * @return the whole budget, in milliseconds
     */
    public long budgetMillis() {
        return budgetMillis;
    }

    /**
     * Milliseconds left. The first call starts the budget's clock.
     *
     * @return milliseconds left; zero when spent or when the call was cancelled
     */
    public long remainingMillis() {
        if (cancellation.isCancelled()) {
            return 0;
        }
        long deadline;
        synchronized (this) {
            if (deadlineNanos == null) {
                deadlineNanos = System.nanoTime() + budgetMillis * 1_000_000L;
            }
            deadline = deadlineNanos;
        }
        long remaining = (deadline - System.nanoTime()) / 1_000_000L;
        return Math.max(0, remaining);
    }

    /**
     * @return {@code true} when the call was cancelled
     */
    boolean cancelled() {
        return cancellation.isCancelled();
    }

    /**
     * @return the call's cancellation signal
     */
    CancellationSignal cancellation() {
        return cancellation;
    }
}
