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

package com.eqixiac.equinix.design.spi;

/**
 * How an objective's raw value becomes a 0..100 score (100 = best) for the weighted ranking.
 *
 * <p>The choice matters. A scale that depends on the spread of the options being compared
 * ({@link Range}) turns any difference, however small, into the full 0..100 span: when every option
 * costs within 7% of every other, the dearest still scores 0 and the cheapest 100, so a trivial saving
 * outweighs a large, real difference in another objective. It also lets an irrelevant option change
 * the order of two others. The other scales are fixed by the objective's meaning, so a score says how
 * good a value is, not where it happens to fall among the candidates.
 */
public sealed interface ValueScale {

    /**
     * @param value     the raw value (never NaN)
     * @param best      the best value among the options being ranked
     * @param worst     the worst value among the options being ranked
     * @param direction whether smaller or larger is better
     * @return the score, 0..100
     */
    double score(double value, double best, double worst, Direction direction);

    /** @return a one-line description for reports */
    String describe();

    /** @return the scale relative to the spread of the options: the best scores 100, the worst 0 */
    static ValueScale range() {
        return new Range();
    }

    /** @return the scale for a value that already is a 0..100 percentage */
    static ValueScale percent() {
        return new Percent();
    }

    /** @return the scale that scores a value by its ratio to the best one (for money and other positive amounts) */
    static ValueScale ratioToBest() {
        return new RatioToBest();
    }

    /**
     * @param bestAt  the value that scores 100 (and anything better)
     * @param worstAt the value that scores 0 (and anything worse)
     * @return a fixed linear scale between two anchors
     */
    static ValueScale anchored(double bestAt, double worstAt) {
        return new Anchored(bestAt, worstAt);
    }

    /**
     * Min-max over the options being ranked. The default for objectives that declare no scale; use it
     * only when nothing fixed can be said about what a good value is.
     */
    record Range() implements ValueScale {
        @Override
        public double score(double value, double best, double worst, Direction direction) {
            return best == worst ? 100.0 : clamp(100.0 * (value - worst) / (best - worst));
        }

        @Override
        public String describe() {
            return "relative to the spread of the options (best 100, worst 0)";
        }
    }

    /** The value is a percentage: it is the score when larger is better, and 100 minus it otherwise. */
    record Percent() implements ValueScale {
        @Override
        public double score(double value, double best, double worst, Direction direction) {
            return clamp(direction == Direction.MAXIMIZE ? value : 100.0 - value);
        }

        @Override
        public String describe() {
            return "the percentage itself";
        }
    }

    /**
     * The ratio to the best value: an option 7% dearer than the cheapest scores about 93, one twice as
     * dear scores 50. Needs positive values; when the best value is not positive it falls back to
     * {@link Range}.
     */
    record RatioToBest() implements ValueScale {
        @Override
        public double score(double value, double best, double worst, Direction direction) {
            if (best <= 0 || value <= 0) {
                return new Range().score(value, best, worst, direction);
            }
            return clamp(100.0 * (direction == Direction.MINIMIZE ? best / value : value / best));
        }

        @Override
        public String describe() {
            return "ratio to the best option (twice as much scores 50)";
        }
    }

    /**
     * Linear between two fixed anchors, clamped.
     *
     * @param bestAt  the value that scores 100
     * @param worstAt the value that scores 0
     */
    record Anchored(double bestAt, double worstAt) implements ValueScale {
        /** Rejects equal or non-finite anchors. */
        public Anchored {
            if (!Double.isFinite(bestAt) || !Double.isFinite(worstAt) || bestAt == worstAt) {
                throw new IllegalArgumentException("anchors must be finite and different");
            }
        }

        @Override
        public double score(double value, double best, double worst, Direction direction) {
            return clamp(100.0 * (value - worstAt) / (bestAt - worstAt));
        }

        @Override
        public String describe() {
            return String.format(java.util.Locale.ROOT, "fixed: %s scores 100, %s scores 0", trim(bestAt), trim(worstAt));
        }

        private static String trim(double v) {
            return v == Math.rint(v) ? Long.toString((long) v) : Double.toString(v);
        }
    }

    private static double clamp(double score) {
        return Math.max(0.0, Math.min(100.0, score));
    }
}
