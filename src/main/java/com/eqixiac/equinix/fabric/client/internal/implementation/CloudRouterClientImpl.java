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

package com.eqixiac.equinix.fabric.client.internal.implementation;

import com.eqixiac.equinix.core.client.ResourceClientBase;
import com.eqixiac.equinix.core.http.ResponseHandler;
import com.eqixiac.equinix.core.http.request.PatchOperation;
import com.eqixiac.equinix.core.http.response.Page;
import com.eqixiac.equinix.core.http.response.PaginatedFilteredList;
import com.eqixiac.equinix.core.model.FilteredSortedPaginatedPost;
import com.eqixiac.equinix.fabric.client.implementation.FabricConfigImpl;
import com.eqixiac.equinix.fabric.client.internal.CloudRouterActionClient;
import com.eqixiac.equinix.fabric.client.internal.CloudRouterClient;
import com.eqixiac.equinix.fabric.client.internal.RouteAggregationAttachmentClient;
import com.eqixiac.equinix.fabric.client.internal.RouteFilterAttachmentClient;
import com.eqixiac.equinix.fabric.model.CloudRouter;
import com.eqixiac.equinix.fabric.model.CloudRouterAction;
import com.eqixiac.equinix.fabric.model.RouteAggregationAttachment;
import com.eqixiac.equinix.fabric.model.RouteFilterAttachment;
import com.eqixiac.equinix.fabric.model.RoutingProtocolValidation;
import com.eqixiac.equinix.fabric.model.implementation.ConnectionValidationRequest;
import com.eqixiac.equinix.fabric.model.implementation.filter.FilterPropertyList;
import com.eqixiac.equinix.fabric.model.implementation.sort.SortPropertyList;
import com.eqixiac.equinix.fabric.model.json.CloudRouterActionJson;
import com.eqixiac.equinix.fabric.model.json.CloudRouterJson;
import com.eqixiac.equinix.fabric.model.json.RouteAggregationAttachmentJson;
import com.eqixiac.equinix.fabric.model.json.RouteFilterAttachmentJson;
import com.eqixiac.equinix.fabric.model.json.RoutingProtocolValidationJson;
import com.eqixiac.equinix.fabric.model.json.creators.CloudRouterCreatorJson;
import com.eqixiac.equinix.fabric.model.wrappers.CloudRouterWrapper;

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

    /**
     * Dry-run variant of {@link #create(CloudRouterCreatorJson)}: POSTs the same body to
     * {@code /fabric/v4/routers} with the spec's {@code dryRun=true} query parameter
     * ("option to verify that API calls will succeed"). Nothing is provisioned; the spec's
     * dry-run example ({@code CloudRouterResponseExampleDryRun}) is the validated request
     * echoed back with no {@code uuid}/{@code href}/{@code state}.
     *
     * @param cloudRouterCreatorJson the create request body to validate
     * @return the validated request echoed back by the API
     */
    public CloudRouterJson dryRunCreate(CloudRouterCreatorJson cloudRouterCreatorJson) {
        return dryRunCreate("PostCloudRouter", cloudRouterCreatorJson);
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
