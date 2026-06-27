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
 * Thrown by {@link ResourceWaiter#await()} when the success condition is not met before the
 * configured timeout elapses. Carries the last-observed resource for diagnostics.
 *
 * @author ianjones
 */
public class WaiterTimeoutException extends WaiterException {

    private final transient Object lastObserved;

    public WaiterTimeoutException(String message, Object lastObserved) {
        super(message);
        this.lastObserved = lastObserved;
    }

    /**
     * @return the most recently fetched resource (the state observed at timeout), or {@code null}
     */
    public Object getLastObserved() {
        return lastObserved;
    }
}
