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

package api.equinix.javasdk.customerportal.client.internal.implementation;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.regex.Pattern;

/**
 * Shared regex for parsing the order id returned by the colocation v2 order APIs in the
 * {@code Location} response header (e.g. {@code /orders/1-23232322}). The order id format
 * ({@code 1-23232322}) is not a UUID, so {@code Constants.UUID_PATTERN} cannot be used.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class OrderLocation {

    static final Pattern ORDER_ID_PATTERN = Pattern.compile(".*/orders/([^/\\s?]+)");

    /**
     * Captures the trailing id segment of a Location header (e.g. {@code /tickets/1-23232322}),
     * for resources whose create/action responses return {@code Location: .../{id}}.
     */
    static final Pattern LAST_SEGMENT_PATTERN = Pattern.compile(".*/([^/\\s?]+)");
}
