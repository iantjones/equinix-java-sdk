package com.eqixiac.equinix.design.optimizer.model;

import com.eqixiac.equinix.design.cost.CostComparability;
import com.eqixiac.equinix.design.geo.RouteFactorCalibration;
import com.eqixiac.equinix.design.placement.PlacementPlan;
import com.eqixiac.equinix.design.solve.SensitivityReport;
import com.eqixiac.equinix.design.spec.DesignSpec;
import com.eqixiac.equinix.design.spi.ConstraintVerdict;
import com.eqixiac.equinix.design.spi.DesignOption;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The result of a Metro Optimizer run.
 *
 * <p>The design answer is {@code getRecommended()} (one {@link DesignOption}: a set of metros with every
 * objective measured, its priced cost lines and every constraint verdict), chosen from
 * {@code getFrontier()} (every Pareto-optimal trade-off between cost, latency, resilience, coverage and
 * compliance). {@code getSensitivity()} says how firm the choice is, {@code getPlacement()} where each
 * workload runs. The result carries the {@code getSpec()} and its {@code getSpecFingerprint()} so it can be stored and compared.</p>
 *
 * <p>The remaining fields restate the recommended design in the shape the Deployment Wizard, the
 * exporters and the MCP server read: {@code getRecommendations()} (one entry per recommended metro,
 * primary first), {@code getTopology()}, {@code getCostEstimate()} (the same lines as
 * {@code getRecommended().cost()}), {@code getRiskAssessment()}, {@code getLatencyMatrix()} and
 * {@code getRequest()} (the spec restated). When nothing is feasible they are empty and
 * {@code getSolverNotes()} says why.</p>
 */
@Value
@Builder
public class OptimizationResult {

    /** The spec restated for the Deployment Wizard. */
    OptimizationRequest request;
    /** The recommended metros, primary first; empty when nothing is feasible. */
    List<MetroRecommendation> recommendations;
    /** Workload placements on the recommended metros (primary placements, then standby ones). */
    DeploymentTopology topology;
    /** Cloud availability per recommended metro. */
    ProviderConnectivityMap providerMap;
    /** Round trip from each recommended metro to each site. */
    LatencyMatrix latencyMatrix;
    /** Findings a reader should act on (unevaluated limits, unpriced items, single points of failure). */
    RiskAssessment riskAssessment;
    /** The recommended design's cost, from the same lines as {@code recommended.cost()}. */
    CostEstimate costEstimate;
    /** Methodology, assumptions and data freshness. */
    OptimizationExplanation explanation;
    /** When the result was produced. */
    Instant computedAt;
    /** Total run time including data gathering, ms. */
    long computeTimeMs;

    /** The spec the run evaluated. */
    DesignSpec spec;
    /** {@code spec.fingerprint()}. */
    String specFingerprint;
    /** The recommended option, or {@code null} when nothing is feasible. */
    DesignOption recommended;
    /** Every Pareto-optimal option, best weighted score first. */
    List<DesignOption> frontier;
    /** Every constraint verdict for the recommended option. */
    List<ConstraintVerdict> constraintVerdicts;
    /** What the reader should know about the search. */
    List<String> solverNotes;
    /** Search statistics. */
    SolveStatistics solveStatistics;
    /** Weight ranges, nearest alternative and binding constraints; {@code null} when nothing is feasible. */
    SensitivityReport sensitivity;
    /** Per objective id, how its values were turned into 0..100 scores ({@code ValueScale.describe()}). */
    Map<String, String> objectiveScales;
    /** Per objective id, the unit of its values (for example {@code ms}). */
    Map<String, String> objectiveUnits;
    /**
     * Whether every feasible design's cost could be compared in the spec currency, and whether cost was
     * withdrawn from the ranking because exchange rates are missing ({@code CostComparability.RULE}).
     */
    CostComparability costComparability;
    /** The route factor used for unmeasured latencies and what it was fitted from. */
    RouteFactorCalibration latencyCalibration;
    /** Where every workload runs on the recommended design; {@code null} when nothing is feasible. */
    PlacementPlan placement;
    /** When the catalogue facts were gathered. */
    Instant dataAsOf;
    /** Where the facts came from and how complete they are. */
    List<String> dataProvenance;

    /** @return the primary recommended metro, or {@code null} when nothing is feasible */
    public MetroRecommendation primaryMetro() {
        return recommendations == null || recommendations.isEmpty() ? null : recommendations.get(0);
    }

    /**
     * @param n how many
     * @return the first {@code n} recommended metros
     */
    public List<MetroRecommendation> top(int n) {
        return recommendations == null ? List.of() : recommendations.stream().limit(n).collect(Collectors.toList());
    }

    /**
     * The spec with every exchange rate this result used pinned in {@code commercial.exchangeRates}, so running it
     * again reproduces the figures exactly.
     *
     * @return the spec with pinned rates (the spec unchanged when nothing was converted)
     */
    public DesignSpec specWithUsedRatesPinned() {
        java.util.List<com.eqixiac.equinix.design.spi.PortfolioCost> costs = new java.util.ArrayList<>();
        if (recommended != null) {
            costs.add(recommended.cost());
        }
        if (frontier != null) {
            for (DesignOption option : frontier) {
                costs.add(option.cost());
            }
        }
        return com.eqixiac.equinix.design.cost.CurrencyConverters.pinUsedRates(spec, costs);
    }

    /** @return a few sentences: the recommendation, its objectives, the frontier size and the search statistics */
    public String toSummary() {
        return OptimizationReport.summary(this);
    }

    /**
     * @return a Markdown report: recommendation and why, frontier table, sensitivity, constraints,
     *         placement, cost lines, latency, risks, assumptions and data provenance
     */
    public String toMarkdown() {
        return OptimizationReport.markdown(this);
    }

    /** @return the result as indented JSON (ISO-8601 instants) */
    @JsonIgnore
    public String toJson() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            mapper.registerModule(new Jdk8Module());
            mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
            return mapper.writeValueAsString(this);
        } catch (Exception e) {
            throw new IllegalStateException("OptimizationResult could not be serialised: " + e.getMessage(), e);
        }
    }
}
