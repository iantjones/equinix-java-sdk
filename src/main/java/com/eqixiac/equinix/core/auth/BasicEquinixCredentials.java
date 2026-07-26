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

package com.eqixiac.equinix.core.auth;

import lombok.Getter;

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
 */
@Getter
public class BasicEquinixCredentials implements EquinixCredentials {

    private final String accessKey;

    private final String secretKey;

    /**
     * Creates credentials using the given OAuth2 Client ID and Client Secret.
     *
     * @param accessKey the OAuth2 Client ID from the Equinix Developer Portal
     * @param secretKey the OAuth2 Client Secret from the Equinix Developer Portal
     * @throws IllegalArgumentException if either value is null or blank — failing here with a
     *         clear message rather than as a confusing 400/401 on the first API call
     */
    public BasicEquinixCredentials(String accessKey, String secretKey) {
        if (accessKey == null || accessKey.isBlank()) {
            throw new IllegalArgumentException("accessKey (OAuth2 Client ID) must not be null or blank.");
        }
        if (secretKey == null || secretKey.isBlank()) {
            throw new IllegalArgumentException("secretKey (OAuth2 Client Secret) must not be null or blank.");
        }
        this.accessKey = accessKey;
        this.secretKey = secretKey;
    }
}
