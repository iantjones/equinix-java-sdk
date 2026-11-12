/**
 * <b>Estate analysis</b>: looks at what a customer already runs on Equinix and produces a priced,
 * evidence-backed improvement report. Read-only: nothing in this package changes a resource.
 *
 * <ul>
 *   <li>{@link com.eqixiac.equinix.design.estate.EstateCollector} reads a scoped, immutable
 *       {@link com.eqixiac.equinix.design.estate.EstateInventory} (ports, connections, Cloud Routers,
 *       networks, service tokens, Network Edge devices) with bounded paging, recording anything it
 *       cannot read as a coverage gap.</li>
 *   <li>{@link com.eqixiac.equinix.design.estate.UtilizationProbe} reduces Fabric bandwidth metrics
 *       to p50/p95/peak per connection ({@link com.eqixiac.equinix.design.estate.UtilizationSnapshot});
 *       missing data is unknown, never zero.</li>
 *   <li>{@link com.eqixiac.equinix.design.estate.EstateRule}s (defaults in
 *       {@code design.estate.rules}) turn the snapshot into
 *       {@link com.eqixiac.equinix.design.estate.EstateFinding}s, priced through a {@code RateCard}
 *       by {@link com.eqixiac.equinix.design.estate.EstatePricing}.</li>
 *   <li>{@link com.eqixiac.equinix.design.estate.EstateReport} orders, totals (per currency) and
 *       renders them (Markdown and JSON).</li>
 *   <li>{@link com.eqixiac.equinix.design.estate.EstateAnalyzer} is the entry point, live
 *       ({@code builder(fabric)}) or offline ({@code fromSnapshot(inventory)}).</li>
 * </ul>
 */
package com.eqixiac.equinix.design.estate;
