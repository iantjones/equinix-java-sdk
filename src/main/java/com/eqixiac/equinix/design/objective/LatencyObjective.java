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
import com.eqixiac.equinix.design.spec.SiteIntent;
import com.eqixiac.equinix.design.spi.DesignContext;
import com.eqixiac.equinix.design.spi.Direction;
import com.eqixiac.equinix.design.spi.MetroPortfolio;
import com.eqixiac.equinix.design.spi.Objective;
import com.eqixiac.equinix.design.spi.ObjectiveValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.OptionalDouble;

/**
 * Site-weighted round-trip latency from each site to its nearest metro in the portfolio, in ms.
 * Sites whose latency to every portfolio metro is unknown are left out and the weights of the rest
 * renormalised; the explanation names them, the worst-case site, and whether its figure is measured.
 * Unknown when no site latency is known.
 */
public final class LatencyObjective implements Objective {

    @Override
    public String id() {
        return LATENCY;
    }

    @Override
    public Direction direction() {
        return Direction.MINIMIZE;
    }

    /** The round trip that scores 0 when the spec states no latency ceiling, in ms. */
    public static final double DEFAULT_WORST_ROUND_TRIP_MS = 200.0;

    /**
     * A fixed scale: 0 ms scores 100 and the spec's latency ceiling scores 0, or
     * {@value #DEFAULT_WORST_ROUND_TRIP_MS} ms round trip when the spec states none (an SDK assumption:
     * about where interactive traffic stops feeling local). A few milliseconds therefore cost a few
     * points, whatever other options are in the set.
     */
    @Override
    public ValueScale scale(DesignContext context) {
        Double ceiling = context.spec().constraints() == null ? null : context.spec().constraints().maxLatencyMs();
        return ValueScale.anchored(0.0, ceiling != null && ceiling > 0 ? ceiling : DEFAULT_WORST_ROUND_TRIP_MS);
    }

    @Override
    public String unit() {
        return "ms";
    }

    @Override
    public ObjectiveValue evaluate(MetroPortfolio portfolio, DesignContext context) {
        List<SiteIntent> sites = context.spec().sites();
        if (sites.isEmpty()) {
            return ObjectiveValue.unknown("No sites defined");
        }
        double sum = 0;
        double weight = 0;
        String worstSite = null;
        double worst = -1;
        MetroId worstMetro = null;
        List<String> unknown = new ArrayList<>();
        for (SiteIntent site : sites) {
            double best = Double.POSITIVE_INFINITY;
            MetroId bestMetro = null;
            for (MetroId metro : portfolio.metros()) {
                OptionalDouble ms = context.roundTripLatencyMs(metro, site);
                if (ms.isPresent() && ms.getAsDouble() < best) {
                    best = ms.getAsDouble();
                    bestMetro = metro;
                }
            }
            if (bestMetro == null) {
                unknown.add(site.label());
                continue;
            }
            double w = context.siteWeight(site);
            sum += w * best;
            weight += w;
            if (best > worst) {
                worst = best;
                worstSite = site.label();
                worstMetro = bestMetro;
            }
        }
        if (worstSite == null) {
            return ObjectiveValue.unknown("No site's latency to " + portfolio.key() + " is known: "
                    + String.join(", ", unknown));
        }
        double value = weight > 0 ? sum / weight : worst;
        String explanation = String.format(Locale.ROOT, "site-weighted round trip %.1f ms; worst case %s %.1f ms via %s (%s)",
                value, worstSite, worst, worstMetro.code(),
                context.latencyMeasured(worstMetro, siteNamed(sites, worstSite)) ? "measured" : "estimated");
        if (!unknown.isEmpty()) {
            explanation += "; latency unknown for " + String.join(", ", unknown) + " (left out)";
        }
        return ObjectiveValue.of(value, explanation);
    }

    private static SiteIntent siteNamed(List<SiteIntent> sites, String label) {
        for (SiteIntent site : sites) {
            if (site.label().equals(label)) {
                return site;
            }
        }
        throw new IllegalStateException(label);
    }
}
