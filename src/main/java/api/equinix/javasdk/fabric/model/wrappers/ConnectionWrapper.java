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

import api.equinix.javasdk.core.enums.ConnectionState;
import api.equinix.javasdk.core.http.response.Pageable;
import api.equinix.javasdk.core.http.response.PageablePost;
import api.equinix.javasdk.core.model.KeyValuePair;
import api.equinix.javasdk.core.model.ResourcePostImpl;
import api.equinix.javasdk.fabric.client.internal.implementation.ConnectionClientImpl;

import api.equinix.javasdk.fabric.enums.ConnectionOperationType;
import api.equinix.javasdk.fabric.enums.ConnectionType;
import api.equinix.javasdk.fabric.enums.Direction;
import api.equinix.javasdk.fabric.model.Connection;
import api.equinix.javasdk.fabric.model.implementation.*;
import api.equinix.javasdk.fabric.model.json.ConnectionJson;
import lombok.Getter;

import java.util.List;

/**
 * <p>ConnectionWrapper class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
public class ConnectionWrapper extends ResourcePostImpl<Connection> implements Connection {

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

    public String getUuid() {
        return  this.jsonObject.getUuid();
    }

    public ConnectionType getType() {
        return  this.jsonObject.getType();
    }

    public String getHref() {
        return  this.jsonObject.getHref();
    }

    public String getName() {
        return  this.jsonObject.getName();
    }

    public ConnectionState getState() {
        return  this.jsonObject.getState();
    }

    public Order getOrder() {
        return this.jsonObject.getOrder();
    }

    public ConnectionOperation getOperation() {
        return  this.jsonObject.getOperation();
    }

    public List<Notification> getNotifications() {
        return  this.jsonObject.getNotifications();
    }

    public Account getAccount() {
        return  this.jsonObject.getAccount();
    }

    public ChangeLog getChangeLog() {
        return  this.jsonObject.getChangeLog();
    }

    public Integer getBandwidth() {
        return  this.jsonObject.getBandwidth();
    }

    public Redundancy getRedundancy() {
        return  this.jsonObject.getRedundancy();
    }

    public Boolean getIsRemote() {
        return  this.jsonObject.getIsRemote();
    }

    public Direction getDirection() {
        return  this.jsonObject.getDirection();
    }

    public ConnectionSide getASide() {
        return  this.jsonObject.getASide();
    }

    public ConnectionSide getZSide() {
        return  this.jsonObject.getZSide();
    }

    public List<KeyValuePair> getAdditionalInfo() {
        return  this.jsonObject.getAdditionalInfo();
    }

    public Change getChange() {
        return this.jsonObject.getChange();
    }

    public void performOperation(ConnectionOperationType connectionOperationType, String description, Object bodyObject) {
        this.jsonObject = ((ConnectionClientImpl)this.serviceClient).performOperation(this.getUuid(), connectionOperationType, description, bodyObject);
    }

    public void performOperation(ConnectionOperationType connectionOperation, String description) {
        performOperation(connectionOperation, description, null);
    }

    public void performOperation(ConnectionOperationType connectionOperation) {
        performOperation(connectionOperation, null, null);
    }

    public Boolean delete() {
        this.jsonObject = ((ConnectionClientImpl)this.serviceClient).delete(this.getUuid());
        return true;
    }

    public void refresh() {
        this.jsonObject = ((ConnectionClientImpl)this.serviceClient).refresh(this.getUuid());
    }
}
