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

package com.eqixiac.equinix.messaging.transport;

import java.time.Duration;
import java.util.List;

/**
 * A peek-lock receiver bound to one EMG queue (or topic subscription). This is the seam between
 * {@link EmgListener} and a concrete messaging client; the bundled implementation is
 * {@link com.eqixiac.equinix.messaging.transport.servicebus.ServiceBusEmgReceiver}.
 */
public interface EmgReceiver extends AutoCloseable {

    /**
     * Receives up to {@code maxMessages}, waiting at most {@code maxWait} for the first one.
     *
     * @param maxMessages upper bound on the batch size, at least 1
     * @param maxWait     how long to wait before returning a possibly empty batch
     * @return the locked deliveries, possibly empty, never {@code null}
     */
    List<EmgDelivery> receive(int maxMessages, Duration maxWait);

    /** Closes the underlying connection. Unsettled locks are released by the broker. */
    @Override
    void close();
}
