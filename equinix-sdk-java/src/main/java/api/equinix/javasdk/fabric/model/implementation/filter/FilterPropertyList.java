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

package api.equinix.javasdk.fabric.model.implementation.filter;

import api.equinix.javasdk.fabric.model.serializers.FilterPropertyListSerializer;
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
        this.filterProperties.add(new FilterProperty(FilterOperator.EQUALS, property, values));
        return this;
    }

    public FilterPropertyList in(String property, Object value) {
        this.filterProperties.add(new FilterProperty(FilterOperator.EQUALS, property, Arrays.asList(value)));
        return this;
    }
}