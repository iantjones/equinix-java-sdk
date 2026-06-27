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
 * Base type for failures raised by {@link ResourceWaiter}. Catch this to handle either a timeout
 * ({@link WaiterTimeoutException}) or a terminal failure state ({@link WaiterFailedException}).
 *
 * @author ianjones
 */
public class WaiterException extends RuntimeException {

    public WaiterException(String message) {
        super(message);
    }

    public WaiterException(String message, Throwable cause) {
        super(message, cause);
    }
}
