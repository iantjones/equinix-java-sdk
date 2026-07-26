package com.eqixiac.equinix.design.optimizer.model;

import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * Human-readable explanation of the optimization methodology, assumptions,
 * and data freshness for inclusion in reports.
 */
@Value
@Builder
public class OptimizationExplanation {

    String methodology;
    List<String> assumptions;
    String dataFreshness;
    String humanReadable;
}
