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
 * The electrical delivery to the cabinet.
 *
 * @param current AC or DC, or {@code null}
 * @param phase the phase, or {@code null}
 * @param voltage the nominal voltage in volts, or {@code null}
 */
public record PowerFeed(
        PowerCurrent current, PowerPhase phase, Integer voltage) {}
