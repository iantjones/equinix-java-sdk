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

    DeploymentArchetype archetype;

    BigDecimal monthlyTotal;

    BigDecimal setupTotal;

    String currency;

    Map<String, BigDecimal> lineItems;

    boolean priced;

    String note;
}
