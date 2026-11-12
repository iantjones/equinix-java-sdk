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

import com.eqixiac.equinix.messaging.EmgMessage;
import com.eqixiac.equinix.messaging.EmgMessageHandler;
import com.eqixiac.equinix.messaging.EmgMessageParser;
import com.eqixiac.equinix.messaging.EmgParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Pulls messages from an {@link EmgReceiver}, parses them with {@link EmgMessageParser}, dispatches
 * them to an {@link EmgMessageHandler} and settles each one explicitly:
 *
 * <ul>
 *   <li><b>complete</b> when the handler returns normally;</li>
 *   <li><b>abandon</b> when the handler throws (the broker redelivers it; after the queue's max
 *       delivery count Service Bus dead-letters it itself);</li>
 *   <li><b>dead-letter</b> with reason {@value #DEAD_LETTER_REASON} when the body is not an EMG
 *       envelope (redelivery could never succeed).</li>
 * </ul>
 *
 * <p>Messages are handled sequentially on the thread calling {@link #run()} or {@link #pollOnce()}.
 * {@link #close()} stops the loop after the in-flight message is settled, abandons the rest of the
 * current batch so they are redelivered promptly, and closes the receiver. Nothing logged here
 * contains a message body or a credential: only broker message ids, delivery counts and exception
 * types.
 */
public final class EmgListener implements AutoCloseable {

    /** Dead-letter reason used for bodies that are not EMG envelopes. */
    public static final String DEAD_LETTER_REASON = "EmgUnparseableMessage";

    private static final Logger LOG = LoggerFactory.getLogger(EmgListener.class);

    private final EmgReceiver receiver;
    private final EmgMessageHandler handler;
    private final EmgMessageParser parser;
    private final int batchSize;
    private final Duration maxWait;
    private final Duration errorBackoff;
    private final Duration closeTimeout;

    private final AtomicBoolean running = new AtomicBoolean();
    private final AtomicBoolean closed = new AtomicBoolean();
    private final CountDownLatch closeSignal = new CountDownLatch(1);
    private final CountDownLatch loopExited = new CountDownLatch(1);
    private volatile Thread runner;
    private volatile boolean closeRequestedByRunner;

    /**
     * Creates a listener with defaults: batches of 10, 30 s receive wait, 5 s back-off after a
     * receive error, and up to 60 s for {@link #close()} to let the in-flight message finish.
     *
     * @param receiver the receiver (owned by the listener: closed by {@link #close()})
     * @param handler  the handler
     */
    public EmgListener(EmgReceiver receiver, EmgMessageHandler handler) {
        this(receiver, handler, 10, Duration.ofSeconds(30), Duration.ofSeconds(5), Duration.ofSeconds(60));
    }

    /**
     * Creates a listener.
     *
     * @param receiver     the receiver (owned by the listener: closed by {@link #close()})
     * @param handler      the handler
     * @param batchSize    maximum messages per receive call, at least 1
     * @param maxWait      how long one receive call waits for messages
     * @param errorBackoff pause after a failed receive call before retrying
     * @param closeTimeout how long {@link #close()} waits for a running loop to finish
     */
    public EmgListener(EmgReceiver receiver, EmgMessageHandler handler, int batchSize, Duration maxWait,
                       Duration errorBackoff, Duration closeTimeout) {
        this.receiver = Objects.requireNonNull(receiver, "receiver");
        this.handler = Objects.requireNonNull(handler, "handler");
        if (batchSize < 1) {
            throw new IllegalArgumentException("batchSize must be at least 1");
        }
        this.batchSize = batchSize;
        this.maxWait = Objects.requireNonNull(maxWait, "maxWait");
        this.errorBackoff = Objects.requireNonNull(errorBackoff, "errorBackoff");
        this.closeTimeout = Objects.requireNonNull(closeTimeout, "closeTimeout");
        this.parser = new EmgMessageParser();
    }

    /**
     * Receives and processes one batch.
     *
     * @return the number of messages received (each one settled or, after close, abandoned)
     */
    public int pollOnce() {
        if (closed.get()) {
            return 0;
        }
        List<EmgDelivery> batch = receiver.receive(batchSize, maxWait);
        for (EmgDelivery delivery : batch) {
            if (closed.get() || Thread.currentThread().isInterrupted()) {
                settle(delivery, Outcome.ABANDON, null);
            } else {
                process(delivery);
            }
        }
        return batch.size();
    }

    /**
     * Runs the receive loop on the calling thread until {@link #close()} is called or the thread is
     * interrupted. Receive failures are logged and retried after the back-off.
     *
     * @throws IllegalStateException if the loop is already running or the listener is closed
     */
    public void run() {
        if (closed.get() || !running.compareAndSet(false, true)) {
            throw new IllegalStateException("EmgListener is already running or closed");
        }
        runner = Thread.currentThread();
        try {
            while (!closed.get() && !Thread.currentThread().isInterrupted()) {
                try {
                    pollOnce();
                } catch (RuntimeException e) {
                    if (closed.get()) {
                        break;
                    }
                    LOG.warn("EMG receive failed ({}); retrying in {}", e.getClass().getName(), errorBackoff);
                    if (closeSignal.await(errorBackoff.toMillis(), TimeUnit.MILLISECONDS)) {
                        break;
                    }
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            loopExited.countDown();
            if (closeRequestedByRunner) {
                receiver.close();
            }
        }
    }

    /**
     * Stops the loop gracefully (waiting up to the close timeout for the in-flight message) and
     * closes the receiver. Idempotent. When called from a handler on the loop thread it returns
     * immediately and the loop closes the receiver as it exits.
     */
    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        closeSignal.countDown();
        if (Thread.currentThread() == runner) {
            // Called from a handler on the loop thread: waiting here would deadlock. The loop
            // finishes the current message, abandons the rest of the batch, then closes the receiver.
            closeRequestedByRunner = true;
            return;
        }
        if (running.get()) {
            try {
                if (!loopExited.await(closeTimeout.toMillis(), TimeUnit.MILLISECONDS)) {
                    LOG.warn("EMG listener loop did not stop within {}; closing the receiver anyway", closeTimeout);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        receiver.close();
    }

    private void process(EmgDelivery delivery) {
        EmgMessage message;
        try {
            message = parser.parse(delivery.body());
        } catch (EmgParseException e) {
            LOG.warn("EMG message {} is not an EMG envelope ({}); dead-lettering", delivery.messageId(), e.getMessage());
            settle(delivery, Outcome.DEAD_LETTER, e.getMessage());
            return;
        }
        try {
            handler.handle(message);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            settle(delivery, Outcome.ABANDON, null);
            return;
        } catch (Exception e) {
            LOG.warn("EMG handler failed for message {} (delivery {}): {}; abandoning for redelivery",
                    delivery.messageId(), delivery.deliveryCount(), e.getClass().getName());
            settle(delivery, Outcome.ABANDON, null);
            return;
        }
        settle(delivery, Outcome.COMPLETE, null);
    }

    private static void settle(EmgDelivery delivery, Outcome outcome, String detail) {
        try {
            switch (outcome) {
                case COMPLETE -> delivery.complete();
                case ABANDON -> delivery.abandon();
                case DEAD_LETTER -> delivery.deadLetter(DEAD_LETTER_REASON, detail);
            }
        } catch (RuntimeException e) {
            // Typically a lost lock: the broker will redeliver, and handlers are idempotent.
            LOG.warn("EMG {} failed for message {} ({}); it will be redelivered",
                    outcome, delivery.messageId(), e.getClass().getName());
        }
    }

    private enum Outcome { COMPLETE, ABANDON, DEAD_LETTER }
}
