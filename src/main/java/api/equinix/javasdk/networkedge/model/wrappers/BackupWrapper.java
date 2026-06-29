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
import api.equinix.javasdk.networkedge.model.Backup;
import api.equinix.javasdk.networkedge.model.Device;
import api.equinix.javasdk.networkedge.model.json.BackupJson;
import api.equinix.javasdk.networkedge.model.json.creators.BackupOperator;
import api.equinix.javasdk.networkedge.model.json.creators.BackupUpdaterJson;
import lombok.Getter;
import lombok.experimental.Delegate;

/**
 *
 * @author ianjones
 */
public class BackupWrapper extends ResourceImpl<Backup> implements Backup {

    @Delegate
    private BackupJson json;

    @Getter
    private final Pageable<Backup> serviceClient;

    public BackupWrapper(BackupJson deviceLinkJson, Pageable<Backup> serviceClient) {
        this.json = deviceLinkJson;
        this.serviceClient = serviceClient;
    }

    /**
     * {@inheritDoc}
     *
     * <p>restore. The restore endpoint identifies the target by this backup's uuid; the backup name
     * is sent in the body as required by the API.</p>
     */
    public Boolean restore(){
        return ((BackupClientImpl)this.serviceClient).restore(this.getUuid(), this.getName());
    }

    @Deprecated
    public Boolean restoreToDevice(Device device){
        return restore();
    }

    /**
     * {@inheritDoc}
     *
     * <p>restoreToDevice. The restore endpoint identifies the target by the backup uuid (this
     * backup), so the supplied {@code deviceUuid} is ignored; the backup name is sent in the body
     * as required by the API. Prefer {@link #restore()}.</p>
     *
     */
    @Deprecated
    public Boolean restoreToDevice(String deviceUuid){
        return restore();
    }

    public BackupOperator.BackupUpdater update() {
        return new BackupOperator(this.serviceClient).update(this.json);
    }

    public Boolean save(BackupUpdaterJson updaterJson) {
        this.json = ((BackupClientImpl)this.serviceClient).update(this.getUuid(), updaterJson);
        return true;
    }

    public Boolean delete() {
        return ((BackupClientImpl)this.serviceClient).delete(this.getUuid());
    }

    public Boolean refresh() {
        this.json = ((BackupClientImpl)this.serviceClient).refresh(this.getUuid());
        return true;
    }
}
