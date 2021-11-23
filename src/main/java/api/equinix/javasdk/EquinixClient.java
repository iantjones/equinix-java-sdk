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

package api.equinix.javasdk;

import api.equinix.javasdk.core.auth.EquinixCredentials;
import api.equinix.javasdk.core.client.CoreConfigImpl;
import api.equinix.javasdk.core.client.CoreImpl;
import api.equinix.javasdk.core.client.interfaces.Core;
import api.equinix.javasdk.core.client.interfaces.CoreConfig;
import api.equinix.javasdk.core.exception.EquinixClientException;
import api.equinix.javasdk.core.model.Service;
import lombok.Getter;

/**
 * <p>BaseClient class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
public class EquinixClient implements Service {

    private Core core;

    final private CoreConfig coreConfig;

    @Getter
    final protected api.equinix.javasdk.core.client.EquinixClient equinixClient;

    /**
     * <p>Constructor for BaseClient.</p>
     *
     * @param equinixCredentials a {@link api.equinix.javasdk.core.auth.EquinixCredentials} object.
     */
    public EquinixClient(EquinixCredentials equinixCredentials) {
        this(equinixCredentials, false);
    }

    /**
     * <p>Constructor for BaseClient.</p>
     *
     * @param equinixCredentials a {@link api.equinix.javasdk.core.auth.EquinixCredentials} object.
     * @param isSandBoxed a boolean.
     */
    public EquinixClient(EquinixCredentials equinixCredentials, boolean isSandBoxed) {
        equinixClient = new api.equinix.javasdk.core.client.EquinixClient(equinixCredentials, isSandBoxed);
        this.coreConfig = new CoreConfigImpl(equinixClient);
    }

    /**
     * Performs authentication using the given user credentials and stores the associated {@link api.equinix.javasdk.core.model.OAuthToken}.
     *
     * @throws api.equinix.javasdk.core.exception.EquinixClientException if any.
     */
    public void authenticate() throws EquinixClientException {
        core().authenticate();
    }

    /**
     * return {@link api.equinix.javasdk.core.client.interfaces.Core}
     * The entry point for accessing core related operations such as authentication.
     *
     * @return a {@link api.equinix.javasdk.core.client.interfaces.Core} object.
     */
    protected Core core() {
        if (this.core == null) {
            this.core = new CoreImpl(this.coreConfig.getCoreClient(), this);
        }
        return this.core;
    }
}
