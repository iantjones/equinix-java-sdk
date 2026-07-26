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

package com.eqixiac.equinix.core.http.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A single <a href="https://datatracker.ietf.org/doc/html/rfc6902">RFC&nbsp;6902 (JSON Patch)</a>
 * operation — {@code {"op": ..., "path": ..., "value": ...}}.
 *
 * <p>Several Equinix endpoints model resource updates as a JSON Patch document: an ordered array of
 * these operations sent with content-type {@code application/json-patch+json} (for example Fabric's
 * {@code PATCH /fabric/v4/networks/{id}} and {@code PATCH /fabric/v4/routers/{id}}). SDK users do not
 * normally construct these directly — the fluent {@code resource.update().field(value).save()}
 * builders translate field changes into the appropriate operations — but the type is public so the
 * raw patch surface is available when needed.</p>
 *
 * @author ianjones
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class PatchOperation {

    public static final String CONTENT_TYPE = "application/json-patch+json";

    @JsonProperty("op")
    private final String op;

    @JsonProperty("path")
    private final String path;

    @JsonProperty("value")
    private final Object value;

    /**
     *
     * @param op the operation name ({@code replace}, {@code add}, or {@code remove})
     * @param path the JSON Pointer to the target member (e.g. {@code /name})
     * @param value the new value; {@code null} (and omitted from the JSON) for {@code remove}
     */
    public PatchOperation(String op, String path, Object value) {
        this.op = op;
        this.path = path;
        this.value = value;
    }

    /**
     * Creates a {@code replace} operation, setting {@code path} to {@code value}.
     *
     * @param path the JSON Pointer to the target member (e.g. {@code /name})
     * @param value the replacement value
     * @return the operation
     */
    public static PatchOperation replace(String path, Object value) {
        return new PatchOperation("replace", path, value);
    }

    /**
     * Creates an {@code add} operation, adding {@code value} at {@code path}.
     *
     * @param path the JSON Pointer to the target member
     * @param value the value to add
     * @return the operation
     */
    public static PatchOperation add(String path, Object value) {
        return new PatchOperation("add", path, value);
    }

    /**
     * Creates a {@code remove} operation, removing the member at {@code path}.
     *
     * @param path the JSON Pointer to the target member
     * @return the operation
     */
    public static PatchOperation remove(String path) {
        return new PatchOperation("remove", path, null);
    }

    /**
     *
     * @return the operation name
     */
    public String getOp() {
        return op;
    }

    /**
     *
     * @return the JSON Pointer path
     */
    public String getPath() {
        return path;
    }

    /**
     *
     * @return the operation value, or {@code null} for {@code remove}
     */
    public Object getValue() {
        return value;
    }
}
