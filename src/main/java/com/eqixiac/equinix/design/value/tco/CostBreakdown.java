package com.eqixiac.equinix.design.value.tco;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * The modelled monthly and one-time cost of a single {@link DeploymentArchetype},
 * broken out into named line items. {@code isPriced()} is false when the model could
 * not resolve the inputs needed for this archetype (e.g. missing egress rates), in
 * which case the totals are zero (or explicitly partial, per {@code getNote()}) and
 * the archetype should be treated as unavailable rather than free.
 *
 * <p>{@code getTotalOverTerm()} is the archetype's cost over the full commitment
 * term - {@code monthlyTotal × term months + setupTotal} - and is the figure the
 * comparison ranks archetypes by, so one-time setup charges are never ignored.</p>
 */
@Value
@Builder(toBuilder = true)
public class CostBreakdown {

    DeploymentArchetype archetype;

    BigDecimal monthlyTotal;

    BigDecimal setupTotal;

    /** Cost over the full commitment term: {@code monthlyTotal × term.months() + setupTotal}. */
    BigDecimal totalOverTerm;

    /**
     * The currency this breakdown's own components reconciled to - which can differ from the
     * comparison-wide currency when, e.g., live Fabric pricing quotes an EMEA metro in EUR.
     */
    String currency;

    /** Named line items summing to the monthly total, in insertion order. */
    Map<String, BigDecimal> lineItems;

    /**
     * {@code true} only when the breakdown is complete: every component it should contain was priced in
     * one currency. An archetype with any entry in {@code getUnpriced()} is not priced and is never
     * recommended.
     */
    boolean priced;

    String note;

    /**
     * Every component left out of the totals, one entry per component, each with its reason (no figure,
     * no reference row, or a currency that cannot be combined without an exchange rate). Empty when the
     * breakdown is complete. Never silently shorter than the archetype's component list.
     */
    @Builder.Default
    List<String> unpriced = List.of();

    /**
     * Per-line price provenance: for each figure that needs it, the source URL, retrieval date,
     * unit conversion (for example {@code 12.33 USD/h x 730 h/month}) and stated assumptions.
     * Populated for the native multicloud archetype and for the two-sided form of the other
     * archetypes; {@code null} for a single-cloud comparison, whose breakdowns are unchanged.
     */
    List<String> provenance;
}
