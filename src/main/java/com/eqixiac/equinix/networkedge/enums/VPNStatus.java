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

package com.eqixiac.equinix.networkedge.enums;

import com.eqixiac.equinix.core.model.APIParam;

/**
 * Status of a Network Edge VPN. The union of the spec's {@code VpnResponse.status} values and the
 * {@code GET /ne/v1/vpn} {@code status[]} filter values (which additionally declare the retrying /
 * updating / phase-failed states).
 *
 * @author ianjones
 */
public enum VPNStatus implements APIParam {
    PROVISIONED,
    PROVISIONING,
    PROVISIONING_RETRYING,
    UPDATING,
    PROVISIONING_UPDATE_RETRYING,
    FAILED,
    PROVISIONING_FAILED,
    PROVISIONING_UPDATE_FAILED,
    DEPROVISIONED,
    DEPROVISIONING,
    DEPROVISIONING_RETRYING,
    DEPROVISIONING_FAILED
}
