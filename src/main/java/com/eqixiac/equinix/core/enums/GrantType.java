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

package com.eqixiac.equinix.core.enums;

/**
 * The OAuth2 grant types accepted by the Equinix token endpoint
 * ({@code POST /oauth2/v1/token}). The {@code toString()} value is the wire
 * {@code grant_type} sent in the token request body.
 *
 * @author ianjones
 */
public enum GrantType {

    /**
     * The OAuth2 client-credentials grant — the SDK default and the only grant the Equinix
     * token endpoint still supports (the resource-owner password grant reached end-of-life
     * in January 2025 and was removed from this SDK).
     */
    CLIENT_CREDENTIALS("client_credentials");

    private final String grantType;

    GrantType(String grantType) {
        this.grantType = grantType;
    }

    @Override
    public String toString() {
        return grantType;
    }
}
