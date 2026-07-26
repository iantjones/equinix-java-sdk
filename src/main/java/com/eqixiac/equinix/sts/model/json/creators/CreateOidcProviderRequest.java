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

package com.eqixiac.equinix.sts.model.json.creators;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import lombok.Getter;

/**
 * Request body for registering a new OIDC provider via {@code POST
 * /v1/projects/{projectId}/oidcProviders} (operationId {@code createOidcProvider}, spec schema
 * {@code CreateOidcProviderBody}).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
public class CreateOidcProviderRequest {

    @JsonProperty("name")
    private String name;

    @JsonProperty("idpPrefix")
    private String idpPrefix;

    @JsonProperty("issuerLocation")
    private String issuerLocation;

    @JsonProperty("trustedClientIds")
    private List<String> trustedClientIds;

    @JsonProperty("groupMembershipClaim")
    private String groupMembershipClaim;

    /**
     * Sets the human-friendly name for the identity provider (required).
     *
     * @param name the provider name
     * @return this request for chaining
     */
    public CreateOidcProviderRequest name(String name) {
        this.name = name;
        return this;
    }

    /**
     * Sets the prefix used to derive a unique {@code idpId} within the root project (required).
     *
     * @param idpPrefix the idp prefix
     * @return this request for chaining
     */
    public CreateOidcProviderRequest idpPrefix(String idpPrefix) {
        this.idpPrefix = idpPrefix;
        return this;
    }

    /**
     * Sets the OIDC issuer location URL (required).
     *
     * @param issuerLocation the issuer location URL
     * @return this request for chaining
     */
    public CreateOidcProviderRequest issuerLocation(String issuerLocation) {
        this.issuerLocation = issuerLocation;
        return this;
    }

    /**
     * Sets the OAuth 2.0 client ids permitted to exchange ID tokens for access tokens (required).
     *
     * @param trustedClientIds the trusted client ids
     * @return this request for chaining
     */
    public CreateOidcProviderRequest trustedClientIds(List<String> trustedClientIds) {
        this.trustedClientIds = trustedClientIds;
        return this;
    }

    /**
     * Sets the name of the group-membership claim to interpret for authorization.
     *
     * @param groupMembershipClaim the group membership claim name
     * @return this request for chaining
     */
    public CreateOidcProviderRequest groupMembershipClaim(String groupMembershipClaim) {
        this.groupMembershipClaim = groupMembershipClaim;
        return this;
    }
}
