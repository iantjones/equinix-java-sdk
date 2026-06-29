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

package api.equinix.javasdk.sts.model.json.creators;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * Request body for modifying an existing OIDC provider via {@code PATCH
 * /v1/projects/{projectId}/oidcProviders/{idpId}} (operationId {@code patchOidcProvider}, spec
 * schema {@code PatchOidcProviderBody}).
 *
 * <p>Only the supplied properties are modified; the rest keep their current values. The
 * {@code lastRev} is required (concurrency control). Optional properties such as
 * {@code groupMembershipClaim} may be unset by assigning the value {@code {"$unset": true}}
 * (see {@link #unsetGroupMembershipClaim()}).</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PatchOidcProviderRequest {

    @JsonProperty("name")
    private String name;

    @JsonProperty("trustedClientIds")
    private List<String> trustedClientIds;

    @JsonProperty("groupMembershipClaim")
    private Object groupMembershipClaim;

    @JsonProperty("lastRev")
    private String lastRev;

    /**
     * Sets the human-friendly name for the identity provider.
     *
     * @param name the provider name
     * @return this request for chaining
     */
    public PatchOidcProviderRequest name(String name) {
        this.name = name;
        return this;
    }

    /**
     * Sets the OAuth 2.0 client ids permitted to exchange ID tokens for access tokens.
     *
     * @param trustedClientIds the trusted client ids
     * @return this request for chaining
     */
    public PatchOidcProviderRequest trustedClientIds(List<String> trustedClientIds) {
        this.trustedClientIds = trustedClientIds;
        return this;
    }

    /**
     * Sets the group-membership claim name.
     *
     * @param groupMembershipClaim the group membership claim name
     * @return this request for chaining
     */
    public PatchOidcProviderRequest groupMembershipClaim(String groupMembershipClaim) {
        this.groupMembershipClaim = groupMembershipClaim;
        return this;
    }

    /**
     * Unsets the group-membership claim (sends {@code {"$unset": true}}).
     *
     * @return this request for chaining
     */
    public PatchOidcProviderRequest unsetGroupMembershipClaim() {
        this.groupMembershipClaim = Map.of("$unset", true);
        return this;
    }

    /**
     * Sets the last-known revision of the provider (required, for concurrency control).
     *
     * @param lastRev the last-known revision
     * @return this request for chaining
     */
    public PatchOidcProviderRequest lastRev(String lastRev) {
        this.lastRev = lastRev;
        return this;
    }

    public String getName() {
        return name;
    }

    public List<String> getTrustedClientIds() {
        return trustedClientIds;
    }

    public Object getGroupMembershipClaim() {
        return groupMembershipClaim;
    }

    public String getLastRev() {
        return lastRev;
    }
}
