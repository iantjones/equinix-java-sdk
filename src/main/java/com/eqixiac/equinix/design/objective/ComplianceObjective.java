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

package com.eqixiac.equinix.design.objective;

import com.eqixiac.equinix.design.spi.ValueScale;
import com.eqixiac.equinix.core.model.MetroId;
import com.eqixiac.equinix.design.optimizer.enums.ComplianceZone;
import com.eqixiac.equinix.design.spi.DesignContext;
import com.eqixiac.equinix.design.spi.Direction;
import com.eqixiac.equinix.design.spi.MetroFacts;
import com.eqixiac.equinix.design.spi.MetroPortfolio;
import com.eqixiac.equinix.design.spi.Objective;
import com.eqixiac.equinix.design.spi.ObjectiveValue;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Share of (metro, stated zone) pairs the portfolio satisfies, 0 to 100: a metro satisfies a
 * {@link ComplianceZone} when its region is one the zone allows, and a {@code geoZones} entry when the
 * live catalogue lists the metro in that zone. 100 when the spec states no zone. The matching
 * constraints enforce the hard version, so among feasible designs this only separates options when a
 * constraint is relaxed (sensitivity analysis) or a plug-in replaces the constraints.
 */
public final class ComplianceObjective implements Objective {

    @Override
    public String id() {
        return COMPLIANCE;
    }

    @Override
    public Direction direction() {
        return Direction.MAXIMIZE;
    }

    /** A percentage is its own score. */
    @Override
    public ValueScale scale(DesignContext context) {
        return ValueScale.percent();
    }

    @Override
    public String unit() {
        return "%";
    }

    @Override
    public ObjectiveValue evaluate(MetroPortfolio portfolio, DesignContext context) {
        List<ComplianceZone> zones = context.spec().constraints().complianceZones();
        List<String> geoZones = context.spec().constraints().geoZones();
        if (zones.isEmpty() && geoZones.isEmpty()) {
            return ObjectiveValue.of(100, "no compliance or sovereignty zone stated");
        }
        int total = 0;
        int met = 0;
        for (MetroId metro : portfolio.metros()) {
            Optional<MetroFacts> facts = context.metro(metro);
            for (ComplianceZone zone : zones) {
                total++;
                if (facts.isPresent() && facts.get().region() != null
                        && zone.getAllowedRegions().contains(facts.get().region())) {
                    met++;
                }
            }
            for (String zone : geoZones) {
                total++;
                if (facts.isPresent() && facts.get().geoZones().contains(zone.toUpperCase(Locale.ROOT))) {
                    met++;
                }
            }
        }
        double value = 100.0 * met / total;
        return ObjectiveValue.of(value, String.format(Locale.ROOT, "%d of %d metro/zone checks satisfied", met, total));
    }
}
