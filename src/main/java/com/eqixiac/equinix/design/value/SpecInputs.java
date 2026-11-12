package com.eqixiac.equinix.design.value;

import com.eqixiac.equinix.core.enums.MetroCode;
import com.eqixiac.equinix.core.model.MetroId;
import com.eqixiac.equinix.design.cost.RateCardCostModel;
import com.eqixiac.equinix.design.spec.CloudIntent;
import com.eqixiac.equinix.design.spec.DesignSpec;
import com.eqixiac.equinix.design.spec.WorkloadIntent;
import com.eqixiac.equinix.design.spi.MetroPortfolio;
import com.eqixiac.equinix.design.value.ratecard.Term;
import com.eqixiac.equinix.fabric.model.implementation.cloud.CloudProviderType;

import java.util.ArrayList;
import java.util.List;

/**
 * The inputs the TCO and savings models take, derived from a {@link DesignSpec} (and optionally the
 * portfolio a run chose) the same way the optimiser's {@code RateCardCostModel} reads them, so the three
 * price the same thing.
 *
 * <ul>
 *   <li>cloud: the named cloud, else the first cloud that names a provider;</li>
 *   <li>provider, region and monthly egress: that cloud's {@code provider}, {@code region} and
 *       {@code monthlyEgressGb} (0 when unset);</li>
 *   <li>bandwidth: the sum of {@code bandwidthMbps} over the workloads that reach the cloud, rounded up
 *       to the Fabric tier ({@link RateCardCostModel#tierFor}); 1000 Mbps when no workload is sized
 *       (stated as an assumption);</li>
 *   <li>metro: the chosen portfolio's metro when it has one; for a larger portfolio its first metro
 *       (stated); with no portfolio, the spec's single required metro, else none (metro-agnostic);</li>
 *   <li>term: {@code commercial.term}, else {@link Term#MONTH_1} (the cost model's default).</li>
 * </ul>
 *
 * @param fingerprint   the spec's fingerprint
 * @param cloud         the cloud being priced
 * @param provider      its provider
 * @param region        its region, or {@code null}
 * @param egressGb      data leaving the cloud per month, GB
 * @param bandwidthMbps the Fabric tier the leg is sized to
 * @param metro         the metro, or {@code null}
 * @param term          the term
 * @param assumptions   what was assumed where the spec was silent
 */
public record SpecInputs(String fingerprint, CloudIntent cloud, CloudProviderType provider, String region,
                         double egressGb, int bandwidthMbps, MetroCode metro, Term term, List<String> assumptions) {

    /** Copies the assumptions. */
    public SpecInputs {
        assumptions = assumptions == null ? List.of() : List.copyOf(assumptions);
    }

    /**
     * @param spec    the design spec
     * @param cloudId the cloud to price, or {@code null} for the first cloud with a provider
     * @param chosen  the portfolio a run chose, or {@code null}
     * @return the derived inputs
     * @throws IllegalArgumentException when the spec has no cloud with a provider, or the named cloud is
     *                                  unknown or names no provider
     */
    public static SpecInputs derive(DesignSpec spec, String cloudId, MetroPortfolio chosen) {
        if (spec == null) {
            throw new IllegalArgumentException("forSpec needs a spec");
        }
        CloudIntent cloud = null;
        if (cloudId != null) {
            cloud = spec.cloud(cloudId);
            if (cloud == null) {
                throw new IllegalArgumentException("The spec has no cloud '" + cloudId + "'");
            }
        } else {
            for (CloudIntent c : spec.clouds()) {
                if (c.provider() != null) {
                    cloud = c;
                    break;
                }
            }
        }
        if (cloud == null || cloud.provider() == null) {
            throw new IllegalArgumentException("TCO and savings price a cloud provider's egress: the spec needs a cloud "
                    + "that names a provider");
        }
        List<String> assumptions = new ArrayList<>();
        int sized = 0;
        for (WorkloadIntent w : spec.workloads()) {
            if (w.clouds().contains(cloud.id()) && w.bandwidthMbps() != null) {
                sized += w.bandwidthMbps();
            }
        }
        int bandwidth;
        if (sized <= 0) {
            bandwidth = 1_000;
            assumptions.add("No workload reaching '" + cloud.id() + "' states bandwidthMbps; 1000 Mbps assumed");
        } else {
            bandwidth = RateCardCostModel.tierFor(sized);
            if (bandwidth < 0) {
                throw new IllegalArgumentException(sized + " Mbps to '" + cloud.id()
                        + "' exceeds the largest Fabric connection tier");
            }
            if (bandwidth != sized) {
                assumptions.add(sized + " Mbps of workloads to '" + cloud.id() + "' rounded up to the " + bandwidth
                        + " Mbps Fabric tier");
            }
        }
        MetroCode metro = null;
        if (chosen != null && chosen.size() > 0) {
            MetroId first = chosen.metros().get(0);
            metro = first.asMetroCode().orElse(null);
            if (chosen.size() > 1) {
                assumptions.add("The portfolio " + chosen.key() + " has several metros; the connection is priced in "
                        + first.code());
            }
        } else if (spec.constraints().requiredMetros().size() == 1) {
            metro = MetroId.of(spec.constraints().requiredMetros().get(0)).asMetroCode().orElse(null);
        } else {
            assumptions.add("No metro chosen: Equinix items are priced metro-agnostically (reference figures)");
        }
        Term term = spec.commercial().term() == null ? Term.MONTH_1 : spec.commercial().term();
        if (spec.commercial().term() == null) {
            assumptions.add("commercial.term not set: priced on demand (1 month), as the optimiser does");
        }
        double egress = cloud.monthlyEgressGb() == null ? 0 : cloud.monthlyEgressGb();
        if (cloud.monthlyEgressGb() == null) {
            assumptions.add("Cloud '" + cloud.id() + "' states no monthlyEgressGb; 0 GB assumed");
        }
        return new SpecInputs(spec.fingerprint(), cloud, cloud.provider(), cloud.region(), egress, bandwidth, metro,
                term, assumptions);
    }
}
