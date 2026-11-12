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

package com.eqixiac.equinix.design.placement;

import com.eqixiac.equinix.core.model.MetroId;
import com.eqixiac.equinix.design.spi.MetroPortfolio;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Where every workload of a spec runs on one portfolio, and the cloud connections that implies.
 * Produced by {@link WorkloadPlacer}; consumed by the cost model (what to price in each metro), the
 * resilience objective, the workload constraints and the Deployment Wizard.
 *
 * @param portfolio  the metros placed on
 * @param placements one entry per workload, in spec order, then one per cloud no workload refers to
 * @param legs       one entry per (metro, cloud) pair the placements need, sorted by metro then cloud
 */
public record PlacementPlan(MetroPortfolio portfolio, List<Placement> placements, List<CloudLeg> legs) {

    /** Makes the lists immutable. */
    public PlacementPlan {
        placements = placements == null ? List.of() : List.copyOf(placements);
        legs = legs == null ? List.of() : List.copyOf(legs);
    }

    /**
     * Where one workload runs.
     *
     * @param workload  the workload label (or {@code cloud:<id>} for a cloud no workload refers to)
     * @param primary   the metro it runs in, or {@code null} when it could not be placed
     * @param standby   the metro its standby connections are in, or {@code null} for none
     * @param clouds    the cloud ids it connects to
     * @param bandwidthMbps the workload's bandwidth, or {@code null} when unsized
     * @param reasoning why it was placed there
     * @param issues    what the placement could not honour (pinned metro outside the portfolio, a latency
     *                  limit no metro meets, a cloud no eligible metro reaches)
     */
    public record Placement(String workload, MetroId primary, MetroId standby, List<String> clouds,
                            Integer bandwidthMbps, String reasoning, List<String> issues) {
        /** Makes the lists immutable. */
        public Placement {
            clouds = clouds == null ? List.of() : List.copyOf(clouds);
            issues = issues == null ? List.of() : List.copyOf(issues);
        }
    }

    /**
     * One Fabric connection the design needs: from a metro to a cloud, sized as the sum of the sized
     * workloads it carries.
     *
     * @param metro             the metro
     * @param cloudId           the spec cloud id
     * @param sizedMbps         total bandwidth of the sized workloads it carries (primary and standby)
     * @param workloads         labels of the workloads it carries; a standby carriage is suffixed {@code (standby)}
     * @param unsizedWorkloads  labels of carried workloads with no bandwidth (the leg cannot be fully sized)
     */
    public record CloudLeg(MetroId metro, String cloudId, int sizedMbps, List<String> workloads,
                           List<String> unsizedWorkloads) {
        /** Makes the lists immutable. */
        public CloudLeg {
            workloads = workloads == null ? List.of() : List.copyOf(workloads);
            unsizedWorkloads = unsizedWorkloads == null ? List.of() : List.copyOf(unsizedWorkloads);
        }
    }

    /**
     * @param label a workload label
     * @return its placement
     */
    public Optional<Placement> placement(String label) {
        return placements.stream().filter(p -> p.workload().equals(label)).findFirst();
    }

    /**
     * @param metro a metro
     * @return the cloud legs in that metro
     */
    public List<CloudLeg> legsIn(MetroId metro) {
        List<CloudLeg> in = new ArrayList<>();
        for (CloudLeg leg : legs) {
            if (leg.metro().equals(metro)) {
                in.add(leg);
            }
        }
        return in;
    }

    /** @return the portfolio metros that carry no placement (primary or standby) and no cloud leg */
    public Set<MetroId> idleMetros() {
        Set<MetroId> idle = new LinkedHashSet<>(portfolio.metros());
        for (Placement p : placements) {
            idle.remove(p.primary());
            idle.remove(p.standby());
        }
        for (CloudLeg leg : legs) {
            idle.remove(leg.metro());
        }
        return idle;
    }
}
