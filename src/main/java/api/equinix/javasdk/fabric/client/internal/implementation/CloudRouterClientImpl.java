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
import api.equinix.javasdk.core.http.ResponseHandler;
import api.equinix.javasdk.core.http.request.PatchOperation;
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.core.http.response.PaginatedFilteredList;
import api.equinix.javasdk.core.model.FilteredSortedPaginatedPost;
import api.equinix.javasdk.fabric.client.implementation.FabricConfigImpl;
import api.equinix.javasdk.fabric.client.internal.CloudRouterActionClient;
import api.equinix.javasdk.fabric.client.internal.CloudRouterClient;
import api.equinix.javasdk.fabric.client.internal.RouteAggregationAttachmentClient;
import api.equinix.javasdk.fabric.client.internal.RouteFilterAttachmentClient;
import api.equinix.javasdk.fabric.model.CloudRouter;
import api.equinix.javasdk.fabric.model.CloudRouterAction;
import api.equinix.javasdk.fabric.model.RouteAggregationAttachment;
import api.equinix.javasdk.fabric.model.RouteFilterAttachment;
import api.equinix.javasdk.fabric.model.RoutingProtocolValidation;
import api.equinix.javasdk.fabric.model.implementation.ConnectionValidationRequest;
import api.equinix.javasdk.fabric.model.implementation.filter.FilterPropertyList;
import api.equinix.javasdk.fabric.model.implementation.sort.SortPropertyList;
import api.equinix.javasdk.fabric.model.json.CloudRouterActionJson;
import api.equinix.javasdk.fabric.model.json.CloudRouterJson;
import api.equinix.javasdk.fabric.model.json.RouteAggregationAttachmentJson;
import api.equinix.javasdk.fabric.model.json.RouteFilterAttachmentJson;
import api.equinix.javasdk.fabric.model.json.RoutingProtocolValidationJson;
import api.equinix.javasdk.fabric.model.json.creators.CloudRouterCreatorJson;
import api.equinix.javasdk.fabric.model.wrappers.CloudRouterWrapper;

import java.util.List;
import java.util.Map;

public class CloudRouterClientImpl extends ResourceClientBase<CloudRouter, CloudRouterJson> implements CloudRouterClient<CloudRouter> {

    private final CloudRouterActionClient<CloudRouterAction> actionsClient;

    private final RouteFilterAttachmentClient<RouteFilterAttachment> routeFilterAttachmentsClient;

    private final RouteAggregationAttachmentClient<RouteAggregationAttachment> routeAggregationAttachmentsClient;

    public CloudRouterClientImpl(FabricConfigImpl configClient) {
        super(configClient, "Fabric", "CloudRouters", CloudRouterJson.class);
        this.actionsClient = new CloudRouterActionClientImpl(configClient);
        this.routeFilterAttachmentsClient = new RouteFilterAttachmentClientImpl(configClient, "CloudRouters");
        this.routeAggregationAttachmentsClient = new RouteAggregationAttachmentClientImpl(configClient, "CloudRouters");
    }

    @Override
    protected CloudRouter wrap(CloudRouterJson json) {
        return new CloudRouterWrapper(json, this);
    }

    public Page<CloudRouterJson> search(FilterPropertyList filter, SortPropertyList sort) {
        return searchPage("SearchCloudRouters", new FilteredSortedPaginatedPost<>(filter, sort));
    }

    public CloudRouterJson getByUuid(String uuid) {
        return getOne("GetCloudRouter", uuid);
    }

    public CloudRouterJson create(CloudRouterCreatorJson cloudRouterCreatorJson) {
        return postOne("PostCloudRouter", cloudRouterCreatorJson);
    }

    public CloudRouterJson update(String uuid, List<PatchOperation> operations) {
        return patchOne("UpdateCloudRouter", uuid, operations);
    }

    public CloudRouterJson delete(String uuid) {
        return deleteOne("DeleteCloudRouter", uuid);
    }

    public CloudRouterJson refresh(String uuid) {
        return getByUuid(uuid);
    }

    public RoutingProtocolValidation validateRoutingProtocol(String routerId, FilterPropertyList filter) {
        return postForType("ValidateRoutingProtocol", Map.of("uuid", routerId),
                new ConnectionValidationRequest(filter), RoutingProtocolValidationJson.getSingleTypeRef());
    }

    public List<CloudRouterAction> getActions(String routerId) {
        return this.actionsClient.list(routerId);
    }

    public CloudRouterAction getAction(String routerId, String uuid) {
        return this.actionsClient.getByUuid(routerId, uuid);
    }

    public PaginatedFilteredList<CloudRouterAction> searchActions(String routerId, FilterPropertyList filter, SortPropertyList sort) {
        Page<CloudRouterActionJson> responsePage = this.actionsClient.search(routerId, filter, sort);
        PaginatedFilteredList<CloudRouterAction> actions = ResponseHandler.mapPaginatedFilteredList(responsePage.getItems(), this.actionsClient, (json, client) -> json);
        return new PaginatedFilteredList<>(actions, this.actionsClient, responsePage.getAssociatedRequest(), responsePage.getAssociatedResponse(), responsePage.getPagination());
    }

    public PaginatedFilteredList<RouteFilterAttachment> searchRouteFilterAttachments(String routerId, FilterPropertyList filter, SortPropertyList sort) {
        Page<RouteFilterAttachmentJson> responsePage = this.routeFilterAttachmentsClient.searchCloudRouterAttachments(routerId, filter, sort);
        PaginatedFilteredList<RouteFilterAttachment> attachments = ResponseHandler.mapPaginatedFilteredList(responsePage.getItems(), this.routeFilterAttachmentsClient, (json, client) -> json);
        return new PaginatedFilteredList<>(attachments, this.routeFilterAttachmentsClient, responsePage.getAssociatedRequest(), responsePage.getAssociatedResponse(), responsePage.getPagination());
    }

    public PaginatedFilteredList<RouteAggregationAttachment> searchRouteAggregationAttachments(String routerId, FilterPropertyList filter, SortPropertyList sort) {
        Page<RouteAggregationAttachmentJson> responsePage = this.routeAggregationAttachmentsClient.searchCloudRouterAttachments(routerId, filter, sort);
        PaginatedFilteredList<RouteAggregationAttachment> attachments = ResponseHandler.mapPaginatedFilteredList(responsePage.getItems(), this.routeAggregationAttachmentsClient, (json, client) -> json);
        return new PaginatedFilteredList<>(attachments, this.routeAggregationAttachmentsClient, responsePage.getAssociatedRequest(), responsePage.getAssociatedResponse(), responsePage.getPagination());
    }
}
