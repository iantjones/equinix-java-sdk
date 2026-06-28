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

import api.equinix.javasdk.core.client.ClientBase;
import api.equinix.javasdk.core.client.Config;
import api.equinix.javasdk.core.enums.RequestType;
import api.equinix.javasdk.core.http.Utils;
import api.equinix.javasdk.core.http.request.EquinixRequest;
import api.equinix.javasdk.core.http.response.EquinixResponse;
import api.equinix.javasdk.core.model.OAuthToken;

/**
 * Performs the OAuth2 refresh-token grant
 * ({@code POST /oauth2/v1/refreshaccesstoken}, operationId {@code RefreshOAuth2AccessToken}).
 *
 * <p>This mirrors the standard token-acquisition path (see the core {@code CoreClientImpl}) but
 * targets the {@code RefreshToken} endpoint declared in {@code apiParams_Core.json} under the
 * Authentication area. It serializes an {@link Oauth2RefreshTokenRequest}
 * ({@code client_id}/{@code client_secret}/{@code refresh_token}) and deserializes the response
 * into an {@link OAuthToken} — the same response shape as the initial token call.</p>
 *
 * <p>Usage is opt-in: construct with the same {@link Config} used by the SDK's core client and call
 * {@link #refresh(String)} with a refresh token from a prior token response. A refresh token is
 * <em>not</em> issued for the {@code client_credentials} grant (the SDK default), so this path only
 * applies to grant types that return one.</p>
 *
 * @author ianjones
 */
public class OAuthRefreshClient extends ClientBase {

    /**
     * Creates a refresh client bound to the supplied core configuration.
     *
     * @param configClient the core {@link Config} (e.g. {@code CoreConfigImpl}) supplying the
     *                     underlying HTTP client and api-params
     */
    public OAuthRefreshClient(Config configClient) {
        super(configClient, "Authentication", "OAuth");
    }

    /**
     * Exchanges a refresh token for a fresh access token via the refresh-token grant. The Client ID
     * and Client Secret are taken from the configured credentials provider.
     *
     * @param refreshToken the refresh token retrieved from a previous successful token response
     * @return the refreshed {@link OAuthToken}
     */
    public OAuthToken refresh(String refreshToken) {
        EquinixCredentials credentials =
                getConfigClient().getEquinixClient().getEquinixCredentialsProvider().getCredentials();
        Oauth2RefreshTokenRequest payload =
                new Oauth2RefreshTokenRequest(credentials.getAccessKey(), credentials.getSecretKey(), refreshToken);

        EquinixRequest<OAuthToken> equinixRequest =
                this.buildRequest("RefreshToken", RequestType.SINGLE, OAuthToken.class);
        Utils.serializeJson(equinixRequest, payload);
        EquinixResponse<OAuthToken> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }
}
