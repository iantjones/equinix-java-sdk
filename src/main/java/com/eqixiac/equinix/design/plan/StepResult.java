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
 * The result of one plan step in an execution or reconcile run.
 *
 * @param key    the step's logical key
 * @param kind   what kind of element it is
 * @param name   the planned resource name
 * @param action what the run did
 * @param uuid   the live uuid, when one is known
 * @param detail a human-readable reason or note; may be {@code null}
 */
public record StepResult(String key, ResourceKind kind, String name, StepAction action, String uuid, String detail) {
}
