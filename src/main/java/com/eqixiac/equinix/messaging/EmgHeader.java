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

package com.eqixiac.equinix.messaging;

import java.time.Instant;

/**
 * The header attributes common to every EMG notification: the {@code Task} attributes other than
 * {@code Body}, plus the envelope's {@code Signature}.
 *
 * <p>Attribute names are matched case-insensitively ({@code Id} and the documented {@code ID} are
 * the same), and {@code CreateTimeUTC} (the name in the header attribute list of the Notification
 * Types guide) is accepted as an alias of {@code CreationTimeUTC} (the name in the AsyncAPI spec and
 * every documented sample).
 *
 * @param id           {@code Task.Id}: unique per message; the natural idempotency key
 * @param source       {@code Task.Source}: identifies the sender; issued when EMG is enabled
 * @param verb         {@code Task.Verb}
 * @param resource     {@code Task.Resource}: the Equinix product, e.g. {@code DataCenter.Maintenance}
 * @param contentType  {@code Task.ContentType}, normally {@code application/json}
 * @param creationTime {@code Task.CreationTimeUTC}, or {@code null} if absent or unparseable
 * @param version      {@code Task.Version}: {@code "1.0"} or {@code "2.0"} in the spec
 * @param signature    the envelope {@code Signature} (Base64, trimmed). Equinix does not document a
 *                     verification algorithm, so the SDK exposes it but does not verify it.
 */
public record EmgHeader(String id, String source, EmgVerb verb, String resource, String contentType,
                        Instant creationTime, String version, String signature) {
}
