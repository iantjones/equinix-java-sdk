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

/**
 * The OAuth2 grant types accepted by the Equinix token endpoint
 * ({@code POST /oauth2/v1/token}). The {@code toString()} value is the wire
 * {@code grant_type} sent in the token request body.
 *
 * @author ianjones
 */
public enum GrantType {

    /**
     * The OAuth2 client-credentials grant — the SDK default. Authenticates with a
     * Client ID and Client Secret only; no refresh token is issued.
     */
    CLIENT_CREDENTIALS("client_credentials"),

    /**
     * The OAuth2 resource-owner password grant. Authenticates with a Client ID and
     * Client Secret plus an Equinix portal username and password.
     *
     * <p>Equinix marks this grant deprecated and recommends {@link #CLIENT_CREDENTIALS}.
     * It is supported here for accounts that still require it; see
     * {@link api.equinix.javasdk.core.auth.PasswordEquinixCredentials}.</p>
     */
    PASSWORD("password");

    private final String grantType;

    GrantType(String grantType) {
        this.grantType = grantType;
    }

    @Override
    public String toString() {
        return grantType;
    }
}
