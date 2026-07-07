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

package api.equinix.javasdk.core.waiter;

/**
 * Thrown by {@link ResourceWaiter#await()} when the resource reaches a terminal failure state
 * (the configured {@code failWhen} condition) before succeeding. Carries the failed resource.
 *
 * @author ianjones
 */
public class WaiterFailedException extends WaiterException {

    private final transient Object resource;

    public WaiterFailedException(String message, Object resource) {
        super(message);
        this.resource = resource;
    }

    /**
     * @return the resource in its failure state, or {@code null} (always {@code null} after
     *         serialization, as the payload is transient)
     */
    public Object getResource() {
        return resource;
    }

    /**
     * Typed convenience accessor for the failed resource, e.g.
     * {@code ex.getResource(Connection.class)}.
     *
     * @param type the expected resource type
     * @param <T> the expected resource type
     * @return the resource cast to {@code type}, or {@code null} if absent
     * @throws ClassCastException if the resource is not of the given type
     */
    public <T> T getResource(Class<T> type) {
        return type.cast(resource);
    }
}
