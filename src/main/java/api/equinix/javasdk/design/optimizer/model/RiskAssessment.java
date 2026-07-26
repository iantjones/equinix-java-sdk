package api.equinix.javasdk.design.optimizer.model;

import api.equinix.javasdk.design.optimizer.enums.RiskSeverity;
import lombok.Value;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Aggregated risk assessment for the recommended deployment topology.
 *
 * <p>{@code resiliencyScore} is on a 0&ndash;100 scale: the engine starts at 100 and
 * subtracts a fixed penalty per finding (floored at 0), so a lower score means more,
 * or more severe, findings. {@code overallSeverity} is the worst severity present.</p>
 */
@Value
public class RiskAssessment {

    /** Every finding raised for the deployment; see {@code RiskFinding.getCategory()} for the vocabulary. */
    List<RiskFinding> findings;

    /** The most severe level among {@code findings}. */
    RiskSeverity overallSeverity;

    /** Deduction-based resiliency score, 0&ndash;100 (100 = no findings). */
    double resiliencyScore;

    /**
     * The findings with {@link RiskSeverity#CRITICAL} severity.
     *
     * @return the critical findings, possibly empty
     */
    public List<RiskFinding> critical() {
        return findings.stream()
                .filter(f -> f.getSeverity() == RiskSeverity.CRITICAL)
                .collect(Collectors.toList());
    }

    /**
     * The findings at exactly the given severity.
     *
     * @param severity the severity to filter on
     * @return the matching findings, possibly empty
     */
    public List<RiskFinding> bySeverity(RiskSeverity severity) {
        return findings.stream()
                .filter(f -> f.getSeverity() == severity)
                .collect(Collectors.toList());
    }

    /**
     * Whether any {@code COMPLIANCE_GAP} finding was raised — i.e. a requested compliance zone
     * that no selected metro covers, or a force-included metro outside every requested zone.
     *
     * @return {@code true} when the deployment leaves a requested compliance zone uncovered
     */
    public boolean hasComplianceGaps() {
        return findings.stream()
                .anyMatch(f -> "COMPLIANCE_GAP".equals(f.getCategory()));
    }
}
