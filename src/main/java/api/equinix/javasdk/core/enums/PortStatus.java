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

package api.equinix.javasdk.core.enums;

import api.equinix.javasdk.core.model.APIParam;

/**
 * <p>PortStatus class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
public enum PortStatus implements APIParam {
    PENDING_APPROVAL, PROVISIONING, PROVISIONED, NOT_PROVISIONED, REJECTED,
    PENDING_DELETE, DEPROVISIONING, DEPROVISIONED, NOT_DEPROVISIONED, PENDING_BGP_PEERING,
    DELETED, MIGRATION_STARTED, MIGRATION_DEPROVISIONED, MIGRATION_DEPROVISION_FAILED,
    MIGRATION_VLAN_RELEASED, MIGRATION_VLAN_RELEASE_FAILED, MIGRATION_VLAN_GENERATED,
    MIGRATION_VLAN_GENERATION_FAILED, MIGRATION_PROVISION_FAILED, FAILED, ADDED, AVAILABLE
}
