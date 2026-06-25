package api.equinix.javasdk.fabric.optimizer.model;

import api.equinix.javasdk.fabric.mcp.bridge.McpBridge;
import api.equinix.javasdk.fabric.optimizer.enums.OptimizationStrategy;
import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * The fully-assembled optimization request, containing all sites, providers,
 * workloads, constraints, strategy, and scoring overrides. Built by
 * {@link api.equinix.javasdk.fabric.optimizer.MetroOptimizer.Builder}.
 */
@Value
@Builder
public class OptimizationRequest {

    /** User-defined locations (workforce hubs, customer markets, operational sites). */
    List<UserSite> sites;

    /** Cloud providers and service profiles that must or should be reachable. */
    List<ProviderRequirement> providers;

    /** Workloads to be placed in the recommended metros. */
    List<WorkloadSpec> workloads;

    /** Hard and soft constraints bounding the optimization search space. */
    OptimizationConstraints constraints;

    /** The scoring weight distribution strategy (e.g., BALANCED, LATENCY_FIRST). */
    OptimizationStrategy strategy;

    /** Optional user-overridable scoring weights and latency thresholds. */
    ScoringWeights scoringWeights;

    /** Optional MCP bridge for real-time data enrichment. Null when MCP is not configured. */
    McpBridge mcpBridge;
}
