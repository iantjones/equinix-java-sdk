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

package api.equinix.javasdk.fabric.model.wrappers;

import api.equinix.javasdk.core.http.response.Pageable;
import api.equinix.javasdk.core.model.ResourceImpl;
import api.equinix.javasdk.fabric.model.Connection;
import api.equinix.javasdk.fabric.model.json.ConnectionJson;
import lombok.Getter;

/**
 * <p>ConnectionWrapper class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
public class ConnectionWrapper extends ResourceImpl<Connection> implements Connection {

    private ConnectionJson jsonObject;
    @Getter
    private final Pageable<Connection> serviceClient;

    /**
     * <p>Constructor for ConnectionWrapper.</p>
     *
     * @param portJson a {@link api.equinix.javasdk.fabric.model.json.ConnectionJson} object.
     * @param serviceClient a {@link api.equinix.javasdk.core.http.response.Pageable} object.
     */
    public ConnectionWrapper(ConnectionJson portJson, Pageable<Connection> serviceClient) {
        this.jsonObject = portJson;
        this.serviceClient = serviceClient;
    }

    /**
     * <p>getUuid.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getUuid() {
        return this.jsonObject.getUuid();
    }
}
