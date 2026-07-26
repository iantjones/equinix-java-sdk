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

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Event or metric selector for a stream subscription, expressed as include/except
 * expression lists (e.g. {@code equinix.fabric.connection.*}).
 *
 * <p>Prefer the {@code include(...)}/{@code except(...)} static factories — the two
 * constructor parameters are identically-typed {@code List<String>}s with opposite
 * semantics, so a positional swap silently inverts the subscription filter.</p>
 */
@Getter
@NoArgsConstructor
public class StreamSubscriptionSelector {

    @JsonProperty("include")
    private List<String> include;

    @JsonProperty("except")
    private List<String> except;

    /**
     * Explicit constructor replacing the Lombok-generated {@code @AllArgsConstructor}: the
     * argument order is pinned here in code (two identically-typed {@code List<String>}
     * parameters with opposite semantics) rather than by field declaration order.
     *
     * @param include the expressions to include, or {@code null}
     * @param except  the expressions to exclude, or {@code null}
     */
    public StreamSubscriptionSelector(List<String> include, List<String> except) {
        this.include = include;
        this.except = except;
    }

    /**
     * Creates a selector that includes only the given expressions.
     *
     * @param expressions the expressions to include
     * @return the selector
     */
    public static StreamSubscriptionSelector include(List<String> expressions) {
        return new StreamSubscriptionSelector(expressions, null);
    }

    /**
     * Creates a selector that includes everything except the given expressions.
     *
     * @param expressions the expressions to exclude
     * @return the selector
     */
    public static StreamSubscriptionSelector except(List<String> expressions) {
        return new StreamSubscriptionSelector(null, expressions);
    }
}
