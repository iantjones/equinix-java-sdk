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

/**
 * A network, cloud, or other organization to connect to. Give an autonomous system number where one
 * exists, because names are ambiguous.
 *
 * @param name the name, or {@code null}
 * @param asn the autonomous system number, or {@code null}
 * @param type the kind of organization, or {@code null}
 */
public record Party(String name, Long asn, PartyType type) {

    /**
     * Creates a party.
     *
     * @throws IllegalArgumentException if both the name and the number are absent
     */
    public Party {
        if (name == null && asn == null) {
            throw new IllegalArgumentException("A party needs a name or an autonomous system number");
        }
    }

    /**
     * Returns a party identified by its autonomous system number.
     *
     * @param asn the autonomous system number
     * @return the party
     */
    public static Party asn(long asn) {
        return new Party(null, asn, null);
    }

    /**
     * Returns a party identified by name.
     *
     * @param name the name
     * @return the party
     */
    public static Party named(String name) {
        return new Party(name, null, null);
    }

    /**
     * Says whether this party and another are the same organization: the same autonomous system
     * number where both have one, otherwise the same name without regard to case.
     *
     * @param other the other party
     * @return {@code true} if they match
     */
    public boolean matches(Party other) {
        if (other == null) {
            return false;
        }
        if (asn != null && other.asn != null) {
            return asn.equals(other.asn);
        }
        return name != null && name.equalsIgnoreCase(other.name);
    }
}
