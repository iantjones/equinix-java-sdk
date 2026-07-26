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

package com.eqixiac.equinix.design.optimizer.wizard.model;

import com.eqixiac.equinix.fabric.enums.PeeringType;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.Map;

/**
 * The customer-supplied, per-connection authorization a {@link DeploymentPlan#execute(ExecutionInputs)}
 * run needs to provision cloud provider connections: the cloud-specific authentication key, the DOT1Q
 * VLAN tag, and (for Azure ExpressRoute) the peering type. A brand-new customer, who owns nothing at
 * plan time, gathers these from each cloud console and supplies them here — the wizard never fabricates
 * them, and enumerates exactly what to collect via {@link ConnectionInputRequirement}.
 *
 * <p>Every map is keyed by the planned connection's {@code name} — the same name that appears on the
 * plan, in {@link ConnectionInputRequirement} ({@code getConnectionName()}), and in the plan's Terraform export.
 * A value supplied here fills in the field the plan deliberately left null; a field the plan already
 * carries (a lens-3b plan built with keys) wins and needs no entry here.</p>
 *
 * <pre>{@code
 * ExecutionInputs inputs = ExecutionInputs.builder()
 *     .authenticationKey("FCR-DC-to-aws", "123456789012")
 *     .vlanTag("FCR-DC-to-aws", 1001)
 *     .authenticationKey("FCR-DC-to-azure", "b2c3d4e5-....-guid")
 *     .peeringType("FCR-DC-to-azure", PeeringType.PRIVATE)
 *     .vlanTag("FCR-DC-to-azure", 1002)
 *     .build();
 *
 * DeploymentOutcome outcome = plan.execute(inputs);
 * }</pre>
 *
 * @see DeploymentPlan#execute(ExecutionInputs)
 * @see ConnectionInputRequirement
 */
@Value
@Builder
public class ExecutionInputs {

    /** Cloud authentication keys, keyed by connection name (AWS Account ID, Azure service key, ...). */
    @Singular("authenticationKey")
    Map<String, String> authenticationKeys;

    /** DOT1Q VLAN tags, keyed by connection name. */
    @Singular("vlanTag")
    Map<String, Integer> vlanTags;

    /** Z-side peering types (Azure {@code PRIVATE}/{@code MICROSOFT}), keyed by connection name. */
    @Singular("peeringType")
    Map<String, PeeringType> peeringTypes;

    /**
     * An empty set of inputs — for a backbone-only plan, or a plan whose provider connections already
     * carry their own authorization keys. {@link DeploymentPlan#execute()} uses this by default.
     *
     * @return empty execution inputs
     */
    public static ExecutionInputs none() {
        return ExecutionInputs.builder().build();
    }

    /**
     * The authentication key supplied for the named connection.
     *
     * @param connectionName the planned connection name
     * @return the key, or {@code null} if none was supplied
     */
    public String authenticationKeyFor(String connectionName) {
        return authenticationKeys == null ? null : authenticationKeys.get(connectionName);
    }

    /**
     * The VLAN tag supplied for the named connection.
     *
     * @param connectionName the planned connection name
     * @return the VLAN tag, or {@code null} if none was supplied
     */
    public Integer vlanTagFor(String connectionName) {
        return vlanTags == null ? null : vlanTags.get(connectionName);
    }

    /**
     * The peering type supplied for the named connection.
     *
     * @param connectionName the planned connection name
     * @return the peering type, or {@code null} if none was supplied
     */
    public PeeringType peeringTypeFor(String connectionName) {
        return peeringTypes == null ? null : peeringTypes.get(connectionName);
    }
}
