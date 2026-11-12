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
import com.eqixiac.equinix.design.spec.CloudIntent;
import com.eqixiac.equinix.design.spi.DesignContext;
import com.eqixiac.equinix.design.spi.Direction;
import com.eqixiac.equinix.design.spi.MetroPortfolio;
import com.eqixiac.equinix.design.spi.Objective;
import com.eqixiac.equinix.design.spi.ObjectiveValue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Share of the spec's clouds reachable privately from at least one portfolio metro, 0 to 100. Required
 * clouds count twice as much as optional ones. 100 when the spec names no cloud.
 */
public final class ProviderCoverageObjective implements Objective {

    @Override
    public String id() {
        return PROVIDER_COVERAGE;
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
        List<CloudIntent> clouds = context.spec().clouds();
        if (clouds.isEmpty()) {
            return ObjectiveValue.of(100, "no clouds requested");
        }
        Set<String> reached = new HashSet<>();
        for (MetroId metro : portfolio.metros()) {
            reached.addAll(context.reachableClouds(metro));
        }
        double total = 0;
        double covered = 0;
        List<String> missing = new ArrayList<>();
        for (CloudIntent cloud : clouds) {
            double w = Boolean.TRUE.equals(cloud.required()) ? 2 : 1;
            total += w;
            if (reached.contains(cloud.id())) {
                covered += w;
            } else {
                missing.add(cloud.id());
            }
        }
        double value = 100.0 * covered / total;
        return ObjectiveValue.of(value, String.format(Locale.ROOT, "%.0f%% of requested clouds reachable", value)
                + (missing.isEmpty() ? "" : "; not reachable: " + String.join(", ", missing)));
    }
}
