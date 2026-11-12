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

package com.eqixiac.equinix.ibxsmartview.client.internal.implementation;

/**
 * Fail-fast checks for query parameters the IBX SmartView specs mark required. Without them the
 * service answers HTTP 400 (live, 2026-11-11: v1 hierarchy and legacy environmental/power calls
 * without {@code accountNo}); checking first gives a named message and sends no request.
 */
final class SmartViewArguments {

    private SmartViewArguments() {
    }

    /**
     * Throws when a required parameter is null or blank.
     *
     * @param api   the API name used in the message, for example {@code "hierarchy"}
     * @param names alternating parameter name / value pairs
     * @throws IllegalArgumentException naming the first missing parameter
     */
    static void require(String api, String... names) {
        for (int i = 0; i + 1 < names.length; i += 2) {
            String value = names[i + 1];
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException(names[i] + " is required by the IBX SmartView " + api
                        + " API (the service answers HTTP 400 without it)");
            }
        }
    }
}
