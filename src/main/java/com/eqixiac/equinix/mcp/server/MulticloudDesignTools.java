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

import com.eqixiac.equinix.core.model.multicloud.ProviderSite;
import com.eqixiac.equinix.design.optimizer.enums.MulticloudEnvironmentStatus;
import com.eqixiac.equinix.design.optimizer.model.MulticloudEnvironment;
import com.eqixiac.equinix.design.optimizer.model.MulticloudEnvironmentCatalog;
import com.eqixiac.equinix.design.optimizer.wizard.model.DeploymentPlan;
import com.eqixiac.equinix.design.optimizer.wizard.model.MulticloudLinkPricing;
import com.eqixiac.equinix.design.optimizer.wizard.model.PlanPricing;
import com.eqixiac.equinix.design.optimizer.wizard.model.PlannedMulticloudInterconnect;
import com.eqixiac.equinix.design.value.ratecard.EgressPath;
import com.eqixiac.equinix.design.value.ratecard.MulticloudLinkQuote;
import com.eqixiac.equinix.design.value.ratecard.PriceQuote;
import com.eqixiac.equinix.design.value.ratecard.RateCard;
import com.eqixiac.equinix.design.value.ratecard.ReferenceRateCard;
import com.eqixiac.equinix.design.value.ratecard.Term;
import com.eqixiac.equinix.design.value.savings.MulticloudPathComparison;
import com.eqixiac.equinix.design.value.savings.SavingsCalculator;
import com.eqixiac.equinix.design.value.savings.SavingsEstimate;
import com.eqixiac.equinix.fabric.model.implementation.cloud.CloudProviderType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static com.eqixiac.equinix.mcp.server.Args.optBool;
import static com.eqixiac.equinix.mcp.server.Args.optInt;
import static com.eqixiac.equinix.mcp.server.Args.optNumber;
import static com.eqixiac.equinix.mcp.server.Args.optString;
import static com.eqixiac.equinix.mcp.server.Args.requireString;
import static com.eqixiac.equinix.mcp.server.Schemas.bool;
import static com.eqixiac.equinix.mcp.server.Schemas.integer;
import static com.eqixiac.equinix.mcp.server.Schemas.looseObject;
import static com.eqixiac.equinix.mcp.server.Schemas.number;
import static com.eqixiac.equinix.mcp.server.Schemas.object;
import static com.eqixiac.equinix.mcp.server.Schemas.props;
import static com.eqixiac.equinix.mcp.server.Schemas.string;
import static com.eqixiac.equinix.mcp.server.Schemas.stringEnum;

/**
 * The cloud-to-cloud part of the {@code design_*} catalog: {@code design_compare_cloud_to_cloud},
 * {@code design_list_multicloud_environments}, and the serialization of a deployment plan's native
 * multicloud links for {@code design_plan_deployment}.
 *
 * <p><b>Beta.</b> A native multicloud link is a direct provider-to-provider connection (AWS
 * Interconnect - multicloud paired with the peer provider's counterpart) that uses no Equinix
 * resource. The products reached general availability in 2026; their price lists are incomplete and
 * the region-pair catalog is a dated copy of provider documentation. Both tools are read-only and
 * call no cloud-provider control-plane API: this server plans and prices a native link and never
 * creates one.</p>
 *
 * <h2>Units used by both tools</h2>
 * <table>
 *   <caption>Units</caption>
 *   <tr><th>Quantity</th><th>Unit</th></tr>
 *   <tr><td>link size, sustained rate, break-even</td><td>Mbps; sustained and break-even rates are
 *       the sum of both directions with traffic split evenly between them</td></tr>
 *   <tr><td>volume</td><td>decimal GB (10<sup>9</sup> bytes) per month</td></tr>
 *   <tr><td>month</td><td>{@value MulticloudLinkQuote#HOURS_PER_MONTH} h, the conversion the value
 *       layer applies to hourly list rates</td></tr>
 *   <tr><td>money</td><td>the currency named next to the figure; amounts in different currencies are
 *       never summed and no FX rate is applied</td></tr>
 * </table>
 *
 * <p>An unpriced figure is JSON {@code null} with a reason, never {@code 0}.</p>
 */
final class MulticloudDesignTools {

    private static final int MAX_ENVIRONMENTS = 50;

    /** Payload names of the three paths, keyed by the value layer's {@link EgressPath}. */
    private static final Map<EgressPath, String> PATH_NAMES = pathNames();

    private MulticloudDesignTools() {
    }

    private static Map<EgressPath, String> pathNames() {
        Map<EgressPath, String> names = new LinkedHashMap<>();
        names.put(EgressPath.INTERNET, "public_internet");
        names.put(EgressPath.PRIVATE, "equinix_fabric");
        names.put(EgressPath.MULTICLOUD_INTERCONNECT, "native_multicloud_link");
        return names;
    }

    // ── design_compare_cloud_to_cloud ───────────────────────────────────────

    static ToolRegistration compareCloudToCloud() {
        Map<String, Object> termMonths = integer("Commitment length in months used ONLY for "
                + "total_over_term (monthly_total x term_months + setup_total). Default 12. It changes no "
                + "monthly figure: no rate card in this server's chain resolves a rate by term, and the "
                + "native link products bill hourly with no commitment.");
        termMonths.put("enum", List.of(1, 12, 24, 36));
        return ToolRegistration.builder()
                .name("design_compare_cloud_to_cloud")
                .title("Compare cloud-to-cloud paths: internet vs Equinix Fabric vs native multicloud link")
                .description("Beta. Prices the traffic between two clouds over three paths and reports the "
                        + "sustained rate at which the two private paths cost the same. Paths: (1) "
                        + "public_internet: each cloud's internet egress rate on the traffic leaving it, no "
                        + "fixed cost. (2) equinix_fabric: two Fabric virtual connections (one per cloud), "
                        + "each cloud's interconnect-port reference figure, the optional Cloud Router, and "
                        + "each cloud's private egress rate. (3) native_multicloud_link: the two providers' "
                        + "flat hourly link fees (AWS Interconnect - multicloud plus the peer provider's "
                        + "counterpart) converted at 730 h/month, and the link's per-GB rate (0 where the "
                        + "provider publishes 0). Every side of every path carries its price source; native "
                        + "link figures come only from provider pricing pages and each note gives the URL "
                        + "and retrieval date. A figure with no verifiable source is null and is listed in "
                        + "unpriced_components with the reason; it is never 0 and never estimated from a "
                        + "neighbouring size. Native link rates match the published size exactly: no "
                        + "rounding up, interpolation or extrapolation. break_even_sustained_mbps is the sum "
                        + "of both directions with traffic split evenly; below it equinix_fabric costs less "
                        + "per month, above it native_multicloud_link does. It is null when either private "
                        + "path is not fully priced, the currencies differ, or no break-even exists. "
                        + "environment reports whether the bundled catalog lists the region pair and at what "
                        + "launch stage; an absent pair means 'not in this dated copy', not 'not offered'. "
                        + "Live cloud egress rates are fetched under a hard timeout and degrade to reference "
                        + "rates, naming the provider that failed. Cost only: latency, resiliency and reach "
                        + "to other sites or clouds are not compared. Nothing is provisioned, and this "
                        + "server cannot create a native link.")
                .inputSchema(object(props(
                                "a", cloudEndSchema("One end of the flow."),
                                "z", cloudEndSchema("The other end. Its cloud must differ from a.cloud."),
                                "bandwidth_mbps", integer("Link size in Mbps, used for all three paths: the "
                                        + "Fabric virtual connections, the cloud interconnect ports and the "
                                        + "native link. Native link prices are published for specific sizes "
                                        + "only (for example 1000 and 10000); any other size leaves that side "
                                        + "unpriced."),
                                "sustained_mbps", number("Sustained traffic between the two clouds in Mbps, "
                                        + "summed over both directions and split evenly between them (the same "
                                        + "unit as break_even_sustained_mbps). Converted to GB per month at "
                                        + "730 h/month and 8000 Mb/GB. Must be between 0 and 2 x bandwidth_mbps. "
                                        + "When omitted, volume is 0: path totals are fixed cost only, "
                                        + "lowest_cost_path is omitted, and the recommendation is stated "
                                        + "against the break-even rate."),
                                "term_months", termMonths,
                                "path_tier", integer("AWS connectivity-scope tier of the native link, 1-5: 1 "
                                        + "local (default), 2 regional, 3 continental, 4 long-haul, 5 maximum "
                                        + "scope. An input, never derived: AWS assigns the tier and publishes "
                                        + "no region-path-to-tier table. Ignored by providers without tiers."),
                                "use_aws_free_tier", bool("Apply the AWS free tier to the native link's AWS "
                                        + "side: one free tier-1 500 Mbps interconnect per AWS Region per "
                                        + "generally-available provider. Default false and never inferred, "
                                        + "because the server cannot know whether the account already uses it. "
                                        + "Requires AWS as a.cloud or z.cloud; when the request is ineligible "
                                        + "the listed rate applies and a note says why."),
                                "metro_code", string("Equinix metro for the equinix_fabric path, e.g. 'DC'. "
                                        + "Used to prefer a metro-specific Fabric price row where one is "
                                        + "published."),
                                "cloud_router_package", string("Price a Fabric Cloud Router of this package "
                                        + "code (e.g. 'STANDARD') into the equinix_fabric fixed cost. Without "
                                        + "it no Equinix A-side is priced, the Equinix fixed cost is "
                                        + "understated and the break-even overstated; unpriced_components "
                                        + "says so.")),
                        "a", "z", "bandwidth_mbps"))
                .outputSchema(looseObject("The three paths (fixed, data transfer, monthly and over-term "
                        + "totals, per-side provenance), break_even_sustained_mbps, unpriced_components, "
                        + "the catalog environment for the region pair, a recommendation sentence, "
                        + "live-pricing status per provider, and the limits of the comparison."))
                .toolset(Toolset.DESIGN)
                .openWorld(true)
                .handler(MulticloudDesignTools::handleCompareCloudToCloud)
                .build();
    }

    private static Map<String, Object> cloudEndSchema(String description) {
        Map<String, Object> schema = object(props(
                        "cloud", stringEnum("The cloud provider.", DesignToolFactory.CLOUD_VALUES),
                        "region", string("The provider's own region name, e.g. 'us-east-1' (AWS), "
                                + "'us-east4' (Google Cloud), 'us-ashburn-1' (Oracle Cloud). It selects the "
                                + "egress rates, Google's transport location for the native link, and the "
                                + "catalog environment.")),
                "cloud", "region");
        schema.put("description", description);
        return schema;
    }

    private static ObjectNode handleCompareCloudToCloud(JsonNode args, ServerContext ctx) {
        CloudEnd a = cloudEnd(args, "a");
        CloudEnd z = cloudEnd(args, "z");
        if (a.provider() == z.provider()) {
            throw new IllegalArgumentException("'a.cloud' and 'z.cloud' must be different providers; both "
                    + "are " + a.provider().name().toLowerCase(Locale.ROOT) + ". For one cloud's egress over "
                    + "the internet vs Equinix use design_compare_cloud_egress.");
        }
        int bandwidth = optInt(args, "bandwidth_mbps").orElseThrow(() -> new IllegalArgumentException(
                "'bandwidth_mbps' is required and must be a whole number of Mbps, e.g. 10000."));
        if (bandwidth <= 0) {
            throw new IllegalArgumentException("'bandwidth_mbps' must be positive: " + bandwidth + ".");
        }
        Optional<Double> sustained = optNumber(args, "sustained_mbps");
        if (sustained.isPresent()) {
            double s = sustained.get();
            if (!Double.isFinite(s) || s < 0) {
                throw new IllegalArgumentException("'sustained_mbps' must be a finite number >= 0: " + s + ".");
            }
            if (s > 2.0 * bandwidth) {
                throw new IllegalArgumentException("'sustained_mbps' " + s + " exceeds what a " + bandwidth
                        + " Mbps link carries: " + (2L * bandwidth) + " Mbps summed over both directions. "
                        + "Raise bandwidth_mbps or lower sustained_mbps.");
            }
        }
        Term term = term(optInt(args, "term_months"));
        Optional<Integer> pathTier = optInt(args, "path_tier");
        if (pathTier.isPresent() && (pathTier.get() < MulticloudLinkQuote.MIN_PATH_TIER
                || pathTier.get() > MulticloudLinkQuote.MAX_PATH_TIER)) {
            throw new IllegalArgumentException("'path_tier' must be between " + MulticloudLinkQuote.MIN_PATH_TIER
                    + " and " + MulticloudLinkQuote.MAX_PATH_TIER + ": " + pathTier.get() + ".");
        }
        boolean freeTier = optBool(args, "use_aws_free_tier", false);
        if (freeTier && a.provider() != CloudProviderType.AWS && z.provider() != CloudProviderType.AWS) {
            throw new IllegalArgumentException("'use_aws_free_tier' requires AWS as 'a.cloud' or 'z.cloud'.");
        }

        double eachWayMbps = sustained.orElse(0.0) / 2.0;
        double eachWayGb = eachWayMbps * MulticloudPathComparison.SECONDS_PER_MONTH
                / MulticloudPathComparison.MEGABITS_PER_GB;

        // Live egress pricing for each end, each behind the hard timeout. The provider adapters
        // answer only for their own provider, so layering both ahead of the standard chain lets each
        // side resolve independently and fall through to reference rates on failure.
        Map<CloudProviderType, TimeoutRateCard> guards = new LinkedHashMap<>();
        List<RateCard> cards = new ArrayList<>();
        for (CloudEnd end : List.of(a, z)) {
            ctx.providerRateCard(end.provider()).ifPresent(card -> {
                TimeoutRateCard guarded = new TimeoutRateCard(end.provider().name(), card,
                        ctx.pricingTimeoutMillis());
                guards.put(end.provider(), guarded);
                cards.add(guarded);
            });
        }
        cards.add(RateCard.standardChain(ctx.fabric()));
        RateCard chain = RateCard.layered(cards.toArray(new RateCard[0]));

        try {
            SavingsCalculator.Builder builder = ctx.design().savingsCalculator()
                    .egressGigabytes(eachWayGb)
                    .fromCloud(a.provider()).inRegion(a.region())
                    .toCloud(z.provider()).toRegion(z.region())
                    .bandwidthMbps(bandwidth)
                    .term(term)
                    .rateCard(chain);
            pathTier.ifPresent(builder::pathTier);
            if (freeTier) {
                builder.useAwsFreeTier(true);
            }
            optString(args, "metro_code").ifPresent(code -> builder.viaMetro(DesignToolFactory.metroCode(code)));
            Optional<String> routerPackage = optString(args, "cloud_router_package");
            routerPackage.ifPresent(builder::includeCloudRouter);

            SavingsEstimate estimate = builder.calculate();
            MulticloudPathComparison comparison = estimate.getMulticloudComparison();
            if (comparison == null) {
                throw new IllegalStateException("The savings engine returned no cloud-to-cloud section for "
                        + a.provider() + " <-> " + z.provider() + ".");
            }
            Optional<MulticloudEnvironment> environment = ctx.multicloudEnvironments()
                    .find(a.provider(), a.region(), z.provider(), z.region());

            return comparePayload(ctx, a, z, bandwidth, sustained, term, routerPackage.isPresent(),
                    comparison, environment, guards, estimate);
        }
        finally {
            guards.values().forEach(TimeoutRateCard::shutdown);
        }
    }

    private static ObjectNode comparePayload(ServerContext ctx, CloudEnd a, CloudEnd z, int bandwidth,
                                             Optional<Double> sustained, Term term, boolean routerPriced,
                                             MulticloudPathComparison c,
                                             Optional<MulticloudEnvironment> environment,
                                             Map<CloudProviderType, TimeoutRateCard> guards,
                                             SavingsEstimate estimate) {
        ObjectMapper mapper = ctx.objectMapper();
        ObjectNode payload = mapper.createObjectNode();
        payload.put("beta", true);
        payload.set("a", a.toNode(mapper));
        payload.set("z", z.toNode(mapper));
        payload.put("bandwidth_mbps", bandwidth);
        payload.put("path_tier", c.getPathTier());
        payload.put("term_months", term.months());
        payload.put("currency", c.getCurrency());

        ObjectNode traffic = payload.putObject("traffic");
        traffic.put("sustained_mbps_supplied", sustained.isPresent());
        if (sustained.isPresent()) {
            traffic.put("sustained_mbps", sustained.get());
        }
        else {
            traffic.putNull("sustained_mbps");
        }
        traffic.put("a_to_z_gb_per_month", c.getForwardEgressGb());
        traffic.put("z_to_a_gb_per_month", c.getReverseEgressGb());
        traffic.put("assumption", sustained.isPresent()
                ? "sustained_mbps is split evenly between the two directions and converted at "
                + MulticloudLinkQuote.HOURS_PER_MONTH + " h/month and " + MulticloudPathComparison.MEGABITS_PER_GB
                + " Mb/GB."
                : "No sustained_mbps was given, so the volume is 0 GB in each direction: data-transfer "
                + "figures are 0 and each monthly_total is the path's fixed cost only.");

        payload.set("environment", compareEnvironmentNode(ctx, a, z, bandwidth, environment));

        // Notes are grouped by the path label the value layer prefixes them with. Anything without a
        // recognised prefix (the hourly conversion, the tier input, the break-even arithmetic) stays
        // in the general list, so rewording in the value layer loses nothing here.
        List<String> internetNotes = new ArrayList<>();
        List<String> equinixNotes = new ArrayList<>();
        List<String> nativeNotes = new ArrayList<>();
        List<String> generalNotes = new ArrayList<>();
        ArrayNode unpriced = mapper.createArrayNode();
        if (c.getNotes() != null) {
            for (String note : c.getNotes()) {
                if (note.startsWith("Public internet path")) {
                    internetNotes.add(note);
                }
                else if (note.startsWith("Equinix path")) {
                    equinixNotes.add(note);
                }
                else if (note.startsWith("Native ")) {
                    nativeNotes.add(note);
                }
                else {
                    generalNotes.add(note);
                }
            }
        }

        ArrayNode paths = payload.putArray("paths");
        paths.add(pathNode(mapper, EgressPath.INTERNET, BigDecimal.ZERO, c.getInternetMonthlyCost(),
                c.getInternetMonthlyCost(), BigDecimal.ZERO, term, internetNotes,
                c.getInternetMonthlyCost() != null));
        paths.add(pathNode(mapper, EgressPath.PRIVATE, c.getEquinixFixedMonthlyCost(),
                c.getEquinixEgressMonthlyCost(), c.getEquinixMonthlyCost(), c.getEquinixSetupCost(), term,
                equinixNotes, c.getEquinixMonthlyCost() != null));

        MulticloudLinkQuote quote = c.getNativeLinkQuote();
        BigDecimal nativeSetup = quote == null ? null : quote.combinedSetup().orElse(null);
        ObjectNode nativePath = pathNode(mapper, EgressPath.MULTICLOUD_INTERCONNECT,
                c.getNativeFixedMonthlyCost(), c.getNativeDataTransferMonthlyCost(), c.getNativeMonthlyCost(),
                nativeSetup, term, nativeNotes, c.getNativeMonthlyCost() != null);
        ArrayNode sides = nativePath.putArray("sides");
        if (quote == null) {
            unpriced.add("native_multicloud_link: no rate card in the chain holds a native link price for "
                    + a.provider() + " <-> " + z.provider() + ".");
        }
        else {
            sides.add(sideNode(mapper, quote.getProviderA(), quote.getRegionA(), quote.getSideA(),
                    quote.getSideAUnpricedReason(), unpriced));
            sides.add(sideNode(mapper, quote.getProviderZ(), quote.getRegionZ(), quote.getSideZ(),
                    quote.getSideZUnpricedReason(), unpriced));
            if (quote.isMixedCurrency()) {
                unpriced.add("native_multicloud_link: the two sides are in different currencies ("
                        + quote.monthlySubtotalsByCurrency() + " per month); no combined figure is given "
                        + "because no FX rate is applied.");
            }
        }
        paths.add(nativePath);

        if (!routerPriced) {
            unpriced.add("equinix_fabric: no Equinix A-side is priced because cloud_router_package was not "
                    + "given. The two virtual connections terminate on a Cloud Router, a Network Edge device "
                    + "or a colocation port; without it the Equinix fixed cost is understated and "
                    + "break_even_sustained_mbps is overstated.");
        }
        if (c.getNotes() != null) {
            for (String note : c.getNotes()) {
                String lower = note.toLowerCase(Locale.ROOT);
                if ((lower.contains(" unpriced") || lower.contains(" withheld"))
                        && !note.startsWith("Native multicloud path unpriced")) {
                    unpriced.add(note);
                }
            }
        }
        payload.set("unpriced_components", unpriced);

        BigDecimal breakEven = c.getBreakEvenSustainedMbps();
        if (breakEven != null) {
            payload.put("break_even_sustained_mbps", breakEven);
            payload.put("break_even_each_way_mbps", breakEven.divide(BigDecimal.valueOf(2), 1, RoundingMode.HALF_UP));
            payload.put("break_even_exceeds_link_capacity",
                    breakEven.compareTo(BigDecimal.valueOf(2L * bandwidth)) > 0);
            payload.put("cheaper_above_break_even", c.isNativeCheaperAboveBreakEven()
                    ? "native_multicloud_link" : "equinix_fabric");
        }
        else {
            payload.putNull("break_even_sustained_mbps");
        }
        payload.put("break_even_definition", "The sustained rate, summed over both directions with traffic "
                + "split evenly, at which native_multicloud_link and equinix_fabric cost the same per month. "
                + "cheaper_above_break_even names the path that costs less above it (the other costs less "
                + "below it); it is native_multicloud_link when the native fixed fee exceeds the Equinix fixed "
                + "cost and the Equinix per-GB rate exceeds the native per-GB rate, and equinix_fabric when both "
                + "differences are reversed. null when either path is not fully priced, the currencies differ, "
                + "or no break-even exists.");

        if (sustained.isPresent() && c.getLowestCostPath() != null) {
            payload.put("lowest_cost_path", PATH_NAMES.get(c.getLowestCostPath()));
        }
        payload.put("recommendation", recommendation(c, sustained, bandwidth, environment,
                ctx.multicloudEnvironments().asOf(), a, z));

        ArrayNode notes = payload.putArray("provenance_notes");
        generalNotes.forEach(notes::add);

        ArrayNode live = payload.putArray("live_pricing");
        for (CloudEnd end : List.of(a, z)) {
            ObjectNode entry = live.addObject();
            entry.put("provider", end.provider().name());
            TimeoutRateCard guarded = guards.get(end.provider());
            entry.put("attempted", guarded != null);
            if (guarded == null) {
                entry.put("note", end.provider() == CloudProviderType.GOOGLE_CLOUD
                        ? "No live GCP adapter: set GCP_BILLING_API_KEY to enable the Cloud Billing Catalog "
                        + "lookup. Reference rates were used."
                        : "No live pricing adapter exists for " + end.provider().name()
                        + "; reference rates were used.");
            }
            else if (guarded.degraded()) {
                entry.put("degraded", true);
                ArrayNode failures = entry.putArray("failures");
                guarded.failures().forEach(failures::add);
                entry.put("note", "Live " + end.provider().name() + " pricing was unavailable within "
                        + ctx.pricingTimeoutMillis() + " ms; its figures fell back to reference rates.");
            }
            else {
                entry.put("degraded", false);
            }
        }
        payload.put("live_pricing_scope", "Live lookups cover internet and private egress rates only. No "
                + "provider price API is wired for native multicloud link fees; those come from the bundled "
                + "reference data.");

        ReferenceRateCard reference = ReferenceRateCard.standard();
        ObjectNode asOf = payload.putObject("reference_data_as_of");
        asOf.put("egress_and_ports", reference.asOf());
        asOf.put("native_multicloud_link", reference.multicloudAsOf());
        asOf.put("native_multicloud_link_retrieved", reference.multicloudRetrieved());

        ArrayNode limits = payload.putArray("limits");
        limits.add("Cost comparison only. Latency, resiliency, reach to on-premises sites or a third cloud, "
                + "and operational ownership are not modelled.");
        limits.add("Traffic is assumed symmetric. Each cloud bills egress on the traffic leaving it.");
        limits.add("One month is " + MulticloudLinkQuote.HOURS_PER_MONTH + " h; a 31-day month has 744 h, so "
                + "an invoice for an hourly-billed link can exceed the modelled figure by up to 1.9 %.");
        limits.add("List prices. Negotiated discounts, commitments and taxes are excluded.");

        String disclaimer = estimate.getDisclaimer();
        if (reference.multicloudDisclaimer() != null) {
            disclaimer = (disclaimer == null ? "" : disclaimer + " ") + reference.multicloudDisclaimer();
        }
        payload.put("disclaimer", disclaimer);
        return payload;
    }

    private static ObjectNode pathNode(ObjectMapper mapper, EgressPath path, BigDecimal fixed,
                                       BigDecimal transfer, BigDecimal monthly, BigDecimal setup, Term term,
                                       List<String> notes, boolean priced) {
        ObjectNode node = mapper.createObjectNode();
        node.put("path", PATH_NAMES.get(path));
        node.put("priced", priced);
        node.put("fixed_monthly", priced ? fixed : null);
        node.put("data_transfer_monthly", priced ? transfer : null);
        node.put("monthly_total", monthly);
        node.put("setup_total", priced ? setup : null);
        node.put("total_over_term", priced && monthly != null && setup != null
                ? monthly.multiply(BigDecimal.valueOf(term.months())).add(setup) : null);
        ArrayNode provenance = node.putArray("provenance");
        notes.forEach(provenance::add);
        return node;
    }

    private static ObjectNode sideNode(ObjectMapper mapper, CloudProviderType provider, String region,
                                       Optional<PriceQuote> side, String unpricedReason, ArrayNode unpriced) {
        ObjectNode node = mapper.createObjectNode();
        node.put("provider", provider.name());
        node.put("region", region);
        node.put("priced", side.isPresent());
        if (side.isPresent()) {
            PriceQuote quote = side.get();
            node.put("monthly_recurring", quote.getMonthlyRecurring());
            node.put("non_recurring", quote.getNonRecurring());
            node.put("currency", quote.getCurrency() == null ? null : quote.getCurrency().getCurrencyCode());
            node.put("price_source", String.valueOf(quote.getSource()));
            node.put("note", quote.getNote());
        }
        else {
            String reason = unpricedReason == null || unpricedReason.isBlank()
                    ? "no rate card in the chain holds a price for this side" : unpricedReason;
            node.putNull("monthly_recurring");
            node.put("unpriced_reason", reason);
            unpriced.add("native_multicloud_link: the " + provider.name() + " side is unpriced: " + reason);
        }
        return node;
    }

    /**
     * The catalog's entry for the compared region pair, or a statement that there is none. The
     * catalog is a dated copy, so an absent pair is reported as "not in this copy".
     */
    private static ObjectNode compareEnvironmentNode(ServerContext ctx, CloudEnd a, CloudEnd z, int bandwidth,
                                                     Optional<MulticloudEnvironment> environment) {
        ObjectNode node = ctx.objectMapper().createObjectNode();
        node.put("listed", environment.isPresent());
        node.put("catalog_as_of", ctx.multicloudEnvironments().asOf());
        if (environment.isEmpty()) {
            node.put("note", "The environment catalog holds no entry for " + a.describe() + " <-> "
                    + z.describe() + ". The catalog is a dated copy of provider documentation: an absent pair "
                    + "means 'not in this copy', not 'not offered'. Check both providers' current region "
                    + "lists (design_list_multicloud_environments gives the sources).");
            return node;
        }
        MulticloudEnvironment env = environment.get();
        environmentFields(node, env);
        node.put("bandwidth_listed", env.sizesMbps().contains(bandwidth));
        return node;
    }

    /**
     * One sentence (two when the catalog adds a caveat) stating which private path costs less at
     * which sustained rate, or why that cannot be stated. It never recommends a path that is not
     * fully priced.
     */
    static String recommendation(MulticloudPathComparison c, Optional<Double> sustained, int bandwidth,
                                 Optional<MulticloudEnvironment> environment, String catalogAsOf,
                                 CloudEnd a, CloudEnd z) {
        StringBuilder sb = new StringBuilder();
        String currency = c.getCurrency();
        List<String> notFullyPriced = new ArrayList<>();
        if (c.getNativeMonthlyCost() == null) {
            notFullyPriced.add("native_multicloud_link (" + unpricedNativeDetail(c.getNativeLinkQuote()) + ")");
        }
        if (c.getEquinixMonthlyCost() == null) {
            notFullyPriced.add("equinix_fabric");
        }
        if (!notFullyPriced.isEmpty()) {
            sb.append("No recommendation between native_multicloud_link and equinix_fabric. Not fully priced: ")
                    .append(String.join(", ", notFullyPriced))
                    .append(". The reason for each missing figure is in unpriced_components.");
        }
        else if (c.getBreakEvenSustainedMbps() != null) {
            BigDecimal breakEven = c.getBreakEvenSustainedMbps();
            long capacity = 2L * bandwidth;
            // The sense of the break-even: which path costs less below it and which above it.
            String below = c.isNativeCheaperAboveBreakEven() ? "equinix_fabric" : "native_multicloud_link";
            String above = c.isNativeCheaperAboveBreakEven() ? "native_multicloud_link" : "equinix_fabric";
            if (breakEven.compareTo(BigDecimal.valueOf(capacity)) > 0) {
                sb.append(below).append(" costs less per month than ").append(above).append(" at every rate a ")
                        .append(bandwidth).append(" Mbps link can carry: the break-even of ")
                        .append(breakEven.toPlainString()).append(" Mbps exceeds the link's ").append(capacity)
                        .append(" Mbps (both directions summed).");
            }
            else if (sustained.isPresent()) {
                boolean belowBreakEven = BigDecimal.valueOf(sustained.get()).compareTo(breakEven) < 0;
                boolean equinixCheaper = belowBreakEven == c.isNativeCheaperAboveBreakEven();
                sb.append("At ").append(plain(sustained.get())).append(" Mbps sustained (both directions "
                                + "summed) ").append(equinixCheaper ? "equinix_fabric" : "native_multicloud_link")
                        .append(" costs less per month (").append(currency).append(' ')
                        .append(money(equinixCheaper ? c.getEquinixMonthlyCost() : c.getNativeMonthlyCost()))
                        .append(" vs ").append(currency).append(' ')
                        .append(money(equinixCheaper ? c.getNativeMonthlyCost() : c.getEquinixMonthlyCost()))
                        .append("); the order reverses at the break-even of ").append(breakEven.toPlainString())
                        .append(" Mbps.");
            }
            else {
                sb.append("No sustained_mbps was given, so no path is recommended: ").append(below)
                        .append(" costs less per month below ").append(breakEven.toPlainString())
                        .append(" Mbps sustained (both directions summed) and ").append(above)
                        .append(" costs less above it.");
            }
        }
        else {
            String reason = null;
            String notComputed = null;
            if (c.getNotes() != null) {
                for (String note : c.getNotes()) {
                    if (note.startsWith("No break-even:")) {
                        reason = note.substring("No break-even:".length()).trim();
                    }
                    else if (note.startsWith("Break-even not computed:")) {
                        notComputed = note.substring("Break-even not computed:".length()).trim();
                    }
                }
            }
            if (reason == null && notComputed != null) {
                // Both paths priced, but the per-GB rates are not all in the comparison currency:
                // a break-even may exist and cannot be stated.
                sb.append("No recommendation between native_multicloud_link and equinix_fabric: the break-even "
                        + "was not computed. ").append(notComputed);
            }
            else {
                sb.append("No break-even exists between the two private paths at these prices")
                        .append(reason == null ? "; see provenance_notes." : ": " + reason);
            }
        }

        if (sustained.isPresent() && c.getLowestCostPath() == EgressPath.INTERNET
                && c.getInternetMonthlyCost() != null) {
            sb.append(" At this volume public_internet has the lowest monthly total (").append(currency)
                    .append(' ').append(money(c.getInternetMonthlyCost())).append(").");
        }

        if (environment.isEmpty()) {
            sb.append(" The environment catalog").append(catalogAsOf == null ? "" : " (as of " + catalogAsOf + ")")
                    .append(" lists no native environment for ").append(a.describe()).append(" <-> ")
                    .append(z.describe()).append(", so confirm with both providers that the link can be ordered.");
        }
        else if (environment.get().getStatus() != MulticloudEnvironmentStatus.GA) {
            sb.append(" The environment catalog lists this region pair as ").append(environment.get().getStatus())
                    .append(environment.get().getAsOf() == null ? "" : " (as of " + environment.get().getAsOf() + ")")
                    .append(", not GA.");
        }
        else if (!environment.get().sizesMbps().isEmpty() && !environment.get().sizesMbps().contains(bandwidth)) {
            sb.append(" The environment catalog confirms sizes ").append(environment.get().sizesMbps().toList())
                    .append(" Mbps for this pair; ").append(bandwidth).append(" Mbps is not among them.");
        }
        return sb.toString();
    }

    /** Which part of the native link has no price, in a few words; the full reasons are listed elsewhere. */
    private static String unpricedNativeDetail(MulticloudLinkQuote quote) {
        if (quote == null) {
            return "no rate card holds a price for this provider pair";
        }
        List<String> sides = new ArrayList<>();
        if (quote.getSideA().isEmpty()) {
            sides.add(quote.getProviderA().name() + " side");
        }
        if (quote.getSideZ().isEmpty()) {
            sides.add(quote.getProviderZ().name() + " side");
        }
        if (!sides.isEmpty()) {
            return String.join(" and ", sides) + " unpriced";
        }
        return quote.isMixedCurrency() ? "the two sides are in different currencies" : "a component is unpriced";
    }

    // ── design_list_multicloud_environments ─────────────────────────────────

    static ToolRegistration listMulticloudEnvironments() {
        return ToolRegistration.builder()
                .name("design_list_multicloud_environments")
                .title("List native multicloud environments (region pairs)")
                .description("Beta. Lists the native provider-to-provider multicloud environments in the "
                        + "catalog bundled with this server: pairs of cloud regions that two providers "
                        + "interconnect directly, with no Equinix resource on the path. Each entry gives "
                        + "both regions, the launch stage observed (GA, PREVIEW, UNVERIFIED), the connection "
                        + "sizes in Mbps confirmed for the pair, the provider pages it was read from, and "
                        + "the date they were read. The catalog is a dated copy of provider documentation "
                        + "and is not refreshed at run time: this server calls no cloud-provider API. "
                        + "Providers add region pairs without notice, so an absent pair means 'not in this "
                        + "copy', not 'not offered'. The environment ids are catalog-local labels; no "
                        + "provider API accepts them. Filters: give a.cloud alone for every pair involving "
                        + "that cloud; give a.cloud and z.cloud for the pairs joining the two (order does "
                        + "not matter); add region to narrow further. With no filter every entry is "
                        + "returned.")
                .inputSchema(object(props(
                        "a", filterEndSchema("Filter: one cloud of the pair, optionally its region."),
                        "z", filterEndSchema("Filter: the other cloud of the pair, optionally its region. "
                                + "Must differ from a.cloud when both are given."))))
                .outputSchema(looseObject("environments (id, the two sites, status, "
                        + "supported_connection_sizes_mbps, sizes_note, sources, as_of, note), count, the "
                        + "catalog's own as-of date and disclaimer, and the filter that was applied."))
                .toolset(Toolset.DESIGN)
                .handler(MulticloudDesignTools::handleListEnvironments)
                .build();
    }

    private static Map<String, Object> filterEndSchema(String description) {
        Map<String, Object> schema = object(props(
                        "cloud", stringEnum("The cloud provider.", DesignToolFactory.CLOUD_VALUES),
                        "region", string("The provider's own region name, e.g. 'us-east-1'. Compared after "
                                + "trimming, ignoring case.")),
                "cloud");
        schema.put("description", description);
        return schema;
    }

    private static ObjectNode handleListEnvironments(JsonNode args, ServerContext ctx) {
        Optional<FilterEnd> a = filterEnd(args, "a");
        Optional<FilterEnd> z = filterEnd(args, "z");
        if (a.isPresent() && z.isPresent() && a.get().provider() == z.get().provider()) {
            throw new IllegalArgumentException("'a.cloud' and 'z.cloud' must differ: an environment joins two "
                    + "different providers. To list every pair involving one cloud, give 'a' only.");
        }

        MulticloudEnvironmentCatalog catalog = ctx.multicloudEnvironments();
        List<MulticloudEnvironment> matched = new ArrayList<>();
        for (MulticloudEnvironment env : catalog.environments()) {
            if (a.map(end -> end.matches(env)).orElse(true) && z.map(end -> end.matches(env)).orElse(true)) {
                matched.add(env);
            }
        }

        ObjectMapper mapper = ctx.objectMapper();
        ObjectNode payload = mapper.createObjectNode();
        payload.put("beta", true);
        ArrayNode environments = payload.putArray("environments");
        for (MulticloudEnvironment env : matched.subList(0, Math.min(matched.size(), MAX_ENVIRONMENTS))) {
            environmentFields(environments.addObject(), env);
        }
        payload.put("count", matched.size());
        if (matched.size() > MAX_ENVIRONMENTS) {
            payload.put("truncated", true);
            payload.put("truncation_note", "Showing " + MAX_ENVIRONMENTS + " of " + matched.size()
                    + " environments; narrow the filter.");
        }
        ObjectNode filter = payload.putObject("filter");
        a.ifPresent(end -> filter.set("a", end.toNode(mapper)));
        z.ifPresent(end -> filter.set("z", end.toNode(mapper)));
        payload.put("catalog_size", catalog.environments().size());
        payload.put("catalog_as_of", catalog.asOf());
        payload.put("disclaimer", catalog.disclaimer());
        payload.put("staleness_note", "Dated copy of provider documentation, not refreshed at run time. An "
                + "absent pair means 'not in this copy', not 'not offered'. Each entry's sources and as_of say "
                + "where and when it was read.");
        return payload;
    }

    /** Writes one catalog entry's fields onto {@code node}; shared by the list and compare tools. */
    private static void environmentFields(ObjectNode node, MulticloudEnvironment env) {
        node.put("id", env.getEnvironment().getEnvironmentId());
        node.put("id_scope", "catalog-local label; not accepted by any provider API");
        ArrayNode sites = node.putArray("sites");
        for (ProviderSite site : env.getEnvironment().getProviderSites()) {
            ObjectNode s = sites.addObject();
            s.put("cloud", MulticloudEnvironment.cloudProviderOf(site.getProviderRef())
                    .map(Enum::name).orElse(null));
            s.put("provider_id", site.getProviderRef().id());
            s.put("region", site.getSite());
            if (site.getDisplayName() != null) {
                s.put("display_name", site.getDisplayName());
            }
        }
        node.put("status", env.getStatus().name());
        ArrayNode sizes = node.putArray("supported_connection_sizes_mbps");
        env.sizesMbps().toList().forEach(sizes::add);
        if (env.getSizesNote() != null) {
            node.put("sizes_note", env.getSizesNote());
        }
        ArrayNode sources = node.putArray("sources");
        env.getSourceUrls().forEach(sources::add);
        node.put("as_of", env.getAsOf());
        if (env.getNote() != null) {
            node.put("note", env.getNote());
        }
    }

    // ── design_plan_deployment: native multicloud links ─────────────────────

    /**
     * Adds {@code native_multicloud_links} and {@code native_multicloud_scope} to a plan payload, and
     * the native-link figures to its {@code pricing} block. Adds nothing for a plan without links,
     * so the payload of a single-cloud plan is unchanged.
     */
    static void serializePlanLinks(ObjectNode payload, DeploymentPlan plan, ObjectMapper mapper) {
        List<PlannedMulticloudInterconnect> links = plan.multicloudLinksOrEmpty();
        if (links.isEmpty()) {
            return;
        }
        payload.put("native_multicloud_scope", "Beta. Native provider-to-provider links between two clouds. "
                + "They use no Equinix resource, are billed by the cloud providers, and are never provisioned "
                + "by this server or by the SDK's plan execution. They are excluded from the plan's resource "
                + "count and from pricing.monthly_total and pricing.setup_total. Roles: ALTERNATIVE = the plan "
                + "keeps the Equinix connections for the flow; REPLACEMENT = the plan omits the named Equinix "
                + "connections and depends on the link; UNAVAILABLE = a native link was required and the "
                + "catalog has no usable environment (a validation error).");
        ArrayNode array = payload.putArray("native_multicloud_links");
        for (PlannedMulticloudInterconnect link : links) {
            ObjectNode n = array.addObject();
            n.put("name", link.getName());
            n.put("role", String.valueOf(link.getRole()));
            n.put("strategy", String.valueOf(link.getStrategy()));
            n.set("a", endNode(mapper, link.getProviderA(), link.getRegionA()));
            n.set("z", endNode(mapper, link.getProviderZ(), link.getRegionZ()));
            n.put("metro", link.getMetro() == null ? null : String.valueOf(link.getMetro()));
            n.put("requested_mbps", link.getRequestedMbps());
            n.put("covering_tier_mbps", link.getCoveringTierMbps());
            n.put("rounded_up", link.isRoundedUp());
            if (link.getEnvironment() == null) {
                n.putNull("environment");
            }
            else {
                environmentFields(n.putObject("environment"), link.getEnvironment());
            }
            addAll(n.putArray("workloads"), link.getWorkloadLabels());
            addAll(n.putArray("equinix_connections"), link.getEquinixConnectionNames());
            addAll(n.putArray("replaced_connections"), link.getReplacedConnectionNames());
            ArrayNode legs = n.putArray("equinix_legs");
            for (PlannedMulticloudInterconnect.EquinixLeg leg : link.equinixLegsOrEmpty()) {
                ObjectNode l = legs.addObject();
                l.put("cloud", String.valueOf(leg.getCloud()));
                l.put("connection", leg.getConnectionName());
                l.put("connection_type", String.valueOf(leg.getConnectionType()));
                l.put("priced_mbps", leg.getPricedMbps());
                l.put("shared_with_other_workloads", leg.isSharedWithOtherWorkloads());
                l.put("on_plan", leg.isOnPlan());
            }
            MulticloudLinkPricing price = link.getPricing();
            if (price == null) {
                n.putNull("pricing");
            }
            else {
                ObjectNode p = n.putObject("pricing");
                p.put("native_priced", price.isNativePriced());
                p.put("native_monthly", price.getNativeMonthly());
                p.put("native_currency", price.getNativeCurrency());
                ArrayNode sides = p.putArray("native_sides");
                MulticloudLinkQuote quote = price.getNativeQuote();
                if (quote != null) {
                    ArrayNode ignored = mapper.createArrayNode();
                    sides.add(sideNode(mapper, quote.getProviderA(), quote.getRegionA(), quote.getSideA(),
                            quote.getSideAUnpricedReason(), ignored));
                    sides.add(sideNode(mapper, quote.getProviderZ(), quote.getRegionZ(), quote.getSideZ(),
                            quote.getSideZUnpricedReason(), ignored));
                }
                p.put("equinix_fixed_monthly", price.getEquinixFixedMonthly());
                p.put("equinix_currency", price.getEquinixCurrency());
                addAll(p.putArray("equinix_components"), price.getEquinixComponents());
                p.put("equinix_per_gb", price.getEquinixPerGb());
                p.put("native_per_gb", price.getNativePerGb());
                p.put("per_gb_currency", price.getPerGbCurrency());
                p.put("break_even_sustained_mbps", price.getBreakEvenSustainedMbps());
                if (price.getBreakEvenSustainedMbps() != null) {
                    p.put("cheaper_above_break_even", price.isNativeCheaperAboveBreakEven()
                            ? "native_multicloud_link" : "equinix_fabric");
                }
                addAll(p.putArray("notes"), price.getNotes());
            }
            n.put("recommendation", link.getRecommendation());
            addAll(n.putArray("reasoning"), link.getReasoning());
            addAll(n.putArray("create_then_accept_recipe"), link.getCreateThenAcceptRecipe());
            n.put("recipe_scope", "Performed by the customer with the two cloud providers, outside Fabric. "
                    + "The activation key in these steps is issued by one cloud provider and entered at the "
                    + "other; it is unrelated to this server's fabric_confirm_change confirm_token.");
            n.put("provisioned_by_this_server", false);
        }

        PlanPricing pricing = plan.getPricing();
        if (pricing != null && payload.get("pricing") instanceof ObjectNode p) {
            p.put("native_replacement_monthly", pricing.getNativeReplacementMonthlyCost());
            p.put("native_replacement_currency", pricing.getNativeReplacementCurrency());
            p.put("native_alternative_monthly", pricing.getNativeAlternativeMonthlyCost());
            p.put("native_alternative_currency", pricing.getNativeAlternativeCurrency());
            addAll(p.putArray("unpriced_multicloud_links"), pricing.getUnpricedMulticloudLinks());
            p.put("native_multicloud_disclaimer", pricing.getNativeMulticloudDisclaimer());
            p.put("native_multicloud_scope", "native_* figures are billed by the cloud providers and are not "
                    + "part of monthly_total or setup_total. null means unpriced, not zero.");
        }
    }

    // ── shared helpers ──────────────────────────────────────────────────────

    /** One end of a compared flow: a cloud and its region, both required. */
    record CloudEnd(CloudProviderType provider, String region) {

        ObjectNode toNode(ObjectMapper mapper) {
            return endNode(mapper, provider, region);
        }

        String describe() {
            return provider.name() + " " + region;
        }
    }

    /** One end of an environment filter: a cloud and, optionally, its region. */
    private record FilterEnd(CloudProviderType provider, String region) {

        boolean matches(MulticloudEnvironment env) {
            Optional<String> site = env.regionOf(provider);
            if (site.isEmpty()) {
                return false;
            }
            return region == null || site.get().trim().equalsIgnoreCase(region.trim());
        }

        ObjectNode toNode(ObjectMapper mapper) {
            ObjectNode node = endNode(mapper, provider, region);
            if (region == null) {
                node.remove("region");
            }
            return node;
        }
    }

    private static ObjectNode endNode(ObjectMapper mapper, CloudProviderType provider, String region) {
        ObjectNode node = mapper.createObjectNode();
        node.put("cloud", provider == null ? null : provider.name());
        node.put("region", region);
        return node;
    }

    private static CloudEnd cloudEnd(JsonNode args, String field) {
        JsonNode node = args.get(field);
        if (node == null || !node.isObject()) {
            throw new IllegalArgumentException("'" + field + "' is required: an object with 'cloud' and "
                    + "'region', e.g. {\"cloud\": \"aws\", \"region\": \"us-east-1\"}.");
        }
        CloudProviderType provider = DesignToolFactory.cloudProvider(
                requireNested(node, field, "cloud"), field + ".cloud");
        return new CloudEnd(provider, requireNested(node, field, "region"));
    }

    private static Optional<FilterEnd> filterEnd(JsonNode args, String field) {
        JsonNode node = args.get(field);
        if (node == null || node.isNull()) {
            return Optional.empty();
        }
        if (!node.isObject()) {
            throw new IllegalArgumentException("'" + field + "' must be an object with 'cloud' and an optional "
                    + "'region', e.g. {\"cloud\": \"aws\"}.");
        }
        CloudProviderType provider = DesignToolFactory.cloudProvider(
                requireNested(node, field, "cloud"), field + ".cloud");
        return Optional.of(new FilterEnd(provider, optString(node, "region").orElse(null)));
    }

    private static String requireNested(JsonNode node, String parent, String field) {
        try {
            return requireString(node, field);
        }
        catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("'" + parent + "." + field + "' is required and must be a "
                    + "non-empty string.");
        }
    }

    private static Term term(Optional<Integer> months) {
        if (months.isEmpty()) {
            return Term.MONTH_12;
        }
        for (Term term : Term.values()) {
            if (term.months() == months.get()) {
                return term;
            }
        }
        throw new IllegalArgumentException("'term_months' value " + months.get()
                + " is not valid. Valid values: 1, 12, 24, 36.");
    }

    private static void addAll(ArrayNode array, List<String> values) {
        if (values != null) {
            values.forEach(array::add);
        }
    }

    private static String money(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private static String plain(double value) {
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }
}
