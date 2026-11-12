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
 * A live resource that carries the plan's marker (its deployment name in {@code additionalInfo}, or the
 * plan's name prefix) but matches no planned resource: left over from an earlier version of the plan,
 * or created by hand. Reconcile never deletes it.
 *
 * @param kind   {@code CLOUD_ROUTER} or a connection kind
 * @param name   the live name
 * @param uuid   the live uuid
 * @param reason why it is attributed to the plan
 */
public record UnmanagedResource(ResourceKind kind, String name, String uuid, String reason) {
}
