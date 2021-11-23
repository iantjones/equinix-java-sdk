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

package api.equinix.javasdk.networkedge.model.wrappers;

import api.equinix.javasdk.core.http.response.Pageable;
import api.equinix.javasdk.core.model.ResourceImpl;
import api.equinix.javasdk.networkedge.client.internal.implementation.BackupClientImpl;
import api.equinix.javasdk.networkedge.enums.BackupRequestType;
import api.equinix.javasdk.networkedge.enums.BackupStatus;
import api.equinix.javasdk.networkedge.enums.BackupType;
import api.equinix.javasdk.networkedge.model.Backup;
import api.equinix.javasdk.networkedge.model.Device;
import api.equinix.javasdk.networkedge.model.implementation.DeviceRestore;
import api.equinix.javasdk.networkedge.model.json.BackupJson;
import api.equinix.javasdk.networkedge.model.json.creators.BackupOperator;
import api.equinix.javasdk.networkedge.model.json.creators.BackupUpdaterJson;
import lombok.Getter;

import java.util.List;

/**
 * <p>BackupWrapper class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
public class BackupWrapper extends ResourceImpl<Backup> implements Backup {

    private BackupJson json;

    @Getter
    private final Pageable<Backup> serviceClient;

    /**
     * <p>Constructor for BackupWrapper.</p>
     *
     * @param deviceLinkJson a {@link api.equinix.javasdk.networkedge.model.json.BackupJson} object.
     * @param serviceClient a {@link api.equinix.javasdk.core.http.response.Pageable} object.
     */
    public BackupWrapper(BackupJson deviceLinkJson, Pageable<Backup> serviceClient) {
        this.json = deviceLinkJson;
        this.serviceClient = serviceClient;
    }

    /**
     * <p>getUuid.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getUuid() {
        return  this.json.getUuid();
    }

    /**
     * <p>getName.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getName() {
        return  this.json.getName();
    }

    /**
     * <p>getType.</p>
     *
     * @return a {@link api.equinix.javasdk.networkedge.enums.BackupType} object.
     */
    public BackupType getType() {
        return  this.json.getType();
    }

    /**
     * <p>getStatus.</p>
     *
     * @return a {@link api.equinix.javasdk.networkedge.enums.BackupStatus} object.
     */
    public BackupStatus getStatus() {
        return  this.json.getStatus();
    }

    /**
     * <p>getRequestType.</p>
     *
     * @return a {@link api.equinix.javasdk.networkedge.enums.BackupRequestType} object.
     */
    public BackupRequestType getRequestType() {
        return  this.json.getRequestType();
    }

    /**
     * <p>getDownloadUrl.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getDownloadUrl() {
        return  this.json.getDownloadUrl();
    }

    /**
     * <p>getVersion.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getVersion() {
        return  this.json.getVersion();
    }

    /**
     * <p>getDeleteAllowed.</p>
     *
     * @return a {@link java.lang.Boolean} object.
     */
    public Boolean getDeleteAllowed() {
        return  this.json.getDeleteAllowed();
    }

    /**
     * <p>getRestores.</p>
     *
     * @return a {@link java.util.List} object.
     */
    public List<DeviceRestore> getRestores() {
        return  this.json.getRestores();
    }

    /** {@inheritDoc} */
    public Boolean restoreToDevice(Device device){
        return restoreToDevice(device.getUuid());
    }

    /**
     * {@inheritDoc}
     *
     * <p>restoreToDevice.</p>
     *
     * @param deviceUuid a {@link java.lang.String} object.
     * @return a {@link java.lang.Boolean} object.
     */
    public Boolean restoreToDevice(String deviceUuid){
        return ((BackupClientImpl)this.serviceClient).restore(this.getUuid(), deviceUuid);
    }

    /**
     * <p>update.</p>
     *
     * @return a {@link api.equinix.javasdk.networkedge.model.json.creators.BackupOperator.BackupUpdater} object.
     */
    public BackupOperator.BackupUpdater update() {
        return new BackupOperator(this.serviceClient).update(this.json);
    }

    /** {@inheritDoc} */
    public Boolean save(BackupUpdaterJson updaterJson) {
        this.json = ((BackupClientImpl)this.serviceClient).update(this.getUuid(), updaterJson);
        return true;
    }

    /**
     * <p>delete.</p>
     *
     * @return a {@link java.lang.Boolean} object.
     */
    public Boolean delete() {
        return ((BackupClientImpl)this.serviceClient).delete(this.getUuid());
    }

    /**
     * <p>refresh.</p>
     *
     * @return a {@link java.lang.Boolean} object.
     */
    public Boolean refresh() {
        this.json = ((BackupClientImpl)this.serviceClient).refresh(this.getUuid());
        return true;
    }
}
