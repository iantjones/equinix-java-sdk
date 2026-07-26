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

package com.eqixiac.equinix.core.client;

import com.eqixiac.equinix.core.auth.Oauth2TokenRequest;
import com.eqixiac.equinix.core.client.interfaces.CoreClient;
import com.eqixiac.equinix.core.http.ResponseHandler;
import com.eqixiac.equinix.core.http.SerializationHelper;
import com.eqixiac.equinix.core.http.request.EquinixRequest;
import com.eqixiac.equinix.core.http.response.EquinixResponse;
import com.eqixiac.equinix.core.model.OAuthToken;
import com.eqixiac.equinix.core.enums.RequestType;

/**
 *
 * @author ianjones
 */
public class CoreClientImpl extends ClientBase implements CoreClient {

    public CoreClientImpl(CoreConfigImpl configClient) {
        super(configClient, "Authentication", "OAuth");
    }

    public OAuthToken authenticate() {
        EquinixRequest<OAuthToken> equinixRequest = this.buildRequest("Authenticate", RequestType.SINGLE, OAuthToken.class);
        // Serialize a dedicated wire DTO, never the EquinixCredentials instance itself, so
        // custom EquinixCredentials implementations need no Jackson annotations to authenticate.
        SerializationHelper.serializeJson(equinixRequest, new Oauth2TokenRequest(
                getConfigClient().getEquinixClient().getEquinixCredentialsProvider().getCredentials()));
        EquinixResponse<OAuthToken> equinixResponse = this.invoke(equinixRequest);
        return ResponseHandler.handleSingletonResponse(equinixResponse, equinixRequest);
    }
}
