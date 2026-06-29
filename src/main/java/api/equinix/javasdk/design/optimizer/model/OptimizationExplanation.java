package api.equinix.javasdk.design.optimizer.model;

import lombok.Value;

import java.util.List;

/**
 * Human-readable explanation of the optimization methodology, assumptions,
 * and data freshness for inclusion in reports.
 */
@Value
public class OptimizationExplanation {

    String methodology;
    List<String> assumptions;
    String dataFreshness;
    String humanReadable;
}
