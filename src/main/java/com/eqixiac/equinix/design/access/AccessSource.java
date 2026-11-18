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
package com.eqixiac.equinix.design.access;

/** Where an access item comes from. */
public enum AccessSource {
    /** Found in the {@link AccessInventory}: the resource exists in the project. */
    EXISTING,
    /** Named by UUID by the caller and not found in the inventory: the plan assumes it exists. */
    ASSUMED,
    /** Not in the project: an order to place outside the plan. */
    ORDER
}
