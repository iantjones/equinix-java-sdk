package com.eqixiac.equinix.fabric.wiremock;

import com.eqixiac.equinix.Fabric;
import com.eqixiac.equinix.core.WireMockTestBase;
import com.eqixiac.equinix.fabric.enums.CloudRouterState;
import com.eqixiac.equinix.fabric.model.CloudRouter;
import com.eqixiac.equinix.fabric.model.Connection;
import com.eqixiac.equinix.fabric.model.RouteFilterRule;
import org.junit.jupiter.api.*;

import static com.eqixiac.equinix.core.ResponseStubs.*;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * A model wrapper stays usable after {@code delete()}: on {@code 204 No Content} it keeps its
 * last-known state (it used to replace its backing JSON with {@code null}, so every getter and
 * {@code refresh()} threw a NullPointerException); on a body it adopts the returned state.
 * One test per wrapper delete pattern: no-argument delete, parent-id delete, delete with a body.
 */
class FabricWrapperDeleteWireMockTest extends WireMockTestBase {

    static Fabric fabric;

    private static final String ROUTER_ID = "a1b2c3d4-e5f6-7890-abcd-ef1234567890";
    private static final String ROUTE_FILTER_ID = "rf-0000-1111";
    private static final String RULE_ID = "b1c2d3e4-f5a6-7890-bcde-f01234567890";
    private static final String CONNECTION_ID = "3a58dd05-f46d-4b1d-a154-2e85c396ea85";

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

    @Test
    @DisplayName("no-argument delete on 204 keeps the last-known state and refresh() still works")
    void cloudRouterDelete204KeepsState() {
        stubSingleton(wireMock, "/fabric/v4/routers/" + ROUTER_ID, "/json/fabric/cloud_router_response.json");
        stubDeleteNoContent(wireMock, "/fabric/v4/routers/" + ROUTER_ID);

        CloudRouter router = fabric.cloudRouters().getByUuid(ROUTER_ID);
        assertFalse(router.isDeleted());
        assertTrue(router.delete());

        assertTrue(router.isDeleted());
        assertEquals(ROUTER_ID, router.getUuid());
        assertEquals(CloudRouterState.PROVISIONED, router.getState());
        assertDoesNotThrow(router::refresh);
        assertEquals(ROUTER_ID, router.getUuid());
        wireMock.verify(deleteRequestedFor(urlPathEqualTo("/fabric/v4/routers/" + ROUTER_ID)));
    }

    @Test
    @DisplayName("parent-id delete on 204 keeps the last-known state")
    void routeFilterRuleDelete204KeepsState() {
        String url = "/fabric/v4/routeFilters/" + ROUTE_FILTER_ID + "/routeFilterRules/" + RULE_ID;
        stubSingleton(wireMock, url, "/json/fabric/route_filter_rule_response.json");
        stubDeleteNoContent(wireMock, url);

        RouteFilterRule rule = fabric.routeFilterRules().getByUuid(ROUTE_FILTER_ID, RULE_ID);
        assertTrue(rule.delete(ROUTE_FILTER_ID));

        assertTrue(rule.isDeleted());
        assertEquals(RULE_ID, rule.getUuid());
        assertEquals("Allow-10-0-0-0-8", rule.getName());
        wireMock.verify(deleteRequestedFor(urlPathEqualTo(url)));
    }

    @Test
    @DisplayName("delete with a response body adopts the returned state")
    void connectionDeleteWithBodyAdoptsState() {
        String url = "/fabric/v4/connections/" + CONNECTION_ID;
        stubSingleton(wireMock, url, "/json/fabric/connection_response.json");
        wireMock.stubFor(delete(urlPathEqualTo(url)).willReturn(aResponse().withStatus(202)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"uuid\":\"" + CONNECTION_ID + "\",\"name\":\"My-EVPL-Connection\",\"state\":\"DEPROVISIONING\"}")));

        Connection connection = fabric.connections().getByUuid(CONNECTION_ID);
        assertTrue(connection.delete());

        assertTrue(connection.isDeleted());
        assertEquals(CONNECTION_ID, connection.getUuid());
        assertEquals("DEPROVISIONING", String.valueOf(connection.getState()));
    }
}
