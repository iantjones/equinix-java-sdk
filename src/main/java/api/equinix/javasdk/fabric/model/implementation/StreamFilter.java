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

package api.equinix.javasdk.fabric.model.implementation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * A single stream subscription filter expression (spec schema {@code StreamFilter}, an anyOf of
 * {@code StreamFilterSimpleExpression} and {@code StreamFilterOrFilter}). Either the simple
 * expression fields ({@code property}, {@code operator}, {@code values}) or the {@code or} list
 * of simple expressions is populated.
 */
@Getter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class StreamFilter {

    /**
     * Field to filter on, e.g. {@code /type} or {@code /subject}.
     */
    @JsonProperty("property")
    private String property;

    /**
     * Filter operator: {@code =}, {@code in}, {@code LIKE} or {@code ILIKE}.
     */
    @JsonProperty("operator")
    private String operator;

    @JsonProperty("values")
    private List<String> values;

    /**
     * Disjunction of simple expressions (spec schema {@code StreamFilterOrFilter}, max 3 items).
     */
    @JsonProperty("or")
    private List<StreamFilter> or;

    /**
     * Creates a simple filter expression.
     *
     * @param property the field to filter on, e.g. {@code /type}
     * @param operator the filter operator ({@code =}, {@code in}, {@code LIKE}, {@code ILIKE})
     * @param values the values to match
     */
    public StreamFilter(String property, String operator, List<String> values) {
        this.property = property;
        this.operator = operator;
        this.values = values;
    }

    /**
     * Creates an or-filter over the given simple expressions.
     *
     * @param or the simple expressions to combine (max 3)
     * @return the or-filter
     */
    public static StreamFilter anyOf(List<StreamFilter> or) {
        StreamFilter filter = new StreamFilter();
        filter.or = or;
        return filter;
    }
}
