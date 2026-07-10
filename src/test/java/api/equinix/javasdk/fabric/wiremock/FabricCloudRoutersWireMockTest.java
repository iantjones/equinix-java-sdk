package api.equinix.javasdk.fabric.wiremock;
import api.equinix.javasdk.fabric.enums.MarketplaceSubscriptionType;
import api.equinix.javasdk.fabric.enums.ChangeType;

import api.equinix.javasdk.Fabric;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.core.exception.*;
import api.equinix.javasdk.core.http.response.PaginatedFilteredList;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.fabric.enums.ChangeStatus;
import api.equinix.javasdk.fabric.enums.CloudRouterCommandType;
import api.equinix.javasdk.fabric.enums.CloudRouterPackageCode;
import api.equinix.javasdk.fabric.enums.GatewayPackageCode;
import api.equinix.javasdk.fabric.model.CloudRouter;
import api.equinix.javasdk.fabric.model.CloudRouterAction;
import api.equinix.javasdk.fabric.model.CloudRouterCommand;
import api.equinix.javasdk.fabric.model.CloudRouterPackage;
import api.equinix.javasdk.fabric.model.RouteAggregationAttachment;
import api.equinix.javasdk.fabric.model.RouteFilterAttachment;
import api.equinix.javasdk.fabric.model.RouteTableEntry;
import api.equinix.javasdk.fabric.model.RoutingProtocolValidation;
import api.equinix.javasdk.fabric.model.implementation.CloudRouterCommandRequest;
import api.equinix.javasdk.fabric.model.implementation.filter.Filter;
import api.equinix.javasdk.fabric.model.implementation.filter.FilterPropertyList;
import api.equinix.javasdk.fabric.model.implementation.sort.Sort;
import api.equinix.javasdk.fabric.model.implementation.sort.SortPropertyList;
import org.junit.jupiter.api.*;

import java.util.List;

import static api.equinix.javasdk.core.ResponseStubs.*;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * WireMock-based API tests for Fabric Cloud Routers.
 */
class FabricCloudRoutersWireMockTest extends WireMockTestBase {

    static Fabric fabric;

    @BeforeAll
    static void setUp() {
        fabric = new Fabric(testCredentials());
        redirectToWireMock(fabric);
        fabric.authenticate();
    }

    @AfterAll
    static void tearDown() throws Exception {
        if (fabric != null) fabric.close();
    }

    @BeforeEach
    void resetBeforeEach() {
        resetStubs();
    }

    @Nested
    @DisplayName("getByUuid()")
    class GetByUuid {

        @Test
        @DisplayName("returns cloud router for valid UUID")
        void returnsCloudRouter() {
            stubSingleton(wireMock, "/fabric/v4/routers/.*",
                    "/json/fabric/cloud_router_response.json");

            CloudRouter router = fabric.cloudRouters().getByUuid("a1b2c3d4-e5f6-7890-abcd-ef1234567890");
            assertNotNull(router);
            assertEquals("a1b2c3d4-e5f6-7890-abcd-ef1234567890", router.getUuid());

            // Spec-fidelity fields: equinixAsn + connectionsCount wire names.
            assertEquals(30000L, router.getEquinixAsn());
            assertEquals(5, router.getConnectionCount());

            // Nested marketplaceSubscription reference.
            assertNotNull(router.getMarketplaceSubscription());
            assertEquals("2823b8ae-b24c-4a86-9dca-4a4e797d94e7", router.getMarketplaceSubscription().getUuid());
            assertEquals(MarketplaceSubscriptionType.AWS_MARKETPLACE_SUBSCRIPTION, router.getMarketplaceSubscription().getType());

            // Latest CloudRouterChange block.
            assertNotNull(router.getChange());
            assertEquals("5c1a2b3c-4d5e-6f70-8192-a3b4c5d6e7f8", router.getChange().getUuid());
            assertEquals(ChangeType.ROUTER_UPDATE, router.getChange().getType());
            assertEquals(ChangeStatus.COMPLETED, router.getChange().getStatus());
            assertEquals("Router package updated", router.getChange().getInformation());
            assertNotNull(router.getChange().getUpdatedDateTime());
        }

        @Test
        @DisplayName("404 throws EquinixNotFoundException")
        void notFound() {
            stubErrorInline(wireMock, "/fabric/v4/routers/.*",
                    404, "[{\"errorCode\":\"ERR-404\",\"errorMessage\":\"Cloud router not found\"}]");

            assertThrows(EquinixNotFoundException.class,
                    () -> fabric.cloudRouters().getByUuid("invalid-uuid"));
        }
    }

    @Nested
    @DisplayName("define() / create()")
    class Create {

        @Test
        @DisplayName("POSTs the cloud router body to the collection and returns the created router")
        void createsCloudRouter() {
            stubCreate(wireMock, "/fabric/v4/routers", "/json/fabric/cloud_router_response.json");

            CloudRouter router = fabric.cloudRouters().define()
                    .name("My-Cloud-Router-Primary")
                    .inMetro("SV")
                    .withPackage(GatewayPackageCode.PREMIUM)
                    .order("PO-9876", 24, "CR-REF-001")
                    .create();

            assertNotNull(router);
            assertEquals("a1b2c3d4-e5f6-7890-abcd-ef1234567890", router.getUuid());
            assertEquals("My-Cloud-Router-Primary", router.getName());

            wireMock.verify(postRequestedFor(urlPathEqualTo("/fabric/v4/routers"))
                    .withHeader("Content-Type", containing("application/json"))
                    .withRequestBody(matchingJsonPath("$.type", equalTo("XF_ROUTER")))
                    .withRequestBody(matchingJsonPath("$.name", equalTo("My-Cloud-Router-Primary")))
                    .withRequestBody(matchingJsonPath("$.location.metroCode", equalTo("SV")))
                    .withRequestBody(matchingJsonPath("$.package.code", equalTo("PREMIUM")))
                    .withRequestBody(matchingJsonPath("$.order.purchaseOrderNumber", equalTo("PO-9876")))
                    .withRequestBody(matchingJsonPath("$.order.termLength", equalTo("24")))
                    .withRequestBody(matchingJsonPath("$.order.customerReferenceNumber", equalTo("CR-REF-001"))));
        }
    }

    @Nested
    @DisplayName("update() / save()")
    class Update {

        @Test
        @DisplayName("sends an RFC 6902 JSON Patch with json-patch content-type")
        void savePatchesName() {
            stubSingleton(wireMock, "/fabric/v4/routers/.*",
                    "/json/fabric/cloud_router_response.json");
            wireMock.stubFor(patch(urlPathMatching("/fabric/v4/routers/.*"))
                    .willReturn(okJson(loadFixture("/json/fabric/cloud_router_response.json"))));

            CloudRouter router = fabric.cloudRouters().getByUuid("a1b2c3d4-e5f6-7890-abcd-ef1234567890");
            CloudRouter updated = router.update().name("Renamed-Router").save();

            assertNotNull(updated);
            wireMock.verify(patchRequestedFor(urlPathMatching("/fabric/v4/routers/a1b2c3d4-e5f6-7890-abcd-ef1234567890"))
                    .withHeader("Content-Type", containing("application/json-patch+json"))
                    .withRequestBody(equalToJson(
                            "[{\"op\":\"replace\",\"path\":\"/name\",\"value\":\"Renamed-Router\"}]")));
        }

        @Test
        @DisplayName("changePackage + termLength patch /package/code and /order/termLength (R2025.6/R2026.1)")
        void savePatchesPackageAndTermLength() {
            stubSingleton(wireMock, "/fabric/v4/routers/.*",
                    "/json/fabric/cloud_router_response.json");
            wireMock.stubFor(patch(urlPathMatching("/fabric/v4/routers/.*"))
                    .willReturn(okJson(loadFixture("/json/fabric/cloud_router_response.json"))));

            CloudRouter router = fabric.cloudRouters().getByUuid("a1b2c3d4-e5f6-7890-abcd-ef1234567890");
            CloudRouter updated = router.update().changePackage(GatewayPackageCode.PREMIUM).termLength(12).save();

            assertNotNull(updated);
            wireMock.verify(patchRequestedFor(urlPathMatching("/fabric/v4/routers/a1b2c3d4-e5f6-7890-abcd-ef1234567890"))
                    .withHeader("Content-Type", containing("application/json-patch+json"))
                    .withRequestBody(equalToJson(
                            "[{\"op\":\"replace\",\"path\":\"/package/code\",\"value\":\"PREMIUM\"},"
                            + "{\"op\":\"replace\",\"path\":\"/order/termLength\",\"value\":12}]")));
        }

        @Test
        @DisplayName("save() with no changes throws and makes no request")
        void emptyUpdateThrows() {
            stubSingleton(wireMock, "/fabric/v4/routers/.*",
                    "/json/fabric/cloud_router_response.json");

            CloudRouter router = fabric.cloudRouters().getByUuid("a1b2c3d4-e5f6-7890-abcd-ef1234567890");
            assertThrows(IllegalStateException.class, () -> router.update().save());
            wireMock.verify(0, patchRequestedFor(urlPathMatching("/fabric/v4/routers/.*")));
        }
    }

    @Nested
    @DisplayName("defineCommand() / create()")
    class DefineCommand {

        @Test
        @DisplayName("POSTs the diagnostic command to the router's commands collection")
        void createsCommand() {
            wireMock.stubFor(post(urlPathEqualTo("/fabric/v4/routers/a1b2c3d4-e5f6-7890-abcd-ef1234567890/commands"))
                    .willReturn(aResponse()
                            .withStatus(201)
                            .withHeader("Content-Type", "application/json")
                            .withBody(loadFixture("/json/fabric/cloud_router_command_response.json"))));

            CloudRouterCommand command = fabric.cloudRouters()
                    .defineCommand("a1b2c3d4-e5f6-7890-abcd-ef1234567890")
                    .ofType(CloudRouterCommandType.PING_COMMAND)
                    .name("ping-to-peer")
                    .description("Ping the remote BGP peer")
                    .withRequest(CloudRouterCommandRequest.builder()
                            .destination("192.168.1.1")
                            .sourceConnection("3a58dd05-f46d-4b1d-a154-2e85c396ea85")
                            .count(5)
                            .build())
                    .create();

            assertNotNull(command);
            assertEquals("9d1e5f0a-2b3c-4d5e-8f90-a1b2c3d4e5f6", command.getUuid());
            assertEquals(CloudRouterCommandType.PING_COMMAND, command.getType());

            wireMock.verify(postRequestedFor(urlPathEqualTo("/fabric/v4/routers/a1b2c3d4-e5f6-7890-abcd-ef1234567890/commands"))
                    .withHeader("Content-Type", containing("application/json"))
                    .withRequestBody(matchingJsonPath("$.type", equalTo("PING_COMMAND")))
                    .withRequestBody(matchingJsonPath("$.name", equalTo("ping-to-peer")))
                    .withRequestBody(matchingJsonPath("$.description", equalTo("Ping the remote BGP peer")))
                    .withRequestBody(matchingJsonPath("$.request.destination", equalTo("192.168.1.1")))
                    .withRequestBody(matchingJsonPath("$.request.sourceConnection.uuid", equalTo("3a58dd05-f46d-4b1d-a154-2e85c396ea85")))
                    .withRequestBody(matchingJsonPath("$.request.count", equalTo("5"))));
        }
    }

    @Nested
    @DisplayName("deleteCommand()")
    class DeleteCommand {

        @Test
        @DisplayName("DELETEs the command by id and returns true")
        void deletesCommand() {
            // DeleteCloudRouterCommand goes through deleteOne(), which reads the deleted command from
            // the response body, so the stub returns one (a 204 would make deleteOne fail on a null body).
            wireMock.stubFor(delete(urlPathEqualTo(
                    "/fabric/v4/routers/a1b2c3d4-e5f6-7890-abcd-ef1234567890/commands/9d1e5f0a-2b3c-4d5e-8f90-a1b2c3d4e5f6"))
                    .willReturn(okJson(loadFixture("/json/fabric/cloud_router_command_response.json"))));

            Boolean deleted = fabric.cloudRouters().deleteCommand(
                    "a1b2c3d4-e5f6-7890-abcd-ef1234567890", "9d1e5f0a-2b3c-4d5e-8f90-a1b2c3d4e5f6");

            assertEquals(Boolean.TRUE, deleted);
            wireMock.verify(deleteRequestedFor(urlPathEqualTo(
                    "/fabric/v4/routers/a1b2c3d4-e5f6-7890-abcd-ef1234567890/commands/9d1e5f0a-2b3c-4d5e-8f90-a1b2c3d4e5f6")));
        }
    }

    @Nested
    @DisplayName("validateRoutingProtocol()")
    class ValidateRoutingProtocol {

        @Test
        @DisplayName("POSTs the filter to the router's validate endpoint and returns the result")
        void validatesRoutingProtocol() {
            wireMock.stubFor(post(urlPathEqualTo("/fabric/v4/routers/a1b2c3d4-e5f6-7890-abcd-ef1234567890/validate"))
                    .willReturn(okJson("{\"additionalInfo\":[{\"key\":\"status\",\"value\":\"VALID\"}]}")));

            FilterPropertyList filter = Filter.filter().and()
                    .equals("/directIpv4/equinixICLAdvertisedIP", "10.1.1.1")
                    .equals("/connection/uuid", "3a58dd05-f46d-4b1d-a154-2e85c396ea85");

            RoutingProtocolValidation validation = fabric.cloudRouters()
                    .validateRoutingProtocol("a1b2c3d4-e5f6-7890-abcd-ef1234567890", filter);

            assertNotNull(validation);
            assertNotNull(validation.getAdditionalInfo());
            assertEquals(1, validation.getAdditionalInfo().size());

            wireMock.verify(postRequestedFor(urlPathEqualTo("/fabric/v4/routers/a1b2c3d4-e5f6-7890-abcd-ef1234567890/validate"))
                    .withHeader("Content-Type", containing("application/json"))
                    .withRequestBody(equalToJson(
                            "{\"filter\":{\"and\":["
                            + "{\"property\":\"/directIpv4/equinixICLAdvertisedIP\",\"operator\":\"=\",\"values\":[\"10.1.1.1\"]},"
                            + "{\"property\":\"/connection/uuid\",\"operator\":\"=\",\"values\":[\"3a58dd05-f46d-4b1d-a154-2e85c396ea85\"]}"
                            + "]}}", true, true)));
        }
    }

    @Nested
    @DisplayName("search()")
    class Search {

        private static final String SEARCH_URL = "/fabric/v4/routers/search";

        @Test
        @DisplayName("no-arg search POSTs the default body to /routers/search and returns a filtered list")
        void searchNoArg() {
            stubPaginatedPost(wireMock, SEARCH_URL, "/json/fabric/paginated_cloud_routers.json");

            PaginatedFilteredList<CloudRouter> routers = fabric.cloudRouters().search();

            assertNotNull(routers);
            assertEquals(2, routers.size());
            assertEquals("a1b2c3d4-e5f6-7890-abcd-ef1234567890", routers.get(0).getUuid());

            // RouterPackageCode spec values STANDARD and LAB round-trip (not UNKNOWN).
            assertEquals(GatewayPackageCode.STANDARD, routers.get(0).getRouterPackage().getCode());
            assertEquals(GatewayPackageCode.LAB, routers.get(1).getRouterPackage().getCode());
            assertEquals(30000L, routers.get(0).getEquinixAsn());
            assertEquals(5, routers.get(0).getConnectionCount());

            // Default no-arg search sends an (empty) filter, no sort.
            wireMock.verify(postRequestedFor(urlPathEqualTo(SEARCH_URL))
                    .withHeader("Content-Type", containing("application/json"))
                    .withRequestBody(matchingJsonPath("$.pagination")));
        }

        @Test
        @DisplayName("search(filter) carries the filter predicate in the POST body")
        void searchWithFilter() {
            stubPaginatedPost(wireMock, SEARCH_URL, "/json/fabric/paginated_cloud_routers.json");

            FilterPropertyList filter = Filter.filter().and()
                    .equals("/name", "My-Cloud-Router-Primary")
                    .equals("/location/metroCode", "SV");

            PaginatedFilteredList<CloudRouter> routers = fabric.cloudRouters().search(filter);

            assertNotNull(routers);
            assertEquals(2, routers.size());

            wireMock.verify(postRequestedFor(urlPathEqualTo(SEARCH_URL))
                    .withRequestBody(matchingJsonPath("$.filter.and[0].property", equalTo("/name")))
                    .withRequestBody(matchingJsonPath("$.filter.and[0].values[0]", equalTo("My-Cloud-Router-Primary")))
                    .withRequestBody(matchingJsonPath("$.filter.and[1].property", equalTo("/location/metroCode")))
                    .withRequestBody(matchingJsonPath("$.filter.and[1].values[0]", equalTo("SV"))));
        }

        @Test
        @DisplayName("search(sort) carries the sort directive in the POST body")
        void searchWithSort() {
            stubPaginatedPost(wireMock, SEARCH_URL, "/json/fabric/paginated_cloud_routers.json");

            SortPropertyList sort = Sort.sort().desc("/changeLog/createdDateTime");

            PaginatedFilteredList<CloudRouter> routers = fabric.cloudRouters().search(sort);

            assertNotNull(routers);
            wireMock.verify(postRequestedFor(urlPathEqualTo(SEARCH_URL))
                    .withRequestBody(matchingJsonPath("$.sort[0].property", equalTo("/changeLog/createdDateTime")))
                    .withRequestBody(matchingJsonPath("$.sort[0].direction", equalTo("DESC"))));
        }

        @Test
        @DisplayName("search(filter, sort) carries both filter and sort in the POST body")
        void searchWithFilterAndSort() {
            stubPaginatedPost(wireMock, SEARCH_URL, "/json/fabric/paginated_cloud_routers.json");

            FilterPropertyList filter = Filter.filter().and()
                    .equals("/state", "PROVISIONED");
            SortPropertyList sort = Sort.sort().asc("/name");

            PaginatedFilteredList<CloudRouter> routers = fabric.cloudRouters().search(filter, sort);

            assertNotNull(routers);
            wireMock.verify(postRequestedFor(urlPathEqualTo(SEARCH_URL))
                    .withRequestBody(matchingJsonPath("$.filter.and[0].property", equalTo("/state")))
                    .withRequestBody(matchingJsonPath("$.filter.and[0].values[0]", equalTo("PROVISIONED")))
                    .withRequestBody(matchingJsonPath("$.sort[0].property", equalTo("/name")))
                    .withRequestBody(matchingJsonPath("$.sort[0].direction", equalTo("ASC"))));
        }
    }

    @Nested
    @DisplayName("searchRoutes()")
    class SearchRoutes {

        private static final String ROUTER_ID = "a1b2c3d4-e5f6-7890-abcd-ef1234567890";
        private static final String ROUTES_URL = "/fabric/v4/routers/" + ROUTER_ID + "/routes/search";

        @Test
        @DisplayName("searchRoutes(routerId) POSTs to /{routerId}/routes/search")
        void searchRoutesNoFilter() {
            stubPaginatedPost(wireMock, ROUTES_URL, "/json/fabric/paginated_route_table_entries.json");

            PaginatedFilteredList<RouteTableEntry> routes = fabric.cloudRouters().searchRoutes(ROUTER_ID);

            assertNotNull(routes);
            assertEquals(1, routes.size());
            assertEquals("10.0.0.0/24", routes.get(0).getPrefix());

            wireMock.verify(postRequestedFor(urlPathEqualTo(ROUTES_URL))
                    .withHeader("Content-Type", containing("application/json")));
        }

        @Test
        @DisplayName("searchRoutes(routerId, filter, sort) carries the filter and sort in the POST body")
        void searchRoutesWithFilterAndSort() {
            stubPaginatedPost(wireMock, ROUTES_URL, "/json/fabric/paginated_route_table_entries.json");

            FilterPropertyList filter = Filter.filter().and()
                    .equals("/prefix", "10.0.0.0/24");
            SortPropertyList sort = Sort.sort().desc("/localPreference");

            PaginatedFilteredList<RouteTableEntry> routes =
                    fabric.cloudRouters().searchRoutes(ROUTER_ID, filter, sort);

            assertNotNull(routes);
            wireMock.verify(postRequestedFor(urlPathEqualTo(ROUTES_URL))
                    .withRequestBody(matchingJsonPath("$.filter.and[0].property", equalTo("/prefix")))
                    .withRequestBody(matchingJsonPath("$.filter.and[0].values[0]", equalTo("10.0.0.0/24")))
                    .withRequestBody(matchingJsonPath("$.sort[0].property", equalTo("/localPreference")))
                    .withRequestBody(matchingJsonPath("$.sort[0].direction", equalTo("DESC"))));
        }
    }

    @Nested
    @DisplayName("searchActions()")
    class SearchActions {

        private static final String ROUTER_ID = "a1b2c3d4-e5f6-7890-abcd-ef1234567890";
        private static final String URL = "/fabric/v4/routers/" + ROUTER_ID + "/actions/search";

        @Test
        @DisplayName("searchActions(routerId, filter, sort) POSTs the filter/sort to /{routerId}/actions/search")
        void searchActions() {
            stubPaginatedPost(wireMock, URL, "/json/fabric/paginated_cloud_router_actions.json");

            FilterPropertyList filter = Filter.filter().and()
                    .equals("/type", "ROUTE_TABLE_ENTRY_UPDATE");
            SortPropertyList sort = Sort.sort().desc("/changeLog/createdDateTime");

            PaginatedFilteredList<CloudRouterAction> actions =
                    fabric.cloudRouters().searchActions(ROUTER_ID, filter, sort);

            assertNotNull(actions);
            assertEquals(1, actions.size());
            assertEquals("1e9414f1-763e-4c0a-86c6-0bc8336048d9", actions.get(0).getUuid());

            wireMock.verify(postRequestedFor(urlPathEqualTo(URL))
                    .withRequestBody(matchingJsonPath("$.filter.and[0].property", equalTo("/type")))
                    .withRequestBody(matchingJsonPath("$.filter.and[0].values[0]", equalTo("ROUTE_TABLE_ENTRY_UPDATE")))
                    .withRequestBody(matchingJsonPath("$.sort[0].property", equalTo("/changeLog/createdDateTime")))
                    .withRequestBody(matchingJsonPath("$.sort[0].direction", equalTo("DESC"))));
        }
    }

    @Nested
    @DisplayName("searchCommands()")
    class SearchCommands {

        private static final String ROUTER_ID = "a1b2c3d4-e5f6-7890-abcd-ef1234567890";
        private static final String URL = "/fabric/v4/routers/" + ROUTER_ID + "/commands/search";

        @Test
        @DisplayName("searchCommands(routerId, filter, sort) POSTs the filter/sort to /{routerId}/commands/search")
        void searchCommands() {
            stubPaginatedPost(wireMock, URL, "/json/fabric/paginated_cloud_router_commands.json");

            FilterPropertyList filter = Filter.filter().and()
                    .equals("/type", "PING_COMMAND");
            SortPropertyList sort = Sort.sort().asc("/name");

            PaginatedFilteredList<CloudRouterCommand> commands =
                    fabric.cloudRouters().searchCommands(ROUTER_ID, filter, sort);

            assertNotNull(commands);
            assertEquals(1, commands.size());
            assertEquals("9d1e5f0a-2b3c-4d5e-8f90-a1b2c3d4e5f6", commands.get(0).getUuid());

            wireMock.verify(postRequestedFor(urlPathEqualTo(URL))
                    .withRequestBody(matchingJsonPath("$.filter.and[0].property", equalTo("/type")))
                    .withRequestBody(matchingJsonPath("$.filter.and[0].values[0]", equalTo("PING_COMMAND")))
                    .withRequestBody(matchingJsonPath("$.sort[0].property", equalTo("/name")))
                    .withRequestBody(matchingJsonPath("$.sort[0].direction", equalTo("ASC"))));
        }
    }

    @Nested
    @DisplayName("searchRouteFilterAttachments()")
    class SearchRouteFilterAttachments {

        private static final String ROUTER_ID = "a1b2c3d4-e5f6-7890-abcd-ef1234567890";
        private static final String URL = "/fabric/v4/routers/" + ROUTER_ID + "/routeFilters/search";

        @Test
        @DisplayName("searchRouteFilterAttachments(routerId, filter, sort) POSTs to /{routerId}/routeFilters/search")
        void searchRouteFilterAttachments() {
            stubPaginatedPost(wireMock, URL, "/json/fabric/paginated_route_filter_attachments.json");

            FilterPropertyList filter = Filter.filter().and()
                    .equals("/direction", "INBOUND");
            SortPropertyList sort = Sort.sort().asc("/changeLog/createdDateTime");

            PaginatedFilteredList<RouteFilterAttachment> attachments =
                    fabric.cloudRouters().searchRouteFilterAttachments(ROUTER_ID, filter, sort);

            assertNotNull(attachments);
            assertEquals(1, attachments.size());
            assertEquals("8e1e2f3a-4b5c-6d7e-8f90-a1b2c3d4e5f6", attachments.get(0).getUuid());

            wireMock.verify(postRequestedFor(urlPathEqualTo(URL))
                    .withRequestBody(matchingJsonPath("$.filter.and[0].property", equalTo("/direction")))
                    .withRequestBody(matchingJsonPath("$.filter.and[0].values[0]", equalTo("INBOUND")))
                    .withRequestBody(matchingJsonPath("$.sort[0].property", equalTo("/changeLog/createdDateTime")))
                    .withRequestBody(matchingJsonPath("$.sort[0].direction", equalTo("ASC"))));
        }
    }

    @Nested
    @DisplayName("searchRouteAggregationAttachments()")
    class SearchRouteAggregationAttachments {

        private static final String ROUTER_ID = "a1b2c3d4-e5f6-7890-abcd-ef1234567890";
        private static final String URL = "/fabric/v4/routers/" + ROUTER_ID + "/routeAggregations/search";

        @Test
        @DisplayName("searchRouteAggregationAttachments(routerId, filter, sort) POSTs to /{routerId}/routeAggregations/search")
        void searchRouteAggregationAttachments() {
            stubPaginatedPost(wireMock, URL, "/json/fabric/paginated_route_aggregation_attachments.json");

            FilterPropertyList filter = Filter.filter().and()
                    .equals("/attachmentStatus", "ATTACHED");
            SortPropertyList sort = Sort.sort().desc("/changeLog/createdDateTime");

            PaginatedFilteredList<RouteAggregationAttachment> attachments =
                    fabric.cloudRouters().searchRouteAggregationAttachments(ROUTER_ID, filter, sort);

            assertNotNull(attachments);
            assertEquals(1, attachments.size());
            assertEquals("7d0e1f2a-3b4c-5d6e-7f80-91a2b3c4d5e6", attachments.get(0).getUuid());

            wireMock.verify(postRequestedFor(urlPathEqualTo(URL))
                    .withRequestBody(matchingJsonPath("$.filter.and[0].property", equalTo("/attachmentStatus")))
                    .withRequestBody(matchingJsonPath("$.filter.and[0].values[0]", equalTo("ATTACHED")))
                    .withRequestBody(matchingJsonPath("$.sort[0].property", equalTo("/changeLog/createdDateTime")))
                    .withRequestBody(matchingJsonPath("$.sort[0].direction", equalTo("DESC"))));
        }
    }

    @Nested
    @DisplayName("routerPackages() / routerPackageByCode()")
    class Packages {

        @Test
        @DisplayName("routerPackages() GETs /routers/routerPackages and returns the list")
        void listPackages() {
            stubPaginatedGet(wireMock, "/fabric/v4/routerPackages",
                    "/json/fabric/paginated_cloud_router_packages.json");

            PaginatedList<CloudRouterPackage> packages = fabric.cloudRouters().routerPackages();

            assertNotNull(packages);
            assertEquals(2, packages.size());
            assertEquals(CloudRouterPackageCode.BASIC, packages.get(0).getCode());

            wireMock.verify(getRequestedFor(urlPathEqualTo("/fabric/v4/routerPackages")));
        }

        @Test
        @DisplayName("routerPackageByCode(code) GETs /routers/routerPackages/{code}")
        void getPackageByCode() {
            stubSingleton(wireMock, "/fabric/v4/routerPackages/.*",
                    "/json/fabric/cloud_router_package_response.json");

            CloudRouterPackage pkg = fabric.cloudRouters().routerPackageByCode(CloudRouterPackageCode.PREMIUM);

            assertNotNull(pkg);
            assertEquals(CloudRouterPackageCode.PREMIUM, pkg.getCode());

            wireMock.verify(getRequestedFor(urlPathEqualTo("/fabric/v4/routerPackages/PREMIUM")));
        }
    }

    @Nested
    @DisplayName("commands() [list]")
    class ListCommands {

        private static final String ROUTER_ID = "a1b2c3d4-e5f6-7890-abcd-ef1234567890";

        @Test
        @DisplayName("commands(routerId) GETs /{routerId}/commands and returns the list")
        void listCommands() {
            stubPaginatedGet(wireMock, "/fabric/v4/routers/" + ROUTER_ID + "/commands",
                    "/json/fabric/paginated_cloud_router_commands.json");

            PaginatedList<CloudRouterCommand> commands = fabric.cloudRouters().commands(ROUTER_ID);

            assertNotNull(commands);
            assertEquals(1, commands.size());
            assertEquals("9d1e5f0a-2b3c-4d5e-8f90-a1b2c3d4e5f6", commands.get(0).getUuid());

            wireMock.verify(getRequestedFor(urlPathEqualTo("/fabric/v4/routers/" + ROUTER_ID + "/commands")));
        }
    }

    @Nested
    @DisplayName("getActions() [list]")
    class ListActions {

        private static final String ROUTER_ID = "a1b2c3d4-e5f6-7890-abcd-ef1234567890";

        @Test
        @DisplayName("getActions(routerId) GETs /{routerId}/actions and returns the list")
        void listActions() {
            stubPaginatedGet(wireMock, "/fabric/v4/routers/" + ROUTER_ID + "/actions",
                    "/json/fabric/paginated_cloud_router_actions.json");

            List<CloudRouterAction> actions = fabric.cloudRouters().getActions(ROUTER_ID);

            assertNotNull(actions);
            assertEquals(1, actions.size());
            assertEquals("1e9414f1-763e-4c0a-86c6-0bc8336048d9", actions.get(0).getUuid());

            wireMock.verify(getRequestedFor(urlPathEqualTo("/fabric/v4/routers/" + ROUTER_ID + "/actions")));
        }
    }

    @Nested
    @DisplayName("getCommand() / getAction() [get-by-id]")
    class GetById {

        private static final String ROUTER_ID = "a1b2c3d4-e5f6-7890-abcd-ef1234567890";
        private static final String COMMAND_ID = "9d1e5f0a-2b3c-4d5e-8f90-a1b2c3d4e5f6";
        private static final String ACTION_ID = "1e9414f1-763e-4c0a-86c6-0bc8336048d9";

        @Test
        @DisplayName("getCommand(routerId, commandId) GETs /{routerId}/commands/{commandId}")
        void getCommand() {
            stubSingleton(wireMock, "/fabric/v4/routers/.*/commands/.*",
                    "/json/fabric/cloud_router_command_response.json");

            CloudRouterCommand command = fabric.cloudRouters().getCommand(ROUTER_ID, COMMAND_ID);

            assertNotNull(command);
            assertEquals(COMMAND_ID, command.getUuid());

            wireMock.verify(getRequestedFor(urlPathEqualTo(
                    "/fabric/v4/routers/" + ROUTER_ID + "/commands/" + COMMAND_ID)));
        }

        @Test
        @DisplayName("getAction(routerId, uuid) GETs /{routerId}/actions/{uuid}")
        void getAction() {
            stubSingleton(wireMock, "/fabric/v4/routers/.*/actions/.*",
                    "/json/fabric/cloud_router_action_response.json");

            CloudRouterAction action = fabric.cloudRouters().getAction(ROUTER_ID, ACTION_ID);

            assertNotNull(action);
            // The singleton fixture carries its own uuid; the request path is what we assert on.
            assertEquals("557400f8-d360-11e9-bb65-2a2ae2dbcce4", action.getUuid());

            wireMock.verify(getRequestedFor(urlPathEqualTo(
                    "/fabric/v4/routers/" + ROUTER_ID + "/actions/" + ACTION_ID)));
        }
    }

    @Nested
    @DisplayName("Error handling")
    class Errors {

        @Test
        @DisplayName("401 throws EquinixAuthenticationException")
        void unauthorized() {
            stubErrorInline(wireMock, "/fabric/v4/routers/.*",
                    401, "[{\"errorCode\":\"ERR-401\",\"errorMessage\":\"Unauthorized\"}]");

            assertThrows(EquinixAuthenticationException.class,
                    () -> fabric.cloudRouters().getByUuid("test-uuid"));
        }

        @Test
        @DisplayName("500 throws EquinixServerException")
        void serverError() {
            stubErrorInline(wireMock, "/fabric/v4/routers/.*",
                    500, "[{\"errorCode\":\"ERR-500\",\"errorMessage\":\"Internal server error\"}]");

            assertThrows(EquinixServerException.class,
                    () -> fabric.cloudRouters().getByUuid("test-uuid"));
        }
    }

    @Nested
    @DisplayName("Wrapper refresh()")
    class WrapperRefresh {

        private static final String ROUTER_ID = "a1b2c3d4-e5f6-7890-abcd-ef1234567890";
        private static final String URL = "/fabric/v4/routers/" + ROUTER_ID;

        @Test
        @DisplayName("re-GETs /routers/{uuid} and swaps the wrapper's state in place")
        void refreshReloadsInPlace() {
            wireMock.stubFor(get(urlPathEqualTo(URL))
                    .inScenario("router-refresh")
                    .whenScenarioStateIs(com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED)
                    .willReturn(okJson(loadFixture("/json/fabric/cloud_router_response.json")))
                    .willSetStateTo("renamed"));
            wireMock.stubFor(get(urlPathEqualTo(URL))
                    .inScenario("router-refresh")
                    .whenScenarioStateIs("renamed")
                    .willReturn(okJson(loadFixture("/json/fabric/cloud_router_response.json")
                            .replace("My-Cloud-Router-Primary", "My-Cloud-Router-Renamed"))));

            CloudRouter router = fabric.cloudRouters().getByUuid(ROUTER_ID);
            assertEquals("My-Cloud-Router-Primary", router.getName());

            router.refresh();

            assertEquals("My-Cloud-Router-Renamed", router.getName(),
                    "refresh() must swap the wrapper's backing state in place");
            wireMock.verify(2, getRequestedFor(urlPathEqualTo(URL)));
        }
    }

    @Nested
    @DisplayName("Wrapper delete()")
    class WrapperDelete {

        private static final String ROUTER_ID = "a1b2c3d4-e5f6-7890-abcd-ef1234567890";

        @Test
        @DisplayName("DELETEs /routers/{uuid} (the router itself, not just commands) and returns true")
        void deletesCloudRouter() {
            stubSingleton(wireMock, "/fabric/v4/routers/" + ROUTER_ID,
                    "/json/fabric/cloud_router_response.json");
            // deleteOne() reads the deleted resource from the response body, so the stub returns one.
            wireMock.stubFor(delete(urlPathEqualTo("/fabric/v4/routers/" + ROUTER_ID))
                    .willReturn(okJson(loadFixture("/json/fabric/cloud_router_response.json"))));

            CloudRouter router = fabric.cloudRouters().getByUuid(ROUTER_ID);
            Boolean deleted = router.delete();

            assertEquals(Boolean.TRUE, deleted);
            wireMock.verify(deleteRequestedFor(urlPathEqualTo("/fabric/v4/routers/" + ROUTER_ID)));
        }
    }

    @Nested
    @DisplayName("Multi-page search paging")
    class Paging {

        private static final String PAGE_1 = """
                {
                  "pagination": { "offset": 0, "limit": 100, "total": 150 },
                  "data": [ { "uuid": "PAGE1_ROUTER" } ]
                }
                """;

        private static final String PAGE_2 = """
                {
                  "pagination": { "offset": 100, "limit": 100, "total": 150 },
                  "data": [ { "uuid": "PAGE2_ROUTER" } ]
                }
                """;

        @Test
        @DisplayName("loadAll() re-POSTs the search with the body's pagination offset advanced to page 2")
        void loadAllFetchesSecondPage() {
            wireMock.stubFor(post(urlPathEqualTo("/fabric/v4/routers/search"))
                    .withRequestBody(matchingJsonPath("$.pagination.offset", equalTo("0")))
                    .willReturn(okJson(PAGE_1)));
            wireMock.stubFor(post(urlPathEqualTo("/fabric/v4/routers/search"))
                    .withRequestBody(matchingJsonPath("$.pagination.offset", equalTo("100")))
                    .willReturn(okJson(PAGE_2)));

            PaginatedFilteredList<CloudRouter> routers = fabric.cloudRouters().search();
            assertEquals(1, routers.size());
            assertTrue(routers.hasNextPage());

            routers.loadAll();

            assertEquals(2, routers.size());
            assertEquals("PAGE1_ROUTER", routers.get(0).getUuid());
            assertEquals("PAGE2_ROUTER", routers.get(1).getUuid());
            assertFalse(routers.hasNextPage());

            wireMock.verify(1, postRequestedFor(urlPathEqualTo("/fabric/v4/routers/search"))
                    .withRequestBody(matchingJsonPath("$.pagination.offset", equalTo("100"))));
        }
    }
}
