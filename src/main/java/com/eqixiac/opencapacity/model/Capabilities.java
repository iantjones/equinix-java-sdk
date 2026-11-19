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

import java.util.List;

/**
 * What a provider supports: the parts of the specification it implements, the location levels it
 * accepts, the features it recognizes, and which optional answer elements it populates.
 *
 * @param specVersion the version of the specification the provider implements
 * @param provider who the provider is
 * @param conformance the parts of the specification the provider implements
 * @param authentication where to get a token
 * @param locationLevels the levels accepted in a requirement
 * @param areaLocations whether the provider accepts a point and a radius, or {@code null} for no
 * @param finestAnswerLevel the finest level at which the provider answers
 * @param features the features the provider recognizes
 * @param defaultPowerFactor the power factor the provider applies when a requirement has none, or
 *     {@code null}
 * @param deliveryTypes the delivery types the provider supports for subscriptions
 * @param disclosure which optional answer elements the provider populates
 * @param limits the provider's limits, or {@code null}
 */
public record Capabilities(
        String specVersion, Provider provider, List<ConformanceClass> conformance, Authentication authentication,
        List<LocationLevel> locationLevels, Boolean areaLocations, LocationLevel finestAnswerLevel,
        List<Feature> features, Double defaultPowerFactor, List<DeliveryType> deliveryTypes,
        Disclosure disclosure, Limits limits) {

    /** Creates a capabilities document. */
    public Capabilities {
        conformance = conformance == null ? List.of() : List.copyOf(conformance);
        locationLevels = locationLevels == null ? List.of() : List.copyOf(locationLevels);
        features = features == null ? List.of() : List.copyOf(features);
        deliveryTypes = deliveryTypes == null ? List.of() : List.copyOf(deliveryTypes);
    }

    /**
     * Says whether the provider implements a part of the specification.
     *
     * @param part the part
     * @return {@code true} if the provider lists it
     */
    public boolean supports(ConformanceClass part) {
        return conformance.contains(part);
    }

    /**
     * Says whether the provider accepts a point and a radius as a location.
     *
     * @return {@code true} if it does
     */
    public boolean acceptsAreas() {
        return Boolean.TRUE.equals(areaLocations);
    }

    /**
     * Who the provider is.
     *
     * @param name the name
     * @param url the web address, or {@code null}
     */
    public record Provider(String name, String url) {
    }

    /**
     * Where to get a token.
     *
     * @param tokenUrl the OAuth 2.0 token endpoint
     */
    public record Authentication(String tokenUrl) {
    }

    /**
     * How often the provider populates each optional answer element.
     *
     * @param timing the sale and service dates
     * @param density the density range
     * @param cabinets the cabinet band
     * @param power the power band
     * @param interconnection the presence of interconnection parties
     * @param reasons the reasons for a grade that is not {@code AVAILABLE}
     */
    public record Disclosure(
            DisclosureLevel timing, DisclosureLevel density, DisclosureLevel cabinets, DisclosureLevel power,
            DisclosureLevel interconnection, DisclosureLevel reasons) {
    }

    /**
     * A provider's limits. A component is {@code null} when the provider states no limit.
     *
     * @param maxLocationsPerRequirement the most locations in one requirement
     * @param answerValiditySeconds how long an answer applies
     * @param maxSignalLifetimeDays the longest life of a signal
     * @param maxSubscriptions the most subscriptions per customer
     * @param minNotificationIntervalSeconds the shortest time between answer events of one
     *     subscription
     * @param eventRetentionHours how long events can be read back
     */
    public record Limits(
            Integer maxLocationsPerRequirement, Integer answerValiditySeconds, Integer maxSignalLifetimeDays,
            Integer maxSubscriptions, Integer minNotificationIntervalSeconds, Integer eventRetentionHours) {
    }
}
