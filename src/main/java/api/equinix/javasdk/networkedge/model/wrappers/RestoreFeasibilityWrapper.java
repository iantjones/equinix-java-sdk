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
import api.equinix.javasdk.networkedge.enums.ServiceType;
import api.equinix.javasdk.networkedge.model.Backup;
import api.equinix.javasdk.networkedge.model.RestoreFeasibility;
import api.equinix.javasdk.networkedge.model.implementation.BackupService;
import api.equinix.javasdk.networkedge.model.json.BackupJson;
import api.equinix.javasdk.networkedge.model.json.RestoreFeasibilityJson;
import lombok.Getter;

import java.util.List;
import java.util.Map;

/**
 * <p>RestoreFeasibilityWrapper class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
public class RestoreFeasibilityWrapper extends ResourceImpl<Backup> implements RestoreFeasibility {

    private RestoreFeasibilityJson json;
    @Getter
    private final Pageable<Backup> serviceClient;

    /**
     * <p>Constructor for RestoreFeasibilityWrapper.</p>
     *
     * @param json a {@link api.equinix.javasdk.networkedge.model.json.RestoreFeasibilityJson} object.
     * @param serviceClient a {@link api.equinix.javasdk.core.http.response.Pageable} object.
     */
    public RestoreFeasibilityWrapper(RestoreFeasibilityJson json, Pageable<Backup> serviceClient) {
        this.json = json;
        this.serviceClient = serviceClient;
    }

    /**
     * <p>getDeviceBackup.</p>
     *
     * @return a {@link api.equinix.javasdk.networkedge.model.json.BackupJson} object.
     */
    public BackupJson getDeviceBackup() {
        return  this.json.getDeviceBackup();
    }

    /**
     * <p>getServices.</p>
     *
     * @return a {@link java.util.Map} object.
     */
    public Map<ServiceType, List<BackupService>> getServices() {
        return null;
    }

    /**
     * <p>getRestoreAllowedAfterDeleteOrEdit.</p>
     *
     * @return a {@link java.lang.Boolean} object.
     */
    public Boolean getRestoreAllowedAfterDeleteOrEdit() {
        return  this.json.getRestoreAllowedAfterDeleteOrEdit();
    }

    /**
     * <p>getRestoreAllowed.</p>
     *
     * @return a {@link java.lang.Boolean} object.
     */
    public Boolean getRestoreAllowed() {
        return  this.json.getRestoreAllowed();
    }
}
