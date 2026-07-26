package api.equinix.javasdk.design.value.tco;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.util.Map;

/**
 * The modelled monthly and one-time cost of a single {@link DeploymentArchetype},
 * broken out into named line items. {@code isPriced()} is false when the model could
 * not resolve the inputs needed for this archetype (e.g. missing egress rates), in
 * which case the totals are zero (or explicitly partial, per {@code getNote()}) and
 * the archetype should be treated as unavailable rather than free.
 *
 * <p>{@code getTotalOverTerm()} is the archetype's cost over the full commitment
 * term — {@code monthlyTotal × term months + setupTotal} — and is the figure the
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
     * The currency this breakdown's own components reconciled to — which can differ from the
     * comparison-wide currency when, e.g., live Fabric pricing quotes an EMEA metro in EUR.
     */
    String currency;

    /** Named line items summing to the monthly total, in insertion order. */
    Map<String, BigDecimal> lineItems;

    boolean priced;

    String note;
}
