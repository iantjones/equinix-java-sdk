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

package api.equinix.javasdk.fabric.client.internal.implementation;

import api.equinix.javasdk.core.client.ResourceClientBase;
import api.equinix.javasdk.core.http.Utils;
import api.equinix.javasdk.core.http.request.PatchOperation;
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.core.model.FilteredSortedPaginatedPost;
import api.equinix.javasdk.fabric.enums.Side;
import api.equinix.javasdk.fabric.client.implementation.FabricConfigImpl;
import api.equinix.javasdk.fabric.client.internal.ConnectionClient;
import api.equinix.javasdk.fabric.enums.ConnectionOperationType;
import api.equinix.javasdk.fabric.model.Connection;
import api.equinix.javasdk.fabric.model.implementation.ManageConnection;
import api.equinix.javasdk.fabric.model.implementation.filter.FilterPropertyList;
import api.equinix.javasdk.fabric.model.implementation.sort.SortPropertyList;
import api.equinix.javasdk.fabric.model.json.ConnectionJson;
import api.equinix.javasdk.fabric.model.json.ConnectionStatisticJson;
import api.equinix.javasdk.fabric.model.json.creators.ConnectionCreatorJson;
import api.equinix.javasdk.fabric.model.wrappers.ConnectionWrapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Internal client for Fabric Connections. Standard CRUD + paging come from {@link ResourceClientBase};
 * the Connection-specific operations (dry-run, actions, bulk, statistics) remain bespoke below.
 *
 * @author ianjones
 * @version $Id: $Id
 */
public class ConnectionClientImpl extends ResourceClientBase<Connection, ConnectionJson> implements ConnectionClient<Connection> {

    public ConnectionClientImpl(FabricConfigImpl configClient) {
        super(configClient, "Fabric", "Connections", ConnectionJson.class);
    }

    @Override
    protected Connection wrap(ConnectionJson json) {
        return new ConnectionWrapper(json, this);
    }

    public Page<Connection, ConnectionJson> search(FilterPropertyList filter, SortPropertyList sort) {
        return searchPage("SearchConnections", new FilteredSortedPaginatedPost<>(filter, sort));
    }

    public ConnectionJson getByUuid(String uuid) {
        return getOne("GetConnection", uuid);
    }

    public ConnectionJson create(ConnectionCreatorJson connectionCreatorJson) {
        return postOne("PostConnection", connectionCreatorJson);
    }

    public ConnectionJson delete(String uuid) {
        return deleteOne("DeleteConnection", uuid);
    }

    public ConnectionJson refresh(String uuid) {
        return getByUuid(uuid);
    }

    public ConnectionJson dryRunCreate(ConnectionCreatorJson connectionCreatorJson) {
        return dryRunCreate("PostConnection", connectionCreatorJson);
    }

    public ConnectionJson performOperation(String uuid, ConnectionOperationType connectionOperationType, String description, Object bodyObject) {
        return postOne("ManageConnection", Map.of("uuid", uuid),
                new ManageConnection(connectionOperationType, description, bodyObject));
    }

    public ConnectionJson update(String uuid, List<PatchOperation> operations) {
        return patchOne("UpdateConnection", uuid, operations);
    }

    public List<Connection> batch(List<ConnectionCreatorJson> connectionCreatorJsonList) {
        return postForType("PostBulkConnections", connectionCreatorJsonList, ConnectionJson.getListTypeRef());
    }

    public ConnectionStatisticJson getStatistics(String uuid, LocalDateTime startDateTime, LocalDateTime endDateTime, Side viewPoint) {
        Map<String, List<String>> qParams = Map.of(
                "startDateTime", Utils.singleParamList(Utils.dateTimeForQuery(startDateTime)),
                "endDateTime", Utils.singleParamList(Utils.dateTimeForQuery(endDateTime)),
                "viewPoint", Utils.singleParamList(viewPoint.toViewPoint())
        );
        return getAs("GetStatistics", Map.of("uuid", uuid), qParams, ConnectionStatisticJson.class);
    }

    public ConnectionStatisticJson refreshStatistics(String uuid, LocalDateTime startDateTime, LocalDateTime endDateTime, Side viewPoint) {
        return this.getStatistics(uuid, startDateTime, endDateTime, viewPoint);
    }
}
