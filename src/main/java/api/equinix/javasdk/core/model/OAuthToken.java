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

package api.equinix.javasdk.core.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * <p>OAuthToken class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
@Getter
@Setter
public class OAuthToken {

    /**
     * <p>singleTypeRef.</p>
     *
     * @return a {@link com.fasterxml.jackson.core.type.TypeReference} object.
     */
    public static TypeReference<OAuthToken> singleTypeRef() {
        return new TypeReference<>() {};
    }

    @JsonProperty("access_token")
    private String sessionToken;

    @JsonProperty("token_timeout")
    private Integer tokenTimeout;

    @JsonProperty("user_name")
    private String userName;

    @JsonProperty("token_type")
    private String tokenType;

    @JsonProperty("refresh_token")
    private String refreshToken;

    @JsonProperty("refresh_token_timeout")
    private String refreshTokenTimeout;

    @JsonIgnore
    private LocalDateTime sessionStart = LocalDateTime.now();

    /**
     * <p>validSession.</p>
     *
     * @return a boolean.
     */
    public boolean validSession() {
        return getSessionToken() != null && (getSessionStart().plusSeconds(getTokenTimeout()).isAfter(LocalDateTime.now()));
    }
}
