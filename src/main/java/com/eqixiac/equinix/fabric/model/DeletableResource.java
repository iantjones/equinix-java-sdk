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

package com.eqixiac.equinix.fabric.model;

/**
 * A Fabric resource model whose {@code delete(...)} can be called on the model itself.
 *
 * <p>After a successful delete the model stays usable: when the service answers with a body
 * (for example a connection or Cloud Router in a {@code DEPROVISIONING} state) the model adopts it;
 * when it answers {@code 204 No Content} the model keeps its last-known state. Either way
 * {@link #isDeleted()} turns {@code true}, and the getters keep returning values instead of
 * throwing.</p>
 */
public interface DeletableResource {

    /**
     * Returns whether {@code delete(...)} was called successfully on this model instance.
     *
     * @return {@code true} once this instance has been deleted
     */
    boolean isDeleted();
}
