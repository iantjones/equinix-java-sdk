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

package com.eqixiac.equinix.design.estate;

import com.eqixiac.equinix.design.value.ratecard.Term;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * The tunable thresholds the default rules use. Every default is an assumption, stated here and
 * repeated in the findings that rely on it.
 *
 * @param rightSizeDownBelowPercent     p95 utilisation below this percent of provisioned bandwidth suggests a smaller tier (default 30)
 * @param rightSizeUpAbovePercent       p95 utilisation above this percent suggests a larger tier (default 80)
 * @param headroomFactor                the target tier must be at least p95 × this factor (default 1.3)
 * @param minUtilizationSamples         the fewest samples a utilisation rule acts on (default 24, one day of hourly data)
 * @param bandwidthTiersMbps            the bandwidth tiers a connection can be moved to, ascending
 *                                      (default the common Fabric virtual-connection speeds 50 Mbps to 100 Gbps)
 * @param minAgeForTermDays             never recommend a term for a resource younger than this (default 90 days)
 * @param candidateTerms                the terms to price against on-demand (default 12, 24 and 36 months)
 * @param consolidateMinConnections     the fewest port-to-cloud connections in one metro that suggest a Cloud Router (default 3)
 * @param portCapacityWarnPercent       allocated bandwidth at or above this percent of port speed is a risk (default 80)
 * @param portCapacityCriticalPercent   ... and at or above this percent is critical (default 95)
 * @param routerLimitWarnPercent        router connections at or above this percent of the package limit is a risk (default 80)
 * @param routerPackageConnectionLimits package code to its connection limit, used when the API does not
 *                                      report one (default empty: the live {@code /routerPackages} response
 *                                      had no {@code vcCountMax} on 2026-11-12, so limits are unknown unless supplied)
 * @param stuckAfterDays                a pending or failed state older than this is "stuck" (default 7)
 * @param referencePriceRangePercent    the ± range applied to reference prices (default 20)
 * @param estimatePriceRangePercent     the ± range applied to estimated prices (default 35)
 */
public record EstateThresholds(double rightSizeDownBelowPercent, double rightSizeUpAbovePercent, double headroomFactor,
                               int minUtilizationSamples, List<Integer> bandwidthTiersMbps, int minAgeForTermDays,
                               List<Term> candidateTerms, int consolidateMinConnections,
                               double portCapacityWarnPercent, double portCapacityCriticalPercent,
                               double routerLimitWarnPercent, Map<String, Integer> routerPackageConnectionLimits,
                               int stuckAfterDays, double referencePriceRangePercent,
                               double estimatePriceRangePercent) {

    /** Validates and copies. */
    public EstateThresholds {
        if (rightSizeDownBelowPercent <= 0 || rightSizeUpAbovePercent <= rightSizeDownBelowPercent) {
            throw new IllegalArgumentException("Right-size thresholds must satisfy 0 < down < up");
        }
        if (headroomFactor < 1.0) {
            throw new IllegalArgumentException("headroomFactor must be at least 1.0");
        }
        if (portCapacityCriticalPercent < portCapacityWarnPercent) {
            throw new IllegalArgumentException("portCapacityCriticalPercent must be at least portCapacityWarnPercent");
        }
        bandwidthTiersMbps = bandwidthTiersMbps == null ? List.of()
                : bandwidthTiersMbps.stream().filter(t -> t != null && t > 0).distinct().sorted().toList();
        candidateTerms = candidateTerms == null ? List.of() : List.copyOf(candidateTerms);
        routerPackageConnectionLimits = routerPackageConnectionLimits == null ? Map.of()
                : java.util.Collections.unmodifiableMap(new TreeMap<>(routerPackageConnectionLimits));
    }

    /**
     * The defaults.
     *
     * @return the default thresholds
     */
    public static EstateThresholds defaults() {
        return builder().build();
    }

    /**
     * Starts from the defaults.
     *
     * @return a builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * The smallest tier at or above the given rate.
     *
     * @param mbps the rate
     * @return the tier, or {@code null} when the rate exceeds every tier
     */
    public Integer tierAtOrAbove(double mbps) {
        for (Integer t : bandwidthTiersMbps) {
            if (t >= mbps) {
                return t;
            }
        }
        return null;
    }

    /** Builds thresholds, starting from the defaults. */
    public static final class Builder {
        private double down = 30;
        private double up = 80;
        private double headroom = 1.3;
        private int minSamples = 24;
        private List<Integer> tiers = List.of(50, 100, 200, 500, 1000, 2000, 5000, 10000, 25000, 50000, 100000);
        private int minAgeDays = 90;
        private List<Term> terms = List.of(Term.MONTH_12, Term.MONTH_24, Term.MONTH_36);
        private int consolidateMin = 3;
        private double portWarn = 80;
        private double portCritical = 95;
        private double routerWarn = 80;
        private Map<String, Integer> routerLimits = Map.of();
        private int stuckDays = 7;
        private double referenceRange = 20;
        private double estimateRange = 35;

        private Builder() {
        }

        /**
         * Sets the right-size thresholds.
         *
         * @param downBelowPercent p95 percent below which to size down
         * @param upAbovePercent   p95 percent above which to size up
         * @return this builder
         */
        public Builder rightSize(double downBelowPercent, double upAbovePercent) {
            this.down = downBelowPercent;
            this.up = upAbovePercent;
            return this;
        }

        /**
         * Sets the headroom factor.
         *
         * @param factor the factor (at least 1.0)
         * @return this builder
         */
        public Builder headroomFactor(double factor) {
            this.headroom = factor;
            return this;
        }

        /**
         * Sets the fewest samples a utilisation rule acts on.
         *
         * @param samples the count
         * @return this builder
         */
        public Builder minUtilizationSamples(int samples) {
            this.minSamples = samples;
            return this;
        }

        /**
         * Sets the bandwidth tiers.
         *
         * @param tiersMbps the tiers in Mbps
         * @return this builder
         */
        public Builder bandwidthTiersMbps(List<Integer> tiersMbps) {
            this.tiers = tiersMbps;
            return this;
        }

        /**
         * Sets the minimum age before a term is recommended.
         *
         * @param days the age in days
         * @return this builder
         */
        public Builder minAgeForTermDays(int days) {
            this.minAgeDays = days;
            return this;
        }

        /**
         * Sets the terms to price.
         *
         * @param candidateTerms the terms
         * @return this builder
         */
        public Builder candidateTerms(List<Term> candidateTerms) {
            this.terms = candidateTerms;
            return this;
        }

        /**
         * Sets the fewest connections that suggest a Cloud Router.
         *
         * @param count the count
         * @return this builder
         */
        public Builder consolidateMinConnections(int count) {
            this.consolidateMin = count;
            return this;
        }

        /**
         * Sets the port capacity thresholds.
         *
         * @param warnPercent     the warning percent
         * @param criticalPercent the critical percent
         * @return this builder
         */
        public Builder portCapacity(double warnPercent, double criticalPercent) {
            this.portWarn = warnPercent;
            this.portCritical = criticalPercent;
            return this;
        }

        /**
         * Sets the router limit warning percent.
         *
         * @param percent the percent
         * @return this builder
         */
        public Builder routerLimitWarnPercent(double percent) {
            this.routerWarn = percent;
            return this;
        }

        /**
         * Supplies router package connection limits (for example from your contract or the Equinix documentation).
         *
         * @param limits package code to limit
         * @return this builder
         */
        public Builder routerPackageConnectionLimits(Map<String, Integer> limits) {
            this.routerLimits = limits;
            return this;
        }

        /**
         * Sets the age after which a pending or failed state is stuck.
         *
         * @param days the age in days
         * @return this builder
         */
        public Builder stuckAfterDays(int days) {
            this.stuckDays = days;
            return this;
        }

        /**
         * Sets the ± price ranges.
         *
         * @param referencePercent for reference prices
         * @param estimatePercent  for estimates
         * @return this builder
         */
        public Builder priceRanges(double referencePercent, double estimatePercent) {
            this.referenceRange = referencePercent;
            this.estimateRange = estimatePercent;
            return this;
        }

        /**
         * Builds the thresholds.
         *
         * @return the thresholds
         */
        public EstateThresholds build() {
            return new EstateThresholds(down, up, headroom, minSamples, tiers, minAgeDays, terms, consolidateMin,
                    portWarn, portCritical, routerWarn, routerLimits, stuckDays, referenceRange, estimateRange);
        }
    }
}
