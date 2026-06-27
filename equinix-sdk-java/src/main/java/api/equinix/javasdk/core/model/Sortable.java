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

package api.equinix.javasdk.core.model;

import api.equinix.javasdk.core.enums.SortOrder;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * <p>Sortable class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
@Getter
@Setter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class Sortable implements APIParam {

    final private String propertyName;

    final private SortOrder sortOrder;

    /**
     * <p>build.</p>
     *
     * @param propertyName a {@link java.lang.String} object.
     * @return a {@link api.equinix.javasdk.core.model.Sortable} object.
     */
    public static Sortable build(String propertyName) {
        return new Sortable(propertyName, SortOrder.ASC);
    }

    /**
     * <p>build.</p>
     *
     * @param propertyName a {@link java.lang.String} object.
     * @param sortOrder a {@link api.equinix.javasdk.core.enums.SortOrder} object.
     * @return a {@link api.equinix.javasdk.core.model.Sortable} object.
     */
    public static Sortable build(String propertyName, SortOrder sortOrder) {
        return new Sortable(propertyName, sortOrder);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return (sortOrder == SortOrder.DESC ? "-" : "").concat(propertyName);
    }
}
