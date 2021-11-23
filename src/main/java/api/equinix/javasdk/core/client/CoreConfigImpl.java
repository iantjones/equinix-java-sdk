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

import api.equinix.javasdk.core.client.interfaces.CoreConfig;
import lombok.Getter;

/**
 * <p>CoreConfigImpl class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
@Getter
public class CoreConfigImpl extends Config implements CoreConfig {

    private final CoreClientImpl coreClient;

    /**
     * <p>Constructor for CoreConfigImpl.</p>
     *
     * @param equinixClient a {@link api.equinix.javasdk.core.client.EquinixClient} object.
     */
    public CoreConfigImpl(EquinixClient equinixClient) {
        super(equinixClient);
        this.coreClient = new CoreClientImpl(this);
    }
}
