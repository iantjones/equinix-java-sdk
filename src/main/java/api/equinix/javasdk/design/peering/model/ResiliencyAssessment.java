/*
 * Copyright 2021 Ian Jones. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
 * OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package api.equinix.javasdk.design.peering.model;

import api.equinix.javasdk.core.model.MetroId;
import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Comprehensive resiliency assessment for a customer's connectivity posture.
 *
 * <p>Combines failover path analysis, blast radius evaluation, correlated failure
 * detection, and geographic diversity scoring into a single report. The overall
 * resiliency score (0.0-1.0) indicates how well the customer is protected against
 * connectivity loss due to infrastructure failures.</p>
 *
 * @author ianjones
 * @see FailoverPath
 * @see BlastRadiusReport
 * @see CorrelatedFailure
 */
@Value
@Builder
public class ResiliencyAssessment {

    double overallScore;

    String overallRating;

    Map<MetroId, List<FailoverPath>> failoverPaths;

    List<BlastRadiusReport> blastRadiusReports;

    List<CorrelatedFailure> correlatedFailures;

    List<DiversityScore> diversityScores;

    List<String> findings;

    /**
     * Returns failover paths for a specific ASN across all customer metros.
     *
     * @param asn the target ASN
     * @return all failover paths for this ASN
     */
    public List<FailoverPath> failoverPathsForAsn(long asn) {
        return failoverPaths.values().stream()
                .flatMap(List::stream)
                .filter(fp -> fp.getTargetAsn() == asn)
                .collect(Collectors.toList());
    }

    /**
     * Returns the blast radius report for a specific metro.
     *
     * @param metro the metro to evaluate
     * @return the blast radius report, or {@code null} if not analyzed
     */
    public BlastRadiusReport blastRadiusFor(MetroId metro) {
        return blastRadiusReports.stream()
                .filter(br -> java.util.Objects.equals(br.getMetro(), metro))
                .findFirst()
                .orElse(null);
    }

    /**
     * Returns only critical and high-severity correlated failures.
     *
     * @return correlated failures with CRITICAL or HIGH severity
     */
    public List<CorrelatedFailure> criticalCorrelations() {
        return correlatedFailures.stream()
                .filter(cf -> "CRITICAL".equals(cf.getSeverity()) || "HIGH".equals(cf.getSeverity()))
                .collect(Collectors.toList());
    }

    /**
     * Checks whether any single metro failure would impact more than the given
     * threshold of total connectivity.
     *
     * @param threshold the impact ratio threshold (e.g., 0.5 for 50%)
     * @return {@code true} if any metro failure exceeds the threshold
     */
    public boolean hasSinglePointOfFailure(double threshold) {
        return blastRadiusReports.stream()
                .anyMatch(br -> br.getImpactRatio() > threshold);
    }
}
