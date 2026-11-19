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

/**
 * Request and answer types of the OpenCapacity API, a proposed open specification for data center
 * capacity queries, demand signals, and subscriptions.
 *
 * <p><strong>Proposal.</strong> No provider implements this specification. It is not an Equinix
 * API, and it can change before version 1.0 is final.
 *
 * <p>The types are immutable. A component that the specification marks optional is {@code null}
 * when it is absent; in an answer that means the provider did not disclose it. Lists are never
 * {@code null}. An enumeration value this SDK does not list reads as {@code UNKNOWN}, and an
 * unknown {@link com.eqixiac.opencapacity.model.Grade} reads as {@code NOT_DISCLOSED}.
 */
package com.eqixiac.opencapacity.model;
