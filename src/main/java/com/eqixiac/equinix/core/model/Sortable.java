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

package com.eqixiac.equinix.core.model;

import com.eqixiac.equinix.core.enums.SortOrder;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 *
 * @author ianjones
 */
@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class Sortable implements APIParam {

    final private String propertyName;

    final private SortOrder sortOrder;

    public static Sortable build(String propertyName) {
        return new Sortable(propertyName, SortOrder.ASC);
    }

    public static Sortable build(String propertyName, SortOrder sortOrder) {
        return new Sortable(propertyName, sortOrder);
    }

    @Override
    public String toString() {
        return (sortOrder == SortOrder.DESC ? "-" : "").concat(propertyName);
    }
}
