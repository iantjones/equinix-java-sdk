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

package com.eqixiac.equinix.core.model;

/**
 * Contract for types usable as a query/path parameter value — the type constraint on the SDK's
 * parameter-building helpers ({@code ParameterMapper}/{@code ModelUtils}), so arbitrary objects
 * cannot end up on a request URI.
 *
 * <p>{@link #paramValue()} is the single wire form for parameters. It defaults to
 * {@code toString()} — for a plain enum that is {@code name()}, which is the wire code for most
 * SDK enums; enums whose wire code differs from the constant name override {@code toString()}
 * (or this method directly), and value types like {@code Sortable} render their own form
 * (e.g. {@code "-propertyName"} for a descending sort).</p>
 *
 * <p><b>Invariant (enforced by {@code ApiParamContractTest}):</b> when an implementing enum also
 * declares a Jackson {@code @JsonValue} accessor for request/response bodies,
 * {@code paramValue()} must return the same string — one wire form per value, whether it travels
 * in a body or a query parameter.</p>
 *
 * @author ianjones
 */
public interface APIParam {

    /**
     * The value exactly as it must appear on the wire in a query or path parameter.
     *
     * @return the wire representation of this parameter value
     */
    default String paramValue() {
        return toString();
    }
}