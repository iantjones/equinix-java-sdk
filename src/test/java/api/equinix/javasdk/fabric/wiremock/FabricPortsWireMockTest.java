package api.equinix.javasdk.fabric.wiremock;

import api.equinix.javasdk.Fabric;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.core.exception.*;
import api.equinix.javasdk.core.http.response.PaginatedFilteredList;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.fabric.enums.ConnectivitySourceType;
import api.equinix.javasdk.fabric.enums.LinkProtocolState;
import api.equinix.javasdk.fabric.enums.PhysicalPortType;
import api.equinix.javasdk.fabric.enums.PortState;
import api.equinix.javasdk.fabric.enums.PortType;
import api.equinix.javasdk.fabric.model.Port;
import api.equinix.javasdk.fabric.model.PortVlan;
import api.equinix.javasdk.fabric.enums.RedundancyPriority;
import api.equinix.javasdk.fabric.model.implementation.PhysicalPort;
import api.equinix.javasdk.fabric.model.implementation.Redundancy;
import api.equinix.javasdk.fabric.model.implementation.filter.Filter;
import api.equinix.javasdk.fabric.model.implementation.filter.FilterPropertyList;
import api.equinix.javasdk.fabric.model.implementation.sort.Sort;
import api.equinix.javasdk.fabric.model.implementation.sort.SortPropertyList;
import api.equinix.javasdk.fabric.model.json.PhysicalPortsResponseJson;
import org.junit.jupiter.api.*;

import java.util.List;

import static api.equinix.javasdk.core.ResponseStubs.*;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * WireMock-based API tests for Fabric Ports.
 */
class FabricPortsWireMockTest extends WireMockTestBase {

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
        @DisplayName("returns port for valid UUID")
        void returnsPort() {
            stubSingleton(wireMock, "/fabric/v4/ports/.*",
                    "/json/fabric/port_response.json");

            Port port = fabric.ports().getByUuid("c791f8cb-5cc9-cc90-8ce0-306a5c00a4ee");
            assertNotNull(port);
            assertEquals("c791f8cb-5cc9-cc90-8ce0-306a5c00a4ee", port.getUuid());
        }

        @Test
        @DisplayName("404 throws EquinixNotFoundException")
        void notFound() {
            stubErrorInline(wireMock, "/fabric/v4/ports/.*",
                    404, "[{\"errorCode\":\"ERR-404\",\"errorMessage\":\"Port not found\"}]");

            assertThrows(EquinixNotFoundException.class,
                    () -> fabric.ports().getByUuid("invalid-uuid"));
        }
    }

    @Nested
    @DisplayName("list()")
    class ListPorts {

        @Test
        @DisplayName("GETs /fabric/v4/ports and returns a paginated list")
        void listsPorts() {
            stubPaginatedGet(wireMock, "/fabric/v4/ports", "/json/fabric/paginated_ports.json");

            PaginatedList<Port> ports = fabric.ports().list();

            assertNotNull(ports);
            assertEquals(2, ports.size());
            assertEquals("c791f8cb-5cc9-cc90-8ce0-306a5c00a4ee", ports.get(0).getUuid());
            assertEquals("a1b2c3d4-e5f6-7890-abcd-ef1234567890", ports.get(1).getUuid());
            // PortState spec values round-trip (PROVISIONED is a real spec state, not UNKNOWN)
            assertEquals(PortState.ACTIVE, ports.get(0).getState());
            assertEquals(PortState.PROVISIONED, ports.get(1).getState());

            wireMock.verify(getRequestedFor(urlPathEqualTo("/fabric/v4/ports")));
        }
    }

    @Nested
    @DisplayName("search()")
    class Search {

        private static final String SEARCH_URL = "/fabric/v4/ports/search";

        @Test
        @DisplayName("no-arg search POSTs the default body to /ports/search and returns a filtered list")
        void searchNoArg() {
            stubPaginatedPost(wireMock, SEARCH_URL, "/json/fabric/paginated_ports.json");

            PaginatedFilteredList<Port> ports = fabric.ports().search();

            assertNotNull(ports);
            assertEquals(2, ports.size());

            // Default no-arg search sends an (empty) filter, no sort.
            wireMock.verify(postRequestedFor(urlPathEqualTo(SEARCH_URL))
                    .withHeader("Content-Type", containing("application/json"))
                    .withRequestBody(matchingJsonPath("$.pagination")));
        }

        @Test
        @DisplayName("search(filter) carries the filter predicate in the POST body")
        void searchWithFilter() {
            stubPaginatedPost(wireMock, SEARCH_URL, "/json/fabric/paginated_ports.json");

            FilterPropertyList filter = Filter.filter().and()
                    .equals("/type", "XF_PORT")
                    .equals("/location/metroCode", "SV");

            PaginatedFilteredList<Port> ports = fabric.ports().search(filter);

            assertNotNull(ports);
            wireMock.verify(postRequestedFor(urlPathEqualTo(SEARCH_URL))
                    .withRequestBody(matchingJsonPath("$.filter.and[0].property", equalTo("/type")))
                    .withRequestBody(matchingJsonPath("$.filter.and[0].values[0]", equalTo("XF_PORT")))
                    .withRequestBody(matchingJsonPath("$.filter.and[1].property", equalTo("/location/metroCode")))
                    .withRequestBody(matchingJsonPath("$.filter.and[1].values[0]", equalTo("SV"))));
        }

        @Test
        @DisplayName("search(sort) carries the sort directive in the POST body")
        void searchWithSort() {
            stubPaginatedPost(wireMock, SEARCH_URL, "/json/fabric/paginated_ports.json");

            SortPropertyList sort = Sort.sort().desc("/changeLog/createdDateTime");

            PaginatedFilteredList<Port> ports = fabric.ports().search(sort);

            assertNotNull(ports);
            wireMock.verify(postRequestedFor(urlPathEqualTo(SEARCH_URL))
                    .withRequestBody(matchingJsonPath("$.sort[0].property", equalTo("/changeLog/createdDateTime")))
                    .withRequestBody(matchingJsonPath("$.sort[0].direction", equalTo("DESC"))));
        }

        @Test
        @DisplayName("search(filter, sort) carries both filter and sort in the POST body")
        void searchWithFilterAndSort() {
            stubPaginatedPost(wireMock, SEARCH_URL, "/json/fabric/paginated_ports.json");

            FilterPropertyList filter = Filter.filter().and()
                    .equals("/state", "ACTIVE");
            SortPropertyList sort = Sort.sort().asc("/name");

            PaginatedFilteredList<Port> ports = fabric.ports().search(filter, sort);

            assertNotNull(ports);
            wireMock.verify(postRequestedFor(urlPathEqualTo(SEARCH_URL))
                    .withRequestBody(matchingJsonPath("$.filter.and[0].property", equalTo("/state")))
                    .withRequestBody(matchingJsonPath("$.filter.and[0].values[0]", equalTo("ACTIVE")))
                    .withRequestBody(matchingJsonPath("$.sort[0].property", equalTo("/name")))
                    .withRequestBody(matchingJsonPath("$.sort[0].direction", equalTo("ASC"))));
        }
    }

    @Nested
    @DisplayName("getVlans()")
    class GetVlans {

        private static final String PORT_ID = "c791f8cb-5cc9-cc90-8ce0-306a5c00a4ee";

        @Test
        @DisplayName("GETs /fabric/v4/ports/{portUuid}/linkProtocols and returns the VLAN list")
        void getsVlans() {
            wireMock.stubFor(get(urlPathEqualTo("/fabric/v4/ports/" + PORT_ID + "/linkProtocols"))
                    .willReturn(okJson(loadFixture("/json/fabric/paginated_port_vlans.json"))));

            List<PortVlan> vlans = fabric.ports().getVlans(PORT_ID);

            assertNotNull(vlans);
            assertEquals(2, vlans.size());
            assertEquals("vlan-1111-2222-3333", vlans.get(0).getUuid());
            assertEquals(LinkProtocolState.RESERVED, vlans.get(0).getState());
            assertEquals(Integer.valueOf(1001), vlans.get(0).getVlanTag());
            assertEquals(LinkProtocolState.RELEASED, vlans.get(1).getState());
            assertEquals(Integer.valueOf(2001), vlans.get(1).getVlanSTag());

            wireMock.verify(getRequestedFor(urlPathEqualTo("/fabric/v4/ports/" + PORT_ID + "/linkProtocols")));
        }
    }

    @Nested
    @DisplayName("define() / create()")
    class Create {

        @Test
        @DisplayName("POSTs the configured PortRequest body and returns the created port")
        void createsPort() {
            stubCreate(wireMock, "/fabric/v4/ports",
                    "/json/fabric/port_response.json");

            Port created = fabric.ports().define()
                    .ofType(PortType.XF_PORT)
                    .physicalPortsSpeed(10000)
                    .physicalPortsType(PhysicalPortType._10GBASE_LR)
                    .physicalPortsCount(1)
                    .connectivitySourceType(ConnectivitySourceType.COLO)
                    .withRedundancy(new Redundancy(true, null, RedundancyPriority.PRIMARY))
                    .lagEnabled(false)
                    .projectId("proj-abc-123")
                    .accountNumber(123456L)
                    .metroCode("SV")
                    .create();

            assertNotNull(created);
            assertEquals("c791f8cb-5cc9-cc90-8ce0-306a5c00a4ee", created.getUuid());

            wireMock.verify(postRequestedFor(urlPathMatching("/fabric/v4/ports"))
                    .withRequestBody(matchingJsonPath("$.type", equalTo("XF_PORT")))
                    // Spec PortRequest has no 'name' member; the create body must not carry one.
                    .withRequestBody(notMatching(".*\"name\".*"))
                    .withRequestBody(matchingJsonPath("$.physicalPortsSpeed", equalTo("10000")))
                    .withRequestBody(matchingJsonPath("$.physicalPortsType", equalTo("10GBASE_LR")))
                    .withRequestBody(matchingJsonPath("$.connectivitySourceType", equalTo("COLO")))
                    // PortRedundancy.enabled (spec) rides on the wire; group is omitted when null.
                    .withRequestBody(matchingJsonPath("$.redundancy.enabled", equalTo("true")))
                    .withRequestBody(matchingJsonPath("$.redundancy.priority", equalTo("PRIMARY")))
                    .withRequestBody(matchingJsonPath("$.project.projectId", equalTo("proj-abc-123")))
                    .withRequestBody(matchingJsonPath("$.account.accountNumber", equalTo("123456")))
                    .withRequestBody(matchingJsonPath("$.location.metroCode", equalTo("SV"))));
        }

        @Test
        @DisplayName("dryRun() sends dryRun=true and deserializes the echoed port order (no uuid/name/state)")
        void createDryRunSendsQueryParamAndDeserializesEcho() {
            // Spec: dry-run create responds 200 (real create is 202) with the validated port
            // order echoed back — no uuid/name/state (example PortCreateDryRunResponse).
            String dryRunEcho = "{"
                    + "\"type\":\"XF_PORT\","
                    + "\"connectivitySourceType\":\"COLO\","
                    + "\"physicalPortsSpeed\":10000,"
                    + "\"physicalPortsType\":\"10GBASE_LR\","
                    + "\"physicalPortsCount\":1,"
                    + "\"lagEnabled\":false,"
                    + "\"location\":{\"metroCode\":\"SV\"},"
                    + "\"account\":{\"accountNumber\":123456}"
                    + "}";
            wireMock.stubFor(post(urlPathEqualTo("/fabric/v4/ports"))
                    .willReturn(okJson(dryRunEcho)));

            Port validated = fabric.ports().define()
                    .ofType(PortType.XF_PORT)
                    .physicalPortsSpeed(10000)
                    .physicalPortsType(PhysicalPortType._10GBASE_LR)
                    .physicalPortsCount(1)
                    .connectivitySourceType(ConnectivitySourceType.COLO)
                    .lagEnabled(false)
                    .accountNumber(123456L)
                    .metroCode("SV")
                    .dryRun()
                    .create();

            assertNotNull(validated);
            assertNull(validated.getUuid(), "dry-run echo carries no uuid — nothing was created");
            assertNull(validated.getState(), "dry-run echo carries no state — nothing was created");
            assertEquals(PortType.XF_PORT, validated.getType());
            assertEquals(Integer.valueOf(10000), validated.getPhysicalPortsSpeed());

            // Regression lock: dryRun=true MUST reach the wire as a query parameter on this exact
            // endpoint — dropping it would turn the validation into a REAL port order.
            wireMock.verify(postRequestedFor(urlPathEqualTo("/fabric/v4/ports"))
                    .withQueryParam("dryRun", equalTo("true"))
                    .withRequestBody(matchingJsonPath("$.type", equalTo("XF_PORT"))));
        }

        @Test
        @DisplayName("default create() does NOT send a dryRun query parameter")
        void createWithoutDryRunOmitsQueryParameter() {
            stubCreate(wireMock, "/fabric/v4/ports",
                    "/json/fabric/port_response.json");

            Port created = fabric.ports().define()
                    .ofType(PortType.XF_PORT)
                    .metroCode("SV")
                    .create();

            assertNotNull(created);
            wireMock.verify(postRequestedFor(urlPathEqualTo("/fabric/v4/ports"))
                    .withQueryParam("dryRun", absent()));
        }
    }

    @Nested
    @DisplayName("update()")
    class Update {

        @Test
        @DisplayName("PATCHes an op/path/value array as application/json")
        void updatePatchesName() {
            wireMock.stubFor(patch(urlPathMatching("/fabric/v4/ports/.*"))
                    .willReturn(okJson(loadFixture("/json/fabric/port_response.json"))));

            Port updated = fabric.ports().update("c791f8cb-5cc9-cc90-8ce0-306a5c00a4ee")
                    .name("Renamed-Port").save();

            assertNotNull(updated);
            wireMock.verify(patchRequestedFor(urlPathMatching("/fabric/v4/ports/c791f8cb-5cc9-cc90-8ce0-306a5c00a4ee"))
                    .withHeader("Content-Type", containing("application/json"))
                    .withRequestBody(equalToJson(
                            "[{\"op\":\"replace\",\"path\":\"/name\",\"value\":\"Renamed-Port\"}]")));
        }

        @Test
        @DisplayName("save() with no changes throws and makes no request")
        void emptyUpdateThrows() {
            assertThrows(IllegalStateException.class,
                    () -> fabric.ports().update("c791f8cb-5cc9-cc90-8ce0-306a5c00a4ee").save());
            wireMock.verify(0, patchRequestedFor(urlPathMatching("/fabric/v4/ports/.*")));
        }

        @Test
        @DisplayName("dryRun() sends dryRun=true and unwraps data[0] of the AllPortsResponse envelope")
        void updateDryRunSendsQueryParamAndUnwrapsEnvelope() {
            // Spec: the dry-run 200 schema is AllPortsResponse — a paginated envelope
            // {pagination, data:[Port]} with the simulated updated port in data[0] (example
            // PortUpdateDryRunResponse) — a different wire shape from the real update's bare Port.
            String envelope = "{"
                    + "\"pagination\":{\"offset\":0,\"limit\":1,\"total\":1},"
                    + "\"data\":[" + loadFixture("/json/fabric/port_response.json")
                            .replace("testBuyer-SV5-NL-Dot1q-BO-PRI-10G-JN-154", "Renamed-Port") + "]"
                    + "}";
            wireMock.stubFor(patch(urlPathEqualTo("/fabric/v4/ports/c791f8cb-5cc9-cc90-8ce0-306a5c00a4ee"))
                    .willReturn(okJson(envelope)));

            Port simulated = fabric.ports().update("c791f8cb-5cc9-cc90-8ce0-306a5c00a4ee")
                    .name("Renamed-Port")
                    .dryRun()
                    .save();

            assertNotNull(simulated);
            assertEquals("c791f8cb-5cc9-cc90-8ce0-306a5c00a4ee", simulated.getUuid());
            assertEquals("Renamed-Port", simulated.getName(),
                    "save() must unwrap the simulated updated port from the envelope's data[0]");

            // Regression lock: dryRun=true MUST reach the wire as a query parameter on this exact
            // endpoint — dropping it would turn the validation into a REAL port update.
            wireMock.verify(patchRequestedFor(urlPathEqualTo("/fabric/v4/ports/c791f8cb-5cc9-cc90-8ce0-306a5c00a4ee"))
                    .withQueryParam("dryRun", equalTo("true"))
                    .withHeader("Content-Type", containing("application/json"))
                    .withRequestBody(equalToJson(
                            "[{\"op\":\"replace\",\"path\":\"/name\",\"value\":\"Renamed-Port\"}]")));
        }

        @Test
        @DisplayName("default save() does NOT send a dryRun query parameter")
        void updateWithoutDryRunOmitsQueryParameter() {
            wireMock.stubFor(patch(urlPathMatching("/fabric/v4/ports/.*"))
                    .willReturn(okJson(loadFixture("/json/fabric/port_response.json"))));

            fabric.ports().update("c791f8cb-5cc9-cc90-8ce0-306a5c00a4ee")
                    .name("Renamed-Port").save();

            wireMock.verify(patchRequestedFor(urlPathMatching("/fabric/v4/ports/.*"))
                    .withQueryParam("dryRun", absent()));
        }
    }

    @Nested
    @DisplayName("delete()")
    class Delete {

        @Test
        @DisplayName("DELETEs the port and returns the deleted object")
        void deletesPort() {
            wireMock.stubFor(delete(urlPathMatching("/fabric/v4/ports/.*"))
                    .willReturn(okJson(loadFixture("/json/fabric/port_response.json"))));

            Port deleted = fabric.ports().delete("c791f8cb-5cc9-cc90-8ce0-306a5c00a4ee");

            assertNotNull(deleted);
            assertEquals("c791f8cb-5cc9-cc90-8ce0-306a5c00a4ee", deleted.getUuid());
            wireMock.verify(deleteRequestedFor(urlPathMatching("/fabric/v4/ports/c791f8cb-5cc9-cc90-8ce0-306a5c00a4ee")));
        }

        @Test
        @DisplayName("delete(uuid, true) sends dryRun=true and returns the port that WOULD be deleted")
        void deleteDryRunSendsQueryParam() {
            // Spec: dry-run delete responds 200 (real delete is 202 'Accepted') with the existing
            // port entity — uuid/name and all — that would be deleted (example portDryRunDelete);
            // nothing is deleted.
            wireMock.stubFor(delete(urlPathEqualTo("/fabric/v4/ports/c791f8cb-5cc9-cc90-8ce0-306a5c00a4ee"))
                    .willReturn(okJson(loadFixture("/json/fabric/port_response.json"))));

            Port wouldDelete = fabric.ports().delete("c791f8cb-5cc9-cc90-8ce0-306a5c00a4ee", true);

            assertNotNull(wouldDelete);
            assertEquals("c791f8cb-5cc9-cc90-8ce0-306a5c00a4ee", wouldDelete.getUuid());
            assertEquals("testBuyer-SV5-NL-Dot1q-BO-PRI-10G-JN-154", wouldDelete.getName());
            assertEquals(PortState.ACTIVE, wouldDelete.getState());

            // Regression lock: dryRun=true MUST reach the wire as a query parameter on this exact
            // endpoint — dropping it would turn the validation into a REAL port delete.
            wireMock.verify(deleteRequestedFor(urlPathEqualTo("/fabric/v4/ports/c791f8cb-5cc9-cc90-8ce0-306a5c00a4ee"))
                    .withQueryParam("dryRun", equalTo("true")));
        }

        @Test
        @DisplayName("delete(uuid) and delete(uuid, false) do NOT send a dryRun query parameter")
        void deleteWithoutDryRunOmitsQueryParameter() {
            wireMock.stubFor(delete(urlPathMatching("/fabric/v4/ports/.*"))
                    .willReturn(okJson(loadFixture("/json/fabric/port_response.json"))));

            fabric.ports().delete("c791f8cb-5cc9-cc90-8ce0-306a5c00a4ee");
            fabric.ports().delete("c791f8cb-5cc9-cc90-8ce0-306a5c00a4ee", false);

            wireMock.verify(2, deleteRequestedFor(urlPathMatching("/fabric/v4/ports/.*"))
                    .withQueryParam("dryRun", absent()));
        }
    }

    @Nested
    @DisplayName("addToLag()")
    class AddToLag {

        @Test
        @DisplayName("POSTs {data:[...]} to physicalPorts/bulk and deserializes the response")
        void addsToLag() {
            String responseBody = "{"
                    + "\"pagination\":{\"offset\":0,\"limit\":20,\"total\":1},"
                    + "\"data\":[{\"uuid\":\"phys-1\",\"type\":\"XF_PHYSICAL_PORT\",\"state\":\"ACTIVE\"}]"
                    + "}";
            wireMock.stubFor(post(urlPathMatching("/fabric/v4/ports/.*/physicalPorts/bulk"))
                    .willReturn(okJson(responseBody)));

            PhysicalPortsResponseJson response = fabric.ports().addToLag(
                    "c791f8cb-5cc9-cc90-8ce0-306a5c00a4ee", List.of(new PhysicalPort()));

            assertNotNull(response);
            assertNotNull(response.getData());
            assertEquals(1, response.getData().size());
            assertEquals("phys-1", response.getData().get(0).getUuid());

            wireMock.verify(postRequestedFor(urlPathMatching("/fabric/v4/ports/c791f8cb-5cc9-cc90-8ce0-306a5c00a4ee/physicalPorts/bulk"))
                    .withRequestBody(matchingJsonPath("$.data")));
        }
    }

    @Nested
    @DisplayName("Error handling")
    class Errors {

        @Test
        @DisplayName("500 throws EquinixServerException")
        void serverError() {
            stubErrorInline(wireMock, "/fabric/v4/ports/.*",
                    500, "[{\"errorCode\":\"ERR-500\",\"errorMessage\":\"Internal server error\"}]");

            assertThrows(EquinixServerException.class,
                    () -> fabric.ports().getByUuid("test-uuid"));
        }
    }

    @Nested
    @DisplayName("Wrapper refresh()")
    class WrapperRefresh {

        private static final String PORT_ID = "c791f8cb-5cc9-cc90-8ce0-306a5c00a4ee";
        private static final String URL = "/fabric/v4/ports/" + PORT_ID;

        @Test
        @DisplayName("re-GETs /ports/{uuid} and swaps the wrapper's state in place")
        void refreshReloadsInPlace() {
            wireMock.stubFor(get(urlPathEqualTo(URL))
                    .inScenario("port-refresh")
                    .whenScenarioStateIs(com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED)
                    .willReturn(okJson(loadFixture("/json/fabric/port_response.json")))
                    .willSetStateTo("renamed"));
            wireMock.stubFor(get(urlPathEqualTo(URL))
                    .inScenario("port-refresh")
                    .whenScenarioStateIs("renamed")
                    .willReturn(okJson(loadFixture("/json/fabric/port_response.json")
                            .replace("testBuyer-SV5-NL-Dot1q-BO-PRI-10G-JN-154", "Renamed-Port"))));

            Port port = fabric.ports().getByUuid(PORT_ID);
            assertEquals("testBuyer-SV5-NL-Dot1q-BO-PRI-10G-JN-154", port.getName());

            // refresh() lives on the wrapper only — the Port interface does not declare it
            // (unlike Connection/CloudRouter/...), so the public path needs a cast.
            Port refreshed = port.refresh();

            assertSame(port, refreshed, "refresh() returns the same live wrapper");
            assertEquals("Renamed-Port", port.getName(),
                    "refresh() must swap the wrapper's backing state in place");
            wireMock.verify(2, getRequestedFor(urlPathEqualTo(URL)));
        }
    }

    @Nested
    @DisplayName("Multi-page list paging")
    class Paging {

        private static final String PAGE_1 = """
                {
                  "pagination": { "offset": 0, "limit": 100, "total": 150 },
                  "data": [ { "uuid": "PAGE1_PORT" } ]
                }
                """;

        private static final String PAGE_2 = """
                {
                  "pagination": { "offset": 100, "limit": 100, "total": 150 },
                  "data": [ { "uuid": "PAGE2_PORT" } ]
                }
                """;

        @Test
        @DisplayName("loadAll() re-GETs /ports with the offset query param advanced to page 2")
        void loadAllFetchesSecondPage() {
            // Page 1: catch-all, registered first (WireMock: the later, more specific stub wins).
            wireMock.stubFor(get(urlPathEqualTo("/fabric/v4/ports"))
                    .willReturn(okJson(PAGE_1)));
            // Page 2: matched by the advanced offset query parameter.
            wireMock.stubFor(get(urlPathEqualTo("/fabric/v4/ports"))
                    .withQueryParam("offset", equalTo("100"))
                    .willReturn(okJson(PAGE_2)));

            PaginatedList<Port> ports = fabric.ports().list();
            assertEquals(1, ports.size());
            assertTrue(ports.hasNextPage());

            ports.loadAll();

            assertEquals(2, ports.size());
            assertEquals("PAGE1_PORT", ports.get(0).getUuid());
            assertEquals("PAGE2_PORT", ports.get(1).getUuid());
            assertFalse(ports.hasNextPage());

            wireMock.verify(1, getRequestedFor(urlPathEqualTo("/fabric/v4/ports"))
                    .withQueryParam("offset", equalTo("100")));
        }
    }
}
