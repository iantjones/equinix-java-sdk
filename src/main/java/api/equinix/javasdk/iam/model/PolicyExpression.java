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

package api.equinix.javasdk.iam.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.TextNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A lossless, typed wrapper for the polymorphic {@code oneOf} union members used throughout the IAM
 * access-control schemas — the {@code permissions}/{@code intersect}/{@code subtract} entries of an
 * {@link AccessPolicy}, {@link PermissionSet} and {@link PrincipalPolicy}, and the
 * {@code managedPolicies}/{@code managedPermissionSets}/{@code subtract} fields of a
 * {@link PolicyMask}.
 *
 * <p>Each of these union members may be:</p>
 * <ul>
 *   <li>a bare string — a literal such as {@code "none"} / {@code "all"}, an Equinix Resource Name
 *       (ERN), a {@code permissionset:}/{@code managedset:}/{@code managedpolicy:} id, etc.; or</li>
 *   <li>a structured value — an inline-permission object, a foreign-access-policy reference, a
 *       general permission-set reference, an array of ids, a {@code subtract} object, and so on.</li>
 * </ul>
 *
 * <p>Rather than collapsing this to a raw {@code Object} (which loses type fidelity and is awkward
 * to inspect), a {@code PolicyExpression} preserves whichever form was received on read and emits
 * exactly that form on write. Internally it carries the underlying {@link JsonNode} verbatim, so
 * <em>both</em> the string and structured shapes round-trip losslessly; serialization simply re-emits
 * the captured node via {@link #toJsonNode()} (annotated {@link JsonValue}), and deserialization
 * captures any incoming JSON via {@link #fromJsonNode(JsonNode)} (annotated {@link JsonCreator}).</p>
 *
 * <p>Convenience accessors ({@link #isString()}, {@link #asString()}, {@link #isArray()},
 * {@link #asStringList()}, {@link #isObject()}) let callers branch on the form without depending on
 * Jackson types, while {@link #toJsonNode()} exposes the full structure for richer navigation.</p>
 */
public final class PolicyExpression {

    private final JsonNode node;

    private PolicyExpression(JsonNode node) {
        this.node = node;
    }

    /**
     * Captures an arbitrary deserialized JSON value (string, array or object) verbatim. Used by
     * Jackson when deserializing a union member.
     *
     * @param node the raw JSON node (never {@code null} in practice; {@code null}/missing maps to a
     *             JSON null node)
     * @return a {@code PolicyExpression} wrapping the node
     */
    @JsonCreator
    public static PolicyExpression fromJsonNode(JsonNode node) {
        return new PolicyExpression(node == null ? JsonNodeFactory.instance.nullNode() : node);
    }

    /**
     * Creates a {@code PolicyExpression} from a bare string member (e.g. {@code "none"}, an ERN, or
     * a permission-set id).
     *
     * @param value the string value
     * @return a string-form {@code PolicyExpression}
     */
    public static PolicyExpression of(String value) {
        return new PolicyExpression(new TextNode(value));
    }

    /**
     * Creates a {@code PolicyExpression} from a list of string members (e.g. an array of
     * {@code managedpolicy:} ids), preserving the array form on write.
     *
     * @param values the string values
     * @return an array-form {@code PolicyExpression}
     */
    public static PolicyExpression ofStrings(List<String> values) {
        var array = JsonNodeFactory.instance.arrayNode();
        if (values != null) {
            for (String value : values) {
                array.add(value);
            }
        }
        return new PolicyExpression(array);
    }

    /**
     * Wraps an already-built structured node (e.g. an inline-permission object or a {@code subtract}
     * object) verbatim.
     *
     * @param node the structured JSON node
     * @return a structured-form {@code PolicyExpression}
     */
    public static PolicyExpression of(JsonNode node) {
        return fromJsonNode(node);
    }

    /**
     * @return {@code true} when this entry is a bare JSON string
     */
    public boolean isString() {
        return node != null && node.isTextual();
    }

    /**
     * @return {@code true} when this entry is a JSON array
     */
    public boolean isArray() {
        return node != null && node.isArray();
    }

    /**
     * @return {@code true} when this entry is a structured JSON object
     */
    public boolean isObject() {
        return node != null && node.isObject();
    }

    /**
     * @return the string value when {@link #isString()}, otherwise {@code null}
     */
    public String asString() {
        return isString() ? node.textValue() : null;
    }

    /**
     * Returns the array members as strings when this entry is an array of textual values, otherwise
     * {@code null}. Non-textual array members are rendered via their JSON text.
     *
     * @return the list of string members, or {@code null} when not an array
     */
    public List<String> asStringList() {
        if (!isArray()) {
            return null;
        }
        List<String> values = new ArrayList<>();
        node.forEach(element -> values.add(element.isTextual() ? element.textValue() : element.toString()));
        return values;
    }

    /**
     * Exposes the underlying JSON node verbatim for full structural navigation, and is the value
     * Jackson serializes (so the captured form is re-emitted exactly).
     *
     * @return the underlying {@link JsonNode}
     */
    @JsonValue
    public JsonNode toJsonNode() {
        return node;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof PolicyExpression)) {
            return false;
        }
        return Objects.equals(node, ((PolicyExpression) other).node);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(node);
    }

    @Override
    public String toString() {
        return node == null ? "null" : node.toString();
    }
}
