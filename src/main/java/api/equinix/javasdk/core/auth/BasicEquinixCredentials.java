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

/**
 * Standard implementation of {@link EquinixCredentials} using OAuth2 client credentials.
 *
 * <p>This is the primary way to authenticate with Equinix Platform APIs. Provide your
 * Client ID and Client Secret obtained from the
 * <a href="https://developer.equinix.com/">Equinix Developer Portal</a>.</p>
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * BasicEquinixCredentials credentials = new BasicEquinixCredentials("clientId", "clientSecret");
 * Fabric fabric = new Fabric(credentials);
 * }</pre>
 *
 * @author ianjones
 * @version $Id: $Id
 */
public class BasicEquinixCredentials implements EquinixCredentials {

    @JsonProperty("client_id")
    private final String accessKey;

    @JsonProperty("client_secret")
    private final String secretKey;

    @JsonProperty("grant_type")
    private final String grantType;

    /**
     * Creates credentials using the given OAuth2 Client ID and Client Secret.
     *
     * @param accessKey the OAuth2 Client ID from the Equinix Developer Portal
     * @param secretKey the OAuth2 Client Secret from the Equinix Developer Portal
     */
    public BasicEquinixCredentials(String accessKey, String secretKey) {
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.grantType = GrantType.CLIENT_CREDENTIALS.toString();
    }

    /** {@inheritDoc} */
    @Override
    public String getAccessKey() {
        return accessKey;
    }

    /** {@inheritDoc} */
    @Override
    public String getSecretKey() {
        return secretKey;
    }

    /**
     * Returns the OAuth2 grant type. Always returns {@code "client_credentials"}.
     *
     * @return the grant type string
     */
    public String getGrantType() {
        return grantType;
    }
}
