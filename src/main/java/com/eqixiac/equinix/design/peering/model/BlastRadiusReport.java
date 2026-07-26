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

package com.eqixiac.equinix.design.peering.model;

import com.eqixiac.equinix.core.model.MetroId;
import com.eqixiac.equinix.design.peering.enums.FailureScope;
import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.Map;

/**
 * Quantifies the impact of a hypothetical failure of one customer metro on the customer's
 * IX peering connectivity.
 *
 * <p>For each customer metro, the engine evaluates which analyzed ASNs would lose their IX
 * peering there and how much aggregate IX port capacity that removes, expressed as
 * {@code impactRatio} — the fraction of analyzed ASNs affected. Metros with no analyzed-ASN
 * presence are excluded from the overall resiliency score (an empty metro is not resilience)
 * and are called out in the assessment findings instead.</p>
 *
 * <h3>Example</h3>
 * <p>"Losing DC metro takes out IX peering with 3 of your 5 analyzed ASNs (AWS, Azure, Google),
 * an impact ratio of 60% — HIGH."</p>
 *
 * @author ianjones
 * @see FailureScope
 * @see CorrelatedFailure
 */
@Value
@Builder
public class BlastRadiusReport {

    /** The customer metro whose hypothetical failure this report quantifies. */
    MetroId metro;

    /** The failure domain evaluated — currently always {@link FailureScope#METRO}. */
    FailureScope scope;

    /** The analyzed ASNs whose IX peering at this metro would be lost. */
    List<Long> lostIxPeeringAsns;

    /**
     * Reserved for Fabric-connection losses; the current analysis evaluates IX peering only,
     * so this list is always empty.
     */
    List<Long> lostFabricAsns;

    /** Display labels parallel to {@code lostIxPeeringAsns}. */
    List<String> lostIxPeeringLabels;

    /** Reserved, parallel to {@code lostFabricAsns}; currently always empty. */
    List<String> lostFabricLabels;

    /** Aggregate IX port capacity lost at this metro, in Mbps. */
    long lostIxCapacityMbps;

    /** The fraction (0.0&ndash;1.0) of analyzed ASNs affected by this metro's failure. */
    double impactRatio;

    /**
     * The engine-stored severity, derived from {@code impactRatio} at analysis time with the same
     * thresholds {@link #computeSeverity()} uses. Closed value set: {@code "CRITICAL"},
     * {@code "HIGH"}, {@code "MEDIUM"}, {@code "LOW"}.
     */
    String severity;

    /** Suggested diversification steps, populated for impact ratios above 50%. */
    List<String> mitigations;

    /**
     * Re-derives the severity classification from {@link #impactRatio} — by construction this
     * matches the stored {@code severity} on engine-built reports.
     *
     * @return {@code "CRITICAL"} ({@code >80%}), {@code "HIGH"} ({@code >50%}),
     *         {@code "MEDIUM"} ({@code >25%}), {@code "LOW"} ({@code <=25%})
     */
    public String computeSeverity() {
        if (impactRatio > 0.8) return "CRITICAL";
        if (impactRatio > 0.5) return "HIGH";
        if (impactRatio > 0.25) return "MEDIUM";
        return "LOW";
    }

    /**
     * Returns the total number of affected ASN entries: the size of {@code lostIxPeeringAsns} plus
     * the size of {@code lostFabricAsns} — a plain sum, not a set union. It cannot double-count
     * today because the Fabric list is always empty (the analysis evaluates IX peering only), but
     * if Fabric losses are ever populated an ASN lost on both paths would count twice.
     *
     * @return the summed count of affected ASN entries
     */
    public int totalAffectedAsns() {
        return lostIxPeeringAsns.size() + lostFabricAsns.size();
    }
}
