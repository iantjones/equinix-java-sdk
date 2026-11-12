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
package com.eqixiac.equinix.design.resilience;

/** The kinds of element an availability is assumed for (see {@link AvailabilityAssumptions}). */
public enum ElementType {
    /** An Equinix metro as a whole (the facilities the design uses there). */
    METRO,
    /** A Fabric Cloud Router. */
    CLOUD_ROUTER,
    /** One Fabric virtual connection to a cloud on-ramp. */
    CLOUD_CONNECTION,
    /** A provider's on-ramp (its ports and service profile) at one metro. */
    CLOUD_ONRAMP,
    /** Fabric connectivity between two metros. */
    BACKBONE_LINK,
    /** A site's access circuit into a metro. */
    SITE_ACCESS,
    /** A cloud provider's region. */
    CLOUD_REGION
}
