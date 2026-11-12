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

package com.eqixiac.equinix.fabric.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Values of the Fabric v4 {@code AccessPointType} schema (spec 4.34.0, catalog fetched
 * 2026-11-11), plus {@link #CHAINGROUP}, which {@code AccessPointType} does not list but
 * {@code VirtualConnectionPriceAccessPointType} (price search) does. The non-spec {@code XF_IC}
 * and {@code CX_PORT} constants were removed in this major version; a response carrying either
 * literal reads as {@link #UNKNOWN}.
 *
 * <p>{@link #METAL_NETWORK} remains because 4.34.0 still lists it. Equinix Metal reached end of
 * life on 2026-06-30, so it is a read-side value only: the SDK has no builder that sends it.</p>
 *
 * <p>{@link #UNKNOWN} is a read-side fallback for values added after this SDK release. Never send
 * it.</p>
 *
 * @author ianjones
 */
public enum AccessPointType {
    VD,
    VG,
    SP,
    IGW,
    COLO,
    SUBNET,
    CLOUD_ROUTER,
    CHAINGROUP,
    NETWORK,
    METAL_NETWORK,
    VPIC_INTERFACE,
    APP_LINK,
    /**
     * Interconnect access point ({@code IC}), the last value of the Fabric v4 4.34.0
     * {@code AccessPointType} enum. Used by Fabric Interconnect (the managed routed network
     * behind Fabric One), together with {@link CloudRouterType#IC_ROUTER}, the {@code IC_PROFILE}
     * service-profile type, the {@code IC_ENV} provider-environment type and the
     * {@code /aSide/accessPoint/interconnect/uuid} search property.
     *
     * <p><b>Beta</b>: the specification publishes no connection-create example with an
     * {@code IC} access point. Probe with {@code ConnectionBuilder.dryRun()} before a live create.</p>
     */
    IC,
    UNKNOWN;

    @JsonCreator
    public static AccessPointType fromString(String value) {
        try { return AccessPointType.valueOf(value); }
        catch (Exception e) { return UNKNOWN; }
    }
}