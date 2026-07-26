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

/**
 * Asynchronous execution support for the Equinix Java SDK, built on Java 21 virtual threads.
 *
 * <p>The central type, {@link com.eqixiac.equinix.core.async.EquinixAsync}, is a small generic
 * facade that runs any blocking SDK call on a virtual thread and returns a
 * {@link java.util.concurrent.CompletableFuture}. This deliberately avoids generating a per-method
 * {@code ...Async} mirror of every domain (Fabric, NetworkEdge, CustomerPortal, ...); one facade
 * covers the whole API surface. {@link com.eqixiac.equinix.core.async.AsyncException} reports
 * interruption while awaiting completion.</p>
 *
 * @see com.eqixiac.equinix.core.async.EquinixAsync
 */
package com.eqixiac.equinix.core.async;
