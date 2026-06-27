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

package api.equinix.javasdk.fabric.model.implementation.sort;

import api.equinix.javasdk.core.enums.SortOrder;
import api.equinix.javasdk.fabric.model.serializers.SortPropertyListSerializer;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@JsonSerialize(using = SortPropertyListSerializer.class)
@Getter
public class SortPropertyList {

    private final List<SortProperty> sortProperties;

    public SortPropertyList() {
        this.sortProperties = new ArrayList<>();
    }

    public SortPropertyList asc(String property) {
        this.sortProperties.add(new SortProperty(SortOrder.ASC, property));
        return this;
    }

    public SortPropertyList desc(String property) {
        this.sortProperties.add(new SortProperty(SortOrder.DESC, property));
        return this;
    }
}