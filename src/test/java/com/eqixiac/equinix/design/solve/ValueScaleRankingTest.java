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

package com.eqixiac.equinix.design.solve;

import com.eqixiac.equinix.core.model.MetroId;
import com.eqixiac.equinix.design.spi.Direction;
import com.eqixiac.equinix.design.spi.MetroPortfolio;
import com.eqixiac.equinix.design.spi.ValueScale;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Fixed value scales. The figures in {@link #londonNewYork()} are the frontier a live run returned on
 * 2026-11-12 for a spec with one site in London and one in New York (latency ms, cost USD/month,
 * provider coverage %, resilience %, compliance %).
 */
class ValueScaleRankingTest {

    private static final Direction[] DIRECTIONS = {Direction.MINIMIZE, Direction.MINIMIZE, Direction.MAXIMIZE,
            Direction.MAXIMIZE, Direction.MAXIMIZE};
    private static final ValueScale[] FIXED = {ValueScale.anchored(0, 200), ValueScale.ratioToBest(),
            ValueScale.percent(), ValueScale.percent(), ValueScale.percent()};
    private static final double[] BALANCED = {0.30, 0.20, 0.25, 0.15, 0.10};

    private final List<String> keys = new ArrayList<>();
    private final List<double[]> rows = new ArrayList<>();
    private final List<MetroPortfolio> portfolios = new ArrayList<>();

    private void option(String key, double latency, double cost, double resilience) {
        keys.add(key);
        rows.add(new double[] {latency, cost, 100, resilience, 100});
        List<MetroId> metros = new ArrayList<>();
        for (String code : key.split("\\+")) {
            metros.add(MetroId.of(code));
        }
        portfolios.add(new MetroPortfolio(metros));
    }

    private void londonNewYork() {
        option("AM+NY", 2.7, 3795.2, 100);
        option("AM+MT", 6.4, 3760.1, 100);
        option("LD+NY", 0.5, 3865.1, 100);
        option("NY+WA", 13.7, 3742.0, 100);
        option("DB", 35.4, 1854.3, 0);
        option("LD", 34.3, 1924.2, 0);
        option("JK+WA", 60.6, 3600.8, 100);
        option("JK", 203.5, 1799.6, 0);
    }

    @Test
    @DisplayName("on fixed scales the London + New York design wins and a far-away pair does not ride a 7% saving")
    void fixedScalesKeepSmallDifferencesSmall() {
        londonNewYork();
        double[][] scores = Ranking.normalise(rows, DIRECTIONS, FIXED);
        double[] weighted = Ranking.weighted(scores, BALANCED);

        assertEquals("LD+NY", keys.get(Ranking.best(rows, DIRECTIONS, FIXED, BALANCED, portfolios)));
        // Jakarta + Warsaw is 7% cheaper than London + New York: that is worth a few cost points, not 60.
        int ldNy = keys.indexOf("LD+NY");
        int jkWa = keys.indexOf("JK+WA");
        assertTrue(scores[jkWa][1] - scores[ldNy][1] < 5, "cost gap " + (scores[jkWa][1] - scores[ldNy][1]));
        assertTrue(weighted[ldNy] - weighted[jkWa] > 5, "LD+NY must clearly outrank JK+WA");
    }

    @Test
    @DisplayName("on the spread-based scale the same figures give the dearest pair a cost score near zero")
    void rangeScaleStretchesTheSpread() {
        londonNewYork();
        double[][] scores = Ranking.normalise(rows, DIRECTIONS);
        assertEquals(0.0, scores[keys.indexOf("LD+NY")][1], 1e-9);
    }

    @Test
    @DisplayName("a fixed score does not change when an unrelated option joins the set")
    void independentOfIrrelevantAlternatives() {
        londonNewYork();
        double before = Ranking.normalise(rows, DIRECTIONS, FIXED)[keys.indexOf("LD+NY")][0];
        option("SY+TY", 290.0, 5200.0, 100);
        double after = Ranking.normalise(rows, DIRECTIONS, FIXED)[keys.indexOf("LD+NY")][0];
        assertEquals(before, after, 1e-9);
    }

    @Test
    @DisplayName("each scale's arithmetic, clamping and fallbacks")
    void scales() {
        assertEquals(50.0, ValueScale.ratioToBest().score(200, 100, 400, Direction.MINIMIZE), 1e-9);
        assertEquals(50.0, ValueScale.ratioToBest().score(50, 100, 10, Direction.MAXIMIZE), 1e-9);
        // a free best option: the ratio is undefined, so the spread is used
        assertEquals(0.0, ValueScale.ratioToBest().score(400, 0, 400, Direction.MINIMIZE), 1e-9);
        assertEquals(75.0, ValueScale.anchored(0, 200).score(50, 0, 0, Direction.MINIMIZE), 1e-9);
        assertEquals(0.0, ValueScale.anchored(0, 200).score(450, 0, 0, Direction.MINIMIZE), 1e-9);
        assertEquals(40.0, ValueScale.percent().score(40, 0, 0, Direction.MAXIMIZE), 1e-9);
        assertEquals(60.0, ValueScale.percent().score(40, 0, 0, Direction.MINIMIZE), 1e-9);
        assertEquals(100.0, ValueScale.range().score(7, 7, 7, Direction.MINIMIZE), 1e-9);
        assertThrows(IllegalArgumentException.class, () -> ValueScale.anchored(5, 5));
    }
}
