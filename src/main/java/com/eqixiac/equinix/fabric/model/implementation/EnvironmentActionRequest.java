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

package com.eqixiac.equinix.fabric.model.implementation;

import com.eqixiac.equinix.fabric.enums.EnvironmentActionType;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.Objects;

/**
 * Request body of the Fabric v4 {@code EnvironmentActionRequest} schema, sent to
 * {@code POST /fabric/v4/serviceProfiles/{serviceProfileId}/environments/{environmentId}/actions}.
 * The schema requires both {@code type} and {@code keyDetails}.
 *
 * <p><b>Beta</b>: the operation ({@code serviceProfileEnvironmentAction}) carries the catalog's
 * Beta marker (catalog fetched 2026-09-21).</p>
 *
 * <p>Serialized form of the catalog's {@code ValidateActivationKey} example:</p>
 * <pre>{@code
 * {"type": "VALIDATE_ACTIVATION_KEY", "keyDetails": {"value": "key_here"}}
 * }</pre>
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EnvironmentActionRequest {

    @JsonProperty("type")
    private final EnvironmentActionType type;

    @JsonProperty("keyDetails")
    private final ActivationKeyDetails keyDetails;

    /**
     * @param type       the action type; must not be {@code null} or
     *                   {@link EnvironmentActionType#UNKNOWN}
     * @param keyDetails the activation key details; must not be {@code null}
     * @throws NullPointerException     if either argument is {@code null}
     * @throws IllegalArgumentException if {@code type} is {@code UNKNOWN}
     */
    public EnvironmentActionRequest(EnvironmentActionType type, ActivationKeyDetails keyDetails) {
        this.type = Objects.requireNonNull(type, "type");
        this.keyDetails = Objects.requireNonNull(keyDetails, "keyDetails");
        if (type == EnvironmentActionType.UNKNOWN) {
            throw new IllegalArgumentException("UNKNOWN is a read-side fallback and cannot be sent");
        }
    }

    /**
     * Builds a {@code VALIDATE_ACTIVATION_KEY} request.
     *
     * @param keyDetails the activation key details; must not be {@code null}
     * @return the request body
     */
    public static EnvironmentActionRequest validateActivationKey(ActivationKeyDetails keyDetails) {
        return new EnvironmentActionRequest(EnvironmentActionType.VALIDATE_ACTIVATION_KEY, keyDetails);
    }
}
