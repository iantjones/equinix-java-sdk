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

package com.eqixiac.equinix.sts.model.json;

import com.eqixiac.equinix.sts.model.OpenIdConfiguration;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

/**
 * Read-only JSON model for the STS {@code OpenIdConfiguration} response. Implements
 * {@link OpenIdConfiguration} directly.
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenIdConfigurationJson implements OpenIdConfiguration {

    @JsonProperty("issuer")
    private String issuer;

    @JsonProperty("jwksUri")
    @JsonAlias("jwks_uri")
    private String jwksUri;

    @JsonProperty("tokenEndpoint")
    @JsonAlias("token_endpoint")
    private String tokenEndpoint;

    @JsonProperty("claimsSupported")
    @JsonAlias("claims_supported")
    private List<String> claimsSupported;

    @JsonProperty("responseTypesSupported")
    @JsonAlias("response_types_supported")
    private List<String> responseTypesSupported;

    @JsonProperty("subjectTypesSupported")
    @JsonAlias("subject_types_supported")
    private List<String> subjectTypesSupported;

    @JsonProperty("idTokenSigningAlgValuesSupported")
    @JsonAlias("id_token_signing_alg_values_supported")
    private List<String> idTokenSigningAlgValuesSupported;
}
