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

package api.equinix.javasdk.core.auth;

import api.equinix.javasdk.core.enums.GrantType;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * The wire body for the OAuth2 token request ({@code POST /oauth2/v1/token}, spec schema
 * {@code Oauth2TokenRequest}): {@code client_id}, {@code client_secret} and {@code grant_type}.
 *
 * <p>This DTO — not the {@link EquinixCredentials} instance itself — is what the SDK serializes
 * when authenticating. That decouples the wire contract from whatever class a custom
 * {@link EquinixCredentialsProvider} returns: implementations of {@code EquinixCredentials} only
 * need to supply {@code getAccessKey()}/{@code getSecretKey()}; they carry no Jackson annotations
 * and are never serialized directly.</p>
 *
 * @author ianjones
 */
@Getter
public class Oauth2TokenRequest {

    @JsonProperty("client_id")
    private final String clientId;

    @JsonProperty("client_secret")
    private final String clientSecret;

    @JsonProperty("grant_type")
    private final String grantType;

    /**
     * Builds the client-credentials token request body from the supplied credentials.
     *
     * @param credentials the credentials sourced from the configured provider
     */
    public Oauth2TokenRequest(EquinixCredentials credentials) {
        this.clientId = credentials.getAccessKey();
        this.clientSecret = credentials.getSecretKey();
        this.grantType = GrantType.CLIENT_CREDENTIALS.toString();
    }
}
