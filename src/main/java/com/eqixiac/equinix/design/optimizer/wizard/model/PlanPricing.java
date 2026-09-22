package com.eqixiac.equinix.design.optimizer.wizard.model;

import com.eqixiac.equinix.design.value.ratecard.PriceSource;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Aggregated pricing breakdown for a complete deployment plan, including
 * Cloud Router costs, provider connection costs, and backbone link costs.
 *
 * <p><strong>Currency honesty.</strong> A plan can legitimately span metros priced in different
 * currencies (live Fabric pricing quotes EUR in Frankfurt next to USD in Ashburn). Amounts are only
 * ever summed within a single currency — the wizard's {@code CurrencyReconciler} rule:</p>
 * <ul>
 *   <li>{@code monthlyTotal}/{@code setupTotal}/{@code currency} are set only when the whole plan
 *       reconciles to one currency; when it is mixed they are {@code null} and
 *       {@code monthlyByCurrency} carries the per-currency subtotals instead.</li>
 *   <li>Each category figure ({@code routerMonthlyCost}, {@code providerConnectionMonthlyCost},
 *       {@code backboneMonthlyCost}) is likewise kept only when that category reconciles to one
 *       currency — its code in the matching {@code *Currency} field — and is {@code null} when the
 *       category itself is mixed, with the truth in the matching {@code *MonthlyByCurrency} map.
 *       A raw cross-currency category sum is never reported.</li>
 * </ul>
 *
 * <p><strong>Native multicloud links (Beta).</strong> The {@code native*} fields and
 * {@code perMulticloudLinkCost} price the plan's native provider-to-provider links. The cloud
 * providers bill those charges, not Equinix, so they are reported separately: no native-link
 * amount is part of {@code monthlyTotal}, {@code setupTotal}, {@code monthlyByCurrency} or any
 * category figure. All of them are {@code null} on a plan without native links.</p>
 */
@Value
@Builder(toBuilder = true)
public class PlanPricing {

    /**
     * The whole-plan monthly recurring cost — {@code null} when the plan spans currencies
     * (see {@code monthlyByCurrency}) so a cross-currency sum is never fabricated.
     */
    BigDecimal monthlyTotal;

    /** The whole-plan one-time setup cost — {@code null} when the plan spans currencies. */
    BigDecimal setupTotal;

    /**
     * The ISO&nbsp;4217 code of {@code monthlyTotal}/{@code setupTotal}. Defaults to {@code "USD"},
     * but the wizard sets it {@code null} for a mixed-currency plan — check it before rendering a
     * symbol next to a total.
     */
    @Builder.Default
    String currency = "USD";

    /** Whole-plan monthly subtotals per currency, in first-seen order; the source of truth when mixed. */
    Map<String, BigDecimal> monthlyByCurrency;

    /** Monthly Cloud Router cost — {@code null} when the router category spans currencies. */
    BigDecimal routerMonthlyCost;

    /** The currency of {@code routerMonthlyCost}; {@code null} when that figure is mixed/absent. */
    String routerCurrency;

    /** Per-currency monthly router subtotals (the reconciler's convention for a mixed category). */
    Map<String, BigDecimal> routerMonthlyByCurrency;

    /** Monthly provider-connection cost — {@code null} when the category spans currencies. */
    BigDecimal providerConnectionMonthlyCost;

    /** The currency of {@code providerConnectionMonthlyCost}; {@code null} when mixed/absent. */
    String providerConnectionCurrency;

    /** Per-currency monthly provider-connection subtotals. */
    Map<String, BigDecimal> providerConnectionMonthlyByCurrency;

    /** Monthly backbone cost — {@code null} when the backbone category spans currencies. */
    BigDecimal backboneMonthlyCost;

    /** The currency of {@code backboneMonthlyCost}; {@code null} when that figure is mixed/absent. */
    String backboneCurrency;

    /** Per-currency monthly backbone subtotals. */
    Map<String, BigDecimal> backboneMonthlyByCurrency;

    /**
     * Monthly cost per connection, keyed by the <em>planned connection name</em> (the same name
     * that appears on {@code PlannedConnection.getName()}, in {@link ExecutionInputs}, and in the
     * Terraform export). Each figure is in that connection's own quote currency.
     */
    Map<String, BigDecimal> perConnectionCost;

    /**
     * Dominant provenance of these figures: {@link PriceSource#EQUINIX_LIVE} when every line
     * item was live-priced, {@link PriceSource#ESTIMATE} when the heuristic fallback was used,
     * or {@link PriceSource#COMPOSITE} when mixed. Read alongside {@link #disclaimer}.
     */
    @Builder.Default
    PriceSource source = PriceSource.ESTIMATE;

    @Builder.Default
    String disclaimer = "Estimates based on published Fabric pricing. Actual costs may vary based on contract terms, volume discounts, and promotional offers.";

    // ── Native multicloud links (Beta). Billed by the cloud providers, never by Equinix. None of
    //    the figures below is part of monthlyTotal, setupTotal or any category figure above. ──

    /**
     * <b>Beta.</b> The monthly fees of the native multicloud links the plan depends on (role
     * {@code REPLACEMENT}), both providers' sides summed. {@code null} when the plan has no such
     * link, when any of them has an unpriced side, or when they span currencies. A {@code null}
     * is not a zero cost. Never included in {@code monthlyTotal}.
     */
    BigDecimal nativeReplacementMonthlyCost;

    /** The ISO&nbsp;4217 code of {@code nativeReplacementMonthlyCost}; {@code null} when that figure is. */
    String nativeReplacementCurrency;

    /**
     * Per-currency monthly subtotals of the priced {@code REPLACEMENT} links; {@code null} when
     * the plan has none. Excludes links with an unpriced side, which
     * {@code unpricedMulticloudLinks} names.
     */
    Map<String, BigDecimal> nativeReplacementMonthlyByCurrency;

    /**
     * <b>Beta.</b> The monthly fees of the native multicloud links reported for comparison only
     * (role {@code ALTERNATIVE}), both providers' sides summed. The customer pays this only by
     * adopting the alternatives. {@code null} under the same conditions as
     * {@code nativeReplacementMonthlyCost}. Never included in {@code monthlyTotal}.
     */
    BigDecimal nativeAlternativeMonthlyCost;

    /** The ISO&nbsp;4217 code of {@code nativeAlternativeMonthlyCost}; {@code null} when that figure is. */
    String nativeAlternativeCurrency;

    /** Per-currency monthly subtotals of the priced {@code ALTERNATIVE} links; {@code null} when the plan has none. */
    Map<String, BigDecimal> nativeAlternativeMonthlyByCurrency;

    /**
     * Monthly fee per fully priced native multicloud link, keyed by
     * {@code PlannedMulticloudInterconnect.getName()}, each in that link's own currency
     * ({@code getPricing().getNativeCurrency()} on the link). {@code null} when the plan has no
     * native link.
     */
    Map<String, BigDecimal> perMulticloudLinkCost;

    /**
     * The names of the native multicloud links with at least one unpriced side, or whose two
     * sides are in different currencies. {@code null} when the plan has no native link.
     */
    List<String> unpricedMulticloudLinks;

    /**
     * Provenance and limits of the native-link figures: the hourly-to-monthly conversion, the
     * reference data's as-of date, and that the cloud providers bill these charges. {@code null}
     * when the plan has no native link.
     */
    String nativeMulticloudDisclaimer;
}
