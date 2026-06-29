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
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * {@link EquinixCredentials} for the OAuth2 resource-owner password grant. Carries the Client ID
 * and Client Secret plus an Equinix portal username and password; the token request is sent with
 * {@code grant_type=password} (see the {@code Oauth2TokenRequest} schema of
 * {@code POST /oauth2/v1/token}).
 *
 * <p><strong>Deprecated by Equinix.</strong> Equinix marks the password grant deprecated and
 * recommends the client-credentials grant ({@link BasicEquinixCredentials}). Use this only for
 * accounts that still require username/password authentication.</p>
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * PasswordEquinixCredentials credentials =
 *         new PasswordEquinixCredentials("clientId", "clientSecret", "user@example.com", "password");
 * Fabric fabric = new Fabric(credentials);
 * }</pre>
 *
 * <p>For enhanced security the password may be hashed before transmission; pass the encoding
 * scheme (currently only {@code md5-b64} is supported by Equinix) to the four-argument-plus-encoding
 * constructor, having already encoded the password value yourself.</p>
 *
 * @author ianjones
 * @see BasicEquinixCredentials
 * @see GrantType#PASSWORD
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PasswordEquinixCredentials implements EquinixCredentials {

    @JsonProperty("client_id")
    private final String accessKey;

    @JsonProperty("client_secret")
    private final String secretKey;

    @JsonProperty("user_name")
    private final String userName;

    @JsonProperty("user_password")
    private final String userPassword;

    @JsonProperty("grant_type")
    private final String grantType;

    @JsonProperty("password_encoding")
    private final String passwordEncoding;

    /**
     * Creates password-grant credentials with a raw (unencoded) password.
     *
     * @param accessKey the OAuth2 Client ID from the Equinix Developer Portal
     * @param secretKey the OAuth2 Client Secret from the Equinix Developer Portal
     * @param userName the Equinix portal username
     * @param userPassword the Equinix portal password
     */
    public PasswordEquinixCredentials(String accessKey, String secretKey, String userName, String userPassword) {
        this(accessKey, secretKey, userName, userPassword, null);
    }

    /**
     * Creates password-grant credentials, declaring how {@code userPassword} is encoded.
     *
     * @param accessKey the OAuth2 Client ID from the Equinix Developer Portal
     * @param secretKey the OAuth2 Client Secret from the Equinix Developer Portal
     * @param userName the Equinix portal username
     * @param userPassword the Equinix portal password, already encoded per {@code passwordEncoding}
     * @param passwordEncoding the password encoding (e.g. {@code md5-b64}); {@code null} sends the
     *                         password as a raw string
     */
    public PasswordEquinixCredentials(String accessKey, String secretKey, String userName,
                                      String userPassword, String passwordEncoding) {
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.userName = userName;
        this.userPassword = userPassword;
        this.grantType = GrantType.PASSWORD.toString();
        this.passwordEncoding = passwordEncoding;
    }
}
