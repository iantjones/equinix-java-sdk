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
package com.eqixiac.equinix.design.resilience;

import com.eqixiac.equinix.design.spi.Constraint;
import com.eqixiac.equinix.design.spi.ConstraintVerdict;
import com.eqixiac.equinix.design.spi.DesignContext;
import com.eqixiac.equinix.design.spi.MetroPortfolio;

/**
 * {@value #ID}: data sovereignty that holds through failover. Every path the design actually uses (the
 * metro a workload runs in, the metros its sites' traffic crosses, the metros its cloud traffic crosses)
 * must lie inside the spec's {@code constraints.geoZones}, at baseline <em>and</em> after every
 * single-element failure: a standby path that leaves the zones is a violation even when the primary path
 * stays inside. Zone membership is the catalogue's {@code geoScopes}/{@code geoZones} for each metro, as
 * the built-in {@code geo_zones} constraint reads them.
 *
 * <p>Inert when the spec states no geo zones. In a Metro Optimizer run the built-in {@code geo_zones}
 * constraint already keeps every portfolio metro inside the zones, so this check can only add findings
 * for paths outside the portfolio; its main use is on a portfolio analysed as given
 * ({@link ResilienceAnalyzer#portfolio(MetroPortfolio)}), where it names the scenario that leaves the zone.</p>
 */
public final class FailoverGeoZonesConstraint implements Constraint {

    /** The constraint id. */
    public static final String ID = "geo_zones_failover";

    private final ResilienceOptions options;

    /** With the default options. */
    public FailoverGeoZonesConstraint() {
        this(ResilienceOptions.defaults());
    }

    /**
     * @param options graph and simulation options
     */
    public FailoverGeoZonesConstraint(ResilienceOptions options) {
        this.options = options == null ? ResilienceOptions.defaults() : options;
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public ConstraintVerdict check(MetroPortfolio portfolio, DesignContext context) {
        if (context.spec().constraints().geoZones().isEmpty()) {
            return ConstraintVerdict.pass(ID, "no geo zones stated");
        }
        N1Survival n1 = N1Survival.of(portfolio, context, options);
        if (!n1.zoneViolations().isEmpty()) {
            return ConstraintVerdict.fail(ID, n1.zoneViolations().get(0)
                    + (n1.zoneViolations().size() > 1 ? " (and " + (n1.zoneViolations().size() - 1) + " more)" : ""));
        }
        return ConstraintVerdict.pass(ID, "every active path stays inside " + context.spec().constraints().geoZones()
                + " at baseline and after each of " + n1.scenarios() + " single failures");
    }
}
