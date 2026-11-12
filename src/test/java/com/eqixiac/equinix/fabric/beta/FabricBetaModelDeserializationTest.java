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

package com.eqixiac.equinix.fabric.beta;

import com.eqixiac.equinix.fabric.beta.model.AppDomain;
import com.eqixiac.equinix.fabric.beta.model.AppDomainAttachedAppLink;
import com.eqixiac.equinix.fabric.beta.model.AppDomainPostRequest;
import com.eqixiac.equinix.fabric.beta.model.AppLink;
import com.eqixiac.equinix.fabric.beta.model.AppLinkAppDomainAttachment;
import com.eqixiac.equinix.fabric.beta.model.AppLinkAppProcessorAttachment;
import com.eqixiac.equinix.fabric.beta.model.AppLinkAppServiceAttachment;
import com.eqixiac.equinix.fabric.beta.model.AppLinkAttachProcessorRequest;
import com.eqixiac.equinix.fabric.beta.model.AppLinkAttachServiceRequest;
import com.eqixiac.equinix.fabric.beta.model.AppLinkAttachedAppDomain;
import com.eqixiac.equinix.fabric.beta.model.AppLinkAttachedAppService;
import com.eqixiac.equinix.fabric.beta.model.AppLinkPostRequest;
import com.eqixiac.equinix.fabric.beta.model.AppProcessor;
import com.eqixiac.equinix.fabric.beta.model.AppService;
import com.eqixiac.equinix.fabric.beta.model.AppServiceAttachedAppLink;
import com.eqixiac.equinix.fabric.beta.model.AppServiceAttachedAppSubscription;
import com.eqixiac.equinix.fabric.beta.model.AppServicePostRequest;
import com.eqixiac.equinix.fabric.beta.model.AppSubscription;
import com.eqixiac.equinix.fabric.beta.model.AppSubscriptionPostRequest;
import com.eqixiac.equinix.fabric.beta.model.Asn;
import com.eqixiac.equinix.fabric.beta.model.BulkOpticalConnectRequest;
import com.eqixiac.equinix.fabric.beta.model.CreateAsnRequest;
import com.eqixiac.equinix.fabric.beta.model.CreateCrossConnect;
import com.eqixiac.equinix.fabric.beta.model.CreateLoa;
import com.eqixiac.equinix.fabric.beta.model.CreateLoaNote;
import com.eqixiac.equinix.fabric.beta.model.CrossConnectActionRequest;
import com.eqixiac.equinix.fabric.beta.model.CrossConnectActionResponse;
import com.eqixiac.equinix.fabric.beta.model.CrossConnectAttachmentsResponse;
import com.eqixiac.equinix.fabric.beta.model.CrossConnectDiverse;
import com.eqixiac.equinix.fabric.beta.model.CrossConnectNoteResponse;
import com.eqixiac.equinix.fabric.beta.model.CrossConnectResponse;
import com.eqixiac.equinix.fabric.beta.model.CrossConnectScheduleAvailability;
import com.eqixiac.equinix.fabric.beta.model.CrossConnectServicesResponse;
import com.eqixiac.equinix.fabric.beta.model.DiverseCrossConnectRequest;
import com.eqixiac.equinix.fabric.beta.model.ExchangeServiceActionRequest;
import com.eqixiac.equinix.fabric.beta.model.ExchangeServiceActionResponse;
import com.eqixiac.equinix.fabric.beta.model.ExchangeServiceCreateRequest;
import com.eqixiac.equinix.fabric.beta.model.ExchangeServiceResponse;
import com.eqixiac.equinix.fabric.beta.model.LoaActionRequest;
import com.eqixiac.equinix.fabric.beta.model.LoaActionResponse;
import com.eqixiac.equinix.fabric.beta.model.LoaConsumersResponse;
import com.eqixiac.equinix.fabric.beta.model.LoaNoteDetails;
import com.eqixiac.equinix.fabric.beta.model.LoaNotesResponse;
import com.eqixiac.equinix.fabric.beta.model.LoaResponse;
import com.eqixiac.equinix.fabric.beta.model.OpticalConnectBulk;
import com.eqixiac.equinix.fabric.beta.model.OpticalConnectPostRequest;
import com.eqixiac.equinix.fabric.beta.model.OpticalConnectResponse;
import com.eqixiac.equinix.fabric.beta.model.SchedulesResponse;
import com.eqixiac.equinix.core.TestFixtures;
import com.eqixiac.equinix.core.internal.Constants;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

/**
 * Reads every request and response example the Fabric v4 4.34.0 spec gives for the Beta operations into
 * its SDK model and asserts nothing is lost: each example value must survive a read and re-serialization
 * (see {@link BetaTestSupport#assertCovers}). Search and list envelopes are checked element by element
 * against their {@code data} item model, as the SDK pages them.
 */
class FabricBetaModelDeserializationTest {

    static Stream<Arguments> examples() {
        return Stream.of(
                Arguments.of("createAsn", "PublicAsnCreated", "/json/fabric/beta/asns/createAsn.response.PublicAsnCreated.json", Asn.class, ""),
                Arguments.of("createAsn", "PrivateAsnCreated", "/json/fabric/beta/asns/createAsn.response.PrivateAsnCreated.json", Asn.class, ""),
                Arguments.of("createAsn", "CreatePublicAsn", "/json/fabric/beta/asns/createAsn.request.CreatePublicAsn.json", CreateAsnRequest.class, ""),
                Arguments.of("createAsn", "CreatePrivateAsn", "/json/fabric/beta/asns/createAsn.request.CreatePrivateAsn.json", CreateAsnRequest.class, ""),
                Arguments.of("searchAsns", "SearchAsnsByProject", "/json/fabric/beta/asns/searchAsns.response.SearchAsnsByProject.json", Asn.class, "data"),
                Arguments.of("getAsnById", "PublicAsnRetrieved", "/json/fabric/beta/asns/getAsnById.response.PublicAsnRetrieved.json", Asn.class, ""),
                Arguments.of("patchAsnById", "PublicAsnCreated", "/json/fabric/beta/asns/patchAsnById.response.PublicAsnCreated.json", Asn.class, ""),
                Arguments.of("patchAsnById", "PrivateAsnCreated", "/json/fabric/beta/asns/patchAsnById.response.PrivateAsnCreated.json", Asn.class, ""),
                Arguments.of("deleteAsnById", "PublicAsnCreated", "/json/fabric/beta/asns/deleteAsnById.response.PublicAsnCreated.json", Asn.class, ""),
                Arguments.of("deleteAsnById", "PrivateAsnCreated", "/json/fabric/beta/asns/deleteAsnById.response.PrivateAsnCreated.json", Asn.class, ""),
                Arguments.of("createExchangeService", "CreateExchangeServiceResponse", "/json/fabric/beta/exchangeservices/createExchangeService.response.CreateExchangeServiceResponse.json", ExchangeServiceResponse.class, ""),
                Arguments.of("createExchangeService", "ExchangeServiceCreateRequest", "/json/fabric/beta/exchangeservices/createExchangeService.request.ExchangeServiceCreateRequest.json", ExchangeServiceCreateRequest.class, ""),
                Arguments.of("getExchangeServiceById", "GetExchangeServiceResponse", "/json/fabric/beta/exchangeservices/getExchangeServiceById.response.GetExchangeServiceResponse.json", ExchangeServiceResponse.class, ""),
                Arguments.of("updateExchangeServiceById", "PatchExchangeServiceResponse", "/json/fabric/beta/exchangeservices/updateExchangeServiceById.response.PatchExchangeServiceResponse.json", ExchangeServiceResponse.class, ""),
                Arguments.of("searchExchangeService", "example", "/json/fabric/beta/exchangeservices/searchExchangeService.response.example.json", ExchangeServiceResponse.class, "data"),
                Arguments.of("createExchangeServiceAction", "example", "/json/fabric/beta/exchangeservices/createExchangeServiceAction.response.example.json", ExchangeServiceActionResponse.class, ""),
                Arguments.of("createExchangeServiceAction", "ActivateExchangeServiceRequest", "/json/fabric/beta/exchangeservices/createExchangeServiceAction.request.ActivateExchangeServiceRequest.json", ExchangeServiceActionRequest.class, ""),
                Arguments.of("createExchangeServiceAction", "ValidateExchangeServiceRequest", "/json/fabric/beta/exchangeservices/createExchangeServiceAction.request.ValidateExchangeServiceRequest.json", ExchangeServiceActionRequest.class, ""),
                Arguments.of("createExchangeServiceAction", "RetryExchangeServiceRequest", "/json/fabric/beta/exchangeservices/createExchangeServiceAction.request.RetryExchangeServiceRequest.json", ExchangeServiceActionRequest.class, ""),
                Arguments.of("createAppDomain", "AppDomainResponse", "/json/fabric/beta/appdomains/createAppDomain.response.AppDomainResponse.json", AppDomain.class, ""),
                Arguments.of("createAppDomain", "CreateAppDomain", "/json/fabric/beta/appdomains/createAppDomain.request.CreateAppDomain.json", AppDomainPostRequest.class, ""),
                Arguments.of("getAppDomainByUuid", "AppDomainResponse", "/json/fabric/beta/appdomains/getAppDomainByUuid.response.AppDomainResponse.json", AppDomain.class, ""),
                Arguments.of("deleteAppDomainByUuid", "AppDomainResponse", "/json/fabric/beta/appdomains/deleteAppDomainByUuid.response.AppDomainResponse.json", AppDomain.class, ""),
                Arguments.of("updateAppDomainByUuid", "AppDomainResponse", "/json/fabric/beta/appdomains/updateAppDomainByUuid.response.AppDomainResponse.json", AppDomain.class, ""),
                Arguments.of("searchAppDomains", "Example", "/json/fabric/beta/appdomains/searchAppDomains.response.Example.json", AppDomain.class, "data"),
                Arguments.of("getAttachedAppLinksByAppDomainId", "Example", "/json/fabric/beta/appdomains/getAttachedAppLinksByAppDomainId.response.Example.json", AppDomainAttachedAppLink.class, "data"),
                Arguments.of("createAppLink", "AppLinkResponse", "/json/fabric/beta/applinks/createAppLink.response.AppLinkResponse.json", AppLink.class, ""),
                Arguments.of("createAppLink", "CreateAppLink", "/json/fabric/beta/applinks/createAppLink.request.CreateAppLink.json", AppLinkPostRequest.class, ""),
                Arguments.of("getAppLinkByUuid", "AppLinkResponse", "/json/fabric/beta/applinks/getAppLinkByUuid.response.AppLinkResponse.json", AppLink.class, ""),
                Arguments.of("deleteAppLinkByUuid", "AppLinkResponse", "/json/fabric/beta/applinks/deleteAppLinkByUuid.response.AppLinkResponse.json", AppLink.class, ""),
                Arguments.of("updateAppLinkByUuid", "AppLinkResponse", "/json/fabric/beta/applinks/updateAppLinkByUuid.response.AppLinkResponse.json", AppLink.class, ""),
                Arguments.of("searchAppLinks", "Example", "/json/fabric/beta/applinks/searchAppLinks.response.Example.json", AppLink.class, "data"),
                Arguments.of("attachAppDomainToAppLink", "AppLinkAttachDomainResponse", "/json/fabric/beta/applinks/attachAppDomainToAppLink.response.AppLinkAttachDomainResponse.json", AppLinkAppDomainAttachment.class, ""),
                Arguments.of("detachAppDomainFromAppLink", "AppLinkDetachDomainResponse", "/json/fabric/beta/applinks/detachAppDomainFromAppLink.response.AppLinkDetachDomainResponse.json", AppLinkAppDomainAttachment.class, ""),
                Arguments.of("getAttachedAppDomainByUuid", "AppLinkDomainResponse", "/json/fabric/beta/applinks/getAttachedAppDomainByUuid.response.AppLinkDomainResponse.json", AppLinkAppDomainAttachment.class, ""),
                Arguments.of("searchAttachedAppDomains", "Example", "/json/fabric/beta/applinks/searchAttachedAppDomains.response.Example.json", AppLinkAppDomainAttachment.class, "data"),
                Arguments.of("getAttachedAppDomainsByAppLinkId", "Example", "/json/fabric/beta/applinks/getAttachedAppDomainsByAppLinkId.response.Example.json", AppLinkAttachedAppDomain.class, "data"),
                Arguments.of("attachAppServiceToAppLink", "AppLinkResponse", "/json/fabric/beta/applinks/attachAppServiceToAppLink.response.AppLinkResponse.json", AppLinkAppServiceAttachment.class, ""),
                Arguments.of("attachAppServiceToAppLink", "AttachAppLinkToService", "/json/fabric/beta/applinks/attachAppServiceToAppLink.request.AttachAppLinkToService.json", AppLinkAttachServiceRequest.class, ""),
                Arguments.of("detachAppServiceFromAppLink", "AppLinkResponse", "/json/fabric/beta/applinks/detachAppServiceFromAppLink.response.AppLinkResponse.json", AppLinkAppServiceAttachment.class, ""),
                Arguments.of("updateAppServiceAttachmentToAppLink", "AppLinkResponse", "/json/fabric/beta/applinks/updateAppServiceAttachmentToAppLink.response.AppLinkResponse.json", AppLinkAppServiceAttachment.class, ""),
                Arguments.of("getAttachedAppServiceByUuid", "AppLinkResponse", "/json/fabric/beta/applinks/getAttachedAppServiceByUuid.response.AppLinkResponse.json", AppLinkAppServiceAttachment.class, ""),
                Arguments.of("searchAttachedAppServices", "Example", "/json/fabric/beta/applinks/searchAttachedAppServices.response.Example.json", AppLinkAppServiceAttachment.class, "data"),
                Arguments.of("getAttachedAppServicesByAppLinkId", "Example", "/json/fabric/beta/applinks/getAttachedAppServicesByAppLinkId.response.Example.json", AppLinkAttachedAppService.class, "data"),
                Arguments.of("attachAppProcessorToAppLink", "AppLinkAttachProcessorResponse", "/json/fabric/beta/applinks/attachAppProcessorToAppLink.response.AppLinkAttachProcessorResponse.json", AppLinkAppProcessorAttachment.class, ""),
                Arguments.of("attachAppProcessorToAppLink", "AttachAppProcessorToAppLink", "/json/fabric/beta/applinks/attachAppProcessorToAppLink.request.AttachAppProcessorToAppLink.json", AppLinkAttachProcessorRequest.class, ""),
                Arguments.of("detachAppProcessorFromAppLink", "AppLinkDetachProcessorResponse", "/json/fabric/beta/applinks/detachAppProcessorFromAppLink.response.AppLinkDetachProcessorResponse.json", AppLinkAppProcessorAttachment.class, ""),
                Arguments.of("getAttachedAppProcessor", "AppLinkAttachProcessorGetResponse", "/json/fabric/beta/applinks/getAttachedAppProcessor.response.AppLinkAttachProcessorGetResponse.json", AppLinkAppProcessorAttachment.class, ""),
                Arguments.of("searchAttachedAppProcessors", "Example", "/json/fabric/beta/applinks/searchAttachedAppProcessors.response.Example.json", AppLinkAppProcessorAttachment.class, "data"),
                Arguments.of("getAttachedAppProcessorsByAppLinkId", "Example", "/json/fabric/beta/applinks/getAttachedAppProcessorsByAppLinkId.response.Example.json", AppLinkAppProcessorAttachment.class, "data"),
                Arguments.of("getAppProcessorByUuid", "AppProcessorResponse", "/json/fabric/beta/appprocessors/getAppProcessorByUuid.response.AppProcessorResponse.json", AppProcessor.class, ""),
                Arguments.of("searchAppProcessors", "Example", "/json/fabric/beta/appprocessors/searchAppProcessors.response.Example.json", AppProcessor.class, "data"),
                Arguments.of("createAppService", "AppServiceResponse", "/json/fabric/beta/appservices/createAppService.response.AppServiceResponse.json", AppService.class, ""),
                Arguments.of("createAppService", "CreateAppService", "/json/fabric/beta/appservices/createAppService.request.CreateAppService.json", AppServicePostRequest.class, ""),
                Arguments.of("getAppServiceByUuid", "AppServiceResponse", "/json/fabric/beta/appservices/getAppServiceByUuid.response.AppServiceResponse.json", AppService.class, ""),
                Arguments.of("deleteAppServiceByUuid", "AppServiceResponse", "/json/fabric/beta/appservices/deleteAppServiceByUuid.response.AppServiceResponse.json", AppService.class, ""),
                Arguments.of("updateAppServiceByUuid", "AppServiceResponse", "/json/fabric/beta/appservices/updateAppServiceByUuid.response.AppServiceResponse.json", AppService.class, ""),
                Arguments.of("searchAppServices", "Example", "/json/fabric/beta/appservices/searchAppServices.response.Example.json", AppService.class, "data"),
                Arguments.of("getAttachedAppLinksByAppServiceId", "Example", "/json/fabric/beta/appservices/getAttachedAppLinksByAppServiceId.response.Example.json", AppServiceAttachedAppLink.class, "data"),
                Arguments.of("getAttachedAppSubscriptionsByAppServiceId", "Example", "/json/fabric/beta/appservices/getAttachedAppSubscriptionsByAppServiceId.response.Example.json", AppServiceAttachedAppSubscription.class, "data"),
                Arguments.of("searchAttachedAppSubscriptionsByAppServiceId", "Example", "/json/fabric/beta/appservices/searchAttachedAppSubscriptionsByAppServiceId.response.Example.json", AppServiceAttachedAppSubscription.class, "data"),
                Arguments.of("createAppSubscription", "AppSubscriptionResponse", "/json/fabric/beta/appsubscriptions/createAppSubscription.response.AppSubscriptionResponse.json", AppSubscription.class, ""),
                Arguments.of("createAppSubscription", "CreateAppSubscription", "/json/fabric/beta/appsubscriptions/createAppSubscription.request.CreateAppSubscription.json", AppSubscriptionPostRequest.class, ""),
                Arguments.of("getAppSubscriptionByUuid", "AppSubscriptionResponse", "/json/fabric/beta/appsubscriptions/getAppSubscriptionByUuid.response.AppSubscriptionResponse.json", AppSubscription.class, ""),
                Arguments.of("deleteAppSubscriptionByUuid", "AppSubscriptionResponse", "/json/fabric/beta/appsubscriptions/deleteAppSubscriptionByUuid.response.AppSubscriptionResponse.json", AppSubscription.class, ""),
                Arguments.of("updateAppSubscriptionByUuid", "AppSubscriptionResponse", "/json/fabric/beta/appsubscriptions/updateAppSubscriptionByUuid.response.AppSubscriptionResponse.json", AppSubscription.class, ""),
                Arguments.of("searchAppSubscriptions", "Example", "/json/fabric/beta/appsubscriptions/searchAppSubscriptions.response.Example.json", AppSubscription.class, "data"),
                Arguments.of("createOpticalConnect", "COLOToCOLO", "/json/fabric/beta/opticalconnects/createOpticalConnect.response.COLOToCOLO.json", OpticalConnectResponse.class, ""),
                Arguments.of("createOpticalConnect", "COLOToRemote", "/json/fabric/beta/opticalconnects/createOpticalConnect.response.COLOToRemote.json", OpticalConnectResponse.class, ""),
                Arguments.of("createOpticalConnect", "COLOToBMMR", "/json/fabric/beta/opticalconnects/createOpticalConnect.response.COLOToBMMR.json", OpticalConnectResponse.class, ""),
                Arguments.of("createOpticalConnect", "COLOToCOLO", "/json/fabric/beta/opticalconnects/createOpticalConnect.request.COLOToCOLO.json", OpticalConnectPostRequest.class, ""),
                Arguments.of("createOpticalConnect", "COLOToRemote", "/json/fabric/beta/opticalconnects/createOpticalConnect.request.COLOToRemote.json", OpticalConnectPostRequest.class, ""),
                Arguments.of("createOpticalConnect", "COLOToBMMR", "/json/fabric/beta/opticalconnects/createOpticalConnect.request.COLOToBMMR.json", OpticalConnectPostRequest.class, ""),
                Arguments.of("createBulkOpticalConnect", "COLOToCOLO", "/json/fabric/beta/opticalconnects/createBulkOpticalConnect.response.COLOToCOLO.json", OpticalConnectBulk.class, ""),
                Arguments.of("createBulkOpticalConnect", "COLOToRemote", "/json/fabric/beta/opticalconnects/createBulkOpticalConnect.response.COLOToRemote.json", OpticalConnectBulk.class, ""),
                Arguments.of("createBulkOpticalConnect", "COLOToBMMR", "/json/fabric/beta/opticalconnects/createBulkOpticalConnect.response.COLOToBMMR.json", OpticalConnectBulk.class, ""),
                Arguments.of("createBulkOpticalConnect", "COLOToCOLO", "/json/fabric/beta/opticalconnects/createBulkOpticalConnect.request.COLOToCOLO.json", BulkOpticalConnectRequest.class, ""),
                Arguments.of("createBulkOpticalConnect", "COLOToRemote", "/json/fabric/beta/opticalconnects/createBulkOpticalConnect.request.COLOToRemote.json", BulkOpticalConnectRequest.class, ""),
                Arguments.of("createBulkOpticalConnect", "COLOToBMMR", "/json/fabric/beta/opticalconnects/createBulkOpticalConnect.request.COLOToBMMR.json", BulkOpticalConnectRequest.class, ""),
                Arguments.of("getOpticalConnectByUuid", "COLOToCOLO", "/json/fabric/beta/opticalconnects/getOpticalConnectByUuid.response.COLOToCOLO.json", OpticalConnectResponse.class, ""),
                Arguments.of("getOpticalConnectByUuid", "COLOToRemote", "/json/fabric/beta/opticalconnects/getOpticalConnectByUuid.response.COLOToRemote.json", OpticalConnectResponse.class, ""),
                Arguments.of("getOpticalConnectByUuid", "COLOToBMMR", "/json/fabric/beta/opticalconnects/getOpticalConnectByUuid.response.COLOToBMMR.json", OpticalConnectResponse.class, ""),
                Arguments.of("searchOpticalConnect", "Example", "/json/fabric/beta/opticalconnects/searchOpticalConnect.response.Example.json", OpticalConnectResponse.class, "data"),
                Arguments.of("createCrossConnect", "StandardCrossConnectByPatchPanel", "/json/fabric/beta/crossconnects/createCrossConnect.response.StandardCrossConnectByPatchPanel.json", CrossConnectResponse.class, ""),
                Arguments.of("createCrossConnect", "StandardCrossConnectByLoa", "/json/fabric/beta/crossconnects/createCrossConnect.response.StandardCrossConnectByLoa.json", CrossConnectResponse.class, ""),
                Arguments.of("createCrossConnect", "ExtendedCrossConnectEquinixManaged", "/json/fabric/beta/crossconnects/createCrossConnect.response.ExtendedCrossConnectEquinixManaged.json", CrossConnectResponse.class, ""),
                Arguments.of("createCrossConnect", "ExtendedCrossConnectSelfManaged", "/json/fabric/beta/crossconnects/createCrossConnect.response.ExtendedCrossConnectSelfManaged.json", CrossConnectResponse.class, ""),
                Arguments.of("createCrossConnect", "DiverseWithExistingCrossConnect", "/json/fabric/beta/crossconnects/createCrossConnect.response.DiverseWithExistingCrossConnect.json", CrossConnectResponse.class, ""),
                Arguments.of("createCrossConnect", "StandardCrossConnect", "/json/fabric/beta/crossconnects/createCrossConnect.request.StandardCrossConnect.json", CreateCrossConnect.class, ""),
                Arguments.of("createCrossConnect", "StandardCrossConnectWithServices", "/json/fabric/beta/crossconnects/createCrossConnect.request.StandardCrossConnectWithServices.json", CreateCrossConnect.class, ""),
                Arguments.of("createCrossConnect", "StandardCrossConnectByPatchPanel", "/json/fabric/beta/crossconnects/createCrossConnect.request.StandardCrossConnectByPatchPanel.json", CreateCrossConnect.class, ""),
                Arguments.of("createCrossConnect", "StandardCrossConnectByLoa", "/json/fabric/beta/crossconnects/createCrossConnect.request.StandardCrossConnectByLoa.json", CreateCrossConnect.class, ""),
                Arguments.of("createCrossConnect", "ExtendedCrossConnectEquinixManaged", "/json/fabric/beta/crossconnects/createCrossConnect.request.ExtendedCrossConnectEquinixManaged.json", CreateCrossConnect.class, ""),
                Arguments.of("createCrossConnect", "ExtendedCrossConnectSelfManaged", "/json/fabric/beta/crossconnects/createCrossConnect.request.ExtendedCrossConnectSelfManaged.json", CreateCrossConnect.class, ""),
                Arguments.of("createCrossConnect", "DiverseWithExistingCrossConnect", "/json/fabric/beta/crossconnects/createCrossConnect.request.DiverseWithExistingCrossConnect.json", CreateCrossConnect.class, ""),
                Arguments.of("createDiverseCrossConnect", "DiverseCrossConnectPair", "/json/fabric/beta/crossconnects/createDiverseCrossConnect.response.DiverseCrossConnectPair.json", CrossConnectDiverse.class, ""),
                Arguments.of("createDiverseCrossConnect", "DiverseCrossConnectPair", "/json/fabric/beta/crossconnects/createDiverseCrossConnect.request.DiverseCrossConnectPair.json", DiverseCrossConnectRequest.class, ""),
                Arguments.of("searchCrossConnects", "example", "/json/fabric/beta/crossconnects/searchCrossConnects.response.example.json", CrossConnectResponse.class, "data"),
                Arguments.of("getCrossConnectByUuid", "StandardCrossConnectByPatchPanel", "/json/fabric/beta/crossconnects/getCrossConnectByUuid.response.StandardCrossConnectByPatchPanel.json", CrossConnectResponse.class, ""),
                Arguments.of("getCrossConnectByUuid", "StandardCrossConnectByLoa", "/json/fabric/beta/crossconnects/getCrossConnectByUuid.response.StandardCrossConnectByLoa.json", CrossConnectResponse.class, ""),
                Arguments.of("getCrossConnectByUuid", "ExtendedCrossConnectEquinixManaged", "/json/fabric/beta/crossconnects/getCrossConnectByUuid.response.ExtendedCrossConnectEquinixManaged.json", CrossConnectResponse.class, ""),
                Arguments.of("getCrossConnectByUuid", "ExtendedCrossConnectSelfManaged", "/json/fabric/beta/crossconnects/getCrossConnectByUuid.response.ExtendedCrossConnectSelfManaged.json", CrossConnectResponse.class, ""),
                Arguments.of("getCrossConnectSchedulesByUuid", "SchedulesExample", "/json/fabric/beta/crossconnects/getCrossConnectSchedulesByUuid.response.SchedulesExample.json", SchedulesResponse.class, ""),
                Arguments.of("getCrossConnectServicesByUuid", "GetCrossConnectServices", "/json/fabric/beta/crossconnects/getCrossConnectServicesByUuid.response.GetCrossConnectServices.json", CrossConnectServicesResponse.class, ""),
                Arguments.of("getCrossConnectAttachmentsByUuid", "GetCrossConnectAttachments", "/json/fabric/beta/crossconnects/getCrossConnectAttachmentsByUuid.response.GetCrossConnectAttachments.json", CrossConnectAttachmentsResponse.class, ""),
                Arguments.of("getCrossConnectNotesByUuid", "GetCrossConnectNotes", "/json/fabric/beta/crossconnects/getCrossConnectNotesByUuid.response.GetCrossConnectNotes.json", CrossConnectNoteResponse.class, "data"),
                Arguments.of("performCrossConnectAction", "SendSignatureQuote", "/json/fabric/beta/crossconnects/performCrossConnectAction.response.SendSignatureQuote.json", CrossConnectActionResponse.class, ""),
                Arguments.of("performCrossConnectAction", "AcceptProposedDateTime", "/json/fabric/beta/crossconnects/performCrossConnectAction.response.AcceptProposedDateTime.json", CrossConnectActionResponse.class, ""),
                Arguments.of("performCrossConnectAction", "RejectProposedDateTime", "/json/fabric/beta/crossconnects/performCrossConnectAction.response.RejectProposedDateTime.json", CrossConnectActionResponse.class, ""),
                Arguments.of("performCrossConnectAction", "CancelCrossConnect", "/json/fabric/beta/crossconnects/performCrossConnectAction.response.CancelCrossConnect.json", CrossConnectActionResponse.class, ""),
                Arguments.of("performCrossConnectAction", "DeinstallCrossConnect", "/json/fabric/beta/crossconnects/performCrossConnectAction.response.DeinstallCrossConnect.json", CrossConnectActionResponse.class, ""),
                Arguments.of("performCrossConnectAction", "UpdateInformation", "/json/fabric/beta/crossconnects/performCrossConnectAction.response.UpdateInformation.json", CrossConnectActionResponse.class, ""),
                Arguments.of("performCrossConnectAction", "SendSignatureQuote", "/json/fabric/beta/crossconnects/performCrossConnectAction.request.SendSignatureQuote.json", CrossConnectActionRequest.class, ""),
                Arguments.of("performCrossConnectAction", "AcceptProposedDateTime", "/json/fabric/beta/crossconnects/performCrossConnectAction.request.AcceptProposedDateTime.json", CrossConnectActionRequest.class, ""),
                Arguments.of("performCrossConnectAction", "RejectProposedDateTime", "/json/fabric/beta/crossconnects/performCrossConnectAction.request.RejectProposedDateTime.json", CrossConnectActionRequest.class, ""),
                Arguments.of("performCrossConnectAction", "CancelCrossConnect", "/json/fabric/beta/crossconnects/performCrossConnectAction.request.CancelCrossConnect.json", CrossConnectActionRequest.class, ""),
                Arguments.of("performCrossConnectAction", "DeinstallCrossConnect", "/json/fabric/beta/crossconnects/performCrossConnectAction.request.DeinstallCrossConnect.json", CrossConnectActionRequest.class, ""),
                Arguments.of("performCrossConnectAction", "UpdateInformation", "/json/fabric/beta/crossconnects/performCrossConnectAction.request.UpdateInformation.json", CrossConnectActionRequest.class, ""),
                Arguments.of("getCrossConnectActionByUuid", "SendSignatureQuote", "/json/fabric/beta/crossconnects/getCrossConnectActionByUuid.response.SendSignatureQuote.json", CrossConnectActionResponse.class, ""),
                Arguments.of("getCrossConnectActionByUuid", "AcceptProposedDateTime", "/json/fabric/beta/crossconnects/getCrossConnectActionByUuid.response.AcceptProposedDateTime.json", CrossConnectActionResponse.class, ""),
                Arguments.of("getCrossConnectActionByUuid", "RejectProposedDateTime", "/json/fabric/beta/crossconnects/getCrossConnectActionByUuid.response.RejectProposedDateTime.json", CrossConnectActionResponse.class, ""),
                Arguments.of("getCrossConnectActionByUuid", "CancelCrossConnect", "/json/fabric/beta/crossconnects/getCrossConnectActionByUuid.response.CancelCrossConnect.json", CrossConnectActionResponse.class, ""),
                Arguments.of("getCrossConnectActionByUuid", "DeinstallCrossConnect", "/json/fabric/beta/crossconnects/getCrossConnectActionByUuid.response.DeinstallCrossConnect.json", CrossConnectActionResponse.class, ""),
                Arguments.of("getCrossConnectActionByUuid", "UpdateInformation", "/json/fabric/beta/crossconnects/getCrossConnectActionByUuid.response.UpdateInformation.json", CrossConnectActionResponse.class, ""),
                Arguments.of("searchCrossConnectAction", "CrossConnectActionSearchExample", "/json/fabric/beta/crossconnects/searchCrossConnectAction.response.CrossConnectActionSearchExample.json", CrossConnectActionResponse.class, "data"),
                Arguments.of("searchCrossConnectSchedules", "SearchInstallSchedules", "/json/fabric/beta/crossconnects/searchCrossConnectSchedules.response.SearchInstallSchedules.json", CrossConnectScheduleAvailability.class, "data"),
                Arguments.of("createLoa", "ByRequestor", "/json/fabric/beta/loas/createLoa.response.ByRequestor.json", LoaResponse.class, ""),
                Arguments.of("createLoa", "ByIssuerWithCageType", "/json/fabric/beta/loas/createLoa.response.ByIssuerWithCageType.json", LoaResponse.class, ""),
                Arguments.of("createLoa", "ByIssuerWithPatchPanelPortType", "/json/fabric/beta/loas/createLoa.response.ByIssuerWithPatchPanelPortType.json", LoaResponse.class, ""),
                Arguments.of("createLoa", "ByIssuerWithPatchPanelType", "/json/fabric/beta/loas/createLoa.response.ByIssuerWithPatchPanelType.json", LoaResponse.class, ""),
                Arguments.of("createLoa", "IssueLoa-OnCageWithCageId", "/json/fabric/beta/loas/createLoa.request.IssueLoa-OnCageWithCageId.json", CreateLoa.class, ""),
                Arguments.of("createLoa", "IssueLoa-OnPatchPanelPort", "/json/fabric/beta/loas/createLoa.request.IssueLoa-OnPatchPanelPort.json", CreateLoa.class, ""),
                Arguments.of("createLoa", "IssueLoa-OnPatchPanel", "/json/fabric/beta/loas/createLoa.request.IssueLoa-OnPatchPanel.json", CreateLoa.class, ""),
                Arguments.of("createLoa", "RequestLoa-OnCage", "/json/fabric/beta/loas/createLoa.request.RequestLoa-OnCage.json", CreateLoa.class, ""),
                Arguments.of("createLoa", "RequestLoa-OnPatchPanel", "/json/fabric/beta/loas/createLoa.request.RequestLoa-OnPatchPanel.json", CreateLoa.class, ""),
                Arguments.of("createLoa", "RequestLoa-OnPatchPanelPort", "/json/fabric/beta/loas/createLoa.request.RequestLoa-OnPatchPanelPort.json", CreateLoa.class, ""),
                Arguments.of("getLoaByUuid", "LoaExample", "/json/fabric/beta/loas/getLoaByUuid.response.LoaExample.json", LoaResponse.class, ""),
                Arguments.of("updateLoaByUuid", "PatchLoaResponse", "/json/fabric/beta/loas/updateLoaByUuid.response.PatchLoaResponse.json", LoaResponse.class, ""),
                Arguments.of("performLoaAction", "AuthorizeLoa", "/json/fabric/beta/loas/performLoaAction.response.AuthorizeLoa.json", LoaActionResponse.class, ""),
                Arguments.of("performLoaAction", "AcceptLoa", "/json/fabric/beta/loas/performLoaAction.response.AcceptLoa.json", LoaActionResponse.class, ""),
                Arguments.of("performLoaAction", "SendLoa", "/json/fabric/beta/loas/performLoaAction.response.SendLoa.json", LoaActionResponse.class, ""),
                Arguments.of("performLoaAction", "CopyLoa", "/json/fabric/beta/loas/performLoaAction.response.CopyLoa.json", LoaActionResponse.class, ""),
                Arguments.of("performLoaAction", "AuthorizeLoa", "/json/fabric/beta/loas/performLoaAction.request.AuthorizeLoa.json", LoaActionRequest.class, ""),
                Arguments.of("performLoaAction", "AcceptLoa", "/json/fabric/beta/loas/performLoaAction.request.AcceptLoa.json", LoaActionRequest.class, ""),
                Arguments.of("performLoaAction", "CopyLoa", "/json/fabric/beta/loas/performLoaAction.request.CopyLoa.json", LoaActionRequest.class, ""),
                Arguments.of("performLoaAction", "SendLoa", "/json/fabric/beta/loas/performLoaAction.request.SendLoa.json", LoaActionRequest.class, ""),
                Arguments.of("performLoaAction", "CancelLoa", "/json/fabric/beta/loas/performLoaAction.request.CancelLoa.json", LoaActionRequest.class, ""),
                Arguments.of("getLoaActionsByUuid", "AuthorizeLoaActionExample", "/json/fabric/beta/loas/getLoaActionsByUuid.response.AuthorizeLoaActionExample.json", LoaActionResponse.class, ""),
                Arguments.of("searchLoa", "example", "/json/fabric/beta/loas/searchLoa.response.example.json", LoaResponse.class, "data"),
                Arguments.of("searchLoaAction", "LoaActionSearchExample", "/json/fabric/beta/loas/searchLoaAction.response.LoaActionSearchExample.json", LoaActionResponse.class, "data"),
                Arguments.of("getLoaConsumersByLoaId", "LoaConsumersExample", "/json/fabric/beta/loas/getLoaConsumersByLoaId.response.LoaConsumersExample.json", LoaConsumersResponse.class, ""),
                Arguments.of("getLoaNotesByUuid", "GetLoaNotesExample", "/json/fabric/beta/loas/getLoaNotesByUuid.response.GetLoaNotesExample.json", LoaNotesResponse.class, ""),
                Arguments.of("createLoaNoteByLoaId", "CreateLoaNoteExample", "/json/fabric/beta/loas/createLoaNoteByLoaId.response.CreateLoaNoteExample.json", LoaNoteDetails.class, ""),
                Arguments.of("createLoaNoteByLoaId", "LoaNoteRequest", "/json/fabric/beta/loas/createLoaNoteByLoaId.request.LoaNoteRequest.json", CreateLoaNote.class, "")
        );
    }

    @ParameterizedTest(name = "{0} / {1}")
    @MethodSource("examples")
    void exampleSurvivesReadAndWrite(String operationId, String example, String fixture, Class<?> model, String pointer)
            throws Exception {
        JsonNode expected = Constants.mapper().readTree(TestFixtures.load(fixture));
        if (!pointer.isEmpty()) {
            for (JsonNode item : expected.path(pointer)) {
                Object read = Constants.mapper().treeToValue(item, model);
                BetaTestSupport.assertCovers(item, read, operationId + "/" + example);
            }
            return;
        }
        Object read = Constants.mapper().treeToValue(expected, model);
        BetaTestSupport.assertCovers(expected, read, operationId + "/" + example);
    }
}
