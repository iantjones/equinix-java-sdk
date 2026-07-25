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

package api.equinix.javasdk.design.optimizer.wizard.model;

import api.equinix.javasdk.fabric.model.implementation.cloud.CloudProviderType;
import lombok.Builder;
import lombok.Value;

/**
 * The per-connection authorization a customer must gather before a provider connection can be
 * provisioned. A brand-new customer targeting a metro where they own nothing has no resources to
 * validate a connection against at plan time; this enumerates exactly what the connection's live
 * endpoint dry-run will need once the target Cloud Router exists — the cloud-specific authorization
 * key, the VLAN tag, and (for Azure) the peering type.
 *
 * <p>These are deliberately kept OUT of the plan's "validated now" section: they are inputs the
 * customer supplies, not facts the SDK can confirm from the public catalog. The Deployment Wizard
 * surfaces them separately so a structurally-fine plan is never reported as a validation error just
 * because a customer authorization key is not yet known.</p>
 */
@Value
@Builder
public class ConnectionInputRequirement {

    /** The planned connection this requirement belongs to. */
    String connectionName;

    /** The provider label as it appears in the catalog (for example {@code "AWS"}). */
    String providerLabel;

    /** The resolved cloud provider type, or {@link CloudProviderType#OTHER} for a third-party profile. */
    CloudProviderType cloudType;

    /**
     * A human-readable label for the cloud-specific authorization key the customer must supply
     * (for example {@code "AWS Account ID (12-digit)"} or {@code "Azure ExpressRoute service key (GUID)"}).
     */
    String authenticationKeyLabel;

    /** Whether the connection requires a cloud authorization key at all. */
    boolean authenticationKeyRequired;

    /** Whether the authorization key has already been supplied on the plan (a lens-3b pre-flight). */
    boolean authenticationKeyProvided;

    /** Whether a VLAN tag (DOT1Q encapsulation) must be supplied — always true for a cloud VC. */
    boolean vlanTagRequired;

    /** Whether an Azure peering type ({@code PRIVATE}/{@code MICROSOFT}) must be supplied. */
    boolean peeringTypeRequired;

    /**
     * A one-line, human-readable description of what the customer must gather for this connection.
     *
     * @return a description such as {@code "FCR-DC-to-aws (AWS): AWS Account ID (12-digit), VLAN tag"}
     */
    public String describe() {
        StringBuilder sb = new StringBuilder();
        sb.append(connectionName);
        if (providerLabel != null) {
            sb.append(" (").append(providerLabel).append(")");
        }
        sb.append(": ");
        StringBuilder needs = new StringBuilder();
        if (authenticationKeyRequired) {
            needs.append(authenticationKeyLabel);
            if (authenticationKeyProvided) {
                needs.append(" (provided)");
            }
        }
        if (vlanTagRequired) {
            if (needs.length() > 0) needs.append(", ");
            needs.append("VLAN tag");
        }
        if (peeringTypeRequired) {
            if (needs.length() > 0) needs.append(", ");
            needs.append("Azure peering type");
        }
        sb.append(needs.length() > 0 ? needs.toString() : "no customer authorization required");
        return sb.toString();
    }
}
