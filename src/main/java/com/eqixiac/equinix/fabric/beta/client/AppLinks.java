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

package com.eqixiac.equinix.fabric.beta.client;

import com.eqixiac.equinix.fabric.beta.Beta;
import com.eqixiac.equinix.core.http.request.PatchOperation;
import com.eqixiac.equinix.core.http.response.PaginatedFilteredList;
import com.eqixiac.equinix.core.http.response.PaginatedList;
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

/**
 * Application Connect links and their domain, service and processor attachments (21 operations).
 *
 * <p>Obtain it from {@code fabric.beta().appLinks()}. Every operation maps one Fabric v4 endpoint; the
 * Javadoc of each method names the path and the spec operation id.</p>
 * <p><strong>Beta</strong>: Fabric v4 4.34.0 (spec read 2026-11-11).</p>
 * <p>Generated from the Fabric v4 OpenAPI spec by the fabric-beta generator; regenerate rather than hand-edit.</p>
 */
@Beta
public interface AppLinks {

    /**
     * Create Link: {@code POST /fabric/v4/appLinks} (operation {@code createAppLink}).
     * <p>This API provides capability to create user's Link</p>
     *
     * @param request the {@code AppLinkPostRequest} request body
     * @return the {@code AppLink} the API returns
     */
    AppLink create(AppLinkPostRequest request);

    /**
     * Get Link: {@code GET /fabric/v4/appLinks/{appLinkId}} (operation {@code getAppLinkByUuid}).
     * <p>This API provides capability to retrieve user's Link</p>
     *
     * @param appLinkId the {@code appLinkId} path parameter
     * @return the {@code AppLink} the API returns
     */
    AppLink getByUuid(String appLinkId);

    /**
     * Delete Link: {@code DELETE /fabric/v4/appLinks/{appLinkId}} (operation {@code deleteAppLinkByUuid}).
     * <p>This API provides capability to delete user's Link</p>
     *
     * @param appLinkId the {@code appLinkId} path parameter
     * @return the {@code AppLink} the API returns
     */
    AppLink delete(String appLinkId);

    /**
     * Update Link: {@code PATCH /fabric/v4/appLinks/{appLinkId}} (operation {@code updateAppLinkByUuid}).
     * <p>This API provides capability to update user's Link</p>
     * <p>Sent as {@code application/json-patch+json}, per the spec. Patchable paths named by the spec: {@code /bandwidth}, {@code /description}, {@code /ipv4Subnet}, {@code /name}.</p>
     *
     * @param appLinkId the {@code appLinkId} path parameter
     * @param operations the patch operations ({@code op}/{@code path}/{@code value})
     * @return the {@code AppLink} the API returns
     */
    AppLink update(String appLinkId, List<PatchOperation> operations);

    /**
     * Search Links: {@code POST /fabric/v4/appLinks/search} (operation {@code searchAppLinks}).
     * <p>The API provides capability to get list of user's App Links using search criteria, including optional filtering, pagination and sorting</p>
     * <p>Filter properties named by the spec: {@code /bandwidth}, {@code /changeLog/createdDateTime}, {@code /changeLog/deletedDateTime}, {@code /changeLog/updatedDateTime}, {@code /description}, {@code /ipv4Subnet}, {@code /name}, {@code /project/projectId}, {@code /state}, {@code /uuid}.</p>
     * <p>Sort properties named by the spec: {@code /changeLog/createdDateTime}, {@code /changeLog/updatedDateTime}, {@code /name}, {@code /state}, {@code /uuid}.</p>
     *
     * @param filter the filter ({@code null} for none)
     * @param sort the sort order ({@code null} for the service default)
     * @return a lazily paged list of {@code AppLink}
     */
    PaginatedFilteredList<AppLink> search(FilterPropertyList filter, SortPropertyList sort);

    /**
     * Search Links: {@code POST /fabric/v4/appLinks/search} (operation {@code searchAppLinks}).
     * <p>The API provides capability to get list of user's App Links using search criteria, including optional filtering, pagination and sorting</p>
     *
     * @param filter the filter ({@code null} for none)
     * @return a lazily paged list of {@code AppLink}
     */
    PaginatedFilteredList<AppLink> search(FilterPropertyList filter);

    /**
     * Search Links: {@code POST /fabric/v4/appLinks/search} (operation {@code searchAppLinks}).
     * <p>The API provides capability to get list of user's App Links using search criteria, including optional filtering, pagination and sorting</p>
     *
     * @return a lazily paged list of {@code AppLink}
     */
    PaginatedFilteredList<AppLink> search();

    /**
     * Attach Domain to Link: {@code PUT /fabric/v4/appLinks/{appLinkId}/appDomains/{appDomainId}} (operation {@code attachAppDomainToAppLink}).
     * <p>This API provides ability to attach the user's App Domain to App Link</p>
     *
     * @param appLinkId the {@code appLinkId} path parameter
     * @param appDomainId the {@code appDomainId} path parameter
     * @return the {@code AppLinkAppDomainAttachment} the API returns
     */
    AppLinkAppDomainAttachment attachAppDomain(String appLinkId, String appDomainId);

    /**
     * Detach Domain from Link: {@code DELETE /fabric/v4/appLinks/{appLinkId}/appDomains/{appDomainId}} (operation {@code detachAppDomainFromAppLink}).
     * <p>This API provides ability to detach App Domain from App Link</p>
     *
     * @param appLinkId the {@code appLinkId} path parameter
     * @param appDomainId the {@code appDomainId} path parameter
     * @return the {@code AppLinkAppDomainAttachment} the API returns
     */
    AppLinkAppDomainAttachment detachAppDomain(String appLinkId, String appDomainId);

    /**
     * Get Attached Domain for Link: {@code GET /fabric/v4/appLinks/{appLinkId}/appDomains/{appDomainId}} (operation {@code getAttachedAppDomainByUuid}).
     * <p>This API provides ability to retrieve an App Domain attached to an App Link</p>
     *
     * @param appLinkId the {@code appLinkId} path parameter
     * @param appDomainId the {@code appDomainId} path parameter
     * @return the {@code AppLinkAppDomainAttachment} the API returns
     */
    AppLinkAppDomainAttachment getAttachedAppDomain(String appLinkId, String appDomainId);

    /**
     * Search Attached Domains for Link: {@code POST /fabric/v4/appLinks/{appLinkId}/appDomains/search} (operation {@code searchAttachedAppDomains}).
     * <p>The API provides capability to get list of user's attached App Domains using search criteria, including optional filtering, pagination and sorting</p>
     * <p>Filter properties named by the spec: {@code /attachmentStatus}, {@code /uuid}.</p>
     * <p>Sort properties named by the spec: {@code /attachmentStatus}, {@code /changeLog/createdDateTime}, {@code /changeLog/updatedDateTime}, {@code /uuid}.</p>
     *
     * @param appLinkId the {@code appLinkId} path parameter
     * @param filter the filter ({@code null} for none)
     * @param sort the sort order ({@code null} for the service default)
     * @return a lazily paged list of {@code AppLinkAppDomainAttachment}
     */
    PaginatedFilteredList<AppLinkAppDomainAttachment> searchAttachedAppDomains(String appLinkId, FilterPropertyList filter, SortPropertyList sort);

    /**
     * Search Attached Domains for Link: {@code POST /fabric/v4/appLinks/{appLinkId}/appDomains/search} (operation {@code searchAttachedAppDomains}).
     * <p>The API provides capability to get list of user's attached App Domains using search criteria, including optional filtering, pagination and sorting</p>
     *
     * @param appLinkId the {@code appLinkId} path parameter
     * @param filter the filter ({@code null} for none)
     * @return a lazily paged list of {@code AppLinkAppDomainAttachment}
     */
    PaginatedFilteredList<AppLinkAppDomainAttachment> searchAttachedAppDomains(String appLinkId, FilterPropertyList filter);

    /**
     * Search Attached Domains for Link: {@code POST /fabric/v4/appLinks/{appLinkId}/appDomains/search} (operation {@code searchAttachedAppDomains}).
     * <p>The API provides capability to get list of user's attached App Domains using search criteria, including optional filtering, pagination and sorting</p>
     *
     * @param appLinkId the {@code appLinkId} path parameter
     * @return a lazily paged list of {@code AppLinkAppDomainAttachment}
     */
    PaginatedFilteredList<AppLinkAppDomainAttachment> searchAttachedAppDomains(String appLinkId);

    /**
     * Get Attached Domains for Link: {@code GET /fabric/v4/appLinks/{appLinkId}/appDomains} (operation {@code getAttachedAppDomainsByAppLinkId}).
     * <p>This API provides capability to retrieve App Domains attached to an App Link</p>
     * <p>Paged with {@code offset}/{@code limit}; {@code attachmentStatus} filters by state.</p>
     *
     * @param appLinkId the {@code appLinkId} path parameter
     * @param query optional {@code attachmentStatus}/{@code order}/{@code style} query parameters ({@code null} for none)
     * @return a lazily paged list of {@code AppLinkAttachedAppDomain}
     */
    PaginatedList<AppLinkAttachedAppDomain> listAttachedAppDomains(String appLinkId, AttachmentQuery query);

    /**
     * Get Attached Domains for Link: {@code GET /fabric/v4/appLinks/{appLinkId}/appDomains} (operation {@code getAttachedAppDomainsByAppLinkId}).
     * <p>This API provides capability to retrieve App Domains attached to an App Link</p>
     *
     * @param appLinkId the {@code appLinkId} path parameter
     * @return a lazily paged list of {@code AppLinkAttachedAppDomain}
     */
    PaginatedList<AppLinkAttachedAppDomain> listAttachedAppDomains(String appLinkId);

    /**
     * Attach Service to Link: {@code PUT /fabric/v4/appLinks/{appLinkId}/appServices/{appServiceId}} (operation {@code attachAppServiceToAppLink}).
     * <p>This API provides ability to attach the user's App Service to App Link</p>
     *
     * @param appLinkId the {@code appLinkId} path parameter
     * @param appServiceId the {@code appServiceId} path parameter
     * @param request the {@code AppLinkAttachServiceRequest} request body
     * @return the {@code AppLinkAppServiceAttachment} the API returns
     */
    AppLinkAppServiceAttachment attachAppService(String appLinkId, String appServiceId, AppLinkAttachServiceRequest request);

    /**
     * Detach Service from Link: {@code DELETE /fabric/v4/appLinks/{appLinkId}/appServices/{appServiceId}} (operation {@code detachAppServiceFromAppLink}).
     * <p>This API provides ability to detach App Service from App Link</p>
     *
     * @param appLinkId the {@code appLinkId} path parameter
     * @param appServiceId the {@code appServiceId} path parameter
     * @return the {@code AppLinkAppServiceAttachment} the API returns
     */
    AppLinkAppServiceAttachment detachAppService(String appLinkId, String appServiceId);

    /**
     * Update Service Attachment for Link: {@code PATCH /fabric/v4/appLinks/{appLinkId}/appServices/{appServiceId}} (operation {@code updateAppServiceAttachmentToAppLink}).
     * <p>This API provides ability to update the App Service attachment to App Link</p>
     * <p>Sent as {@code application/json-patch+json}, per the spec. Patchable paths named by the spec: {@code /destinationIp}.</p>
     *
     * @param appLinkId the {@code appLinkId} path parameter
     * @param appServiceId the {@code appServiceId} path parameter
     * @param operations the patch operations ({@code op}/{@code path}/{@code value})
     * @return the {@code AppLinkAppServiceAttachment} the API returns
     */
    AppLinkAppServiceAttachment updateAppServiceAttachment(String appLinkId, String appServiceId, List<PatchOperation> operations);

    /**
     * Get Attached Service for Link: {@code GET /fabric/v4/appLinks/{appLinkId}/appServices/{appServiceId}} (operation {@code getAttachedAppServiceByUuid}).
     * <p>This API provides ability to retrieve an App Service attached to an App Link</p>
     *
     * @param appLinkId the {@code appLinkId} path parameter
     * @param appServiceId the {@code appServiceId} path parameter
     * @return the {@code AppLinkAppServiceAttachment} the API returns
     */
    AppLinkAppServiceAttachment getAttachedAppService(String appLinkId, String appServiceId);

    /**
     * Search Attached Services for Link: {@code POST /fabric/v4/appLinks/{appLinkId}/appServices/search} (operation {@code searchAttachedAppServices}).
     * <p>The API provides capability to get list of user's attached App Services using search criteria, including optional filtering, pagination and sorting</p>
     * <p>Filter properties named by the spec: {@code /attachmentStatus}, {@code /uuid}.</p>
     * <p>Sort properties named by the spec: {@code /attachmentStatus}, {@code /changeLog/createdDateTime}, {@code /changeLog/updatedDateTime}, {@code /uuid}.</p>
     *
     * @param appLinkId the {@code appLinkId} path parameter
     * @param filter the filter ({@code null} for none)
     * @param sort the sort order ({@code null} for the service default)
     * @return a lazily paged list of {@code AppLinkAppServiceAttachment}
     */
    PaginatedFilteredList<AppLinkAppServiceAttachment> searchAttachedAppServices(String appLinkId, FilterPropertyList filter, SortPropertyList sort);

    /**
     * Search Attached Services for Link: {@code POST /fabric/v4/appLinks/{appLinkId}/appServices/search} (operation {@code searchAttachedAppServices}).
     * <p>The API provides capability to get list of user's attached App Services using search criteria, including optional filtering, pagination and sorting</p>
     *
     * @param appLinkId the {@code appLinkId} path parameter
     * @param filter the filter ({@code null} for none)
     * @return a lazily paged list of {@code AppLinkAppServiceAttachment}
     */
    PaginatedFilteredList<AppLinkAppServiceAttachment> searchAttachedAppServices(String appLinkId, FilterPropertyList filter);

    /**
     * Search Attached Services for Link: {@code POST /fabric/v4/appLinks/{appLinkId}/appServices/search} (operation {@code searchAttachedAppServices}).
     * <p>The API provides capability to get list of user's attached App Services using search criteria, including optional filtering, pagination and sorting</p>
     *
     * @param appLinkId the {@code appLinkId} path parameter
     * @return a lazily paged list of {@code AppLinkAppServiceAttachment}
     */
    PaginatedFilteredList<AppLinkAppServiceAttachment> searchAttachedAppServices(String appLinkId);

    /**
     * Get Attached Services for Link: {@code GET /fabric/v4/appLinks/{appLinkId}/appServices} (operation {@code getAttachedAppServicesByAppLinkId}).
     * <p>This API provides capability to retrieve App Services attached to an App Link</p>
     * <p>Paged with {@code offset}/{@code limit}; {@code attachmentStatus} filters by state.</p>
     *
     * @param appLinkId the {@code appLinkId} path parameter
     * @param query optional {@code attachmentStatus}/{@code order}/{@code style} query parameters ({@code null} for none)
     * @return a lazily paged list of {@code AppLinkAttachedAppService}
     */
    PaginatedList<AppLinkAttachedAppService> listAttachedAppServices(String appLinkId, AttachmentQuery query);

    /**
     * Get Attached Services for Link: {@code GET /fabric/v4/appLinks/{appLinkId}/appServices} (operation {@code getAttachedAppServicesByAppLinkId}).
     * <p>This API provides capability to retrieve App Services attached to an App Link</p>
     *
     * @param appLinkId the {@code appLinkId} path parameter
     * @return a lazily paged list of {@code AppLinkAttachedAppService}
     */
    PaginatedList<AppLinkAttachedAppService> listAttachedAppServices(String appLinkId);

    /**
     * Attach Processor to Link: {@code PUT /fabric/v4/appLinks/{appLinkId}/appProcessors/{appProcessorId}} (operation {@code attachAppProcessorToAppLink}).
     * <p>This API provides ability to attach an App Processor to an App Link.</p>
     *
     * @param appLinkId the {@code appLinkId} path parameter
     * @param appProcessorId the {@code appProcessorId} path parameter
     * @param request the {@code AppLinkAttachProcessorRequest} request body
     * @return the {@code AppLinkAppProcessorAttachment} the API returns
     */
    AppLinkAppProcessorAttachment attachAppProcessor(String appLinkId, String appProcessorId, AppLinkAttachProcessorRequest request);

    /**
     * Detach App Processor from App Link: {@code DELETE /fabric/v4/appLinks/{appLinkId}/appProcessors/{appProcessorId}} (operation {@code detachAppProcessorFromAppLink}).
     * <p>This API provides ability to detach an App Processor from an App Link.</p>
     *
     * @param appLinkId the {@code appLinkId} path parameter
     * @param appProcessorId the {@code appProcessorId} path parameter
     * @return the {@code AppLinkAppProcessorAttachment} the API returns
     */
    AppLinkAppProcessorAttachment detachAppProcessor(String appLinkId, String appProcessorId);

    /**
     * Get attached App Processor for App Link: {@code GET /fabric/v4/appLinks/{appLinkId}/appProcessors/{appProcessorId}} (operation {@code getAttachedAppProcessor}).
     * <p>This API provides ability to get an App Processor attached to an App Link.</p>
     *
     * @param appLinkId the {@code appLinkId} path parameter
     * @param appProcessorId the {@code appProcessorId} path parameter
     * @return the {@code AppLinkAppProcessorAttachment} the API returns
     */
    AppLinkAppProcessorAttachment getAttachedAppProcessor(String appLinkId, String appProcessorId);

    /**
     * Search attached Processors to Link: {@code POST /fabric/v4/appLinks/{appLinkId}/appProcessors/search} (operation {@code searchAttachedAppProcessors}).
     * <p>The API provides capability to get list of App Processors attached to an App Link using search criteria, including optional filtering, pagination and sorting</p>
     * <p>Filter properties named by the spec: {@code /attachmentStatus}, {@code /uuid}.</p>
     * <p>Sort properties named by the spec: {@code /attachmentStatus}, {@code /changeLog/createdDateTime}, {@code /changeLog/updatedDateTime}, {@code /uuid}.</p>
     *
     * @param appLinkId the {@code appLinkId} path parameter
     * @param filter the filter ({@code null} for none)
     * @param sort the sort order ({@code null} for the service default)
     * @return a lazily paged list of {@code AppLinkAppProcessorAttachment}
     */
    PaginatedFilteredList<AppLinkAppProcessorAttachment> searchAttachedAppProcessors(String appLinkId, FilterPropertyList filter, SortPropertyList sort);

    /**
     * Search attached Processors to Link: {@code POST /fabric/v4/appLinks/{appLinkId}/appProcessors/search} (operation {@code searchAttachedAppProcessors}).
     * <p>The API provides capability to get list of App Processors attached to an App Link using search criteria, including optional filtering, pagination and sorting</p>
     *
     * @param appLinkId the {@code appLinkId} path parameter
     * @param filter the filter ({@code null} for none)
     * @return a lazily paged list of {@code AppLinkAppProcessorAttachment}
     */
    PaginatedFilteredList<AppLinkAppProcessorAttachment> searchAttachedAppProcessors(String appLinkId, FilterPropertyList filter);

    /**
     * Search attached Processors to Link: {@code POST /fabric/v4/appLinks/{appLinkId}/appProcessors/search} (operation {@code searchAttachedAppProcessors}).
     * <p>The API provides capability to get list of App Processors attached to an App Link using search criteria, including optional filtering, pagination and sorting</p>
     *
     * @param appLinkId the {@code appLinkId} path parameter
     * @return a lazily paged list of {@code AppLinkAppProcessorAttachment}
     */
    PaginatedFilteredList<AppLinkAppProcessorAttachment> searchAttachedAppProcessors(String appLinkId);

    /**
     * Get attached Processors for Link: {@code GET /fabric/v4/appLinks/{appLinkId}/appProcessors} (operation {@code getAttachedAppProcessorsByAppLinkId}).
     * <p>This API provides capability to retrieve App Processors attached to an App Link.</p>
     * <p>Paged with {@code offset}/{@code limit}; {@code attachmentStatus} filters by state.</p>
     *
     * @param appLinkId the {@code appLinkId} path parameter
     * @param query optional {@code attachmentStatus}/{@code order}/{@code style} query parameters ({@code null} for none)
     * @return a lazily paged list of {@code AppLinkAppProcessorAttachment}
     */
    PaginatedList<AppLinkAppProcessorAttachment> listAttachedAppProcessors(String appLinkId, AttachmentQuery query);

    /**
     * Get attached Processors for Link: {@code GET /fabric/v4/appLinks/{appLinkId}/appProcessors} (operation {@code getAttachedAppProcessorsByAppLinkId}).
     * <p>This API provides capability to retrieve App Processors attached to an App Link.</p>
     *
     * @param appLinkId the {@code appLinkId} path parameter
     * @return a lazily paged list of {@code AppLinkAppProcessorAttachment}
     */
    PaginatedList<AppLinkAppProcessorAttachment> listAttachedAppProcessors(String appLinkId);
}
