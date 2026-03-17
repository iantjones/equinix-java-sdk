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

package api.equinix.javasdk.fabric.peering.model;

import api.equinix.javasdk.core.enums.MetroCode;
import api.equinix.javasdk.fabric.peering.enums.FailureScope;
import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.Map;

/**
 * Quantifies the impact of hypothetical failure scenarios on a customer's connectivity.
 *
 * <p>For each customer metro, evaluates what percentage of target ASN connectivity
 * would be lost under various failure scenarios (metro outage, IX failure, facility
 * loss, provider outage, regional event). The report identifies the most impactful
 * failure domains and recommends diversification strategies.</p>
 *
 * <h3>Example</h3>
 * <p>"Losing DC metro takes out IX peering with 3 ASNs (AWS, Azure, Google) and
 * 2 Fabric connections (AWS Direct Connect, Azure ExpressRoute), affecting 65%
 * of your total analyzed connectivity."</p>
 *
 * @author ianjones
 * @see FailureScope
 * @see CorrelatedFailure
 */
@Value
@Builder
public class BlastRadiusReport {

    /** The metro being evaluated for failure impact. */
    MetroCode metro;

    /** The failure scope being simulated. */
    FailureScope scope;

    /** ASNs whose IX peering would be lost in this scenario. */
    List<Long> lostIxPeeringAsns;

    /** ASNs whose Fabric connections would be lost in this scenario. */
    List<Long> lostFabricAsns;

    /** Human-readable labels for lost IX peering. */
    List<String> lostIxPeeringLabels;

    /** Human-readable labels for lost Fabric connections. */
    List<String> lostFabricLabels;

    /** Total IX capacity lost in Mbps. */
    long lostIxCapacityMbps;

    /** Percentage of total analyzed ASN connectivity affected (0.0 - 1.0). */
    double impactRatio;

    /** Severity based on impact ratio. */
    String severity;

    /** Recommended mitigations for this failure scenario. */
    List<String> mitigations;

    /**
     * Returns a severity classification based on the impact ratio.
     *
     * @return "CRITICAL" (>80%), "HIGH" (>50%), "MEDIUM" (>25%), "LOW" (<=25%)
     */
    public String computeSeverity() {
        if (impactRatio > 0.8) return "CRITICAL";
        if (impactRatio > 0.5) return "HIGH";
        if (impactRatio > 0.25) return "MEDIUM";
        return "LOW";
    }

    /**
     * Returns the total number of ASNs affected (IX + Fabric combined, deduplicated concept).
     *
     * @return count of unique affected ASNs
     */
    public int totalAffectedAsns() {
        return lostIxPeeringAsns.size() + lostFabricAsns.size();
    }
}
