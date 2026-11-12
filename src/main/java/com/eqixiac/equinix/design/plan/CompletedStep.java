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
 * A step recorded in a {@link Checkpoint}: the resource exists (or its create was accepted) under
 * this uuid.
 *
 * @param key        the step's logical key
 * @param kind       the element kind
 * @param name       the resource name
 * @param uuid       the Fabric uuid
 * @param parentUuid for a routing protocol, the uuid of its connection; otherwise {@code null}
 * @param state      the last state observed (for example {@code PROVISIONED}), or {@code CREATE_ACCEPTED}
 *                   when the checkpoint was written before the state wait
 * @param createdByRun {@code true} when an SDK run created it, {@code false} when it was adopted
 */
public record CompletedStep(String key, ResourceKind kind, String name, String uuid, String parentUuid,
                            String state, boolean createdByRun) {
}
