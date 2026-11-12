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

package com.eqixiac.equinix.fabric.model.implementation.filter;

import com.eqixiac.equinix.fabric.model.serializers.FilterPropertyListSerializer;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@JsonSerialize(using = FilterPropertyListSerializer.class)
@Getter
public class FilterPropertyList {

    private final FilterType filterType;

    private final List<FilterProperty> filterProperties;

    public FilterPropertyList(FilterType filterType) {
        this.filterType = filterType;
        this.filterProperties = new ArrayList<>();
    }

    public FilterPropertyList equals(String property, Object value) {
        this.filterProperties.add(new FilterProperty(FilterOperator.EQUALS, property, List.of(value)));
        return this;
    }

    public FilterPropertyList in(String property, List<Object> values) {
        this.filterProperties.add(new FilterProperty(FilterOperator.IN, property, values));
        return this;
    }

    public FilterPropertyList in(String property, Object value) {
        this.filterProperties.add(new FilterProperty(FilterOperator.IN, property, Arrays.asList(value)));
        return this;
    }

    /**
     * Adds a {@code BETWEEN} expression with an inclusive lower and upper bound.
     *
     * @param property the filter property path (for example {@code /dataPoints/endDateTime})
     * @param from the lower bound
     * @param to the upper bound
     * @return this list, for chaining
     */
    public FilterPropertyList between(String property, Object from, Object to) {
        this.filterProperties.add(new FilterProperty(FilterOperator.BETWEEN, property, List.of(from, to)));
        return this;
    }

    /**
     * Adds a {@code >} expression.
     *
     * @param property the filter property path
     * @param value the exclusive lower bound
     * @return this list, for chaining
     */
    public FilterPropertyList greaterThan(String property, Object value) {
        this.filterProperties.add(new FilterProperty(FilterOperator.GREATER_THAN, property, List.of(value)));
        return this;
    }

    /**
     * Returns whether any expression in this list targets {@code property} with one of the given
     * operators (any operator when none is given) and at least one value.
     *
     * @param property the filter property path
     * @param operators the accepted operators; empty means any
     * @return {@code true} when such an expression is present
     */
    public boolean hasProperty(String property, FilterOperator... operators) {
        List<FilterOperator> accepted = Arrays.asList(operators);
        return this.filterProperties.stream().anyMatch(p -> property.equals(p.getProperty())
                && (accepted.isEmpty() || accepted.contains(p.getOperator()))
                && p.getValues() != null && !p.getValues().isEmpty());
    }
}