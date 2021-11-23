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
 * <p>BasicEquinixCredentials class.</p>
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
     * <p>Constructor for BasicEquinixCredentials.</p>
     *
     * @param accessKey a {@link java.lang.String} object.
     * @param secretKey a {@link java.lang.String} object.
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
     * <p>Getter for the field <code>grantType</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getGrantType() {
        return grantType;
    }
}
