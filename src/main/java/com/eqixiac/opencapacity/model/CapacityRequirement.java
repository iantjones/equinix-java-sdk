/*
 * Copyright 2026 Ian Jones. All Rights Reserved.
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

package com.eqixiac.opencapacity.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The capacity a customer needs. Queries, signals, and subscriptions all carry it.
 *
 * <p>Build one with {@link #define()}:
 *
 * <pre>{@code
 * CapacityRequirement requirement = CapacityRequirement.define()
 *     .addLocation(Location.metro("FR"))
 *     .cabinets(40)
 *     .density(DensityRange.kva().min(5).target(8).max(100).create())   // kVA per cabinet
 *     .neededForService(FlexibleDate.month(2027, 6))
 *     .addFeature(Feature.LIQUID_COOLING, true)
 *     .create();
 * }</pre>
 *
 * @param locations the alternatives, at least one
 * @param demand how much capacity is needed
 * @param timeframe when it is needed, or {@code null} for an immediate need
 * @param interconnection the parties that must or should be reachable
 * @param desiredFeatures the features wanted
 * @param redundancy the minimum redundancy, or {@code null}
 * @param powerFeed the electrical delivery, or {@code null}
 * @param offering the commercial form, or {@code null}
 */
public record CapacityRequirement(
        List<Location> locations, Demand demand, Timeframe timeframe,
        List<InterconnectionRequirement> interconnection, List<DesiredFeature> desiredFeatures,
        Redundancy redundancy, PowerFeed powerFeed, Offering offering) {

    /** Creates a requirement. */
    public CapacityRequirement {
        locations = locations == null ? List.of() : List.copyOf(locations);
        interconnection = interconnection == null ? List.of() : List.copyOf(interconnection);
        desiredFeatures = desiredFeatures == null ? List.of() : List.copyOf(desiredFeatures);
    }

    /**
     * Starts a new requirement.
     *
     * @return a builder
     */
    public static Builder define() {
        return new Builder();
    }

    /**
     * Returns a builder that starts from this requirement.
     *
     * @return a builder with every value of this requirement
     */
    public Builder toBuilder() {
        Builder builder = new Builder();
        builder.locations.addAll(locations);
        builder.interconnection.addAll(interconnection);
        builder.features.addAll(desiredFeatures);
        builder.redundancy = redundancy;
        builder.powerFeed = powerFeed;
        builder.offering = offering;
        if (demand != null) {
            builder.power = demand.power();
            builder.powerBasis = demand.powerBasis();
            builder.powerFactor = demand.powerFactor();
            builder.cabinets = demand.cabinets();
            builder.density = demand.density();
            builder.spaceType = demand.spaceType();
            builder.contiguous = demand.contiguous();
        }
        if (timeframe != null) {
            builder.neededForSale = timeframe.neededForSale();
            builder.neededForService = timeframe.neededForService();
            builder.latestAcceptable = timeframe.latestAcceptable();
            builder.termMonths = timeframe.termMonths();
            builder.ramp.addAll(timeframe.ramp());
        }
        return builder;
    }

    /** Builds a {@link CapacityRequirement}. Nothing is sent to a provider by this builder. */
    public static final class Builder {
        private final List<Location> locations = new ArrayList<>();
        private final List<InterconnectionRequirement> interconnection = new ArrayList<>();
        private final List<DesiredFeature> features = new ArrayList<>();
        private final List<RampStep> ramp = new ArrayList<>();
        private Power power;
        private PowerBasis powerBasis;
        private Double powerFactor;
        private CabinetDemand cabinets;
        private DensityRange density;
        private SpaceType spaceType;
        private Boolean contiguous;
        private FlexibleDate neededForSale;
        private FlexibleDate neededForService;
        private FlexibleDate latestAcceptable;
        private Integer termMonths;
        private Redundancy redundancy;
        private PowerFeed powerFeed;
        private Offering offering;

        private Builder() {
        }

        /**
         * Adds a location. Locations are alternatives, and the answer has a result for each.
         *
         * @param location the location
         * @return this builder
         */
        public Builder addLocation(Location location) {
            locations.add(Objects.requireNonNull(location, "location"));
            return this;
        }

        /**
         * Removes every location added so far.
         *
         * @return this builder
         */
        public Builder clearLocations() {
            locations.clear();
            return this;
        }

        /**
         * Sets the total power.
         *
         * @param value the total power
         * @return this builder
         */
        public Builder power(Power value) {
            this.power = value;
            return this;
        }

        /**
         * Says what the total power measures.
         *
         * @param value the equipment load or the circuit capacity
         * @return this builder
         */
        public Builder powerBasis(PowerBasis value) {
            this.powerBasis = value;
            return this;
        }

        /**
         * Sets the power factor used to convert between apparent and real power.
         *
         * @param value greater than 0 and at most 1
         * @return this builder
         */
        public Builder powerFactor(double value) {
            this.powerFactor = value;
            return this;
        }

        /**
         * Sets the number of cabinets.
         *
         * @param count the number of cabinets
         * @return this builder
         */
        public Builder cabinets(int count) {
            this.cabinets = new CabinetDemand(count, null);
            return this;
        }

        /**
         * Sets the number of cabinets and the smallest number worth an answer.
         *
         * @param count the number of cabinets
         * @param minimum the smallest useful number
         * @return this builder
         */
        public Builder cabinets(int count, int minimum) {
            this.cabinets = new CabinetDemand(count, minimum);
            return this;
        }

        /**
         * Sets the density per cabinet.
         *
         * @param value the density range
         * @return this builder
         */
        public Builder density(DensityRange value) {
            this.density = value;
            return this;
        }

        /**
         * Sets the form of space.
         *
         * @param value the form of space
         * @return this builder
         */
        public Builder spaceType(SpaceType value) {
            this.spaceType = value;
            return this;
        }

        /**
         * Says whether the cabinets must be adjacent in one space.
         *
         * @param value {@code true} if they must be adjacent
         * @return this builder
         */
        public Builder contiguous(boolean value) {
            this.contiguous = value;
            return this;
        }

        /**
         * Sets when you need to be able to contract for the capacity.
         *
         * @param value the date
         * @return this builder
         */
        public Builder neededForSale(FlexibleDate value) {
            this.neededForSale = value;
            return this;
        }

        /**
         * Sets when the capacity must be usable. Without it the requirement states an immediate need.
         *
         * @param value the date
         * @return this builder
         */
        public Builder neededForService(FlexibleDate value) {
            this.neededForService = value;
            return this;
        }

        /**
         * Sets the last service date that still works.
         *
         * @param value the date
         * @return this builder
         */
        public Builder latestAcceptable(FlexibleDate value) {
            this.latestAcceptable = value;
            return this;
        }

        /**
         * Sets the intended length of the commitment.
         *
         * @param months the term in months
         * @return this builder
         */
        public Builder termMonths(int months) {
            this.termMonths = months;
            return this;
        }

        /**
         * Adds a step of a phased need. Add steps in date order; the last is 100 percent.
         *
         * @param by the date of the step
         * @param percent the cumulative share of the demand needed by that date
         * @return this builder
         */
        public Builder addRampStep(FlexibleDate by, double percent) {
            ramp.add(new RampStep(Objects.requireNonNull(by, "by"), percent));
            return this;
        }

        /**
         * Adds a feature.
         *
         * @param feature the feature
         * @param required {@code true} for a hard constraint, {@code false} for a preference
         * @return this builder
         */
        public Builder addFeature(Feature feature, boolean required) {
            features.add(new DesiredFeature(Objects.requireNonNull(feature, "feature"), required));
            return this;
        }

        /**
         * Adds an interconnection party.
         *
         * @param party the party
         * @param within how close the party must be
         * @param required {@code true} for a hard constraint, {@code false} for a preference
         * @return this builder
         */
        public Builder addInterconnection(Party party, InterconnectionScope within, boolean required) {
            return addInterconnection(new InterconnectionRequirement(party, within, null, required));
        }

        /**
         * Adds an interconnection party with every option.
         *
         * @param requirement the party, the scope, the latency limit, and the flag
         * @return this builder
         */
        public Builder addInterconnection(InterconnectionRequirement requirement) {
            Objects.requireNonNull(requirement, "requirement");
            Objects.requireNonNull(requirement.party(), "party");
            interconnection.add(requirement);
            return this;
        }

        /**
         * Sets the minimum redundancy.
         *
         * @param powerLevel the power redundancy, or {@code null}
         * @param coolingLevel the cooling redundancy, or {@code null}
         * @return this builder
         */
        public Builder redundancy(RedundancyLevel powerLevel, RedundancyLevel coolingLevel) {
            this.redundancy = new Redundancy(powerLevel, coolingLevel);
            return this;
        }

        /**
         * Sets the electrical delivery to the cabinet.
         *
         * @param value the power feed
         * @return this builder
         */
        public Builder powerFeed(PowerFeed value) {
            this.powerFeed = value;
            return this;
        }

        /**
         * Sets the commercial form.
         *
         * @param value the offering
         * @return this builder
         */
        public Builder offering(Offering value) {
            this.offering = value;
            return this;
        }

        /**
         * Creates the requirement.
         *
         * @return the requirement
         * @throws IllegalStateException if there is no location, if the demand has neither power nor
         *     cabinets with a density, or if the total power cannot fit the cabinets at the density
         */
        public CapacityRequirement create() {
            if (locations.isEmpty()) {
                throw new IllegalStateException("A requirement needs at least one location. Call addLocation().");
            }
            if (power == null && (cabinets == null || density == null)) {
                throw new IllegalStateException(
                        "A requirement needs power(), or both cabinets() and density().");
            }
            if (power != null && cabinets != null && density != null) {
                double total = power.inKilo();
                if (density.max() != null && total > cabinets.count() * density.max()) {
                    throw new IllegalStateException("power() is greater than cabinets times the density max.");
                }
                if (density.min() != null && total < cabinets.count() * density.min()) {
                    throw new IllegalStateException("power() is less than cabinets times the density min.");
                }
            }
            Demand demand = new Demand(power, powerBasis, powerFactor, cabinets, density, spaceType, contiguous);
            boolean timed = neededForSale != null || neededForService != null || latestAcceptable != null
                    || termMonths != null || !ramp.isEmpty();
            Timeframe timeframe = timed
                    ? new Timeframe(neededForSale, neededForService, latestAcceptable, termMonths, ramp)
                    : null;
            return new CapacityRequirement(
                    locations, demand, timeframe, interconnection, features, redundancy, powerFeed, offering);
        }
    }
}
