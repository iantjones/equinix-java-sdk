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

package api.equinix.javasdk.mcp.server;

import api.equinix.javasdk.core.enums.MetroCode;
import api.equinix.javasdk.design.value.ratecard.ColocationItem;
import api.equinix.javasdk.design.value.ratecard.EgressPath;
import api.equinix.javasdk.design.value.ratecard.EgressRate;
import api.equinix.javasdk.design.value.ratecard.PriceQuote;
import api.equinix.javasdk.design.value.ratecard.PriceSource;
import api.equinix.javasdk.design.value.ratecard.RateCard;
import api.equinix.javasdk.design.value.ratecard.Term;
import api.equinix.javasdk.fabric.enums.ConnectionType;
import api.equinix.javasdk.fabric.model.implementation.cloud.CloudProviderType;
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
 * A {@link RateCard} decorator that runs every lookup on a daemon worker under a hard
 * timeout. An agent tool call may never hang: if the delegate (a live cloud-pricing adapter)
 * stalls or throws, the lookup degrades to {@link Optional#empty()} — letting a layered chain
 * fall through to reference rates — and the failure is recorded so the tool payload can name
 * the provider that failed.
 */
final class TimeoutRateCard implements RateCard {

    private static final Logger logger = LoggerFactory.getLogger(TimeoutRateCard.class);
    private static final AtomicInteger POOL_SEQUENCE = new AtomicInteger();

    private final String providerLabel;
    private final RateCard delegate;
    private final long timeoutMillis;
    private final ConcurrentLinkedQueue<String> failures = new ConcurrentLinkedQueue<>();
    private final ExecutorService executor;

    TimeoutRateCard(String providerLabel, RateCard delegate, long timeoutMillis) {
        this.providerLabel = providerLabel;
        this.delegate = delegate;
        this.timeoutMillis = timeoutMillis;
        ThreadFactory daemonFactory = runnable -> {
            Thread thread = new Thread(runnable,
                    "mcp-pricing-" + providerLabel + "-" + POOL_SEQUENCE.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
        this.executor = Executors.newSingleThreadExecutor(daemonFactory);
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

    void shutdown() {
        executor.shutdownNow();
    }

    private <T> Optional<T> guarded(String lookup, Callable<Optional<T>> call) {
        Future<Optional<T>> future = executor.submit(call);
        try {
            return future.get(timeoutMillis, TimeUnit.MILLISECONDS);
        }
        catch (TimeoutException e) {
            future.cancel(true);
            record(lookup, "timed out after " + timeoutMillis + " ms");
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
        String message = providerLabel + " live pricing " + lookup + " lookup failed: " + reason;
        failures.add(message);
        logger.warn("{}", message);
    }
}
