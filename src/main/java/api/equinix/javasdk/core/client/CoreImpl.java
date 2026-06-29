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

import api.equinix.javasdk.EquinixClient;
import api.equinix.javasdk.core.client.interfaces.Core;
import api.equinix.javasdk.core.client.interfaces.CoreClient;
import api.equinix.javasdk.core.model.OAuthToken;

/**
 * <p>CoreImpl class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
public class CoreImpl implements Core {

    private final EquinixClient serviceManager;

    private final CoreClient serviceClient;

    /**
     * <p>Constructor for CoreImpl.</p>
     *
     * @param serviceClient a {@link api.equinix.javasdk.core.client.interfaces.CoreClient} object.
     * @param serviceManager a {@link api.equinix.javasdk.EquinixClient} object.
     */
    public CoreImpl(CoreClient serviceClient, EquinixClient serviceManager) {
        this.serviceManager = serviceManager;
        this.serviceClient = serviceClient;
    }

    /**
     * <p>authenticate.</p>
     */
    public void authenticate() {
        OAuthToken oAuthToken = getServiceClient().authenticate();
        this.serviceManager.getEquinixClient().setOAuthToken(oAuthToken);
    }

    /**
     * <p>Getter for the field <code>serviceManager</code>.</p>
     *
     * @return a {@link api.equinix.javasdk.EquinixClient} object.
     */
    public EquinixClient getServiceManager() {
        return serviceManager;
    }

    /**
     * <p>Getter for the field <code>serviceClient</code>.</p>
     *
     * @return a {@link api.equinix.javasdk.core.client.interfaces.CoreClient} object.
     */
    public CoreClient getServiceClient() {
        return serviceClient;
    }
}
