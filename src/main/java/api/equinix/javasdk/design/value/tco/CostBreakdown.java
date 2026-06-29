package api.equinix.javasdk.design.value.tco;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.util.Map;

/**
 * The modelled monthly and one-time cost of a single {@link DeploymentArchetype},
 * broken out into named line items. {@link #priced} is false when the model could
 * not resolve the inputs needed for this archetype (e.g. missing egress rates), in
 * which case the totals are zero and the archetype should be treated as
 * unavailable rather than free.
 */
@Value
@Builder
public class CostBreakdown {

    /** The archetype this breakdown describes. */
    DeploymentArchetype archetype;

    /** Total estimated monthly recurring cost. */
    BigDecimal monthlyTotal;

    /** Total estimated one-time setup cost. */
    BigDecimal setupTotal;

    /** Currency code (ISO 4217). */
    String currency;

    /** Named monthly line items contributing to {@link #monthlyTotal}. */
    Map<String, BigDecimal> lineItems;

    /** Whether the model could fully price this archetype. */
    boolean priced;

    /** Optional note (e.g. why an archetype is unavailable, or provenance caveats). */
    String note;
}
