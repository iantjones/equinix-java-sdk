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

package api.equinix.javasdk.sts.model.json;

import api.equinix.javasdk.sts.model.OidcProvider;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Read-only JSON model for the STS {@code OIDCProvider} response. Implements
 * {@link OidcProvider} directly.
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class OidcProviderJson implements OidcProvider {

    @JsonProperty("idpId")
    private String idpId;

    @JsonProperty("name")
    private String name;

    @JsonProperty("issuerUri")
    private String issuerUri;

    @JsonProperty("issuerLocation")
    private String issuerLocation;

    @JsonProperty("trustedClientIds")
    private List<String> trustedClientIds;

    @JsonProperty("groupMembershipClaim")
    private String groupMembershipClaim;

    @JsonProperty("status")
    private String status;

    @JsonProperty("jwks")
    private Object jwks;

    @JsonProperty("jwksRetrievedAt")
    private String jwksRetrievedAt;

    @JsonProperty("rev")
    private String rev;

    @JsonProperty("createdBy")
    private String createdBy;

    @JsonProperty("createdAt")
    private String createdAt;

    @JsonProperty("updatedBy")
    private String updatedBy;

    @JsonProperty("updatedAt")
    private String updatedAt;
}
