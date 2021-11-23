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

package api.equinix.javasdk.networkedge.client.internal;

import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.core.http.response.Pageable;
import api.equinix.javasdk.networkedge.client.RequestBuilder;
import api.equinix.javasdk.networkedge.model.Backup;
import api.equinix.javasdk.networkedge.model.json.BackupJson;
import api.equinix.javasdk.networkedge.model.json.RestoreFeasibilityJson;
import api.equinix.javasdk.networkedge.model.json.creators.BackupCreatorJson;

/**
 * <p>BackupClient interface.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
public interface BackupClient<T> extends Pageable<T> {

    /**
     * <p>list.</p>
     *
     * @param deviceUuid a {@link java.lang.String} object.
     * @param requestBuilder a {@link api.equinix.javasdk.networkedge.client.RequestBuilder.Backup} object.
     * @return a {@link api.equinix.javasdk.core.http.response.Page} object.
     */
    Page<Backup, BackupJson> list(String deviceUuid, RequestBuilder.Backup requestBuilder);

    /**
     * <p>getByUuid.</p>
     *
     * @param uuid a {@link java.lang.String} object.
     * @return a {@link api.equinix.javasdk.networkedge.model.json.BackupJson} object.
     */
    BackupJson getByUuid(String uuid);

    /**
     * <p>checkRestoreFeasibility.</p>
     *
     * @param uuid a {@link java.lang.String} object.
     * @param deviceUuid a {@link java.lang.String} object.
     * @return a {@link api.equinix.javasdk.networkedge.model.json.RestoreFeasibilityJson} object.
     */
    RestoreFeasibilityJson checkRestoreFeasibility(String uuid, String deviceUuid);

    /**
     * <p>restore.</p>
     *
     * @param uuid a {@link java.lang.String} object.
     * @param deviceUuid a {@link java.lang.String} object.
     * @return a {@link java.lang.Boolean} object.
     */
    Boolean restore(String uuid, String deviceUuid);

    /**
     * <p>download.</p>
     *
     * @param uuid a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    String download(String uuid);

    /**
     * <p>create.</p>
     *
     * @param backupCreatorJson a {@link api.equinix.javasdk.networkedge.model.json.creators.BackupCreatorJson} object.
     * @return a {@link api.equinix.javasdk.networkedge.model.json.BackupJson} object.
     */
    BackupJson create(BackupCreatorJson backupCreatorJson);

    /**
     * <p>delete.</p>
     *
     * @param uuid a {@link java.lang.String} object.
     * @return a {@link java.lang.Boolean} object.
     */
    Boolean delete(String uuid);

    /**
     * <p>refresh.</p>
     *
     * @param uuid a {@link java.lang.String} object.
     * @return a {@link api.equinix.javasdk.networkedge.model.json.BackupJson} object.
     */
    BackupJson refresh(String uuid);
}
