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

import java.util.List;

/**
 * One element of a plan in its uniform, dependency-ordered form: what {@link PlanSteps#of} derives
 * from a {@code DeploymentPlan} for execution, reconcile, drift detection and checkpoints.
 *
 * @param key                 the stable logical key
 * @param kind                the element kind
 * @param name                the planned resource name
 * @param dependsOn           keys of the steps that must exist first
 * @param notExecutableReason why the SDK will not create the element, or {@code null} when it will
 */
public record PlanStep(String key, ResourceKind kind, String name, List<String> dependsOn,
                       String notExecutableReason) {

    /** Makes {@code dependsOn} immutable and non-null. */
    public PlanStep {
        dependsOn = dependsOn == null ? List.of() : List.copyOf(dependsOn);
    }

    /**
     * Whether the SDK creates this element.
     *
     * @return {@code true} when there is no not-executable reason
     */
    public boolean executable() {
        return notExecutableReason == null;
    }
}
