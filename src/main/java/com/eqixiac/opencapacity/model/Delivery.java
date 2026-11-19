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
 * How the events of a subscription are delivered.
 *
 * @param type webhook or pull
 * @param url the HTTPS URL of a webhook, or {@code null} for pull
 * @param signingSecret the secret that signs webhook deliveries. The provider returns it once, when
 *     the subscription is created, and {@code null} afterwards
 */
public record Delivery(
        DeliveryType type, String url, String signingSecret) {}
