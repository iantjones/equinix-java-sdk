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

package com.eqixiac.equinix.mcp.server;

import com.eqixiac.equinix.CustomerPortal;
import com.eqixiac.equinix.FabricGateway;
import com.eqixiac.equinix.core.enums.MetroCode;
import com.eqixiac.equinix.core.enums.Region;
import com.eqixiac.equinix.core.http.response.PaginatedFilteredList;
import com.eqixiac.equinix.core.http.response.PaginatedList;
import com.eqixiac.equinix.core.model.MetroId;
import com.eqixiac.equinix.customerportal.client.BillingAccounts;
import com.eqixiac.equinix.customerportal.model.BillingAccount;
import com.eqixiac.equinix.fabric.client.Metros;
import com.eqixiac.equinix.fabric.client.ServiceProfiles;
import com.eqixiac.equinix.fabric.model.Metro;
import com.eqixiac.equinix.fabric.model.MetroRegistry;
import com.eqixiac.equinix.fabric.model.ServiceProfile;
import com.eqixiac.equinix.fabric.model.implementation.ConnectedMetro;
import com.eqixiac.equinix.fabric.model.implementation.GeoCoordinate;
import com.eqixiac.equinix.fabric.model.implementation.ServiceProfileMetro;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * End-to-end {@code design_plan_deployment} account-number auto-resolution over a Mockito-stubbed
 * Fabric gateway (the shared three-AMER-metro / one-AWS-profile fixture) and a stubbed Customer Portal
 * billing surface. The exchange is bound the way the live adapter binds it — via
 * {@link ServerContext#withExchange}.
 */
@DisplayName("design_plan_deployment — billing account auto-resolution")
class PlanDeploymentAccountTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String OPTIMIZATION = """
            {"optimization": {
                "workloads": [{"label": "Web Tier", "type": "general_compute", "bandwidth_mbps": 1000}],
                "sites": [{"label": "HQ", "metro_code": "DC"}],
                "require_clouds": ["aws"],
                "constraints": {"max_metros": 1}},
             "deployment": {"router_package": "STANDARD"%s}}
            """;

    private FabricGateway fabric;
    private Metros metros;

    @BeforeEach
    void stubGateway() throws Exception {
        Metro dc = metro("DC", "Ashburn", 39.0438, -77.4874);
        Metro da = metro("DA", "Dallas", 32.7767, -96.7970);
        Metro sv = metro("SV", "Silicon Valley", 37.3382, -121.8863);

        metros = mock(Metros.class);
        when(metros.list()).thenReturn(new PaginatedList<>(List.of(dc, da, sv), null, null, null, null));

        ServiceProfile awsProfile = mock(ServiceProfile.class);
        when(awsProfile.getUuid()).thenReturn("sp-aws-1");
        when(awsProfile.getName()).thenReturn("Amazon Web Services Direct Connect");
        when(awsProfile.metros()).thenReturn(List.of(
                serviceProfileMetro("DC", "us-east-1"), serviceProfileMetro("DA", "us-east-1")));

        ServiceProfiles serviceProfiles = mock(ServiceProfiles.class);
        when(serviceProfiles.search()).thenReturn(
                new PaginatedFilteredList<>(List.of(awsProfile), null, null, null, null));

        fabric = mock(FabricGateway.class);
        when(fabric.metros()).thenReturn(metros);
        when(fabric.serviceProfiles()).thenReturn(serviceProfiles);
    }

    private ServerContext contextWithAccounts(BillingAccount... accounts) throws Exception {
        BillingAccounts billingAccounts = mock(BillingAccounts.class);
        when(billingAccounts.summaries()).thenReturn(
                new PaginatedList<>(List.of(accounts), null, null, null, null));
        CustomerPortal portal = mock(CustomerPortal.class);
        when(portal.billingAccounts()).thenReturn(billingAccounts);
        return ServerContext.builder()
                .fabric(fabric)
                .metroRegistry(MetroRegistry.load(metros))
                .customerPortal(portal)
                .environment(Map.of())
                .build();
    }

    /** A context whose Customer Portal must never be consulted (asserts the explicit path skips resolution). */
    private ServerContext contextWithNoPortal() throws Exception {
        return ServerContext.builder()
                .fabric(fabric)
                .metroRegistry(MetroRegistry.load(metros))
                .environment(Map.of())
                .build();
    }

    private static BillingAccount account(String number, String name) {
        BillingAccount account = mock(BillingAccount.class);
        when(account.getAccountNumber()).thenReturn(number);
        when(account.getAccountName()).thenReturn(name);
        return account;
    }

    private static ToolRegistration tool() {
        return EquinixMcpServer.catalog(EnumSet.of(Toolset.DESIGN)).stream()
                .filter(t -> t.getName().equals("design_plan_deployment"))
                .findFirst()
                .orElseThrow();
    }

    private static ObjectNode plan(ServerContext ctx, String deploymentExtra, McpSyncServerExchange exchange)
            throws Exception {
        JsonNode args = MAPPER.readTree(String.format(OPTIMIZATION, deploymentExtra));
        return ctx.withExchange(exchange, () -> tool().getHandler().handle(args, ctx));
    }

    @Test
    @DisplayName("a single visible account is auto-filled and stamped on the planned Cloud Router body")
    void singleAccountAutoFilled() throws Exception {
        ServerContext ctx = contextWithAccounts(account("272010", "Acme Corp"));
        ObjectNode payload = plan(ctx, "", null);

        assertEquals("auto_resolved", payload.get("account_resolution").get("resolution").asText());
        assertEquals("272010", payload.get("account_resolution").get("account_number").asText());
        assertTrue(payload.get("cloud_routers").size() > 0, "a Cloud Router is planned");
        assertEquals(272010, payload.get("cloud_routers").get(0).get("account_number").asLong(),
                "the resolved account is carried on the router the plan will dry-run/create: "
                        + payload.get("cloud_routers").toPrettyString());
    }

    @Test
    @DisplayName("several accounts + an elicitation-capable client uses the picked account")
    void multipleAccountsElicitedPickApplied() throws Exception {
        ServerContext ctx = contextWithAccounts(account("111", "Alpha"), account("222", "Bravo"));
        ObjectNode payload = plan(ctx, "", StubExchanges.accepts("222"));

        assertEquals("auto_resolved", payload.get("account_resolution").get("resolution").asText());
        assertEquals("222", payload.get("account_resolution").get("account_number").asText());
        assertEquals(222, payload.get("cloud_routers").get(0).get("account_number").asLong());
    }

    @Test
    @DisplayName("several accounts + a client that cannot prompt returns 'choice_required' naming the candidates")
    void multipleAccountsUnsupportedChoiceRequired() throws Exception {
        ServerContext ctx = contextWithAccounts(account("111", "Alpha"), account("222", "Bravo"));
        ObjectNode payload = plan(ctx, "", null);

        assertEquals("choice_required", payload.get("status").asText(),
                "no plan is built until the account is chosen: " + payload.toPrettyString());
        assertFalse(payload.has("cloud_routers"), "a choice_required result carries no plan");
        List<String> candidates = payload.get("account_number").get("candidates").findValuesAsText("account_number");
        assertTrue(candidates.contains("111") && candidates.contains("222"),
                "both candidate account numbers are named: " + candidates);
        assertEquals(0, ctx.planStore().size(), "nothing is stored when the account is unresolved");
    }

    @Test
    @DisplayName("an explicit account_number overrides resolution and never consults the billing API")
    void explicitAccountOverrides() throws Exception {
        ServerContext ctx = contextWithNoPortal();
        ObjectNode payload = plan(ctx, ", \"account_number\": 999888", null);

        assertEquals("explicit", payload.get("account_resolution").get("resolution").asText());
        assertEquals("999888", payload.get("account_resolution").get("account_number").asText());
        assertEquals(999888, payload.get("cloud_routers").get(0).get("account_number").asLong());
    }

    @Test
    @DisplayName("zero visible accounts leaves the account unset and says so — no fabricated number")
    void zeroAccountsHonestAndUnfabricated() throws Exception {
        ServerContext ctx = contextWithAccounts();
        ObjectNode payload = plan(ctx, "", null);

        assertEquals("unresolved", payload.get("account_resolution").get("resolution").asText());
        assertTrue(payload.get("account_resolution").get("account_number").isNull(),
                "no account number is fabricated");
        assertTrue(payload.get("cloud_routers").get(0).get("account_number").isNull(),
                "the planned router carries no fabricated account number");
        assertFalse(payload.get("executed").asBoolean(), "the plan is still produced, just without an account");
    }

    // ── shared fabric stub builders (same recipe as DesignToolsTest) ─────────

    private static Metro metro(String code, String name, double lat, double lon) throws Exception {
        Metro m = mock(Metro.class);
        when(m.metroId()).thenReturn(MetroId.of(code));
        when(m.getCode()).thenReturn(MetroCode.fromCode(code));
        when(m.getName()).thenReturn(name);
        when(m.getRegion()).thenReturn(Region.AMER);
        when(m.geoCoordinates()).thenReturn(geo(lat, lon));
        when(m.getConnectedMetros()).thenReturn(List.<ConnectedMetro>of());
        return m;
    }

    private static ServiceProfileMetro serviceProfileMetro(String code, String sellerRegion) throws Exception {
        return MAPPER.readValue("{\"code\":\"" + code + "\",\"name\":\"" + code
                        + "\",\"sellerRegions\":{\"" + sellerRegion + "\":\"" + sellerRegion + "\"}}",
                ServiceProfileMetro.class);
    }

    private static GeoCoordinate geo(double lat, double lon) throws Exception {
        return MAPPER.readValue("{\"latitude\":" + lat + ",\"longitude\":" + lon + "}", GeoCoordinate.class);
    }
}
