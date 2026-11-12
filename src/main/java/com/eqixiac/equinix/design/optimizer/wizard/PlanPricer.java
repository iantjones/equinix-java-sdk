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


package com.eqixiac.equinix.design.optimizer.wizard;

import com.eqixiac.equinix.FabricGateway;
import com.eqixiac.equinix.core.enums.MetroCode;
import com.eqixiac.equinix.core.model.MetroId;
import com.eqixiac.equinix.fabric.enums.ConnectionType;
import com.eqixiac.equinix.fabric.enums.RoutingProtocolType;
import com.eqixiac.equinix.design.optimizer.model.*;
import com.eqixiac.equinix.design.optimizer.wizard.enums.BackboneTopology;
import com.eqixiac.equinix.design.optimizer.wizard.enums.BandwidthStrategy;
import com.eqixiac.equinix.design.optimizer.wizard.enums.ConnectionPurpose;
import com.eqixiac.equinix.design.optimizer.wizard.enums.MulticloudLinkRole;
import com.eqixiac.equinix.design.optimizer.wizard.model.*;
import com.eqixiac.equinix.design.value.CurrencyReconciler;
import com.eqixiac.equinix.design.value.ratecard.EquinixRateCard;
import com.eqixiac.equinix.design.value.ratecard.MulticloudLinkQuote;
import com.eqixiac.equinix.design.value.ratecard.ReferenceRateCard;
import com.eqixiac.equinix.design.value.ratecard.PriceQuote;
import com.eqixiac.equinix.design.value.ratecard.PriceSource;
import com.eqixiac.equinix.design.value.ratecard.RateCard;
import com.eqixiac.equinix.design.value.ratecard.Term;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Prices a plan's Cloud Routers, provider connections and backbone links (and reports already-priced
 * native multicloud links beside the Equinix totals). The wizard's single pricing authority, split out
 * of {@code DeploymentWizardEngine} so plan generation and pricing can be read and changed separately;
 * {@code DeploymentWizard.Builder.reprice} and {@code MulticloudLinkPlanner} price through it too.
 */
final class PlanPricer {

    private PlanPricer() {
    }

    //  Phase 5: Pricing

    // Package-private (not private) so DeploymentWizard - in this package - can reprice an existing
    // plan after its connections change (e.g. an MCP profile choice altered a billable tier). The
    // logic is unchanged; only the visibility is widened for that one same-package caller.
    static PlanPricing estimatePricing(
            DeploymentWizard.Builder config,
            List<PlannedCloudRouter> routers,
            List<PlannedConnection> providerConnections,
            List<PlannedBackboneLink> backboneLinks) {
        return estimatePricing(config, routers, providerConnections, backboneLinks, Collections.emptyList());
    }

    /**
     * As the four-argument overload, and additionally reports the already-priced native multicloud
     * links in the {@code native*} fields of {@link PlanPricing}. No native-link amount enters the
     * plan-wide or per-category reconcilers: the Equinix totals are the same with or without links.
     */
    static PlanPricing estimatePricing(
            DeploymentWizard.Builder config,
            List<PlannedCloudRouter> routers,
            List<PlannedConnection> providerConnections,
            List<PlannedBackboneLink> backboneLinks,
            List<PlannedMulticloudInterconnect> pricedMulticloudLinks) {
        return estimatePricing(config, resolveRateCard(config), routers, providerConnections, backboneLinks,
                pricedMulticloudLinks);
    }

    /**
     * As the five-argument overload, pricing with the given resolved rate card instead of
     * resolving one from {@code config}. The caller that also prices native links passes the same
     * instance to {@code MulticloudLinkPlanner.price}, so one live card (and its cached
     * catalogue fetch) serves the whole plan.
     *
     * @param rateCard the resolved card, or {@code null} for the built-in heuristic only
     */
    static PlanPricing estimatePricing(
            DeploymentWizard.Builder config,
            RateCard rateCard,
            List<PlannedCloudRouter> routers,
            List<PlannedConnection> providerConnections,
            List<PlannedBackboneLink> backboneLinks,
            List<PlannedMulticloudInterconnect> pricedMulticloudLinks) {

        Term term = config.getTerm();
        String target = config.getPlanCurrency();
        com.eqixiac.equinix.design.cost.CurrencyConverters.Chain fx = config.getConverter();

        Map<String, BigDecimal> perConnectionCost = new LinkedHashMap<>();
        Set<PriceSource> sources = new HashSet<>();
        List<com.eqixiac.equinix.design.spi.CostLine> lines = new ArrayList<>();
        // Every price is restated in the plan currency first (CurrencyConverters.convert: the line keeps its
        // original amount, currency, rate, source and date). A price no rate source covers stays in its own
        // currency, and then every priced line flows through one plan-wide reconciler PLUS its category's
        // reconciler, so amounts in different currencies are never summed at the plan level or in a category.
        CurrencyReconciler recon = CurrencyReconciler.create();
        CurrencyReconciler routerRecon = CurrencyReconciler.create();
        CurrencyReconciler providerRecon = CurrencyReconciler.create();
        CurrencyReconciler backboneRecon = CurrencyReconciler.create();

        // Cloud Routers
        for (PlannedCloudRouter router : routers) {
            PriceQuote quote = priceRouter(rateCard, router, term);
            com.eqixiac.equinix.design.spi.CostLine line = inPlanCurrency(router.getName() != null ? router.getName()
                    : "Cloud Router in " + router.getMetroId(), router.getMetroId(), quote, target, fx);
            lines.add(line);
            recon.add(Currency.getInstance(line.currency()), line.monthly(), line.oneTime());
            routerRecon.add(Currency.getInstance(line.currency()), line.monthly(), line.oneTime());
            sources.add(line.source());
        }

        // Provider connections
        for (PlannedConnection conn : providerConnections) {
            PriceQuote quote = priceConnection(rateCard, conn.getConnectionType(),
                    conn.getBandwidthMbps(), conn.getASideMetro(), term);
            com.eqixiac.equinix.design.spi.CostLine line = inPlanCurrency(conn.getName(), conn.getASideMetro(), quote,
                    target, fx);
            lines.add(line);
            perConnectionCost.put(conn.getName(), line.monthly());
            recon.add(Currency.getInstance(line.currency()), line.monthly(), line.oneTime());
            providerRecon.add(Currency.getInstance(line.currency()), line.monthly(), line.oneTime());
            sources.add(line.source());
        }

        // Backbone links
        for (PlannedBackboneLink link : backboneLinks) {
            ConnectionType type = link.getConnection() != null ? link.getConnection().getConnectionType() : null;
            PriceQuote quote = priceConnection(rateCard, type, link.getBandwidthMbps(), link.getMetroA(), term);
            com.eqixiac.equinix.design.spi.CostLine line = inPlanCurrency(link.getName(), link.getMetroA(), quote,
                    target, fx);
            lines.add(line);
            perConnectionCost.put(link.getName(), line.monthly());
            recon.add(Currency.getInstance(line.currency()), line.monthly(), line.oneTime());
            backboneRecon.add(Currency.getInstance(line.currency()), line.monthly(), line.oneTime());
            sources.add(line.source());
        }
        List<String> currencyNotes = new ArrayList<>();
        com.eqixiac.equinix.design.cost.CurrencyConverters.advisory(
                new com.eqixiac.equinix.design.spi.PortfolioCost(lines, List.of())).ifPresent(currencyNotes::add);
        for (com.eqixiac.equinix.design.spi.CostLine line : lines) {
            if (!line.currency().equals(target)) {
                currencyNotes.add(line.label() + " stays in " + line.currency() + ": "
                        + fx.missingReason(line.currency(), target));
            }
        }
        currencyNotes.addAll(fx.notes());

        PriceSource source = dominantSource(sources);
        boolean authoritative = source == PriceSource.EQUINIX_LIVE || source == PriceSource.CUSTOM;
        boolean mixed = recon.isMixed();

        String currency = mixed ? null : recon.soleCurrencyOr(target);

        String disclaimer;
        if (mixed) {
            // Mixed currencies: do not fabricate a combined total. Omit the monthly/setup totals and
            // the single currency, and surface the per-currency subtotals. Each CATEGORY figure gets
            // the same treatment: kept (with its own currency) only when that category reconciles to
            // a single currency, otherwise nulled with its per-currency subtotals carrying the truth.
            disclaimer = "This plan's connections are priced in multiple currencies ("
                    + recon.describeCurrencies() + "): " + recon.describeMonthlySubtotals() + " per month. A single "
                    + "plan total cannot be formed without an FX rate, so the monthly and setup totals are omitted "
                    + "rather than reported as a fabricated cross-currency sum. A per-category figure is shown (in "
                    + "its own currency) only when that category reconciles to one currency; a category that itself "
                    + "spans currencies is reported as per-currency subtotals instead of a single figure.";
        }
        else {
            disclaimer = authoritative
                    ? "Based on live Equinix Fabric pricing. Actual costs may vary based on contract terms, volume "
                        + "discounts, and promotional offers."
                    : "Some or all figures are heuristic or reference estimates (live Fabric pricing was unavailable "
                        + "for part of this plan). Actual costs may vary; contact your Equinix account team for precise quotes.";
        }

        PlanPricing equinixPricing = PlanPricing.builder()
                .monthlyTotal(mixed ? null : recon.monthlyTotal().orElse(BigDecimal.ZERO))
                .setupTotal(mixed ? null : recon.setupTotal().orElse(BigDecimal.ZERO))
                .currency(currency)
                .monthlyByCurrency(recon.monthlySubtotals())
                .routerMonthlyCost(categoryMonthly(routerRecon))
                .routerCurrency(routerRecon.soleCurrencyOr(currency))
                .routerMonthlyByCurrency(routerRecon.monthlySubtotals())
                .providerConnectionMonthlyCost(categoryMonthly(providerRecon))
                .providerConnectionCurrency(providerRecon.soleCurrencyOr(currency))
                .providerConnectionMonthlyByCurrency(providerRecon.monthlySubtotals())
                .backboneMonthlyCost(categoryMonthly(backboneRecon))
                .backboneCurrency(backboneRecon.soleCurrencyOr(currency))
                .backboneMonthlyByCurrency(backboneRecon.monthlySubtotals())
                .perConnectionCost(perConnectionCost)
                .lines(lines)
                .currencyNotes(currencyNotes)
                .source(source)
                .disclaimer(currencyNotes.isEmpty() || mixed ? disclaimer : disclaimer + " " + currencyNotes.get(0)
                        + "; each converted line keeps its original amount, the rate and its date.")
                .build();
        return withMulticloudLinks(equinixPricing, pricedMulticloudLinks);
    }

    /**
     * Returns the pricing with the native-link fields filled from links that were already priced.
     * Returns the argument itself, every native field {@code null}, when there is no link. {@code REPLACEMENT} and
     * {@code ALTERNATIVE} links are totalled separately, each through its own reconciler; a role's
     * total is withheld when any link of that role is not fully priced or the role spans
     * currencies. {@code UNAVAILABLE} entries have no price and are not listed as unpriced links.
     */
    private static PlanPricing withMulticloudLinks(PlanPricing equinixPricing,
                                                   List<PlannedMulticloudInterconnect> links) {
        if (links == null || links.isEmpty()) {
            return equinixPricing;
        }
        // 'var': the Lombok-generated builder type must not appear in a signature, because the
        // javadoc tool reads these sources without running Lombok.
        var pricing = equinixPricing.toBuilder();
        Map<String, BigDecimal> perLink = new LinkedHashMap<>();
        List<String> unpriced = new ArrayList<>();
        CurrencyReconciler replacement = CurrencyReconciler.create();
        CurrencyReconciler alternative = CurrencyReconciler.create();
        boolean replacementComplete = true;
        boolean alternativeComplete = true;
        int replacements = 0;
        int alternatives = 0;
        for (PlannedMulticloudInterconnect link : links) {
            boolean isReplacement = link.getRole() == MulticloudLinkRole.REPLACEMENT;
            boolean isAlternative = link.getRole() == MulticloudLinkRole.ALTERNATIVE;
            if (!isReplacement && !isAlternative) {
                continue;
            }
            if (isReplacement) {
                replacements++;
            } else {
                alternatives++;
            }
            MulticloudLinkPricing price = link.getPricing();
            if (price == null || price.getNativeMonthly() == null) {
                unpriced.add(link.getName());
                if (isReplacement) {
                    replacementComplete = false;
                } else {
                    alternativeComplete = false;
                }
                continue;
            }
            perLink.put(link.getName(), price.getNativeMonthly());
            (isReplacement ? replacement : alternative)
                    .add(price.getNativeCurrency(), price.getNativeMonthly(), BigDecimal.ZERO);
        }
        pricing.perMulticloudLinkCost(perLink)
                .unpricedMulticloudLinks(unpriced)
                .nativeMulticloudDisclaimer("Native multicloud link figures are the two cloud providers' published "
                        + "hourly list prices converted at " + MulticloudLinkQuote.HOURS_PER_MONTH + " hours per month "
                        + "(reference data as of " + ReferenceRateCard.standard().multicloudAsOf() + ", retrieved "
                        + ReferenceRateCard.standard().multicloudRetrieved() + ") unless a configured rate card "
                        + "supplied them. The cloud providers bill these charges; they are not part of the Equinix "
                        + "totals. An unpriced link is not a zero cost. Estimates, not quotes.");
        if (replacements > 0) {
            boolean ok = replacementComplete && !replacement.isMixed() && !replacement.sawUnknownCurrency();
            pricing.nativeReplacementMonthlyCost(ok ? replacement.monthlyTotal().orElse(null) : null)
                    .nativeReplacementCurrency(ok ? replacement.soleCurrency() : null)
                    .nativeReplacementMonthlyByCurrency(replacement.monthlySubtotals());
        }
        if (alternatives > 0) {
            boolean ok = alternativeComplete && !alternative.isMixed() && !alternative.sawUnknownCurrency();
            pricing.nativeAlternativeMonthlyCost(ok ? alternative.monthlyTotal().orElse(null) : null)
                    .nativeAlternativeCurrency(ok ? alternative.soleCurrency() : null)
                    .nativeAlternativeMonthlyByCurrency(alternative.monthlySubtotals());
        }
        return pricing.build();
    }

    /**
     * A category's single monthly figure: its reconciled sum when all its line items share a
     * currency (zero for an empty category), or {@code null} when the category itself is mixed - 
     * a raw cross-currency category sum (EUR&nbsp;280 + USD&nbsp;300 = "580") is a fabricated
     * number and is never reported.
     */
    private static BigDecimal categoryMonthly(CurrencyReconciler categoryRecon) {
        return categoryRecon.monthlyTotal().orElse(null);
    }

    /** The quote as a cost line, restated in the plan currency when a rate source covers its currency. */
    private static com.eqixiac.equinix.design.spi.CostLine inPlanCurrency(String label, MetroId metro, PriceQuote quote,
                                                                          String target,
                                                                          com.eqixiac.equinix.design.spi.CurrencyConverter fx) {
        PriceSource source = quote.getSource() == null ? PriceSource.ESTIMATE : quote.getSource();
        com.eqixiac.equinix.design.spi.CostLine line = com.eqixiac.equinix.design.spi.CostLine.exact(
                label == null || label.isBlank() ? "priced item" : label, metro, quote.getMonthlyRecurring(),
                quote.getNonRecurring(), quote.getCurrency().getCurrencyCode(), source, quote.getNote());
        return com.eqixiac.equinix.design.cost.CurrencyConverters.convert(line, target, fx).orElse(line);
    }

    private static PriceSource dominantSource(Set<PriceSource> sources) {
        if (sources.isEmpty()) {
            return PriceSource.ESTIMATE;
        }
        if (sources.size() == 1) {
            return sources.iterator().next();
        }
        return PriceSource.COMPOSITE;
    }

    /**
     * Resolves the rate card to price the plan with: the explicitly-configured
     * card if set, otherwise live Equinix pricing over the wizard's Fabric
     * gateway. Returns {@code null} only when neither is available (no gateway),
     * in which case pricing falls back entirely to the built-in heuristic.
     */
    static RateCard resolveRateCard(DeploymentWizard.Builder config) {
        if (config.getRateCard() != null) {
            return config.getRateCard();
        }
        FabricGateway fabric = config.getFabric();
        return fabric != null ? EquinixRateCard.of(fabric) : null;
    }

    /**
     * Resolves a {@link MetroId} to a {@link MetroCode} for the rate-card APIs (which are keyed by
     * the enum). A metro not listed by the enum maps to {@link MetroCode#UNKNOWN}, so it prices via
     * the rate card's fallback rather than a metro-specific rate.
     */
    private static MetroCode toMetroCode(MetroId metroId) {
        return metroId == null ? MetroCode.UNKNOWN : metroId.asMetroCode().orElse(MetroCode.UNKNOWN);
    }

    /**
     * Prices a single connection via the rate card, falling back to the legacy
     * tiered heuristic (tagged {@link PriceSource#ESTIMATE}) when the card
     * cannot resolve a price.
     */
    static PriceQuote priceConnection(RateCard rateCard, ConnectionType type,
                                              int bandwidthMbps, MetroId metro, Term term) {
        if (rateCard != null) {
            Optional<PriceQuote> quote = rateCard.connection(type, bandwidthMbps, toMetroCode(metro), term);
            if (quote.isPresent()) {
                return quote.get();
            }
        }
        return PriceQuote.of(estimateConnectionCost(bandwidthMbps), BigDecimal.valueOf(500),
                Currency.getInstance("USD"), PriceSource.ESTIMATE);
    }

    /**
     * Prices a single Cloud Router via the rate card, falling back to the legacy
     * ~$300/month heuristic (tagged {@link PriceSource#ESTIMATE}) when the card
     * cannot resolve a price.
     */
    static PriceQuote priceRouter(RateCard rateCard, PlannedCloudRouter router, Term term) {
        if (rateCard != null) {
            String packageCode = router.getPackageCode() != null ? router.getPackageCode().name() : null;
            Optional<PriceQuote> quote = rateCard.cloudRouter(packageCode, toMetroCode(router.getMetroId()), term);
            if (quote.isPresent()) {
                return quote.get();
            }
        }
        return PriceQuote.of(BigDecimal.valueOf(300), BigDecimal.ZERO,
                Currency.getInstance("USD"), PriceSource.ESTIMATE);
    }

    private static BigDecimal estimateConnectionCost(int bandwidthMbps) {
        // Tiered pricing estimate based on bandwidth
        if (bandwidthMbps <= 50) return BigDecimal.valueOf(150);
        if (bandwidthMbps <= 200) return BigDecimal.valueOf(300);
        if (bandwidthMbps <= 500) return BigDecimal.valueOf(600);
        if (bandwidthMbps <= 1000) return BigDecimal.valueOf(1000);
        if (bandwidthMbps <= 5000) return BigDecimal.valueOf(3000);
        if (bandwidthMbps <= 10000) return BigDecimal.valueOf(5000);
        return BigDecimal.valueOf(8000);
    }
}
