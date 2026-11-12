/*
 * Copyright 2026 Ian Jones. All Rights Reserved.
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

package com.eqixiac.equinix.fabric.beta.client.implementation;

import com.eqixiac.equinix.core.http.request.PatchOperation;
import com.eqixiac.equinix.core.http.response.PaginatedFilteredList;
import com.eqixiac.equinix.core.http.response.PaginatedList;
import com.eqixiac.equinix.fabric.beta.Beta;
import com.eqixiac.equinix.fabric.beta.client.AppLinks;
import com.eqixiac.equinix.fabric.beta.client.AttachmentQuery;
import com.eqixiac.equinix.fabric.beta.client.internal.BetaClientFactory;
import com.eqixiac.equinix.fabric.beta.client.internal.BetaResourceClient;
import com.eqixiac.equinix.fabric.beta.model.AppLink;
import com.eqixiac.equinix.fabric.beta.model.AppLinkAppDomainAttachment;
import com.eqixiac.equinix.fabric.beta.model.AppLinkAppProcessorAttachment;
import com.eqixiac.equinix.fabric.beta.model.AppLinkAppServiceAttachment;
import com.eqixiac.equinix.fabric.beta.model.AppLinkAttachProcessorRequest;
import com.eqixiac.equinix.fabric.beta.model.AppLinkAttachServiceRequest;
import com.eqixiac.equinix.fabric.beta.model.AppLinkAttachedAppDomain;
import com.eqixiac.equinix.fabric.beta.model.AppLinkAttachedAppService;
import com.eqixiac.equinix.fabric.beta.model.AppLinkPostRequest;
import com.eqixiac.equinix.fabric.model.implementation.filter.FilterPropertyList;
import com.eqixiac.equinix.fabric.model.implementation.sort.SortPropertyList;
import java.util.List;
import java.util.Map;

/**
 * Implementation of {@link AppLinks} over the shared {@code FabricBeta} apiParams area.
 * <p><strong>Beta</strong>: Fabric v4 4.34.0 (spec read 2026-11-11).</p>
 * <p>Generated from the Fabric v4 OpenAPI spec by the fabric-beta generator; regenerate rather than hand-edit.</p>
 */
@Beta
public class AppLinksImpl implements AppLinks {

    private final BetaClientFactory clients;

    /**
     * Creates the client.
     *
     * @param clients the shared internal client factory
     */
    public AppLinksImpl(BetaClientFactory clients) {
        this.clients = clients;
    }

    private <J> BetaResourceClient<J> client(String requestParent, Class<J> type) {
        return clients.client(requestParent, type);
    }

    @Override
    public AppLink create(AppLinkPostRequest request) {
        return client("AppLinks", AppLink.class).one("CreateAppLink", Map.of(), request);
    }

    @Override
    public AppLink getByUuid(String appLinkId) {
        return client("AppLinks", AppLink.class).one("GetAppLinkByUuid", Map.of("appLinkId", appLinkId), null);
    }

    @Override
    public AppLink delete(String appLinkId) {
        return client("AppLinks", AppLink.class).one("DeleteAppLinkByUuid", Map.of("appLinkId", appLinkId), null);
    }

    @Override
    public AppLink update(String appLinkId, List<PatchOperation> operations) {
        return client("AppLinks", AppLink.class).patch("UpdateAppLinkByUuid", Map.of("appLinkId", appLinkId), operations, true);
    }

    @Override
    public PaginatedFilteredList<AppLink> search(FilterPropertyList filter, SortPropertyList sort) {
        return client("AppLinks", AppLink.class).search("SearchAppLinks", Map.of(), filter, sort);
    }

    @Override
    public PaginatedFilteredList<AppLink> search(FilterPropertyList filter) {
        return search(filter, null);
    }

    @Override
    public PaginatedFilteredList<AppLink> search() {
        return search(null, null);
    }

    @Override
    public AppLinkAppDomainAttachment attachAppDomain(String appLinkId, String appDomainId) {
        return client("AppLinks", AppLinkAppDomainAttachment.class).one("AttachAppDomainToAppLink", Map.of("appLinkId", appLinkId, "appDomainId", appDomainId), null);
    }

    @Override
    public AppLinkAppDomainAttachment detachAppDomain(String appLinkId, String appDomainId) {
        return client("AppLinks", AppLinkAppDomainAttachment.class).one("DetachAppDomainFromAppLink", Map.of("appLinkId", appLinkId, "appDomainId", appDomainId), null);
    }

    @Override
    public AppLinkAppDomainAttachment getAttachedAppDomain(String appLinkId, String appDomainId) {
        return client("AppLinks", AppLinkAppDomainAttachment.class).one("GetAttachedAppDomainByUuid", Map.of("appLinkId", appLinkId, "appDomainId", appDomainId), null);
    }

    @Override
    public PaginatedFilteredList<AppLinkAppDomainAttachment> searchAttachedAppDomains(String appLinkId, FilterPropertyList filter, SortPropertyList sort) {
        return client("AppLinks", AppLinkAppDomainAttachment.class).search("SearchAttachedAppDomains", Map.of("appLinkId", appLinkId), filter, sort);
    }

    @Override
    public PaginatedFilteredList<AppLinkAppDomainAttachment> searchAttachedAppDomains(String appLinkId, FilterPropertyList filter) {
        return searchAttachedAppDomains(appLinkId, filter, null);
    }

    @Override
    public PaginatedFilteredList<AppLinkAppDomainAttachment> searchAttachedAppDomains(String appLinkId) {
        return searchAttachedAppDomains(appLinkId, null, null);
    }

    @Override
    public PaginatedList<AppLinkAttachedAppDomain> listAttachedAppDomains(String appLinkId, AttachmentQuery query) {
        return client("AppLinks", AppLinkAttachedAppDomain.class).list("GetAttachedAppDomainsByAppLinkId", Map.of("appLinkId", appLinkId), query == null ? null : query.toQueryParams("attachmentStatus"));
    }

    @Override
    public PaginatedList<AppLinkAttachedAppDomain> listAttachedAppDomains(String appLinkId) {
        return listAttachedAppDomains(appLinkId, null);
    }

    @Override
    public AppLinkAppServiceAttachment attachAppService(String appLinkId, String appServiceId, AppLinkAttachServiceRequest request) {
        return client("AppLinks", AppLinkAppServiceAttachment.class).one("AttachAppServiceToAppLink", Map.of("appLinkId", appLinkId, "appServiceId", appServiceId), request);
    }

    @Override
    public AppLinkAppServiceAttachment detachAppService(String appLinkId, String appServiceId) {
        return client("AppLinks", AppLinkAppServiceAttachment.class).one("DetachAppServiceFromAppLink", Map.of("appLinkId", appLinkId, "appServiceId", appServiceId), null);
    }

    @Override
    public AppLinkAppServiceAttachment updateAppServiceAttachment(String appLinkId, String appServiceId, List<PatchOperation> operations) {
        return client("AppLinks", AppLinkAppServiceAttachment.class).patch("UpdateAppServiceAttachmentToAppLink", Map.of("appLinkId", appLinkId, "appServiceId", appServiceId), operations, true);
    }

    @Override
    public AppLinkAppServiceAttachment getAttachedAppService(String appLinkId, String appServiceId) {
        return client("AppLinks", AppLinkAppServiceAttachment.class).one("GetAttachedAppServiceByUuid", Map.of("appLinkId", appLinkId, "appServiceId", appServiceId), null);
    }

    @Override
    public PaginatedFilteredList<AppLinkAppServiceAttachment> searchAttachedAppServices(String appLinkId, FilterPropertyList filter, SortPropertyList sort) {
        return client("AppLinks", AppLinkAppServiceAttachment.class).search("SearchAttachedAppServices", Map.of("appLinkId", appLinkId), filter, sort);
    }

    @Override
    public PaginatedFilteredList<AppLinkAppServiceAttachment> searchAttachedAppServices(String appLinkId, FilterPropertyList filter) {
        return searchAttachedAppServices(appLinkId, filter, null);
    }

    @Override
    public PaginatedFilteredList<AppLinkAppServiceAttachment> searchAttachedAppServices(String appLinkId) {
        return searchAttachedAppServices(appLinkId, null, null);
    }

    @Override
    public PaginatedList<AppLinkAttachedAppService> listAttachedAppServices(String appLinkId, AttachmentQuery query) {
        return client("AppLinks", AppLinkAttachedAppService.class).list("GetAttachedAppServicesByAppLinkId", Map.of("appLinkId", appLinkId), query == null ? null : query.toQueryParams("attachmentStatus"));
    }

    @Override
    public PaginatedList<AppLinkAttachedAppService> listAttachedAppServices(String appLinkId) {
        return listAttachedAppServices(appLinkId, null);
    }

    @Override
    public AppLinkAppProcessorAttachment attachAppProcessor(String appLinkId, String appProcessorId, AppLinkAttachProcessorRequest request) {
        return client("AppLinks", AppLinkAppProcessorAttachment.class).one("AttachAppProcessorToAppLink", Map.of("appLinkId", appLinkId, "appProcessorId", appProcessorId), request);
    }

    @Override
    public AppLinkAppProcessorAttachment detachAppProcessor(String appLinkId, String appProcessorId) {
        return client("AppLinks", AppLinkAppProcessorAttachment.class).one("DetachAppProcessorFromAppLink", Map.of("appLinkId", appLinkId, "appProcessorId", appProcessorId), null);
    }

    @Override
    public AppLinkAppProcessorAttachment getAttachedAppProcessor(String appLinkId, String appProcessorId) {
        return client("AppLinks", AppLinkAppProcessorAttachment.class).one("GetAttachedAppProcessor", Map.of("appLinkId", appLinkId, "appProcessorId", appProcessorId), null);
    }

    @Override
    public PaginatedFilteredList<AppLinkAppProcessorAttachment> searchAttachedAppProcessors(String appLinkId, FilterPropertyList filter, SortPropertyList sort) {
        return client("AppLinks", AppLinkAppProcessorAttachment.class).search("SearchAttachedAppProcessors", Map.of("appLinkId", appLinkId), filter, sort);
    }

    @Override
    public PaginatedFilteredList<AppLinkAppProcessorAttachment> searchAttachedAppProcessors(String appLinkId, FilterPropertyList filter) {
        return searchAttachedAppProcessors(appLinkId, filter, null);
    }

    @Override
    public PaginatedFilteredList<AppLinkAppProcessorAttachment> searchAttachedAppProcessors(String appLinkId) {
        return searchAttachedAppProcessors(appLinkId, null, null);
    }

    @Override
    public PaginatedList<AppLinkAppProcessorAttachment> listAttachedAppProcessors(String appLinkId, AttachmentQuery query) {
        return client("AppLinks", AppLinkAppProcessorAttachment.class).list("GetAttachedAppProcessorsByAppLinkId", Map.of("appLinkId", appLinkId), query == null ? null : query.toQueryParams("attachmentStatus"));
    }

    @Override
    public PaginatedList<AppLinkAppProcessorAttachment> listAttachedAppProcessors(String appLinkId) {
        return listAttachedAppProcessors(appLinkId, null);
    }
}
