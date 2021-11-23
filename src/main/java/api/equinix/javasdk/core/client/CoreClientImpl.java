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

package api.equinix.javasdk.core.client;

import api.equinix.javasdk.core.client.interfaces.CoreClient;
import api.equinix.javasdk.core.http.Utils;
import api.equinix.javasdk.core.http.request.EquinixRequest;
import api.equinix.javasdk.core.http.response.EquinixResponse;
import api.equinix.javasdk.core.model.OAuthToken;
import api.equinix.javasdk.core.enums.RequestType;

/**
 * <p>CoreClientImpl class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
public class CoreClientImpl extends ClientBase implements CoreClient {

    /**
     * <p>Constructor for CoreClientImpl.</p>
     *
     * @param configClient a {@link api.equinix.javasdk.core.client.CoreConfigImpl} object.
     */
    public CoreClientImpl(CoreConfigImpl configClient) {
        super(configClient, "Authentication", "OAuth");
    }

    /**
     * <p>authenticate.</p>
     *
     * @return a {@link api.equinix.javasdk.core.model.OAuthToken} object.
     */
    public OAuthToken authenticate() {
        EquinixRequest<OAuthToken> equinixRequest = this.buildRequest("Authenticate", RequestType.SINGLE, OAuthToken.singleTypeRef());
        Utils.serializeJson(equinixRequest, getConfigClient().getEquinixClient().getEquinixCredentialsProvider().getCredentials());
        EquinixResponse<OAuthToken> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }
}
