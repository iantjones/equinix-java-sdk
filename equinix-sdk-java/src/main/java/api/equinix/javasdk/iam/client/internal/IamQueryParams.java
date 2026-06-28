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

package api.equinix.javasdk.iam.client.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Small internal builder for assembling the optional query-parameter map used by IAM list
 * operations (e.g. {@code pageToken}, {@code pageSize}, {@code projectErn}, {@code serviceId}).
 * {@code null} values are skipped so that only the parameters the caller actually supplied are
 * sent on the request.
 */
public final class IamQueryParams {

    private final Map<String, List<String>> params = new HashMap<>();

    private IamQueryParams() {
    }

    /**
     * Starts a new query-parameter builder.
     *
     * @return a fresh builder
     */
    public static IamQueryParams builder() {
        return new IamQueryParams();
    }

    /**
     * Adds a string parameter unless its value is {@code null}.
     *
     * @param name the parameter name
     * @param value the parameter value (skipped when {@code null})
     * @return this builder for chaining
     */
    public IamQueryParams add(String name, String value) {
        if (value != null) {
            List<String> values = new ArrayList<>();
            values.add(value);
            params.put(name, values);
        }
        return this;
    }

    /**
     * Adds a numeric parameter (rendered via {@code toString}) unless its value is {@code null}.
     *
     * @param name the parameter name
     * @param value the parameter value (skipped when {@code null})
     * @return this builder for chaining
     */
    public IamQueryParams add(String name, Integer value) {
        if (value != null) {
            add(name, value.toString());
        }
        return this;
    }

    /**
     * Builds the assembled query-parameter map.
     *
     * @return the query-parameter map (may be empty, never {@code null})
     */
    public Map<String, List<String>> build() {
        return params;
    }
}
