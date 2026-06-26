package api.equinix.javasdk.design.optimizer.model;

import api.equinix.javasdk.design.optimizer.enums.RiskSeverity;
import lombok.Value;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Aggregated risk assessment for the recommended deployment topology.
 */
@Value
public class RiskAssessment {

    List<RiskFinding> findings;
    RiskSeverity overallSeverity;
    double resiliencyScore;

    public List<RiskFinding> critical() {
        return findings.stream()
                .filter(f -> f.getSeverity() == RiskSeverity.CRITICAL)
                .collect(Collectors.toList());
    }

    public List<RiskFinding> bySeverity(RiskSeverity severity) {
        return findings.stream()
                .filter(f -> f.getSeverity() == severity)
                .collect(Collectors.toList());
    }

    public boolean hasComplianceGaps() {
        return findings.stream()
                .anyMatch(f -> "COMPLIANCE_GAP".equals(f.getCategory()));
    }
}
