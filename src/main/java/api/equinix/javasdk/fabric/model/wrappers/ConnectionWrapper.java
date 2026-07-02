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
import api.equinix.javasdk.fabric.client.internal.implementation.ConnectionClientImpl;
import api.equinix.javasdk.fabric.enums.ConnectionOperationType;
import api.equinix.javasdk.fabric.model.Connection;
import api.equinix.javasdk.fabric.model.ConnectionAction;
import api.equinix.javasdk.fabric.model.json.ConnectionJson;
import api.equinix.javasdk.fabric.model.json.creators.ConnectionOperator;
import lombok.Getter;
import lombok.experimental.Delegate;

/**
 *
 * @author ianjones
 */
public class ConnectionWrapper extends ResourceImpl<Connection> implements Connection {

    @Delegate(excludes = ConnectionMutability.class)
    private ConnectionJson jsonObject;
    @Getter
    private final Pageable<Connection> serviceClient;

    public ConnectionWrapper(ConnectionJson portJson, Pageable<Connection> serviceClient) {
        this.jsonObject = portJson;
        this.serviceClient = serviceClient;
    }

    public ConnectionAction performOperation(ConnectionOperationType connectionOperationType, String description, Object bodyObject) {
        return ((ConnectionClientImpl)this.serviceClient).performOperation(this.getUuid(), connectionOperationType, description, bodyObject);
    }

    public ConnectionAction performOperation(ConnectionOperationType connectionOperation, String description) {
        return performOperation(connectionOperation, description, null);
    }

    public ConnectionOperator.ConnectionUpdater update() {
        return new ConnectionOperator((ConnectionClientImpl) this.serviceClient).update(this.getUuid());
    }

    public ConnectionAction performOperation(ConnectionOperationType connectionOperation) {
        return performOperation(connectionOperation, null, null);
    }

    public Boolean delete() {
        this.jsonObject = ((ConnectionClientImpl)this.serviceClient).delete(this.getUuid());
        return true;
    }

    public void refresh() {
        this.jsonObject = ((ConnectionClientImpl)this.serviceClient).refresh(this.getUuid());
    }

    private interface ConnectionMutability {
        ConnectionAction performOperation(ConnectionOperationType connectionOperationType, String description, Object bodyObject);
        ConnectionAction performOperation(ConnectionOperationType connectionOperation, String description);
        ConnectionAction performOperation(ConnectionOperationType connectionOperation);
        Boolean delete();
        void refresh();
    }
}
