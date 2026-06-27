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

package api.equinix.javasdk.samples;

import api.equinix.javasdk.Fabric;
import api.equinix.javasdk.core.async.EquinixAsync;
import api.equinix.javasdk.core.auth.BasicEquinixCredentials;
import api.equinix.javasdk.fabric.model.Metro;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Fans out several blocking SDK calls concurrently with {@link EquinixAsync}, a small
 * virtual-thread-backed facade over any Equinix SDK call.
 *
 * <p>Rather than mirroring every method with an {@code ...Async} variant, you wrap an
 * ordinary blocking call in {@code async.call(() -> ...)} and get back a
 * {@link CompletableFuture}. Each call runs on its own (cheap) virtual thread, so many
 * requests can be in flight at once. {@link EquinixAsync#awaitAll(java.util.Collection)}
 * is a convenience for the common "fan out, then join" pattern.</p>
 *
 * <h3>Running</h3>
 * <pre>{@code
 * export EQUINIX_CLIENT_ID=...
 * export EQUINIX_CLIENT_SECRET=...
 * }</pre>
 *
 * <p>This program is illustrative; it is not executed by CI.</p>
 */
public final class AsyncSample {

    private AsyncSample() {
    }

    public static void main(String[] args) {
        BasicEquinixCredentials credentials = new BasicEquinixCredentials(
                requireEnv("EQUINIX_CLIENT_ID"),
                requireEnv("EQUINIX_CLIENT_SECRET"));

        // The facade and the Fabric client are both closed when the block exits.
        try (Fabric fabric = new Fabric(credentials);
             EquinixAsync async = EquinixAsync.create()) {

            // Kick off independent calls concurrently. Each lambda is an ordinary
            // blocking SDK call that runs on its own virtual thread.
            CompletableFuture<Integer> metroCount =
                    async.call(() -> fabric.metros().list().loadAll().size());

            CompletableFuture<Integer> connectionCount =
                    async.call(() -> fabric.connections().search().size());

            CompletableFuture<String> health =
                    async.call(() -> String.valueOf(fabric.health()));

            // Combine the independent futures without blocking until we ask for the result.
            CompletableFuture<Void> report = CompletableFuture
                    .allOf(metroCount, connectionCount, health)
                    .thenRun(() -> {
                        System.out.println("Metros:            " + metroCount.join());
                        System.out.println("Connections (p1):  " + connectionCount.join());
                        System.out.println("Fabric health:     " + health.join());
                    });

            // Block here (at the edge of the program) for everything to finish.
            report.join();

            // The fan-out / join convenience: collect a list of results in order.
            List<CompletableFuture<Metro>> lookups = List.of(
                    async.call(() -> fabric.metros().list().get(0)),
                    async.call(() -> fabric.metros().list().get(0)));
            List<Metro> firstMetros = EquinixAsync.awaitAll(lookups);
            System.out.println("Fetched " + firstMetros.size() + " metro(s) concurrently.");
        } catch (Exception e) {
            System.err.println("Async sample failed: " + e.getMessage());
        }
    }

    private static String requireEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required environment variable: " + name);
        }
        return value;
    }
}
