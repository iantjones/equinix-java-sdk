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

package com.eqixiac.equinix.design.optimizer.enums;

/**
 * The availability of a native provider-to-provider multicloud environment, as recorded in a
 * {@code MulticloudEnvironmentCatalog} entry on that entry's as-of date.
 *
 * <p><b>Beta</b>: the status is a dated observation of provider documentation, not a live
 * lookup.</p>
 *
 * @see com.eqixiac.equinix.design.optimizer.model.MulticloudEnvironment
 */
public enum MulticloudEnvironmentStatus {

    /**
     * The entry's source documents the region pair under a provider offering that the providers
     * have announced as generally available. Only this status permits the Deployment Wizard to
     * omit Equinix connections in favour of the native link.
     */
    GA,

    /**
     * The entry's source labels the offering as a preview. AWS states that preview interconnects
     * are limited to 1 Gbps and one per customer per Region, and are removed before general
     * availability (source:
     * {@code https://docs.aws.amazon.com/interconnect/latest/userguide/getting-started-multicloud.html},
     * retrieved 2026-09-21).
     */
    PREVIEW,

    /**
     * The region pair is reported but not confirmed from provider documentation. Also the value
     * assigned when a catalog file carries no status or an unrecognized one.
     */
    UNVERIFIED
}
