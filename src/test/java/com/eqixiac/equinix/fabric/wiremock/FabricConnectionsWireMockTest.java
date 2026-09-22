package com.eqixiac.equinix.fabric.wiremock;

import com.eqixiac.equinix.Fabric;
import com.eqixiac.equinix.core.WireMockTestBase;
import com.eqixiac.equinix.core.exception.*;
import com.eqixiac.equinix.fabric.enums.AccessPointType;
import com.eqixiac.equinix.fabric.enums.ConnectionType;
import com.eqixiac.equinix.fabric.enums.Direction;
import com.eqixiac.equinix.fabric.model.implementation.LinkProtocol;
import com.eqixiac.equinix.fabric.model.implementation.SimpleAccessPoint;
import com.eqixiac.equinix.fabric.model.implementation.cloud.AwsDirectConnectAdapter;
import com.eqixiac.equinix.fabric.model.implementation.cloud.CloudProviderConnectionAdapter;
import com.eqixiac.equinix.fabric.model.implementation.cloud.CloudProviderType;
import com.eqixiac.equinix.core.http.response.PaginatedFilteredList;
import com.eqixiac.equinix.fabric.model.Connection;
import com.eqixiac.equinix.fabric.model.RouteAggregationAttachment;
import com.eqixiac.equinix.fabric.model.RouteFilterAttachment;
import com.eqixiac.equinix.fabric.model.RouteTableEntry;
import com.eqixiac.equinix.fabric.model.implementation.filter.Filter;
import com.eqixiac.equinix.fabric.model.implementation.filter.FilterPropertyList;
import com.eqixiac.equinix.fabric.model.implementation.sort.Sort;
import com.eqixiac.equinix.fabric.model.implementation.sort.SortPropertyList;
import org.junit.jupiter.api.*;

import java.util.List;

import static com.eqixiac.equinix.core.ResponseStubs.*;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * WireMock-based API tests for Fabric Connections.
 * Tests search, getByUuid, create/delete lifecycle, and error handling.
 */
class FabricConnectionsWireMockTest extends WireMockTestBase {

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
        @DisplayName("returns connection for valid UUID")
        void returnsConnection() {
            stubSingleton(wireMock, "/fabric/v4/connections/3a58dd05-f46d-4b1d-a154-2e85c396ea85",
                    "/json/fabric/connection_response.json");

            Connection connection = fabric.connections().getByUuid("3a58dd05-f46d-4b1d-a154-2e85c396ea85");
            assertNotNull(connection);
            assertEquals("3a58dd05-f46d-4b1d-a154-2e85c396ea85", connection.getUuid());
            assertEquals("My-EVPL-Connection", connection.getName());
        }

        @Test
        @DisplayName("404 throws EquinixNotFoundException")
        void notFound_throws404() {
            stubErrorInline(wireMock, "/fabric/v4/connections/invalid-uuid",
                    404, "[{\"errorCode\":\"ERR-404\",\"errorMessage\":\"Connection not found\"}]");

            assertThrows(EquinixNotFoundException.class,
                    () -> fabric.connections().getByUuid("invalid-uuid"));
        }
    }

    @Nested
    @DisplayName("update() / save()")
    class Update {

        @Test
        @DisplayName("PATCHes a JSON Patch array as application/json-patch+json")
        void savePatchesNameAndBandwidth() {
            stubSingleton(wireMock, "/fabric/v4/connections/3a58dd05-f46d-4b1d-a154-2e85c396ea85",
                    "/json/fabric/connection_response.json");
            wireMock.stubFor(patch(urlPathMatching("/fabric/v4/connections/.*"))
                    .willReturn(okJson(loadFixture("/json/fabric/connection_response.json"))));

            Connection connection = fabric.connections().getByUuid("3a58dd05-f46d-4b1d-a154-2e85c396ea85");
            Connection updated = connection.update().name("Renamed-Connection").bandwidth(200).save();

            assertNotNull(updated);
            wireMock.verify(patchRequestedFor(urlPathMatching("/fabric/v4/connections/3a58dd05-f46d-4b1d-a154-2e85c396ea85"))
                    .withHeader("Content-Type", containing("application/json-patch+json"))
                    .withRequestBody(equalToJson(
                            "[{\"op\":\"replace\",\"path\":\"/name\",\"value\":\"Renamed-Connection\"},"
                            + "{\"op\":\"replace\",\"path\":\"/bandwidth\",\"value\":200}]")));
        }

        @Test
        @DisplayName("termLength(12) patches /order/termLength (on-demand to term upgrade, R2025.5)")
        void savePatchesTermLength() {
            stubSingleton(wireMock, "/fabric/v4/connections/3a58dd05-f46d-4b1d-a154-2e85c396ea85",
                    "/json/fabric/connection_response.json");
            wireMock.stubFor(patch(urlPathMatching("/fabric/v4/connections/.*"))
                    .willReturn(okJson(loadFixture("/json/fabric/connection_response.json"))));

            Connection connection = fabric.connections().getByUuid("3a58dd05-f46d-4b1d-a154-2e85c396ea85");
            Connection updated = connection.update().termLength(12).save();

            assertNotNull(updated);
            wireMock.verify(patchRequestedFor(urlPathMatching("/fabric/v4/connections/3a58dd05-f46d-4b1d-a154-2e85c396ea85"))
                    .withHeader("Content-Type", containing("application/json-patch+json"))
                    .withRequestBody(equalToJson(
                            "[{\"op\":\"replace\",\"path\":\"/order/termLength\",\"value\":12}]")));
        }

        @Test
        @DisplayName("save() with no changes throws and makes no request")
        void emptyUpdateThrows() {
            stubSingleton(wireMock, "/fabric/v4/connections/3a58dd05-f46d-4b1d-a154-2e85c396ea85",
                    "/json/fabric/connection_response.json");

            Connection connection = fabric.connections().getByUuid("3a58dd05-f46d-4b1d-a154-2e85c396ea85");
            assertThrows(IllegalStateException.class, () -> connection.update().save());
            wireMock.verify(0, patchRequestedFor(urlPathMatching("/fabric/v4/connections/.*")));
        }

        @Test
        @DisplayName("dryRun().save() PATCHes with dryRun=true and deserializes the simulated post-update connection")
        void dryRunSendsQueryParamAndDeserializesSimulation() {
            stubSingleton(wireMock, "/fabric/v4/connections/3a58dd05-f46d-4b1d-a154-2e85c396ea85",
                    "/json/fabric/connection_response.json");
            // Dry-run responds 200 (the real update is 202) with the simulated post-update
            // connection: href/uuid present and the patched values applied, nothing persisted.
            wireMock.stubFor(patch(urlPathEqualTo("/fabric/v4/connections/3a58dd05-f46d-4b1d-a154-2e85c396ea85"))
                    .withQueryParam("dryRun", equalTo("true"))
                    .willReturn(okJson(loadFixture("/json/fabric/connection_response.json")
                            .replace("My-EVPL-Connection", "Renamed-Connection"))));

            Connection connection = fabric.connections().getByUuid("3a58dd05-f46d-4b1d-a154-2e85c396ea85");
            Connection simulated = connection.update()
                    .name("Renamed-Connection")
                    .dryRun()
                    .save();

            assertNotNull(simulated);
            assertEquals("3a58dd05-f46d-4b1d-a154-2e85c396ea85", simulated.getUuid());
            assertEquals("Renamed-Connection", simulated.getName());

            // Regression lock: the dry run MUST carry dryRun=true on the wire — if a future
            // change drops the parameter, this "verification" becomes a REAL mutation.
            wireMock.verify(patchRequestedFor(urlPathEqualTo("/fabric/v4/connections/3a58dd05-f46d-4b1d-a154-2e85c396ea85"))
                    .withQueryParam("dryRun", equalTo("true"))
                    .withHeader("Content-Type", containing("application/json-patch+json"))
                    .withRequestBody(equalToJson(
                            "[{\"op\":\"replace\",\"path\":\"/name\",\"value\":\"Renamed-Connection\"}]")));
        }

        @Test
        @DisplayName("save() without dryRun() sends no dryRun query parameter")
        void defaultSaveOmitsDryRunQueryParam() {
            stubSingleton(wireMock, "/fabric/v4/connections/3a58dd05-f46d-4b1d-a154-2e85c396ea85",
                    "/json/fabric/connection_response.json");
            wireMock.stubFor(patch(urlPathMatching("/fabric/v4/connections/.*"))
                    .willReturn(okJson(loadFixture("/json/fabric/connection_response.json"))));

            Connection connection = fabric.connections().getByUuid("3a58dd05-f46d-4b1d-a154-2e85c396ea85");
            connection.update().name("Renamed-Connection").save();

            wireMock.verify(patchRequestedFor(urlPathEqualTo("/fabric/v4/connections/3a58dd05-f46d-4b1d-a154-2e85c396ea85"))
                    .withQueryParam("dryRun", absent()));
        }
    }

    @Nested
    @DisplayName("define(...).create()")
    class Create {

        @Test
        @DisplayName("POSTs a connection to /connections with the built request body")
        void createPostsConnection() {
            wireMock.stubFor(post(urlPathEqualTo("/fabric/v4/connections"))
                    .willReturn(aResponse()
                            .withStatus(201)
                            .withHeader("Content-Type", "application/json")
                            .withBody(loadFixture("/json/fabric/connection_response.json"))));

            Connection created = fabric.connections()
                    .define(ConnectionType.EVPL_VC)
                    .name("My-EVPL-Connection")
                    .bandwidth(100)
                    .aSideAccessPointPort("c791f8cb-5cc9-cc90-8ce0-306a5c00a4ee",
                            LinkProtocol.dot1q().vlanTag(1001).create())
                    .zSideAccessPointServiceProfile("20d32a80-0d61-4333-bc03-4b2d446794a0",
                            LinkProtocol.dot1q().vlanTag(1002).create())
                    .notification("ops@example.com")
                    .create();

            assertNotNull(created);
            assertEquals("3a58dd05-f46d-4b1d-a154-2e85c396ea85", created.getUuid());
            wireMock.verify(postRequestedFor(urlPathEqualTo("/fabric/v4/connections"))
                    .withRequestBody(matchingJsonPath("$.type", equalTo("EVPL_VC")))
                    .withRequestBody(matchingJsonPath("$.name", equalTo("My-EVPL-Connection")))
                    .withRequestBody(matchingJsonPath("$.bandwidth", equalTo("100")))
                    .withRequestBody(matchingJsonPath("$.aSide.accessPoint.port.uuid",
                            equalTo("c791f8cb-5cc9-cc90-8ce0-306a5c00a4ee")))
                    .withRequestBody(matchingJsonPath("$.aSide.accessPoint.linkProtocol.vlanTag",
                            equalTo("1001")))
                    .withRequestBody(matchingJsonPath("$.zSide.accessPoint.profile.uuid",
                            equalTo("20d32a80-0d61-4333-bc03-4b2d446794a0"))));
        }
    }

    /**
     * Beta: {@code AccessPoint.activationKey} and {@code AccessPoint.environment} (Fabric v4
     * catalog fetched 2026-09-21). The catalog publishes no connection-create example that sets
     * either property, so these tests pin the serialized request shape only: each value travels
     * in its own JSON property and {@code authenticationKey} is never read from or written by
     * the activation-key path.
     */
    @Nested
    @DisplayName("define(...).create() with activationKey / environment [Beta]")
    class CreateWithActivationKey {

        private static final String URL = "/fabric/v4/connections";
        private static final String PROFILE = "23ad37d0-6b1c-4d1f-a4eb-9f3d68fbe4fd";
        private static final String ENVIRONMENT = "6ad498b5-d929-44ac-a199-ce9f0d31d9ac";

        @BeforeEach
        void stubConnectionCreate() {
            wireMock.stubFor(post(urlPathEqualTo(URL))
                    .willReturn(aResponse()
                            .withStatus(201)
                            .withHeader("Content-Type", "application/json")
                            .withBody(loadFixture("/json/fabric/connection_response.json"))));
        }

        @Test
        @DisplayName("activationKey and authenticationKey serialize as separate properties with their own values")
        void bothKeysAreSiblings() {
            SimpleAccessPoint zSide = SimpleAccessPoint.define(AccessPointType.SP)
                    .serviceProfile(PROFILE)
                    .environment(ENVIRONMENT)
                    .sellerRegion("us-west-1")
                    .authenticationKey("123456789012")
                    .activationKey("key_here")
                    .create();

            fabric.connections()
                    .define(ConnectionType.IP_VC)
                    .name("IC-Profile-Connection")
                    .bandwidth(1000)
                    .aSideAccessPointCloudRouter("557400f8-d360-11e9-bb65-2a2ae2dbcce4")
                    .zSideAccessPoint(zSide)
                    .create();

            wireMock.verify(1, postRequestedFor(urlPathEqualTo(URL))
                    .withRequestBody(matchingJsonPath("$.zSide.accessPoint.type", equalTo("SP")))
                    .withRequestBody(matchingJsonPath("$.zSide.accessPoint.profile.uuid", equalTo(PROFILE)))
                    .withRequestBody(matchingJsonPath("$.zSide.accessPoint.activationKey", equalTo("key_here")))
                    .withRequestBody(matchingJsonPath("$.zSide.accessPoint.authenticationKey", equalTo("123456789012")))
                    .withRequestBody(matchingJsonPath("$.zSide.accessPoint.sellerRegion", equalTo("us-west-1"))));
        }

        @Test
        @DisplayName("environment serializes as a uuid-only reference")
        void environmentIsUuidOnlyReference() {
            SimpleAccessPoint zSide = SimpleAccessPoint.define(AccessPointType.SP)
                    .serviceProfile(PROFILE)
                    .environment(ENVIRONMENT)
                    .activationKey("key_here")
                    .create();

            fabric.connections()
                    .define(ConnectionType.IP_VC)
                    .name("IC-Profile-Connection")
                    .bandwidth(1000)
                    .aSideAccessPointCloudRouter("557400f8-d360-11e9-bb65-2a2ae2dbcce4")
                    .zSideAccessPoint(zSide)
                    .create();

            wireMock.verify(postRequestedFor(urlPathEqualTo(URL))
                    .withRequestBody(matchingJsonPath("$.zSide.accessPoint.environment",
                            equalToJson("{\"uuid\":\"" + ENVIRONMENT + "\"}", false, false))));
        }

        @Test
        @DisplayName("activationKey alone does not populate authenticationKey")
        void activationKeyDoesNotLeakIntoAuthenticationKey() {
            SimpleAccessPoint zSide = SimpleAccessPoint.define(AccessPointType.SP)
                    .serviceProfile(PROFILE)
                    .activationKey("key_here")
                    .create();

            fabric.connections()
                    .define(ConnectionType.IP_VC)
                    .name("IC-Profile-Connection")
                    .bandwidth(1000)
                    .aSideAccessPointCloudRouter("557400f8-d360-11e9-bb65-2a2ae2dbcce4")
                    .zSideAccessPoint(zSide)
                    .create();

            wireMock.verify(postRequestedFor(urlPathEqualTo(URL))
                    .withRequestBody(matchingJsonPath("$.zSide.accessPoint.activationKey", equalTo("key_here")))
                    .withRequestBody(notContaining("authenticationKey"))
                    .withRequestBody(notContaining("\"environment\"")));
        }

        @Test
        @DisplayName("a built-in adapter (no activation key) produces the same body as before: authenticationKey only")
        void builtInAdapterBodyIsUnchanged() {
            fabric.connections()
                    .define(ConnectionType.EVPL_VC)
                    .name("AWS-Connection")
                    .bandwidth(100)
                    .aSideAccessPointPort("c791f8cb-5cc9-cc90-8ce0-306a5c00a4ee",
                            LinkProtocol.dot1q().vlanTag(1001).create())
                    .zSideCloudProvider(AwsDirectConnectAdapter.of("123456789012", "us-east-1", PROFILE),
                            LinkProtocol.dot1q().vlanTag(1002).create())
                    .create();

            wireMock.verify(postRequestedFor(urlPathEqualTo(URL))
                    .withRequestBody(matchingJsonPath("$.zSide.accessPoint.authenticationKey", equalTo("123456789012")))
                    .withRequestBody(matchingJsonPath("$.zSide.accessPoint.sellerRegion", equalTo("us-east-1")))
                    .withRequestBody(notContaining("activationKey"))
                    .withRequestBody(notContaining("\"environment\"")));
        }

        @Test
        @DisplayName("an adapter that overrides getActivationKey() sends both keys, each in its own property")
        void adapterSuppliedActivationKey() {
            CloudProviderConnectionAdapter<Void> adapter = new CloudProviderConnectionAdapter<>() {
                public String getServiceProfileUuid() { return PROFILE; }
                public String getAuthenticationKey() { return "123456789012"; }
                public String getSellerRegion() { return "us-west-1"; }
                public Void getSource() { return null; }
                public CloudProviderType getProviderType() { return CloudProviderType.AWS; }
                @Override public String getActivationKey() { return "key_here"; }
            };

            fabric.connections()
                    .define(ConnectionType.IP_VC)
                    .name("IC-Profile-Connection")
                    .bandwidth(1000)
                    .aSideAccessPointCloudRouter("557400f8-d360-11e9-bb65-2a2ae2dbcce4")
                    .zSideCloudProvider(adapter)
                    .create();

            wireMock.verify(postRequestedFor(urlPathEqualTo(URL))
                    .withRequestBody(matchingJsonPath("$.zSide.accessPoint.activationKey", equalTo("key_here")))
                    .withRequestBody(matchingJsonPath("$.zSide.accessPoint.authenticationKey", equalTo("123456789012"))));
        }

        @Test
        @DisplayName("zSideAccessPoint(null) is rejected")
        void nullAccessPointRejected() {
            assertThrows(NullPointerException.class, () -> fabric.connections()
                    .define(ConnectionType.IP_VC)
                    .zSideAccessPoint((SimpleAccessPoint) null));
            wireMock.verify(0, postRequestedFor(urlPathEqualTo(URL)));
        }
    }

    @Nested
    @DisplayName("Route Aggregation attach / detach")
    class RouteAggregationActions {

        @Test
        @DisplayName("attachRouteAggregation PUTs to /connections/{id}/routeAggregations/{raId}")
        void attach() {
            wireMock.stubFor(put(urlPathEqualTo(
                    "/fabric/v4/connections/81331c52-04c0-4656-a4a7-18c52669348f/routeAggregations/695a8471-6595-4ac6-a2f4-b3d96ed3a59d"))
                    .willReturn(okJson(loadFixture("/json/fabric/connection_route_aggregation_attachment_response.json"))));

            RouteAggregationAttachment attachment = fabric.connections().attachRouteAggregation(
                    "81331c52-04c0-4656-a4a7-18c52669348f", "695a8471-6595-4ac6-a2f4-b3d96ed3a59d");

            assertNotNull(attachment);
            assertEquals("695a8471-6595-4ac6-a2f4-b3d96ed3a59d", attachment.getUuid());
            wireMock.verify(putRequestedFor(urlPathEqualTo(
                    "/fabric/v4/connections/81331c52-04c0-4656-a4a7-18c52669348f/routeAggregations/695a8471-6595-4ac6-a2f4-b3d96ed3a59d")));
        }

        @Test
        @DisplayName("detachRouteAggregation DELETEs to /connections/{id}/routeAggregations/{raId}")
        void detach() {
            wireMock.stubFor(delete(urlPathEqualTo(
                    "/fabric/v4/connections/81331c52-04c0-4656-a4a7-18c52669348f/routeAggregations/695a8471-6595-4ac6-a2f4-b3d96ed3a59d"))
                    .willReturn(noContent()));

            Boolean result = fabric.connections().detachRouteAggregation(
                    "81331c52-04c0-4656-a4a7-18c52669348f", "695a8471-6595-4ac6-a2f4-b3d96ed3a59d");

            assertTrue(result);
            wireMock.verify(deleteRequestedFor(urlPathEqualTo(
                    "/fabric/v4/connections/81331c52-04c0-4656-a4a7-18c52669348f/routeAggregations/695a8471-6595-4ac6-a2f4-b3d96ed3a59d")));
        }
    }

    @Nested
    @DisplayName("Route Filter attach / detach")
    class RouteFilterActions {

        @Test
        @DisplayName("attachRouteFilter PUTs the direction body to /connections/{id}/routeFilters/{rfId}")
        void attach() {
            wireMock.stubFor(put(urlPathEqualTo(
                    "/fabric/v4/connections/81331c52-04c0-4656-a4a7-18c52669348f/routeFilters/695a8471-6595-4ac6-a2f4-b3d96ed3a59d"))
                    .willReturn(okJson(loadFixture("/json/fabric/connection_route_filter_attachment_response.json"))));

            RouteFilterAttachment attachment = fabric.connections().attachRouteFilter(
                    "81331c52-04c0-4656-a4a7-18c52669348f", "695a8471-6595-4ac6-a2f4-b3d96ed3a59d",
                    Direction.INBOUND);

            assertNotNull(attachment);
            assertEquals("695a8471-6595-4ac6-a2f4-b3d96ed3a59d", attachment.getUuid());
            wireMock.verify(putRequestedFor(urlPathEqualTo(
                    "/fabric/v4/connections/81331c52-04c0-4656-a4a7-18c52669348f/routeFilters/695a8471-6595-4ac6-a2f4-b3d96ed3a59d"))
                    .withRequestBody(equalToJson("{\"direction\":\"INBOUND\"}", true, true)));
        }

        @Test
        @DisplayName("detachRouteFilter DELETEs to /connections/{id}/routeFilters/{rfId}")
        void detach() {
            wireMock.stubFor(delete(urlPathEqualTo(
                    "/fabric/v4/connections/81331c52-04c0-4656-a4a7-18c52669348f/routeFilters/695a8471-6595-4ac6-a2f4-b3d96ed3a59d"))
                    .willReturn(noContent()));

            Boolean result = fabric.connections().detachRouteFilter(
                    "81331c52-04c0-4656-a4a7-18c52669348f", "695a8471-6595-4ac6-a2f4-b3d96ed3a59d");

            assertTrue(result);
            wireMock.verify(deleteRequestedFor(urlPathEqualTo(
                    "/fabric/v4/connections/81331c52-04c0-4656-a4a7-18c52669348f/routeFilters/695a8471-6595-4ac6-a2f4-b3d96ed3a59d")));
        }
    }

    @Nested
    @DisplayName("search()")
    class Search {

        @Test
        @DisplayName("search() POSTs an empty filter to /connections/search")
        void searchNoArgs() {
            stubPaginatedPost(wireMock, "/fabric/v4/connections/search",
                    "/json/fabric/paginated_connections.json");

            PaginatedFilteredList<Connection> results = fabric.connections().search();

            assertNotNull(results);
            assertEquals(2, results.size());
            assertEquals("3a58dd05-f46d-4b1d-a154-2e85c396ea85", results.get(0).getUuid());

            wireMock.verify(postRequestedFor(urlPathEqualTo("/fabric/v4/connections/search")));
        }

        @Test
        @DisplayName("search(filter) POSTs the AND/equals filter in the body")
        void searchWithFilter() {
            stubPaginatedPost(wireMock, "/fabric/v4/connections/search",
                    "/json/fabric/paginated_connections.json");

            FilterPropertyList filter = Filter.filter().and()
                    .equals("/name", "My-EVPL-Connection");

            PaginatedFilteredList<Connection> results = fabric.connections().search(filter);

            assertNotNull(results);
            wireMock.verify(postRequestedFor(urlPathEqualTo("/fabric/v4/connections/search"))
                    .withRequestBody(matchingJsonPath("$.filter.and[0].property", equalTo("/name")))
                    .withRequestBody(matchingJsonPath("$.filter.and[0].operator", equalTo("=")))
                    .withRequestBody(matchingJsonPath("$.filter.and[0].values[0]", equalTo("My-EVPL-Connection"))));
        }

        @Test
        @DisplayName("search(sort) POSTs the sort array in the body")
        void searchWithSort() {
            stubPaginatedPost(wireMock, "/fabric/v4/connections/search",
                    "/json/fabric/paginated_connections.json");

            SortPropertyList sort = Sort.sort().desc("/changeLog/createdDateTime");

            PaginatedFilteredList<Connection> results = fabric.connections().search(sort);

            assertNotNull(results);
            wireMock.verify(postRequestedFor(urlPathEqualTo("/fabric/v4/connections/search"))
                    .withRequestBody(matchingJsonPath("$.sort[0].property", equalTo("/changeLog/createdDateTime")))
                    .withRequestBody(matchingJsonPath("$.sort[0].direction", equalTo("DESC"))));
        }

        @Test
        @DisplayName("search(filter, sort) POSTs both filter and sort in the body")
        void searchWithFilterAndSort() {
            stubPaginatedPost(wireMock, "/fabric/v4/connections/search",
                    "/json/fabric/paginated_connections.json");

            FilterPropertyList filter = Filter.filter().and()
                    .equals("/operation/equinixStatus", "PROVISIONED");
            SortPropertyList sort = Sort.sort().asc("/name");

            PaginatedFilteredList<Connection> results = fabric.connections().search(filter, sort);

            assertNotNull(results);
            wireMock.verify(postRequestedFor(urlPathEqualTo("/fabric/v4/connections/search"))
                    .withRequestBody(matchingJsonPath("$.filter.and[0].property", equalTo("/operation/equinixStatus")))
                    .withRequestBody(matchingJsonPath("$.filter.and[0].values[0]", equalTo("PROVISIONED")))
                    .withRequestBody(matchingJsonPath("$.sort[0].property", equalTo("/name")))
                    .withRequestBody(matchingJsonPath("$.sort[0].direction", equalTo("ASC"))));
        }
    }

    @Nested
    @DisplayName("searchAdvertisedRoutes()")
    class SearchAdvertisedRoutes {

        @Test
        @DisplayName("POSTs to /connections/{id}/advertisedRoutes/search")
        void searchNoArgs() {
            stubPaginatedPost(wireMock,
                    "/fabric/v4/connections/3a58dd05-f46d-4b1d-a154-2e85c396ea85/advertisedRoutes/search",
                    "/json/fabric/paginated_route_table_entries.json");

            PaginatedFilteredList<RouteTableEntry> routes =
                    fabric.connections().searchAdvertisedRoutes("3a58dd05-f46d-4b1d-a154-2e85c396ea85");

            assertNotNull(routes);
            assertEquals(1, routes.size());
            assertEquals("10.0.0.0/24", routes.get(0).getPrefix());

            wireMock.verify(postRequestedFor(urlPathEqualTo(
                    "/fabric/v4/connections/3a58dd05-f46d-4b1d-a154-2e85c396ea85/advertisedRoutes/search")));
        }

        @Test
        @DisplayName("POSTs the filter and sort in the body")
        void searchWithFilterAndSort() {
            stubPaginatedPost(wireMock,
                    "/fabric/v4/connections/3a58dd05-f46d-4b1d-a154-2e85c396ea85/advertisedRoutes/search",
                    "/json/fabric/paginated_route_table_entries.json");

            FilterPropertyList filter = Filter.filter().and().equals("/protocolType", "BGP");
            SortPropertyList sort = Sort.sort().asc("/prefix");

            PaginatedFilteredList<RouteTableEntry> routes = fabric.connections()
                    .searchAdvertisedRoutes("3a58dd05-f46d-4b1d-a154-2e85c396ea85", filter, sort);

            assertNotNull(routes);
            wireMock.verify(postRequestedFor(urlPathEqualTo(
                    "/fabric/v4/connections/3a58dd05-f46d-4b1d-a154-2e85c396ea85/advertisedRoutes/search"))
                    .withRequestBody(matchingJsonPath("$.filter.and[0].property", equalTo("/protocolType")))
                    .withRequestBody(matchingJsonPath("$.filter.and[0].values[0]", equalTo("BGP")))
                    .withRequestBody(matchingJsonPath("$.sort[0].property", equalTo("/prefix")))
                    .withRequestBody(matchingJsonPath("$.sort[0].direction", equalTo("ASC"))));
        }
    }

    @Nested
    @DisplayName("searchReceivedRoutes()")
    class SearchReceivedRoutes {

        @Test
        @DisplayName("POSTs to /connections/{id}/receivedRoutes/search")
        void searchNoArgs() {
            stubPaginatedPost(wireMock,
                    "/fabric/v4/connections/3a58dd05-f46d-4b1d-a154-2e85c396ea85/receivedRoutes/search",
                    "/json/fabric/paginated_route_table_entries.json");

            PaginatedFilteredList<RouteTableEntry> routes =
                    fabric.connections().searchReceivedRoutes("3a58dd05-f46d-4b1d-a154-2e85c396ea85");

            assertNotNull(routes);
            assertEquals(1, routes.size());
            assertEquals("10.0.0.0/24", routes.get(0).getPrefix());

            wireMock.verify(postRequestedFor(urlPathEqualTo(
                    "/fabric/v4/connections/3a58dd05-f46d-4b1d-a154-2e85c396ea85/receivedRoutes/search")));
        }

        @Test
        @DisplayName("POSTs the filter and sort in the body")
        void searchWithFilterAndSort() {
            stubPaginatedPost(wireMock,
                    "/fabric/v4/connections/3a58dd05-f46d-4b1d-a154-2e85c396ea85/receivedRoutes/search",
                    "/json/fabric/paginated_route_table_entries.json");

            FilterPropertyList filter = Filter.filter().and().equals("/state", "ACTIVE");
            SortPropertyList sort = Sort.sort().desc("/prefix");

            PaginatedFilteredList<RouteTableEntry> routes = fabric.connections()
                    .searchReceivedRoutes("3a58dd05-f46d-4b1d-a154-2e85c396ea85", filter, sort);

            assertNotNull(routes);
            wireMock.verify(postRequestedFor(urlPathEqualTo(
                    "/fabric/v4/connections/3a58dd05-f46d-4b1d-a154-2e85c396ea85/receivedRoutes/search"))
                    .withRequestBody(matchingJsonPath("$.filter.and[0].property", equalTo("/state")))
                    .withRequestBody(matchingJsonPath("$.filter.and[0].values[0]", equalTo("ACTIVE")))
                    .withRequestBody(matchingJsonPath("$.sort[0].property", equalTo("/prefix")))
                    .withRequestBody(matchingJsonPath("$.sort[0].direction", equalTo("DESC"))));
        }
    }

    @Nested
    @DisplayName("getRouteAggregations() / getRouteAggregation()")
    class RouteAggregationReads {

        @Test
        @DisplayName("getRouteAggregations GETs /connections/{id}/routeAggregations (list)")
        void listAggregations() {
            stubPaginatedGet(wireMock,
                    "/fabric/v4/connections/81331c52-04c0-4656-a4a7-18c52669348f/routeAggregations",
                    "/json/fabric/paginated_route_aggregation_attachments.json");

            List<RouteAggregationAttachment> aggregations = fabric.connections()
                    .getRouteAggregations("81331c52-04c0-4656-a4a7-18c52669348f");

            assertNotNull(aggregations);
            assertEquals(1, aggregations.size());
            assertEquals("7d0e1f2a-3b4c-5d6e-7f80-91a2b3c4d5e6", aggregations.get(0).getUuid());

            wireMock.verify(getRequestedFor(urlPathEqualTo(
                    "/fabric/v4/connections/81331c52-04c0-4656-a4a7-18c52669348f/routeAggregations")));
        }

        @Test
        @DisplayName("getRouteAggregation GETs /connections/{id}/routeAggregations/{raId} (single)")
        void getAggregation() {
            stubSingleton(wireMock,
                    "/fabric/v4/connections/81331c52-04c0-4656-a4a7-18c52669348f/routeAggregations/695a8471-6595-4ac6-a2f4-b3d96ed3a59d",
                    "/json/fabric/connection_route_aggregation_attachment_response.json");

            RouteAggregationAttachment attachment = fabric.connections().getRouteAggregation(
                    "81331c52-04c0-4656-a4a7-18c52669348f", "695a8471-6595-4ac6-a2f4-b3d96ed3a59d");

            assertNotNull(attachment);
            assertEquals("695a8471-6595-4ac6-a2f4-b3d96ed3a59d", attachment.getUuid());

            wireMock.verify(getRequestedFor(urlPathEqualTo(
                    "/fabric/v4/connections/81331c52-04c0-4656-a4a7-18c52669348f/routeAggregations/695a8471-6595-4ac6-a2f4-b3d96ed3a59d")));
        }
    }

    @Nested
    @DisplayName("getRouteFilters() / getRouteFilter()")
    class RouteFilterReads {

        @Test
        @DisplayName("getRouteFilters GETs /connections/{id}/routeFilters (list)")
        void listFilters() {
            stubPaginatedGet(wireMock,
                    "/fabric/v4/connections/81331c52-04c0-4656-a4a7-18c52669348f/routeFilters",
                    "/json/fabric/paginated_route_filter_attachments.json");

            List<RouteFilterAttachment> filters = fabric.connections()
                    .getRouteFilters("81331c52-04c0-4656-a4a7-18c52669348f");

            assertNotNull(filters);
            assertEquals(1, filters.size());
            assertEquals("8e1e2f3a-4b5c-6d7e-8f90-a1b2c3d4e5f6", filters.get(0).getUuid());

            wireMock.verify(getRequestedFor(urlPathEqualTo(
                    "/fabric/v4/connections/81331c52-04c0-4656-a4a7-18c52669348f/routeFilters")));
        }

        @Test
        @DisplayName("getRouteFilter GETs /connections/{id}/routeFilters/{rfId} (single)")
        void getFilter() {
            stubSingleton(wireMock,
                    "/fabric/v4/connections/81331c52-04c0-4656-a4a7-18c52669348f/routeFilters/695a8471-6595-4ac6-a2f4-b3d96ed3a59d",
                    "/json/fabric/connection_route_filter_attachment_response.json");

            RouteFilterAttachment attachment = fabric.connections().getRouteFilter(
                    "81331c52-04c0-4656-a4a7-18c52669348f", "695a8471-6595-4ac6-a2f4-b3d96ed3a59d");

            assertNotNull(attachment);
            assertEquals("695a8471-6595-4ac6-a2f4-b3d96ed3a59d", attachment.getUuid());

            wireMock.verify(getRequestedFor(urlPathEqualTo(
                    "/fabric/v4/connections/81331c52-04c0-4656-a4a7-18c52669348f/routeFilters/695a8471-6595-4ac6-a2f4-b3d96ed3a59d")));
        }
    }

    @Nested
    @DisplayName("Error handling")
    class Errors {

        @Test
        @DisplayName("401 throws EquinixAuthenticationException")
        void unauthorized() {
            stubErrorInline(wireMock, "/fabric/v4/connections/.*",
                    401, "[{\"errorCode\":\"ERR-401\",\"errorMessage\":\"Unauthorized\"}]");

            assertThrows(EquinixAuthenticationException.class,
                    () -> fabric.connections().getByUuid("test-uuid"));
        }

        @Test
        @DisplayName("403 throws EquinixAuthorizationException")
        void forbidden() {
            stubErrorInline(wireMock, "/fabric/v4/connections/.*",
                    403, "[{\"errorCode\":\"ERR-403\",\"errorMessage\":\"Forbidden\"}]");

            assertThrows(EquinixAuthorizationException.class,
                    () -> fabric.connections().getByUuid("test-uuid"));
        }

        @Test
        @DisplayName("429 throws EquinixRateLimitException")
        void rateLimited() {
            stubErrorInline(wireMock, "/fabric/v4/connections/.*",
                    429, "[{\"errorCode\":\"ERR-429\",\"errorMessage\":\"Rate limit exceeded\"}]");

            assertThrows(EquinixRateLimitException.class,
                    () -> fabric.connections().getByUuid("test-uuid"));
        }

        @Test
        @DisplayName("500 throws EquinixServerException")
        void serverError() {
            stubErrorInline(wireMock, "/fabric/v4/connections/.*",
                    500, "[{\"errorCode\":\"ERR-500\",\"errorMessage\":\"Internal server error\"}]");

            assertThrows(EquinixServerException.class,
                    () -> fabric.connections().getByUuid("test-uuid"));
        }
    }

    @Nested
    @DisplayName("Wrapper refresh()")
    class WrapperRefresh {

        private static final String CONN_ID = "3a58dd05-f46d-4b1d-a154-2e85c396ea85";
        private static final String URL = "/fabric/v4/connections/" + CONN_ID;

        @Test
        @DisplayName("re-GETs /connections/{uuid} and swaps the wrapper's state in place")
        void refreshReloadsInPlace() {
            wireMock.stubFor(get(urlPathEqualTo(URL))
                    .inScenario("connection-refresh")
                    .whenScenarioStateIs(com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED)
                    .willReturn(okJson(loadFixture("/json/fabric/connection_response.json")))
                    .willSetStateTo("renamed"));
            wireMock.stubFor(get(urlPathEqualTo(URL))
                    .inScenario("connection-refresh")
                    .whenScenarioStateIs("renamed")
                    .willReturn(okJson(loadFixture("/json/fabric/connection_response.json")
                            .replace("My-EVPL-Connection", "My-EVPL-Connection-Renamed"))));

            Connection connection = fabric.connections().getByUuid(CONN_ID);
            assertEquals("My-EVPL-Connection", connection.getName());

            connection.refresh();

            assertEquals("My-EVPL-Connection-Renamed", connection.getName(),
                    "refresh() must swap the wrapper's backing state in place");
            wireMock.verify(2, getRequestedFor(urlPathEqualTo(URL)));
        }
    }

    @Nested
    @DisplayName("Wrapper delete()")
    class WrapperDelete {

        private static final String CONN_ID = "3a58dd05-f46d-4b1d-a154-2e85c396ea85";

        @Test
        @DisplayName("DELETEs /connections/{uuid} and returns true")
        void deletesConnection() {
            stubSingleton(wireMock, "/fabric/v4/connections/" + CONN_ID,
                    "/json/fabric/connection_response.json");
            // deleteOne() reads the deleted resource from the response body, so the stub returns one.
            wireMock.stubFor(delete(urlPathEqualTo("/fabric/v4/connections/" + CONN_ID))
                    .willReturn(okJson(loadFixture("/json/fabric/connection_response.json"))));

            Connection connection = fabric.connections().getByUuid(CONN_ID);
            Boolean deleted = connection.delete();

            assertEquals(Boolean.TRUE, deleted);
            wireMock.verify(deleteRequestedFor(urlPathEqualTo("/fabric/v4/connections/" + CONN_ID)));
        }
    }

    @Nested
    @DisplayName("Multi-page search paging")
    class Paging {

        private static final String PAGE_1 = """
                {
                  "pagination": { "offset": 0, "limit": 100, "total": 150 },
                  "data": [ { "uuid": "PAGE1_CONNECTION" } ]
                }
                """;

        private static final String PAGE_2 = """
                {
                  "pagination": { "offset": 100, "limit": 100, "total": 150 },
                  "data": [ { "uuid": "PAGE2_CONNECTION" } ]
                }
                """;

        @Test
        @DisplayName("loadAll() re-POSTs the search with the body's pagination offset advanced to page 2")
        void loadAllFetchesSecondPage() {
            wireMock.stubFor(post(urlPathEqualTo("/fabric/v4/connections/search"))
                    .withRequestBody(matchingJsonPath("$.pagination.offset", equalTo("0")))
                    .willReturn(okJson(PAGE_1)));
            wireMock.stubFor(post(urlPathEqualTo("/fabric/v4/connections/search"))
                    .withRequestBody(matchingJsonPath("$.pagination.offset", equalTo("100")))
                    .willReturn(okJson(PAGE_2)));

            PaginatedFilteredList<Connection> connections = fabric.connections().search();
            assertEquals(1, connections.size());
            assertTrue(connections.hasNextPage());

            connections.loadAll();

            assertEquals(2, connections.size());
            assertEquals("PAGE1_CONNECTION", connections.get(0).getUuid());
            assertEquals("PAGE2_CONNECTION", connections.get(1).getUuid());
            assertFalse(connections.hasNextPage());

            wireMock.verify(1, postRequestedFor(urlPathEqualTo("/fabric/v4/connections/search"))
                    .withRequestBody(matchingJsonPath("$.pagination.offset", equalTo("100"))));
        }
    }
}
