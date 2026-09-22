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

package com.eqixiac.equinix.fabric.model;

import com.eqixiac.equinix.fabric.enums.EnvironmentActionState;
import com.eqixiac.equinix.fabric.enums.EnvironmentActionType;
import com.eqixiac.equinix.fabric.model.implementation.ActivationKeyDetails;
import com.eqixiac.equinix.fabric.model.implementation.ChangeLog;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * The Fabric v4 {@code EnvironmentActionResponse} schema: the {@code 201} body of
 * {@code POST /fabric/v4/serviceProfiles/{serviceProfileId}/environments/{environmentId}/actions}.
 *
 * <p><b>Beta</b>: the schema carries the catalog's Beta marker (catalog fetched 2026-09-21). The
 * catalog publishes the schema but no response example, so which properties the service populates
 * is unverified. Every getter can return {@code null}.</p>
 *
 * <table>
 *   <caption>Properties</caption>
 *   <tr><th>Getter</th><th>Catalog property</th><th>Catalog description</th></tr>
 *   <tr><td>{@code getHref()}</td><td>{@code href} (uri, read-only)</td><td>"Environment action URI"</td></tr>
 *   <tr><td>{@code getType()}</td><td>{@code type}</td><td>{@code EnvironmentActionTypeEnum}</td></tr>
 *   <tr><td>{@code getUuid()}</td><td>{@code uuid} (uuid)</td><td>"Equinix-assigned action identifier"</td></tr>
 *   <tr><td>{@code getState()}</td><td>{@code state}</td><td>"ACTIVE - already used , INACTIVE - not used"</td></tr>
 *   <tr><td>{@code getKeyDetails()}</td><td>{@code keyDetails}</td><td>{@code ActivationKeyDetails}</td></tr>
 *   <tr><td>{@code getChangeLog()}</td><td>{@code changeLog}</td><td>{@code Changelog}</td></tr>
 * </table>
 */
public interface EnvironmentActionResponse {

    String getHref();

    EnvironmentActionType getType();

    String getUuid();

    /**
     * Usage state of the activation key. {@code INACTIVE} means not used; {@code ACTIVE} means
     * already used.
     *
     * @return the state, {@code UNKNOWN} for a value this SDK release does not define, or
     *         {@code null} if the response omits it
     */
    EnvironmentActionState getState();

    ActivationKeyDetails getKeyDetails();

    ChangeLog getChangeLog();

    /**
     * Reports whether the response states that the activation key has not been used. Derived
     * from {@code getState()}; it is not a catalog property and is excluded from serialization.
     *
     * @return {@code true} only when {@code getState()} is {@code INACTIVE}; {@code false} for
     *         {@code ACTIVE}, {@code UNKNOWN} and {@code null}
     */
    @JsonIgnore
    default boolean isKeyUnused() {
        return getState() == EnvironmentActionState.INACTIVE;
    }
}
