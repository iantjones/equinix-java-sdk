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
import com.eqixiac.equinix.design.spi.Constraint;
import com.eqixiac.equinix.design.spi.ConstraintVerdict;
import com.eqixiac.equinix.design.spi.DesignContext;
import com.eqixiac.equinix.design.spi.MetroFacts;
import com.eqixiac.equinix.design.spi.MetroPortfolio;

import java.util.Locale;

/**
 * Data sovereignty: every metro must belong to at least one of {@code constraints.geoZones} according
 * to the live catalogue's {@code geoScopes}/{@code geoZones} for that metro (matched case-insensitively;
 * {@code CH}, {@code AU}, {@code BR}, {@code JP}, {@code CA}, {@code US} also match the corresponding
 * geo scope). A metro the catalogue lists in no zone is not admitted: membership cannot be shown.
 */
public final class GeoZonesConstraint implements Constraint {

    /** The id. */
    public static final String ID = "geo_zones";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public boolean admits(MetroFacts metro, DesignContext context) {
        for (String zone : context.spec().constraints().geoZones()) {
            if (metro.geoZones().contains(zone.toUpperCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public ConstraintVerdict check(MetroPortfolio portfolio, DesignContext context) {
        for (MetroId id : portfolio.metros()) {
            MetroFacts facts = context.metro(id).orElse(null);
            if (facts == null || !admits(facts, context)) {
                return ConstraintVerdict.fail(ID, id.code() + " is not in any of " + context.spec().constraints().geoZones()
                        + (facts == null ? "" : " (catalogue zones " + facts.geoZones() + ")"));
            }
        }
        return ConstraintVerdict.pass(ID, "every metro is inside " + context.spec().constraints().geoZones());
    }
}
