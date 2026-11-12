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


package com.eqixiac.equinix.design.optimizer.wizard.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Renders a {@link DeploymentPlan} as a plain-text summary and a Markdown report. Split out of
 * {@code DeploymentPlan} so the plan type holds data and lifecycle entry points only.
 */
final class PlanMarkdown {

    private PlanMarkdown() {
    }

    /**
     * Generates a concise plain-text summary of the deployment plan.
     */
    static String summary(DeploymentPlan plan) {
        StringBuilder sb = new StringBuilder();
        sb.append("Deployment Plan: ");
        sb.append(plan.getCloudRouters() != null ? plan.getCloudRouters().size() : 0).append(" Cloud Router(s), ");
        sb.append(plan.getProviderConnections() != null ? plan.getProviderConnections().size() : 0).append(" provider connection(s), ");
        sb.append(plan.getBackboneLinks() != null ? plan.getBackboneLinks().size() : 0).append(" backbone link(s), ");
        sb.append(plan.getRoutingProtocols() != null ? plan.getRoutingProtocols().size() : 0).append(" routing protocol(s). ");
        sb.append("Total resources: ").append(plan.totalResourceCount()).append(".");
        if (!plan.multicloudLinksOrEmpty().isEmpty()) {
            sb.append(" Native multicloud link(s): ").append(plan.multicloudLinksOrEmpty().size())
                    .append(" (not provisioned by this SDK; excluded from the resource count and from "
                            + "the cost below).");
        }

        // Money always renders with its ACTUAL currency (mirroring OptimizationResult.money) - a
        // hardcoded "$" next to a EUR figure misstated the amount by the exchange rate.
        if (plan.getPricing() != null && plan.getPricing().getMonthlyTotal() != null) {
            sb.append(" Estimated monthly cost: ")
                    .append(money(plan.getPricing().getMonthlyTotal(), plan.getPricing().getCurrency())).append(".");
        }
        else if (plan.getPricing() != null && plan.getPricing().getMonthlyByCurrency() != null
                && plan.getPricing().getMonthlyByCurrency().size() > 1) {
            sb.append(" Estimated monthly cost spans multiple currencies: ")
                    .append(describeByCurrency(plan.getPricing().getMonthlyByCurrency()))
                    .append(" (no single-currency total).");
        }

        if (!plan.isValid() && plan.getValidationErrors() != null && !plan.getValidationErrors().isEmpty()) {
            sb.append(" VALIDATION ERRORS: ").append(plan.getValidationErrors().size());
        }

        if (plan.getDeferredValidations() != null && !plan.getDeferredValidations().isEmpty()) {
            sb.append(" DEFERRED TO PROVISIONING: ").append(plan.getDeferredValidations().size())
                    .append(" (validated at provisioning; needs your authorization keys).");
        }

        if (plan.getSkippedValidations() != null && !plan.getSkippedValidations().isEmpty()) {
            sb.append(" SKIPPED (could not validate now): ").append(plan.getSkippedValidations().size())
                    .append(" (offline or the API could not be reached - see the reasons).");
        }

        long roundedUp = plan.getProviderConnections() == null ? 0 : plan.getProviderConnections().stream()
                .filter(c -> c.getProfileSelection() != null && c.getProfileSelection().isRoundedUp())
                .count();
        if (roundedUp > 0) {
            sb.append(" BANDWIDTH ROUNDED UP: ").append(roundedUp)
                    .append(" connection(s) to the nearest service-profile tier (raises billed bandwidth).");
        }

        return sb.toString();
    }

    /**
     * Generates a full markdown-formatted deployment plan report with bandwidth
     * and plan.getPricing() tables.
     */
    static String markdown(DeploymentPlan plan) {
        StringBuilder md = new StringBuilder();
        md.append("# Deployment Plan\n\n");
        md.append("Plan id `").append(plan.planId()).append("`, deployment `").append(plan.getDeploymentName())
                .append("`");
        if (plan.getProjectId() != null) {
            md.append(", project `").append(plan.getProjectId()).append("`");
        }
        md.append(".\n");
        if (plan.getSpecFingerprint() != null) {
            md.append("Derived from design spec `").append(plan.getSpecFingerprint()).append("`.\n");
        }
        if (plan.getSourceComputedAt() != null) {
            md.append("_Generated from optimization computed at ").append(plan.getSourceComputedAt()).append("_\n");
        }
        md.append("\n");
        md.append("**Total Resources:** ").append(plan.totalResourceCount());
        if (!plan.multicloudLinksOrEmpty().isEmpty()) {
            md.append(" (excludes ").append(plan.multicloudLinksOrEmpty().size())
                    .append(" native multicloud link(s), which this SDK does not create)");
        }
        md.append("\n\n");

        // Validation status - validated now (structural + router dry-run).
        if (!plan.isValid() && plan.getValidationErrors() != null && !plan.getValidationErrors().isEmpty()) {
            md.append("> **VALIDATION ERRORS** (validated now - structural + router dry-run)\n");
            for (String error : plan.getValidationErrors()) {
                md.append("> - ").append(error).append("\n");
            }
            md.append("\n");
        }

        // Deferred to provisioning - the live connection endpoint dry-run (needs the target Cloud
        // Router to exist and the customer's authorization keys). These are NOT errors.
        if (plan.getDeferredValidations() != null && !plan.getDeferredValidations().isEmpty()) {
            md.append("> **DEFERRED TO PROVISIONING** - the live connection endpoint dry-run runs when the "
                    + "plan is applied (needs your authorization keys)\n");
            for (String note : plan.getDeferredValidations()) {
                md.append("> - ").append(note).append("\n");
            }
            md.append("\n");
        }

        // Skipped - validations that could NOT be attempted now (an offline/bare-stub run, or the API
        // answered a live dry-run with a non-rejection failure such as 403/429/5xx/timeout). These are
        // NOT errors: each note says what could not be checked and why, so the gap is called out.
        if (plan.getSkippedValidations() != null && !plan.getSkippedValidations().isEmpty()) {
            md.append("> **SKIPPED (could not validate now)** - these checks could not be attempted; "
                    + "each reason says why. They do not invalidate the plan.\n");
            for (String note : plan.getSkippedValidations()) {
                md.append("> - ").append(note).append("\n");
            }
            md.append("\n");
        }

        // Bandwidth round-up + profile choice - surfaced, never silent, since rounding up raises the
        // billed bandwidth and several profiles may cover a connection in different ways.
        appendBandwidthSelectionNotes(plan, md);

        // Required customer inputs - what a first-time customer must gather before provisioning.
        if (plan.getRequiredInputs() != null && !plan.getRequiredInputs().isEmpty()) {
            md.append("## Required Customer Inputs (before provisioning)\n\n");
            md.append("| Connection | Provider | Authorization key | VLAN | Azure peering |\n");
            md.append("|------------|----------|-------------------|------|---------------|\n");
            for (ConnectionInputRequirement req : plan.getRequiredInputs()) {
                md.append("| ").append(req.getConnectionName())
                        .append(" | ").append(req.getProviderLabel() != null ? req.getProviderLabel() : "-")
                        .append(" | ");
                if (req.isAuthenticationKeyRequired()) {
                    md.append(req.getAuthenticationKeyLabel())
                            .append(req.isAuthenticationKeyProvided() ? " (provided)" : "");
                } else {
                    md.append("-");
                }
                md.append(" | ").append(req.isVlanTagRequired() ? "required" : "-")
                        .append(" | ").append(req.isPeeringTypeRequired() ? "required" : "-")
                        .append(" |\n");
            }
            md.append("\n");
        }

        // Cloud Routers
        if (plan.getCloudRouters() != null && !plan.getCloudRouters().isEmpty()) {
            md.append("## Cloud Routers\n\n");
            md.append("| Name | Metro | Package |\n");
            md.append("|------|-------|---------|\n");
            for (PlannedCloudRouter cr : plan.getCloudRouters()) {
                md.append("| ").append(cr.getName())
                        .append(" | ").append(cr.getMetroId())
                        .append(" | ").append(cr.getPackageCode())
                        .append(" |\n");
            }
            md.append("\n");
        }

        // Provider Connections
        if (plan.getProviderConnections() != null && !plan.getProviderConnections().isEmpty()) {
            md.append("## Provider Connections\n\n");
            md.append("| Name | Type | A-Side (Metro) | Z-Side (Provider) | Bandwidth | Workload Breakdown |\n");
            md.append("|------|------|----------------|-------------------|-----------|--------------------|\n");
            for (PlannedConnection conn : plan.getProviderConnections()) {
                md.append("| ").append(conn.getName())
                        .append(" | ").append(conn.getConnectionType())
                        .append(" | ").append(conn.getASideRouterName()).append(" (").append(conn.getASideMetro()).append(")")
                        .append(" | ").append(conn.getZSideProviderLabel());
                if (conn.getZSideSellerRegion() != null) {
                    md.append(" [").append(conn.getZSideSellerRegion()).append("]");
                }
                md.append(" | ").append(conn.getBandwidthMbps()).append(" Mbps");
                ProfileSelection sel = conn.getProfileSelection();
                if (sel != null && sel.isRoundedUp()) {
                    md.append(" (rounded up from ").append(sel.getRequestedMbps()).append(")");
                }
                md.append(" | ");
                if (conn.getBandwidthAllocation() != null && conn.getBandwidthAllocation().getPerWorkload() != null) {
                    md.append(conn.getBandwidthAllocation().getPerWorkload().entrySet().stream()
                            .map(e -> e.getKey() + ": " + e.getValue() + " Mbps")
                            .collect(Collectors.joining(", ")));
                }
                md.append(" |\n");
            }
            md.append("\n");
        }

        // Backbone Links
        if (plan.getBackboneLinks() != null && !plan.getBackboneLinks().isEmpty()) {
            md.append("## Backbone Links\n\n");
            md.append("| Name | Metro A | Metro Z | Bandwidth | Topology |\n");
            md.append("|------|---------|---------|-----------|----------|\n");
            for (PlannedBackboneLink link : plan.getBackboneLinks()) {
                md.append("| ").append(link.getName())
                        .append(" | ").append(link.getMetroA())
                        .append(" | ").append(link.getMetroZ())
                        .append(" | ").append(link.getBandwidthMbps()).append(" Mbps")
                        .append(" | ").append(link.getTopology())
                        .append(" |\n");
            }
            md.append("\n");
        }

        // Routing Protocols
        if (plan.getRoutingProtocols() != null && !plan.getRoutingProtocols().isEmpty()) {
            md.append("## Routing Protocols\n\n");
            md.append("| Connection | Type | Customer Peer | Equinix Peer | ASN | BFD |\n");
            md.append("|------------|------|---------------|--------------|-----|-----|\n");
            for (PlannedRoutingProtocol rp : plan.getRoutingProtocols()) {
                md.append("| ").append(rp.getConnectionName())
                        .append(" | ").append(rp.getType())
                        .append(" | ").append(rp.getCustomerPeerIpv4() != null ? rp.getCustomerPeerIpv4() : "-")
                        .append(" | ").append(rp.getEquinixPeerIpv4() != null ? rp.getEquinixPeerIpv4() : rp.getEquinixIfaceIpv4())
                        .append(" | ").append(rp.getCustomerAsn() != null ? rp.getCustomerAsn() : "-")
                        .append(" | ").append(rp.isBfdEnabled() ? rp.getBfdInterval() + "ms" : "off")
                        .append(" |\n");
            }
            md.append("\n");
        }

        appendMulticloudLinks(plan, md);
        appendInterconnects(plan, md);

        // Pricing. Every figure renders with its ACTUAL currency (mirroring OptimizationResult.money)
        // - never a hardcoded "$", which misstated any non-USD amount by the exchange rate. A category
        // that itself spans currencies carries no single figure; its per-currency subtotals are shown.
        if (plan.getPricing() != null) {
            md.append("## Cost Estimate\n\n");
            md.append("| Category | Monthly Cost |\n");
            md.append("|----------|-------------:|\n");
            appendCostRow(md, "Cloud Routers", plan.getPricing().getRouterMonthlyCost(),
                    plan.getPricing().getRouterCurrency(), plan.getPricing().getRouterMonthlyByCurrency());
            appendCostRow(md, "Provider Connections", plan.getPricing().getProviderConnectionMonthlyCost(),
                    plan.getPricing().getProviderConnectionCurrency(), plan.getPricing().getProviderConnectionMonthlyByCurrency());
            appendCostRow(md, "Backbone Links", plan.getPricing().getBackboneMonthlyCost(),
                    plan.getPricing().getBackboneCurrency(), plan.getPricing().getBackboneMonthlyByCurrency());
            if (plan.getPricing().getMonthlyTotal() != null) {
                md.append("| **Total Monthly** | **")
                        .append(money(plan.getPricing().getMonthlyTotal(), plan.getPricing().getCurrency())).append("** |\n");
            }
            else if (plan.getPricing().getMonthlyByCurrency() != null && plan.getPricing().getMonthlyByCurrency().size() > 1) {
                md.append("| **Total Monthly** | **")
                        .append(describeByCurrency(plan.getPricing().getMonthlyByCurrency()))
                        .append("** (multiple currencies - no single total) |\n");
            }
            if (plan.getPricing().getSetupTotal() != null) {
                md.append("| Setup (one-time) | ")
                        .append(money(plan.getPricing().getSetupTotal(), plan.getPricing().getCurrency())).append(" |\n");
            }
            md.append("\n_").append(plan.getPricing().getDisclaimer()).append("_\n\n");
            if (!plan.multicloudLinksOrEmpty().isEmpty()) {
                md.append("_Native multicloud link charges are billed by the cloud providers and are not "
                        + "included in the table above; see Native multicloud alternatives._\n\n");
            }
        }

        // Execution instructions
        md.append("## Execution\n\n");
        md.append("Resources will be created in this order:\n\n");
        md.append("1. Cloud Routers (").append(size(plan.getCloudRouters())).append(")\n");
        md.append("2. Provider Connections (").append(size(plan.getProviderConnections())).append(")\n");
        md.append("3. Backbone Links (").append(size(plan.getBackboneLinks())).append(")\n");
        md.append("4. Routing Protocols (").append(size(plan.getRoutingProtocols())).append(")\n\n");
        md.append("Call `plan.dryRun()` to validate, `plan.diff()` to compare with live state, ")
                .append("`plan.reconcile(...)` to create only what is missing (resumable from a checkpoint), ")
                .append("or `plan.execute(...)` to provision everything in one run.\n");
        if (!plan.multicloudLinksOrEmpty().isEmpty()) {
            md.append("\nExecution does not create the ").append(plan.multicloudLinksOrEmpty().size())
                    .append(" native multicloud link(s): they are created outside Fabric with the two cloud "
                            + "providers (create, then accept with the activation key).\n");
        }
        if (!plan.interconnectsOrEmpty().isEmpty()) {
            md.append("\nExecution does not create the ").append(plan.interconnectsOrEmpty().size())
                    .append(" Fabric Interconnect(s): ").append(PlannedInterconnect.NOT_EXECUTABLE_REASON).append("\n");
        }

        return md.toString();
    }

    /**
     * Appends the "Native multicloud alternatives" section: one table row per native link (clouds
     * and regions, environment status with its as-of date, requested and covering bandwidth, the
     * native monthly fee, the Equinix path's fixed monthly cost for the same flow, the break-even
     * sustained rate, the role), then per link the recommendation, the reasoning, each provider's
     * side of the quote, the Equinix cost components, the plan.getPricing() notes and the create-then-accept
     * steps. Every amount goes through {@code money(amount, currency)} with its own currency;
     * an unpriced amount renders as {@code unpriced}, never as zero. Emits nothing when the plan
     * has no native link.
     */
    private static void appendMulticloudLinks(DeploymentPlan plan, StringBuilder md) {
        List<PlannedMulticloudInterconnect> links = plan.multicloudLinksOrEmpty();
        if (links.isEmpty()) {
            return;
        }
        md.append("## Native multicloud alternatives\n\n");
        md.append("Beta. Direct links between two clouds that use no Equinix resource. This SDK plans and "
                + "prices them and does not create them; the cloud providers bill them. ALTERNATIVE: the plan "
                + "keeps the Equinix connections for the flow. REPLACEMENT: the plan omits the named Equinix "
                + "connections and depends on the link. UNAVAILABLE: a native link was required and the "
                + "catalog has no usable environment.\n\n");
        md.append("| Name | Clouds | Environment | Bandwidth | Native monthly | Equinix path fixed monthly "
                + "| Break-even (sustained) | Role |\n");
        md.append("|------|--------|-------------|-----------|---------------:|---------------------------:"
                + "|-----------------------:|------|\n");
        for (PlannedMulticloudInterconnect link : links) {
            MulticloudLinkPricing price = link.getPricing();
            md.append("| ").append(link.getName())
                    .append(" | ").append(link.getProviderA())
                    .append(link.getRegionA() != null ? " " + link.getRegionA() : "")
                    .append(" <-> ").append(link.getProviderZ())
                    .append(link.getRegionZ() != null ? " " + link.getRegionZ() : "")
                    .append(" | ");
            if (link.getEnvironment() == null) {
                md.append("none in catalog");
            } else {
                md.append(link.getEnvironment().getStatus());
                if (link.getEnvironment().getAsOf() != null) {
                    md.append(" (as of ").append(link.getEnvironment().getAsOf()).append(")");
                }
            }
            md.append(" | ");
            if (link.getCoveringTierMbps() == null) {
                md.append(link.getRequestedMbps()).append(" Mbps (no covering size)");
            } else {
                md.append(link.getCoveringTierMbps()).append(" Mbps");
                if (link.isRoundedUp()) {
                    md.append(" (rounded up from ").append(link.getRequestedMbps()).append(")");
                }
            }
            md.append(" | ").append(price == null ? "unpriced"
                            : moneyOrUnpriced(price.getNativeMonthly(), price.getNativeCurrency()))
                    .append(" | ").append(price == null ? "unpriced"
                            : moneyOrUnpriced(price.getEquinixFixedMonthly(), price.getEquinixCurrency()))
                    .append(" | ").append(price == null || price.getBreakEvenSustainedMbps() == null
                            ? "n/a" : price.getBreakEvenSustainedMbps().toPlainString() + " Mbps")
                    .append(" | ").append(link.getRole())
                    .append(" |\n");
        }
        md.append("\n");

        for (PlannedMulticloudInterconnect link : links) {
            md.append("- **").append(link.getName()).append("**: ")
                    .append(link.getRecommendation() != null ? link.getRecommendation() : "No recommendation.")
                    .append("\n");
            if (link.getWorkloadLabels() != null && !link.getWorkloadLabels().isEmpty()) {
                md.append("  - Workloads: ").append(String.join(", ", link.getWorkloadLabels())).append("\n");
            }
            appendIndented(md, link.getReasoning());
            MulticloudLinkPricing price = link.getPricing();
            if (price != null) {
                if (price.getNativeQuote() != null) {
                    appendNativeSide(md, price.getNativeQuote().getProviderA(), price.getNativeQuote().getSideA(),
                            price.getNativeQuote().getSideAUnpricedReason());
                    appendNativeSide(md, price.getNativeQuote().getProviderZ(), price.getNativeQuote().getSideZ(),
                            price.getNativeQuote().getSideZUnpricedReason());
                }
                appendIndented(md, price.getEquinixComponents());
                appendIndented(md, price.getNotes());
            }
            if (link.getEnvironment() != null && !link.getEnvironment().getSourceUrls().isEmpty()) {
                md.append("  - Environment source: ")
                        .append(String.join(", ", link.getEnvironment().getSourceUrls())).append("\n");
            }
            if (link.getCreateThenAcceptRecipe() != null && !link.getCreateThenAcceptRecipe().isEmpty()) {
                md.append("  - Create-then-accept steps (performed by the customer, outside Fabric):\n");
                int step = 1;
                for (String line : link.getCreateThenAcceptRecipe()) {
                    md.append("    ").append(step++).append(". ").append(line).append("\n");
                }
            }
        }
        md.append("\n");
        if (plan.getPricing() != null && plan.getPricing().getNativeMulticloudDisclaimer() != null) {
            md.append("_").append(plan.getPricing().getNativeMulticloudDisclaimer()).append("_\n\n");
        }
    }

    private static void appendNativeSide(StringBuilder md, Object provider,
                                         java.util.Optional<com.eqixiac.equinix.design.value.ratecard.PriceQuote> side,
                                         String unpricedReason) {
        md.append("  - Native link, ").append(provider).append(" side: ");
        if (side.isPresent()) {
            String currency = side.get().getCurrency() != null ? side.get().getCurrency().getCurrencyCode() : null;
            md.append(money(side.get().getMonthlyRecurring(), currency)).append(" per month (")
                    .append(side.get().getSource()).append(")");
            if (side.get().getNote() != null && !side.get().getNote().isBlank()) {
                md.append(": ").append(side.get().getNote());
            }
        } else {
            md.append("unpriced").append(unpricedReason == null || unpricedReason.isBlank()
                    ? "" : ": " + unpricedReason);
        }
        md.append("\n");
    }

    private static void appendIndented(StringBuilder md, List<String> lines) {
        if (lines == null) {
            return;
        }
        for (String line : lines) {
            md.append("  - ").append(line).append("\n");
        }
    }

    /** {@code money(amount, currency)}, with {@code unpriced} for a {@code null} amount. */
    private static String moneyOrUnpriced(BigDecimal amount, String currency) {
        return amount == null ? "unpriced" : money(amount, currency);
    }

    /**
     * Appends one Cost Estimate row: the single-currency figure rendered with its actual currency
     * when the category reconciled, otherwise the category's per-currency subtotals (a mixed
     * category has no single figure). Emits nothing when the category has neither.
     */
    private static void appendCostRow(StringBuilder md, String label, BigDecimal cost,
                                      String currency, Map<String, BigDecimal> byCurrency) {
        if (cost != null) {
            md.append("| ").append(label).append(" | ").append(money(cost, currency)).append(" |\n");
        }
        else if (byCurrency != null && !byCurrency.isEmpty()) {
            md.append("| ").append(label).append(" | ").append(describeByCurrency(byCurrency))
                    .append(" (multiple currencies) |\n");
        }
    }

    /**
     * Renders a monetary amount with the symbol of its actual currency ({@code "$2300.00"},
     * {@code "€1800.00"}), falling back to {@code "<amount> <code>"} when the code has no distinct
     * symbol or is unknown, and to the bare amount when no currency was stated at all. Mirrors
     * {@code OptimizationResult.money} and replaces the former hardcoded {@code "$"}, which asserted
     * US dollars against figures that live Fabric plan.getPricing() legitimately quotes in other currencies.
     */
    static String money(BigDecimal amount, String currency) {
        if (amount == null) {
            return "unavailable";
        }
        if (currency == null || currency.isBlank()) {
            return amount.toPlainString();
        }
        try {
            String symbol = java.util.Currency.getInstance(currency).getSymbol(java.util.Locale.US);
            if (!symbol.equals(currency)) {
                return symbol + amount.toPlainString();
            }
        }
        catch (IllegalArgumentException notIso4217) {
            // Not an ISO 4217 code: fall through to the "<amount> <code>" form below.
        }
        return amount.toPlainString() + " " + currency;
    }

    /**
     * Renders a per-currency breakdown, e.g. {@code "USD 2300.00, EUR 1800.00"}, used wherever
     * amounts span currencies and no single aggregate figure exists.
     */
    static String describeByCurrency(Map<String, BigDecimal> byCurrency) {
        if (byCurrency == null || byCurrency.isEmpty()) {
            return "unavailable";
        }
        return byCurrency.entrySet().stream()
                .map(e -> e.getKey() + " " + e.getValue().toPlainString())
                .collect(Collectors.joining(", "));
    }

    /**
     * Appends the bandwidth round-up and profile-choice notes to the markdown report. A round-up raises
     * the billed bandwidth, so it is called out with the requirement, the billed tier, and the increase;
     * a connection with more than one covering profile is called out as a decision point (the default is
     * used, but the alternatives are named for review). Emits nothing when neither applies.
     */
    private static void appendBandwidthSelectionNotes(DeploymentPlan plan, StringBuilder md) {
        if (plan.getProviderConnections() == null || plan.getProviderConnections().isEmpty()) {
            return;
        }
        List<PlannedConnection> roundedUp = new ArrayList<>();
        List<PlannedConnection> multiChoice = new ArrayList<>();
        for (PlannedConnection conn : plan.getProviderConnections()) {
            ProfileSelection sel = conn.getProfileSelection();
            if (sel == null) {
                continue;
            }
            if (sel.isRoundedUp()) {
                roundedUp.add(conn);
            }
            if (sel.hasChoice()) {
                multiChoice.add(conn);
            }
        }
        if (roundedUp.isEmpty() && multiChoice.isEmpty()) {
            return;
        }
        if (!roundedUp.isEmpty()) {
            md.append("> **BANDWIDTH ROUNDED UP** - a requirement with no exact service-profile tier was "
                    + "rounded up to the smallest tier that satisfies it (this raises the billed bandwidth)\n");
            for (PlannedConnection conn : roundedUp) {
                ProfileSelection sel = conn.getProfileSelection();
                md.append("> - ").append(conn.getName()).append(": ")
                        .append(sel.getRequestedMbps()).append(" -> ").append(sel.getSelectedTierMbps())
                        .append(" Mbps (+").append(sel.roundedUpByMbps()).append(" Mbps billable)\n");
            }
            md.append("\n");
        }
        if (!multiChoice.isEmpty()) {
            md.append("> **PROFILE CHOICE** - more than one service profile can carry these connections; "
                    + "the tightest-fitting default is used, and the alternatives are available to choose\n");
            for (PlannedConnection conn : multiChoice) {
                ProfileSelection sel = conn.getProfileSelection();
                String alts = sel.getAlternatives().stream()
                        .map(c -> c.getServiceProfileUuid() + " @ " + c.getCoveringTierMbps() + " Mbps")
                        .collect(Collectors.joining(", "));
                md.append("> - ").append(conn.getName()).append(": default ")
                        .append(sel.getSelectedProfileUuid()).append("; options: ").append(alts).append("\n");
            }
            md.append("\n");
        }
    }

    private static void appendInterconnects(DeploymentPlan plan, StringBuilder md) {
        List<PlannedInterconnect> ics = plan.interconnectsOrEmpty();
        if (ics.isEmpty()) {
            return;
        }
        md.append("## Fabric Interconnects (planned, not executable)\n\n");
        md.append("| Name | Metro | Bandwidth | Existing UUID | Price |\n|------|-------|-----------|---------------|-------|\n");
        for (PlannedInterconnect ic : ics) {
            md.append("| ").append(ic.getName()).append(" | ").append(ic.getMetro())
                    .append(" | ").append(ic.getBandwidthMbps() == null ? "-" : ic.getBandwidthMbps() + " Mbps")
                    .append(" | ").append(ic.getExistingInterconnectUuid() == null ? "-" : ic.getExistingInterconnectUuid())
                    .append(" | unpriced |\n");
        }
        md.append("\n_").append(PlannedInterconnect.NOT_EXECUTABLE_REASON).append(" No public price exists, so ")
                .append("the cost is unknown, not zero, and is not in the totals._\n\n");
    }

    private static int size(List<?> list) {
        return list == null ? 0 : list.size();
    }
}
