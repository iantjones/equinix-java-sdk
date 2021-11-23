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

package api.equinix.javasdk.networkedge.model.json.creators;

import api.equinix.javasdk.core.http.response.Pageable;
import api.equinix.javasdk.core.internal.Constants;
import api.equinix.javasdk.core.model.ResourceImpl;
import api.equinix.javasdk.networkedge.client.internal.implementation.BackupClientImpl;
import api.equinix.javasdk.networkedge.model.Backup;
import api.equinix.javasdk.networkedge.model.json.BackupJson;
import api.equinix.javasdk.networkedge.model.wrappers.BackupWrapper;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>BackupOperator class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
@Setter(AccessLevel.PRIVATE)
public class BackupOperator extends ResourceImpl<Backup> {

    @Getter
    private final Pageable<Backup> serviceClient;

    /**
     * <p>Constructor for BackupOperator.</p>
     *
     * @param serviceClient a {@link api.equinix.javasdk.core.http.response.Pageable} object.
     */
    public BackupOperator(Pageable<Backup> serviceClient) {
        this.serviceClient = serviceClient;
    }

    /**
     * <p>create.</p>
     *
     * @param deviceUuid a {@link java.lang.String} object.
     * @param backupName a {@link java.lang.String} object.
     * @return a {@link api.equinix.javasdk.networkedge.model.json.creators.BackupOperator.BackupBuilder} object.
     */
    public BackupBuilder create(String deviceUuid, String backupName) {
        return new BackupBuilder(deviceUuid, backupName);
    }

    /**
     * <p>update.</p>
     *
     * @param json a {@link api.equinix.javasdk.networkedge.model.json.BackupJson} object.
     * @return a {@link api.equinix.javasdk.networkedge.model.json.creators.BackupOperator.BackupUpdater} object.
     */
    public BackupUpdater update(BackupJson json) {
        return new BackupUpdater(json);
    }

    @Getter
    public class BackupBuilder {
        private String deviceUuid;
        private String backupName;

        protected BackupBuilder(String deviceUuid, String backupName) {
            this.deviceUuid = deviceUuid;
            this.backupName = backupName;
        }

        public Backup save() {
            BackupCreatorJson deviceLinkCreatorJson = new BackupCreatorJson(this);
            BackupJson deviceLinkJson = ((BackupClientImpl) BackupOperator.this.getServiceClient()).create(deviceLinkCreatorJson);
            return new BackupWrapper(deviceLinkJson, BackupOperator.this.getServiceClient());
        }
    }

    public class BackupUpdater {

        private BackupJson json;
        private BackupUpdaterJson updaterJson;

        protected BackupUpdater(BackupJson json) {
            this.json = json;
            this.updaterJson = Constants.JSON_CONVERTOR.convertValue(this.json, BackupUpdaterJson.class);
        }

        public BackupUpdater withConfigName(String backupName) {
            this.updaterJson.setName(backupName);
            return this;
        }

        public Backup save() {
            json = ((BackupClientImpl) BackupOperator.this.getServiceClient()).update(this.json.getUuid(), this.updaterJson);
            return new BackupWrapper(json, BackupOperator.this.getServiceClient());
        }
    }
}
