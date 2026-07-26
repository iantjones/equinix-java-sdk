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

package com.eqixiac.equinix.ibxsmartview.model;

import com.eqixiac.equinix.ibxsmartview.model.implementation.Category;
import com.eqixiac.equinix.ibxsmartview.model.implementation.Status;

import java.util.List;

/**
 * The hierarchical (category / template / asset) assets list returned by the asset/list endpoint,
 * wrapped in the {@code payLoad}/{@code status} envelope ({@code AssetsList} in the spec).
 */
public interface AssetsList {

    /**
     * @return the assets-list payload, or {@code null} when the response carried no data
     */
    Payload getPayLoad();

    /**
     * @return the response status envelope
     */
    Status getStatus();

    /**
     * The {@code payLoad} of an {@link AssetsList} response.
     */
    interface Payload {

        String getClassification();

        List<Category> getCategories();
    }
}
