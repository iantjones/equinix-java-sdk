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


package com.eqixiac.equinix.design.plan;

/**
 * A resource a rollback deleted and confirmed gone ({@code DEPROVISIONED} or HTTP 404).
 *
 * @param key  the plan's logical key
 * @param kind what kind of resource it was
 * @param name the resource name
 * @param uuid the Fabric uuid
 */
public record RemovedResource(String key, ResourceKind kind, String name, String uuid) {
}
