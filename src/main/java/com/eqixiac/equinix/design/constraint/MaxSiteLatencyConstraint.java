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

package com.eqixiac.equinix.design.constraint;

import com.eqixiac.equinix.core.model.MetroId;
import com.eqixiac.equinix.design.spec.SiteIntent;
import com.eqixiac.equinix.design.spi.Constraint;
import com.eqixiac.equinix.design.spi.ConstraintVerdict;
import com.eqixiac.equinix.design.spi.DesignContext;
import com.eqixiac.equinix.design.spi.MetroPortfolio;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.OptionalDouble;

/**
 * Every site's round trip to its nearest portfolio metro must be within {@code constraints.maxLatencyMs}.
 * A site whose latency to every portfolio metro is unknown cannot be checked: it is not counted against
 * the limit, and the verdict names it. With no evaluable site the limit is reported as not applied.
 */
public final class MaxSiteLatencyConstraint implements Constraint {

    /** The id. */
    public static final String ID = "max_site_latency";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public ConstraintVerdict check(MetroPortfolio portfolio, DesignContext context) {
        double limit = context.spec().constraints().maxLatencyMs();
        List<String> unknown = new ArrayList<>();
        int checked = 0;
        for (SiteIntent site : context.spec().sites()) {
            OptionalDouble best = OptionalDouble.empty();
            for (MetroId metro : portfolio.metros()) {
                OptionalDouble ms = context.roundTripLatencyMs(metro, site);
                if (ms.isPresent() && (best.isEmpty() || ms.getAsDouble() < best.getAsDouble())) {
                    best = ms;
                }
            }
            if (best.isEmpty()) {
                unknown.add(site.label());
                continue;
            }
            checked++;
            if (best.getAsDouble() > limit) {
                return ConstraintVerdict.fail(ID, String.format(Locale.ROOT, "site %s is %.1f ms from its nearest metro, "
                        + "over the %.1f ms limit", site.label(), best.getAsDouble(), limit));
            }
        }
        if (checked == 0) {
            return ConstraintVerdict.pass(ID, String.format(Locale.ROOT, "the %.1f ms limit was NOT applied: no site "
                    + "latency is known (no user sites, or no measurement or coordinates)", limit));
        }
        return ConstraintVerdict.pass(ID, String.format(Locale.ROOT, "%d site(s) within %.1f ms", checked, limit)
                + (unknown.isEmpty() ? "" : "; not evaluable: " + String.join(", ", unknown)));
    }
}
