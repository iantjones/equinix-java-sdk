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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * Request body for the OAuth2 refresh-token grant
 * ({@code POST /oauth2/v1/refreshaccesstoken}, operationId {@code RefreshOAuth2AccessToken},
 * spec schema {@code Oauth2RefreshTokenRequest}).
 *
 * <p>Carries the Client ID, Client Secret and a previously issued {@code refresh_token}; the
 * authorization endpoint returns a fresh access token without re-supplying user credentials.</p>
 *
 * <p>Note: a {@code refresh_token} is <em>not</em> issued for the {@code client_credentials} grant
 * type (the SDK's default), so this flow only applies when a refresh token was obtained from a
 * prior token response.</p>
 *
 * @author ianjones
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Oauth2RefreshTokenRequest {

    @JsonProperty("client_id")
    private final String clientId;

    @JsonProperty("client_secret")
    private final String clientSecret;

    @JsonProperty("refresh_token")
    private final String refreshToken;

    /**
     * Creates a refresh-token request.
     *
     * @param clientId the OAuth2 Client ID from the Equinix Developer Portal
     * @param clientSecret the OAuth2 Client Secret from the Equinix Developer Portal
     * @param refreshToken the refresh token retrieved from a previous successful token response
     */
    public Oauth2RefreshTokenRequest(String clientId, String clientSecret, String refreshToken) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.refreshToken = refreshToken;
    }
}
