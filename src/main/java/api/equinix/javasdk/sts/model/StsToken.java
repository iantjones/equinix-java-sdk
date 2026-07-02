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

package api.equinix.javasdk.sts.model;

import api.equinix.javasdk.sts.enums.TokenType;

/**
 * A security token issued by the Equinix Security Token Service, as returned by the STS token
 * exchange operation.
 *
 * <p>This is a read-only response view (spec schema {@code TokenResponse}).</p>
 */
public interface StsToken {

    /**
     * @return the issued access token
     */
    String getAccessToken();

    /**
     * @return the type of the issued token
     */
    String getIssuedTokenType();

    /**
     * @return the token type (e.g. {@code Bearer})
     */
    TokenType getTokenType();

    /**
     * @return the lifetime of the token in seconds
     */
    Integer getExpiresIn();
}
