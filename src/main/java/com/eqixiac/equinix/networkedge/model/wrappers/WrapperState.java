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

package com.eqixiac.equinix.networkedge.model.wrappers;

/**
 * Keeps a wrapper's backing JSON when a mutating call returns no body. A {@code 204 No Content}
 * (delete, some updates) or an empty refresh deserializes to {@code null}; assigning that would
 * make every later getter on the wrapper throw a {@code NullPointerException}.
 */
final class WrapperState {

    private WrapperState() {
    }

    /**
     * Returns {@code next} when the call returned a body, otherwise the current state.
     *
     * @param current the current backing JSON
     * @param next    the value the call returned, possibly {@code null}
     * @param <T>     the JSON type
     * @return the backing JSON to keep
     */
    static <T> T keep(T current, T next) {
        return next != null ? next : current;
    }
}
