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
     * Whether to enrich the metro registry with per-IBX detail from Equinix Internet Access (EIA) —
     * today the only Equinix API that returns per-data-center geo coordinates. When enabled,
     * {@code fabric.metroRegistry()} / {@code Equinix.metroRegistry()} also queries
     * {@code GET /internetAccess/v2/ibxs} (both EIA connection types, unioned) and exposes the
     * result through {@code MetroRegistry.ibx(String)} / {@code ibxDetails(String)} — giving
     * IBX-to-IBX latency math ({@code design.geo.SpeedOfLightLatency}) live coordinates without
     * wiring up the InternetAccess domain yourself.
     *
     * <p>The extra calls are best-effort: if EIA is unavailable the registry still loads from
     * Fabric alone (and reports {@code isEnriched() == false}). Defaults to {@code false} (no
     * extra API calls).</p>
     */
    @Builder.Default
    private final boolean enrichMetroRegistry = false;

    /**
     * A PeeringDB API key used by the Peering Intelligence entry points
     * ({@code fabric.peeringIntelligence()} / {@code design.peeringIntelligence()}) when no key is
     * passed explicitly. This is a separate credential from the Equinix OAuth client — it is created
     * on <a href="https://docs.peeringdb.com/howto/api_keys/">peeringdb.com</a> — and unlocks higher
     * rate limits than anonymous PeeringDB access (~20 requests/minute) plus auth-gated fields.
     *
     * <p>Resolution order at analysis time: an explicit {@code peeringIntelligence(key)} argument,
     * then this option, then the {@code PEERINGDB_API_KEY} environment variable, then anonymous
     * access. Defaults to {@code null} (not configured).</p>
     */
    private final String peeringDbApiKey;

    /**
     * @return a config with all defaults (production, auto-load metros on, default retry policy)
     */
    public static EquinixConfig defaults() {
        return EquinixConfig.builder().build();
    }
}
