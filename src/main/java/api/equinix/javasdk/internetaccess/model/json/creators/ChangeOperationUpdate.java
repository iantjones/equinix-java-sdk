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

package api.equinix.javasdk.internetaccess.model.json.creators;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * A single change operation in the {@code ChangeRequest} body of an Equinix Internet Access
 * (EIA) v2 service update ({@code PATCH /internetAccess/v2/services/{serviceId}}).
 *
 * <p>Shaped like a JSON Patch operation — {@code {"op": ..., "path": ..., "value": ...}} — but
 * sent as a plain {@code application/json} array (not {@code application/json-patch+json}), and
 * with an optional nested {@link ServiceOrderRequest order}.</p>
 *
 * <p>Per the EIA v2 specification the allowed {@code op} values are {@code replace}, {@code add}
 * and {@code remove}, and the allowed {@code path} values are {@code /bandwidth},
 * {@code /routingProtocol/ipv4/customerRoutes[*]} and
 * {@code /routingProtocol/ipv6/customerRoutes[*]}. Only one {@code replace} operation is allowed
 * for the {@code /bandwidth} path.</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
public final class ChangeOperationUpdate {

    /** The operation name. */
    @JsonProperty("op")
    private final String op;

    /** The path to the updated parameter. */
    @JsonProperty("path")
    private final String path;

    /** The new value, or {@code null} for {@code remove}. */
    @JsonProperty("value")
    private final String value;

    /** The associated service order, or {@code null}. */
    @JsonProperty("order")
    private final ServiceOrderRequest order;

    /**
     *
     * @param op the operation name ({@code replace}, {@code add}, or {@code remove})
     * @param path the path to the updated parameter (e.g. {@code /bandwidth})
     * @param value the new value for the updated parameter
     * @param order the associated service order, or {@code null} if not applicable
     */
    public ChangeOperationUpdate(String op, String path, String value, ServiceOrderRequest order) {
        this.op = op;
        this.path = path;
        this.value = value;
        this.order = order;
    }

    /**
     * Creates a {@code replace} operation, setting {@code path} to {@code value}.
     *
     * @param path the path to the target parameter (e.g. {@code /bandwidth})
     * @param value the replacement value
     * @return the operation
     */
    public static ChangeOperationUpdate replace(String path, String value) {
        return new ChangeOperationUpdate("replace", path, value, null);
    }

    /**
     * Creates an {@code add} operation, adding {@code value} at {@code path}.
     *
     * @param path the path to the target parameter
     * @param value the value to add
     * @return the operation
     */
    public static ChangeOperationUpdate add(String path, String value) {
        return new ChangeOperationUpdate("add", path, value, null);
    }

    /**
     * Creates a {@code remove} operation, removing the parameter at {@code path}.
     *
     * @param path the path to the target parameter
     * @return the operation
     */
    public static ChangeOperationUpdate remove(String path) {
        return new ChangeOperationUpdate("remove", path, null, null);
    }

}
