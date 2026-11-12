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

import com.eqixiac.equinix.FabricGateway;
import com.eqixiac.equinix.core.enums.MetroCode;
import com.eqixiac.equinix.design.value.ratecard.ColocationItem;
import com.eqixiac.equinix.design.value.ratecard.EgressPath;
import com.eqixiac.equinix.design.value.ratecard.EgressRate;
import com.eqixiac.equinix.design.value.ratecard.EquinixRateCard;
import com.eqixiac.equinix.design.value.ratecard.MulticloudLinkQuote;
import com.eqixiac.equinix.design.value.ratecard.MulticloudLinkRequest;
import com.eqixiac.equinix.design.value.ratecard.PriceQuote;
import com.eqixiac.equinix.design.value.ratecard.PriceSource;
import com.eqixiac.equinix.design.value.ratecard.RateCard;
import com.eqixiac.equinix.design.value.ratecard.ReferenceRateCard;
import com.eqixiac.equinix.design.value.ratecard.Term;
import com.eqixiac.equinix.fabric.enums.ConnectionType;
import com.eqixiac.equinix.fabric.model.implementation.cloud.CloudProviderType;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A {@link RateCard} decorator that runs every lookup on a daemon worker under the tool call's
 * {@link PricingBudget}. An agent tool call may never hang: if the delegate (a live cloud-pricing
 * adapter or the live Equinix rate card) stalls or throws, or the call's budget is spent, the
 * lookup degrades to {@link Optional#empty()} - letting a layered chain fall through to reference
 * rates - and the failure is recorded so the tool payload can name the source that failed.
 *
 * <p>The budget is per tool call, not per lookup: a tool that makes ten lookups against a stalled
 * provider waits at most the budget once, not ten times.</p>
 */
final class TimeoutRateCard implements RateCard {

    private static final Logger logger = LoggerFactory.getLogger(TimeoutRateCard.class);
    private static final AtomicInteger POOL_SEQUENCE = new AtomicInteger();

    private final String providerLabel;
    private final RateCard delegate;
    private final PricingBudget budget;
    private final ConcurrentLinkedQueue<String> failures = new ConcurrentLinkedQueue<>();

    /**
     * Shared daemon workers. A lookup abandoned at the deadline keeps its worker until the
     * delegate returns; the pool grows for that and shrinks after 60 s idle.
     */
    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool(daemonFactory());

    TimeoutRateCard(String providerLabel, RateCard delegate, PricingBudget budget) {
        this.providerLabel = providerLabel;
        this.delegate = delegate;
        this.budget = budget;
    }

    /**
     * The Equinix rate-card chain this server prices with - the live Equinix rate card under the
     * call's budget, then the bundled reference figures, which need no network and are not
     * guarded. The live layer is appended to {@code guards} so the caller can report its failures.
     *
     * @param fabric the Fabric gateway the live rate card reads
     * @param budget the call's pricing budget
     * @param guards collects the guarded live layer
     * @return the chain
     */
    static RateCard equinixChain(FabricGateway fabric, PricingBudget budget, List<TimeoutRateCard> guards) {
        TimeoutRateCard live = new TimeoutRateCard("Equinix", EquinixRateCard.of(fabric), budget);
        guards.add(live);
        return RateCard.layered(live, ReferenceRateCard.standard());
    }

    /**
     * The live Equinix rate card alone under the call's budget (no reference layer), for engines
     * whose default is that card alone.
     *
     * @param fabric the Fabric gateway the live rate card reads
     * @param budget the call's pricing budget
     * @param guards collects the guarded card
     * @return the guarded card
     */
    static TimeoutRateCard equinixLive(FabricGateway fabric, PricingBudget budget, List<TimeoutRateCard> guards) {
        TimeoutRateCard live = new TimeoutRateCard("Equinix", EquinixRateCard.of(fabric), budget);
        guards.add(live);
        return live;
    }

    /**
     * Adds an {@code equinix_live_pricing} block to {@code payload} when any guarded Equinix lookup
     * failed or was skipped (nothing is added when all succeeded): the number of failed lookups and each
     * distinct failure message (untrusted text) with its count, at most {@link #MAX_REPORTED_FAILURES}.
     *
     * @param payload the tool result
     * @param guards the guarded Equinix layers created by {@link #equinixChain}
     * @param budget the call's pricing budget
     */
    static void reportEquinix(ObjectNode payload, List<TimeoutRateCard> guards, PricingBudget budget) {
        List<String> failures = new java.util.ArrayList<>();
        guards.forEach(guard -> failures.addAll(guard.failures()));
        if (failures.isEmpty()) {
            return;
        }
        // Identical messages (every lookup skipped once the budget is spent) are listed once with a count,
        // so a run with hundreds of lookups stays readable.
        java.util.Map<String, Integer> distinct = new java.util.LinkedHashMap<>();
        failures.forEach(failure -> distinct.merge(failure, 1, Integer::sum));
        ObjectNode block = payload.putObject("equinix_live_pricing");
        block.put("degraded", true);
        block.put("lookups_failed", failures.size());
        ArrayNode list = block.putArray("failures");
        int shown = 0;
        for (java.util.Map.Entry<String, Integer> e : distinct.entrySet()) {
            if (shown++ >= MAX_REPORTED_FAILURES) {
                break;
            }
            ObjectNode f = list.addObject();
            UntrustedText.put(f, "message", e.getKey());
            f.put("count", e.getValue());
        }
        if (distinct.size() > MAX_REPORTED_FAILURES) {
            block.put("failures_omitted", distinct.size() - MAX_REPORTED_FAILURES);
        }
        block.put("note", "Live Equinix pricing was unavailable within the call's " + budget.budgetMillis()
                + " ms live-pricing budget (EQUINIX_MCP_PRICING_TIMEOUT_MS, counted from the first lookup) for "
                + "these lookups; they fell back to the bundled reference figures.");
    }

    /** Distinct failure messages listed in {@code equinix_live_pricing} before the rest are only counted. */
    static final int MAX_REPORTED_FAILURES = 8;

    private static ThreadFactory daemonFactory() {
        return runnable -> {
            Thread thread = new Thread(runnable, "mcp-pricing-" + POOL_SEQUENCE.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
    }

    @Override
    public Optional<PriceQuote> connection(ConnectionType type, int bandwidthMbps, MetroCode metro, Term term) {
        return guarded("connection", () -> delegate.connection(type, bandwidthMbps, metro, term));
    }

    @Override
    public Optional<PriceQuote> cloudRouter(String packageCode, MetroCode metro, Term term) {
        return guarded("cloudRouter", () -> delegate.cloudRouter(packageCode, metro, term));
    }

    @Override
    public Optional<EgressRate> egress(CloudProviderType provider, String region, EgressPath path, Term term) {
        return guarded("egress", () -> delegate.egress(provider, region, path, term));
    }

    @Override
    public Optional<PriceQuote> colocation(ColocationItem item, MetroCode metro, Term term) {
        return guarded("colocation", () -> delegate.colocation(item, metro, term));
    }

    /**
     * Guards the native multicloud link lookup like every other lookup. The bundled provider-API
     * adapters return empty for it; a card supplied through
     * {@code ServerContext.Builder.providerRateCardFactory(...)} may price it and may stall.
     */
    @Override
    public Optional<MulticloudLinkQuote> multicloudLink(MulticloudLinkRequest request) {
        return guarded("multicloudLink", () -> delegate.multicloudLink(request));
    }

    @Override
    public PriceSource source() {
        return delegate.source();
    }

    /**
     * @return human-readable descriptions of every lookup that timed out or threw, in order
     */
    List<String> failures() {
        return List.copyOf(failures);
    }

    /**
     * @return {@code true} when at least one delegate lookup succeeded ({@code Optional} present)
     */
    boolean degraded() {
        return !failures.isEmpty();
    }

    /**
     * @return the label of the guarded source, for example {@code AWS} or {@code Equinix}
     */
    String label() {
        return providerLabel;
    }

    private <T> Optional<T> guarded(String lookup, Callable<Optional<T>> call) {
        if (budget.cancelled()) {
            record(lookup, "skipped: the tool call was cancelled");
            return Optional.empty();
        }
        long remaining = budget.remainingMillis();
        if (remaining <= 0) {
            record(lookup, "skipped: the call's live-pricing budget of " + budget.budgetMillis()
                    + " ms was spent");
            return Optional.empty();
        }
        Future<Optional<T>> future = EXECUTOR.submit(call);
        Runnable abandon = () -> future.cancel(true);
        budget.cancellation().onCancel(abandon);
        try {
            return future.get(remaining, TimeUnit.MILLISECONDS);
        }
        catch (TimeoutException e) {
            future.cancel(true);
            record(lookup, "timed out: the call's live-pricing budget of " + budget.budgetMillis()
                    + " ms ran out");
            return Optional.empty();
        }
        catch (java.util.concurrent.CancellationException e) {
            record(lookup, "skipped: the tool call was cancelled");
            return Optional.empty();
        }
        catch (ExecutionException e) {
            Throwable cause = e.getCause() == null ? e : e.getCause();
            record(lookup, cause.toString());
            return Optional.empty();
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            record(lookup, "interrupted");
            return Optional.empty();
        }
    }

    private void record(String lookup, String reason) {
        // The reason may carry a remote error message: sanitised here, labelled where it is output.
        String message = providerLabel + " live pricing " + lookup + " lookup failed: "
                + UntrustedText.sanitize(reason, UntrustedText.MAX_FIELD_CHARS);
        failures.add(message);
        logger.warn("{}", message);
    }
}
