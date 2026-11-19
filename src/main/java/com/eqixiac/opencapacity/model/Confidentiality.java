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
 * What a provider can do with a signal. An absent value takes the most restrictive default.
 *
 * @param sharing whether the signal can be counted in anonymous totals, or {@code null} for no
 * @param contactPermitted whether the provider can contact the customer, or {@code null} for no
 */
public record Confidentiality(
        Sharing sharing, Boolean contactPermitted) {}
