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

package api.equinix.javasdk;

import api.equinix.javasdk.core.http.RetryPolicy;
import lombok.Builder;
import lombok.Getter;

/**
 * Construction-time options for an Equinix client or {@link Equinix} session, passed instead of the
 * bare {@code (credentials, sandbox)} arguments when finer control is needed:
 *
 * <pre>{@code
 * Fabric fabric = new Fabric(credentials, EquinixConfig.builder()
 *         .sandbox(false)
 *         .autoLoadMetros(true)
 *         .retryPolicy(RetryPolicy.none())
 *         .build());
 * }</pre>
 *
 * <p>All options have sensible defaults ({@link #defaults()}); the plain {@code new Fabric(credentials)}
 * and {@code new Fabric(credentials, sandbox)} constructors are equivalent to passing a config that
 * differs only in {@code sandbox}.</p>
 *
 * @author ianjones
 */
@Getter
@Builder
public class EquinixConfig {

    /**
     * Whether to target the Equinix sandbox environment rather than production. Defaults to
     * {@code false} (production).
     */
    @Builder.Default
    private final boolean sandbox = false;

    /**
     * Whether to eagerly load the metro catalog ({@code fabric.metroRegistry()}) on an explicit
     * {@link Equinix#authenticate()} / {@link Fabric#authenticate()}, so the full set of metros (and
     * their IBXs, coordinates, region, and inter-metro latencies) is resolved up front rather than
     * lazily on first access. Defaults to {@code true}; set {@code false} to skip the extra call (the
     * catalog then still loads lazily on the first {@code metroRegistry()} use). Has no effect on
     * domains without a metro catalog.
     */
    @Builder.Default
    private final boolean autoLoadMetros = true;

    /**
     * The retry policy for transient failures (429/5xx, transient IO). {@code null} (the default)
     * leaves the client's built-in default policy in place; supply
     * {@link RetryPolicy#none()} to disable retries, or a custom policy to override it.
     */
    private final RetryPolicy retryPolicy;

    /**
     * @return a config with all defaults (production, auto-load metros on, default retry policy)
     */
    public static EquinixConfig defaults() {
        return EquinixConfig.builder().build();
    }
}
