/*
 * Copyright 2026 Ian Jones. All Rights Reserved.
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

package com.eqixiac.equinix.fabric.beta.client.implementation;

import com.eqixiac.equinix.fabric.beta.Beta;
import com.eqixiac.equinix.fabric.beta.client.Logos;
import com.eqixiac.equinix.fabric.beta.client.internal.BetaClientFactory;
import com.eqixiac.equinix.fabric.beta.client.internal.BetaResourceClient;
import com.eqixiac.equinix.fabric.beta.model.LogoResponse;
import java.util.Map;

/**
 * Implementation of {@link Logos} over the shared {@code FabricBeta} apiParams area.
 * <p><strong>Beta</strong>: Fabric v4 4.34.0 (spec read 2026-11-11).</p>
 * <p>Generated from the Fabric v4 OpenAPI spec by the fabric-beta generator; regenerate rather than hand-edit.</p>
 */
@Beta
public class LogosImpl implements Logos {

    private final BetaClientFactory clients;

    /**
     * Creates the client.
     *
     * @param clients the shared internal client factory
     */
    public LogosImpl(BetaClientFactory clients) {
        this.clients = clients;
    }

    private <J> BetaResourceClient<J> client(String requestParent, Class<J> type) {
        return clients.client(requestParent, type);
    }

    @Override
    public LogoResponse create(byte[] logo, String fileName, String name, String type, String description) {
        return client("Logos", LogoResponse.class).multipart("CreateLogo", "logo", logo, fileName,
                java.util.Map.of("name", name, "type", type, "description", description));
    }

    @Override
    public byte[] download(String uuid) {
        return client("Logos", Object.class).bytes("GetLogoByUuid", Map.of("uuid", uuid));
    }

    @Override
    public LogoResponse delete(String uuid) {
        return client("Logos", LogoResponse.class).one("DeleteLogoByUuid", Map.of("uuid", uuid), null);
    }
}
