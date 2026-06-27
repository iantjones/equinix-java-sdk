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

package api.equinix.javasdk.fabric.client.internal.implementation;

import api.equinix.javasdk.core.client.ResourceClientBase;
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.fabric.client.implementation.FabricConfigImpl;
import api.equinix.javasdk.fabric.client.internal.CloudEventClient;
import api.equinix.javasdk.fabric.model.CloudEvent;
import api.equinix.javasdk.fabric.model.json.CloudEventJson;

/**
 * Internal client for Fabric Cloud Events (read-only). The JSON model implements the public
 * interface directly, so {@link #wrap(CloudEventJson)} is the identity.
 *
 * @author ianjones
 * @version $Id: $Id
 */
public class CloudEventClientImpl extends ResourceClientBase<CloudEvent, CloudEventJson> implements CloudEventClient<CloudEvent> {

    public CloudEventClientImpl(FabricConfigImpl configClient) {
        super(configClient, "Fabric", "CloudEvents", CloudEventJson.class);
    }

    @Override
    protected CloudEvent wrap(CloudEventJson json) {
        return json;
    }

    public Page<CloudEvent, CloudEventJson> list() {
        return listPage("GetCloudEvents");
    }
}
