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
import api.equinix.javasdk.core.auth.BasicEquinixCredentials;
import api.equinix.javasdk.core.http.response.PaginatedFilteredList;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.fabric.model.Connection;
import api.equinix.javasdk.fabric.model.Metro;

/**
 * Demonstrates the paginated response types returned by Equinix list/search calls.
 *
 * <p>{@link PaginatedList} (GET list endpoints) and {@link PaginatedFilteredList}
 * (POST search endpoints) are {@link Iterable} views of the loaded page plus pagination
 * metadata. You can iterate the current page, walk pages one at a time with
 * {@code hasNextPage()} / {@code next()}, or eagerly pull every page with
 * {@code loadAll()} and then {@code stream()} / {@code toList()} over the full result.</p>
 *
 * <h3>Running</h3>
 * <pre>{@code
 * export EQUINIX_CLIENT_ID=...
 * export EQUINIX_CLIENT_SECRET=...
 * }</pre>
 *
 * <p>This program is illustrative; it is not executed by CI.</p>
 */
public final class ListAndPageSample {

    private ListAndPageSample() {
    }

    public static void main(String[] args) {
        BasicEquinixCredentials credentials = new BasicEquinixCredentials(
                requireEnv("EQUINIX_CLIENT_ID"),
                requireEnv("EQUINIX_CLIENT_SECRET"));

        try (Fabric fabric = new Fabric(credentials)) {

            // --- A GET-based paginated list: metros ---
            // Eagerly load every page, then stream the full result set.
            PaginatedList<Metro> metros = fabric.metros().list().loadAll();
            System.out.println("Total metros across all pages: " + metros.size());
            metros.stream()
                    .limit(5)
                    .forEach(metro -> System.out.println(
                            "  " + metro.getCode() + " - " + metro.getName()
                                    + " (" + metro.getRegion() + ")"));

            System.out.println();

            // --- A POST-based filtered/sorted search: connections ---
            // Walk pages manually instead of loading everything at once.
            PaginatedFilteredList<Connection> connections = fabric.connections().search();
            System.out.println("First page connection count: " + connections.size());

            int page = 1;
            while (connections.hasNextPage()) {
                connections.next();
                page++;
                System.out.println("Loaded page " + page
                        + "; cumulative connections: " + connections.size());
            }

            long active = connections.stream()
                    .filter(c -> c.getState() != null)
                    .count();
            System.out.println("Connections with a known state: " + active);
        } catch (Exception e) {
            System.err.println("List/page sample failed: " + e.getMessage());
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
