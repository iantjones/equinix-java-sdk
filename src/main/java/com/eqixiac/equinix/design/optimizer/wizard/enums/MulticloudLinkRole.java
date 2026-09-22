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

package com.eqixiac.equinix.design.optimizer.wizard.enums;

/**
 * The relationship between a planned native multicloud link and the Equinix connections that
 * could carry the same cloud-to-cloud flow.
 *
 * <p><b>Beta</b>: see {@link CloudToCloudStrategy}.</p>
 */
public enum MulticloudLinkRole {

    /**
     * The plan keeps every Equinix connection for the flow. The link is reported for comparison
     * and the plan does not depend on it.
     */
    ALTERNATIVE,

    /**
     * The plan omits at least one Cloud Router to cloud connection because this link carries the
     * flow. The deployment is incomplete until the customer creates the link with the two cloud
     * providers; {@code DeploymentPlan.execute()} does not create it.
     */
    REPLACEMENT,

    /**
     * {@link CloudToCloudStrategy#NATIVE_ONLY} required a native link for the flow and no usable
     * catalog environment exists. The entry records the flow; plan validation reports it as an
     * error.
     */
    UNAVAILABLE
}
