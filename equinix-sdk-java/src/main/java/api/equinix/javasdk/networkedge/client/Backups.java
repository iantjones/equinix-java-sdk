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

package api.equinix.javasdk.networkedge.client;

import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.networkedge.model.ACLTemplate;
import api.equinix.javasdk.networkedge.model.Backup;
import api.equinix.javasdk.networkedge.model.RestoreFeasibility;
import api.equinix.javasdk.networkedge.model.json.creators.ACLTemplateOperator;
import api.equinix.javasdk.networkedge.model.json.creators.BackupOperator;

/**
 * Client interface for managing device backups on Network Edge. Provides operations to
 * list, retrieve, download, and create device backups, as well as check restore feasibility.
 *
 * @author ianjones
 * @version $Id: $Id
 */
public interface Backups {

    /**
     * Lists available backups for the provided deviceUuid.
     *
     * @param deviceUuid the unique device identifier.
     * @return {@link api.equinix.javasdk.core.http.response.PaginatedList}
     */
    PaginatedList<Backup> list(String deviceUuid);

    /**
     * Lists available backups for the provided deviceUuid and desired query parameters.
     *
     * @param deviceUuid the unique device identifier.
     * @param requestBuilder the desired query parameters.
     * @return {@link api.equinix.javasdk.core.http.response.PaginatedList}
     */
    PaginatedList<Backup> list(String deviceUuid, RequestBuilder.Backup requestBuilder);

    /**
     * Gets the specified backup.
     *
     * @param uuid the unique identifier of the backup.
     * @return {@link api.equinix.javasdk.networkedge.model.Backup}
     */
    Backup getByUuid(String uuid);

    /**
     * Gets the contents of the specified backup.
     *
     * @param uuid the unique identifier of the backup.
     * @return {@link java.lang.String}
     */
    String download(String uuid);

    /**
     * Gets the information as to the feasibility of a specific restore operation.
     *
     * @param uuid the unique identifier of the backup.
     * @param deviceUuid the unique identifier of the device.
     * @return {@link api.equinix.javasdk.networkedge.model.RestoreFeasibility}
     */
    RestoreFeasibility checkRestoreFeasibility(String uuid, String deviceUuid);

    /**
     * Returns an instance of BackupBuilder for defining a new backup.
     *
     * @param deviceUuid the unique identifier of the device.
     * @param backupName the name of the new backup.
     * @return {@link api.equinix.javasdk.networkedge.model.json.creators.BackupOperator.BackupBuilder}
     */
    BackupOperator.BackupBuilder define(String deviceUuid, String backupName);
}
