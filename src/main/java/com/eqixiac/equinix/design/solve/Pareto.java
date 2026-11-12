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

import com.eqixiac.equinix.design.spi.Direction;

import java.util.ArrayList;
import java.util.List;

/**
 * Pareto dominance over rows of objective values. Row {@code a} dominates row {@code b} when
 * {@code a} is at least as good on every objective and strictly better on at least one. An unknown
 * value ({@link Double#NaN}) counts as worse than any known value and equal to another unknown, so
 * an option is never promoted for lacking data.
 */
public final class Pareto {

    private Pareto() {
    }

    /**
     * @param a          one row of values, one per objective
     * @param b          another row
     * @param directions the direction of each objective, same length as the rows
     * @return whether {@code a} dominates {@code b}
     */
    public static boolean dominates(double[] a, double[] b, Direction[] directions) {
        boolean strictlyBetter = false;
        for (int i = 0; i < directions.length; i++) {
            int cmp = compare(a[i], b[i], directions[i]);
            if (cmp > 0) {
                return false;
            }
            if (cmp < 0) {
                strictlyBetter = true;
            }
        }
        return strictlyBetter;
    }

    /**
     * Finds the rows no other row dominates. Rows with identical values are all kept.
     *
     * @param rows       the value rows
     * @param directions the direction of each objective
     * @return the indexes of the non-dominated rows, ascending
     */
    public static List<Integer> frontier(List<double[]> rows, Direction[] directions) {
        List<Integer> frontier = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            double[] candidate = rows.get(i);
            boolean dominated = false;
            for (int k = 0; k < frontier.size(); k++) {
                if (dominates(rows.get(frontier.get(k)), candidate, directions)) {
                    dominated = true;
                    break;
                }
            }
            if (dominated) {
                continue;
            }
            frontier.removeIf(index -> dominates(candidate, rows.get(index), directions));
            frontier.add(i);
        }
        frontier.sort(null);
        return frontier;
    }

    /** Negative when {@code x} is better, positive when {@code y} is better; NaN is worst. */
    private static int compare(double x, double y, Direction direction) {
        boolean xUnknown = Double.isNaN(x);
        boolean yUnknown = Double.isNaN(y);
        if (xUnknown || yUnknown) {
            return xUnknown == yUnknown ? 0 : (xUnknown ? 1 : -1);
        }
        return direction.compare(x, y);
    }
}
