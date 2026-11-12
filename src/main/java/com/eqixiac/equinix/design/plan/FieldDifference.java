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
 * One field that differs between the plan and the live resource.
 *
 * @param field     the field, named as on the plan (for example {@code bandwidthMbps})
 * @param planned   the planned value, as text
 * @param live      the live value, as text
 * @param patchable whether reconcile can update it in place ({@code PATCH}); otherwise it is reported only
 */
public record FieldDifference(String field, String planned, String live, boolean patchable) {
}
