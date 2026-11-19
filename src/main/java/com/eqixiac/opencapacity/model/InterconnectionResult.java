/*
 * Copyright 2026 Ian Jones. All Rights Reserved.
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

package com.eqixiac.opencapacity.model;

/**
 * The provider's answer for one interconnection party.
 *
 * @param party the party
 * @param within the scope the answer applies to, or {@code null}
 * @param presence the grade
 * @param required the flag from the request, or {@code null}
 * @param roundTripMs the latency band in milliseconds, or {@code null} when not disclosed
 */
public record InterconnectionResult(
        Party party, InterconnectionScope within, Grade presence, Boolean required, LatencyBand roundTripMs) {}
