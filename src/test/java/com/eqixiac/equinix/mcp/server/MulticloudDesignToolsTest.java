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

import com.eqixiac.equinix.FabricGateway;
import com.eqixiac.equinix.core.enums.MetroCode;
import com.eqixiac.equinix.core.enums.Region;
import com.eqixiac.equinix.core.http.response.PaginatedFilteredList;
import com.eqixiac.equinix.core.http.response.PaginatedList;
import com.eqixiac.equinix.core.model.MetroId;
import com.eqixiac.equinix.core.model.multicloud.BandwidthTier;
import com.eqixiac.equinix.design.optimizer.enums.MulticloudEnvironmentStatus;
import com.eqixiac.equinix.design.optimizer.model.MulticloudEnvironment;
import com.eqixiac.equinix.design.optimizer.model.MulticloudEnvironmentCatalog;
import com.eqixiac.equinix.design.optimizer.wizard.enums.ConnectionPurpose;
import com.eqixiac.equinix.design.optimizer.wizard.enums.MulticloudLinkRole;
import com.eqixiac.equinix.design.optimizer.wizard.model.DeploymentPlan;
import com.eqixiac.equinix.design.optimizer.wizard.model.PlannedConnection;
import com.eqixiac.equinix.design.optimizer.wizard.model.PlannedMulticloudInterconnect;
import com.eqixiac.equinix.design.optimizer.wizard.model.ProfileCandidate;
import com.eqixiac.equinix.design.optimizer.wizard.model.ProfileSelection;
import com.eqixiac.equinix.design.value.ratecard.EgressPath;
import com.eqixiac.equinix.design.value.ratecard.EgressRate;
import com.eqixiac.equinix.design.value.ratecard.PriceQuote;
import com.eqixiac.equinix.design.value.ratecard.PriceSource;
import com.eqixiac.equinix.design.value.ratecard.RateCard;
import com.eqixiac.equinix.design.value.ratecard.Term;
import com.eqixiac.equinix.fabric.client.Metros;
import com.eqixiac.equinix.fabric.client.ServiceProfiles;
import com.eqixiac.equinix.fabric.enums.ConnectionType;
import com.eqixiac.equinix.fabric.model.Metro;
import com.eqixiac.equinix.fabric.model.MetroRegistry;
import com.eqixiac.equinix.fabric.model.ServiceProfile;
import com.eqixiac.equinix.fabric.model.implementation.ConnectedMetro;
import com.eqixiac.equinix.fabric.model.implementation.GeoCoordinate;
import com.eqixiac.equinix.fabric.model.implementation.ServiceProfileMetro;
import com.eqixiac.equinix.fabric.model.implementation.cloud.CloudProviderType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Currency;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Handler tests for the cloud-to-cloud tools over a Mockito-stubbed {@link FabricGateway} (one
 * metro, DC, carrying an AWS profile in {@code us-east-1} and a Google Cloud profile in
 * {@code us-east4}) and the reference data bundled with the SDK. No HTTP calls: every context
 * replaces the live provider-pricing factory.
 *
 * <p>The expected native-link figures are the providers' published list prices as bundled on
 * 2026-09-21: AWS Interconnect - multicloud 10 Gbps tier 1 at 12.33 USD/h
 * ({@code https://aws.amazon.com/interconnect/multicloud/pricing/}) and Google Cloud Partner
 * Cross-Cloud Interconnect transport 10 Gbps, North America, at 19.00 USD/h
 * ({@code https://cloud.google.com/network-connectivity/docs/interconnect/pricing}), each converted
 * at 730 h/month. A change to those assertions means the bundled reference data changed.</p>
 */
@DisplayName("cloud-to-cloud design tools (stubbed FabricGateway, bundled reference data)")
class MulticloudDesignToolsTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String AWS_GCP_10G = """
            {"a": {"cloud": "aws", "region": "us-east-1"},
             "z": {"cloud": "gcp", "region": "us-east4"},
             "bandwidth_mbps": 10000""";

    private FabricGateway fabric;
    private Metros metros;
    private ServerContext context;

    @BeforeEach
    void stubGateway() throws Exception {
        Metro dc = metro("DC", "Ashburn", 39.0438, -77.4874);
        metros = mock(Metros.class);
        when(metros.list()).thenReturn(new PaginatedList<>(List.of(dc), null, null, null, null));

        ServiceProfile aws = profile("sp-aws-1", "Amazon Web Services Direct Connect",
                serviceProfileMetro("DC", "us-east-1"));
        ServiceProfile gcp = profile("sp-gcp-1", "Google Cloud Partner Interconnect Zone 1",
                serviceProfileMetro("DC", "us-east4"));
        ServiceProfiles serviceProfiles = mock(ServiceProfiles.class);
        when(serviceProfiles.search()).thenReturn(
                new PaginatedFilteredList<>(List.of(aws, gcp), null, null, null, null));

        fabric = mock(FabricGateway.class);
        when(fabric.metros()).thenReturn(metros);
        when(fabric.serviceProfiles()).thenReturn(serviceProfiles);

        context = contextWithLiveCards((provider, ctx) -> Optional.empty(), Map.of());
    }

    private ServerContext contextWithLiveCards(ServerContext.ProviderRateCardFactory factory,
                                               Map<String, String> environment) {
        return ServerContext.builder()
                .fabric(fabric)
                .metroRegistry(MetroRegistry.load(metros))
                .environment(environment)
                .providerRateCardFactory(factory)
                .build();
    }

    private static ToolRegistration tool(String name) {
        return EquinixMcpServer.catalog(EnumSet.of(Toolset.DESIGN)).stream()
                .filter(t -> t.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new AssertionError("tool not in catalog: " + name));
    }

    private ObjectNode call(String name, String argsJson) throws Exception {
        return call(name, context, argsJson);
    }

    private static ObjectNode call(String name, ServerContext ctx, String argsJson) throws Exception {
        return tool(name).getHandler().handle(MAPPER.readTree(argsJson), ctx);
    }

    private IllegalArgumentException rejected(String name, String argsJson) {
        return assertThrows(IllegalArgumentException.class, () -> call(name, argsJson));
    }

    private static JsonNode path(ObjectNode payload, String name) {
        for (JsonNode p : payload.get("paths")) {
            if (name.equals(p.get("path").asText())) {
                return p;
            }
        }
        throw new AssertionError("no path '" + name + "' in " + payload.toPrettyString());
    }

    private static JsonNode side(JsonNode nativePath, String provider) {
        for (JsonNode s : nativePath.get("sides")) {
            if (provider.equals(s.get("provider").asText())) {
                return s;
            }
        }
        throw new AssertionError("no side '" + provider + "' in " + nativePath.toPrettyString());
    }

    private static void assertMoney(String expected, JsonNode actual, String what) {
        assertNotNull(actual, what + " is missing");
        assertFalse(actual.isNull(), what + " is null");
        assertEquals(0, new BigDecimal(expected).compareTo(actual.decimalValue()),
                what + ": expected " + expected + " but was " + actual);
    }

    private static List<String> texts(JsonNode array) {
        List<String> values = new ArrayList<>();
        array.forEach(n -> values.add(n.asText()));
        return values;
    }

    // ── design_compare_cloud_to_cloud ───────────────────────────────────────

    @Nested
    @DisplayName("design_compare_cloud_to_cloud")
    class Compare {

        @Test
        @DisplayName("AWS us-east-1 to Google Cloud us-east4 at 10 Gbps: three priced paths, per-side provenance, break-even")
        void threeWayComparison() throws Exception {
            ObjectNode payload = call("design_compare_cloud_to_cloud", AWS_GCP_10G
                    + ", \"sustained_mbps\": 2000, \"cloud_router_package\": \"STANDARD\"}");

            assertTrue(payload.get("beta").asBoolean());
            assertEquals(List.of("public_internet", "equinix_fabric", "native_multicloud_link"),
                    texts(MAPPER.valueToTree(payload.get("paths").findValuesAsText("path"))));
            assertEquals("USD", payload.get("currency").asText());

            // Traffic: 2000 Mbps summed = 1000 Mbps each way = 1000 x 2,628,000 s / 8000 Mb/GB.
            assertMoney("328500", payload.get("traffic").get("a_to_z_gb_per_month"), "a to z volume");
            assertMoney("328500", payload.get("traffic").get("z_to_a_gb_per_month"), "z to a volume");

            // Native link: the two published list prices, each converted at 730 h/month.
            JsonNode nativePath = path(payload, "native_multicloud_link");
            assertTrue(nativePath.get("priced").asBoolean());
            JsonNode awsSide = side(nativePath, "AWS");
            assertMoney("9000.90", awsSide.get("monthly_recurring"), "AWS side, 12.33 USD/h x 730 h");
            assertEquals("REFERENCE", awsSide.get("price_source").asText());
            assertTrue(awsSide.get("note").asText().contains("https://aws.amazon.com/interconnect/multicloud/pricing/"),
                    "the AWS side cites its pricing page: " + awsSide);
            assertTrue(awsSide.get("note").asText().contains("retrieved 2026-09-21"), awsSide.toString());
            JsonNode gcpSide = side(nativePath, "GOOGLE_CLOUD");
            assertMoney("13870.00", gcpSide.get("monthly_recurring"), "Google side, 19.00 USD/h x 730 h");
            assertTrue(gcpSide.get("note").asText()
                            .contains("https://cloud.google.com/network-connectivity/docs/interconnect/pricing"),
                    "the Google side cites its pricing page: " + gcpSide);
            assertMoney("22870.90", nativePath.get("fixed_monthly"), "native fixed monthly");
            assertMoney("0", nativePath.get("data_transfer_monthly"), "both providers publish 0 per GB");
            assertMoney("22870.90", nativePath.get("monthly_total"), "native monthly total");

            // The other two paths are priced and explain themselves.
            JsonNode internet = path(payload, "public_internet");
            assertTrue(internet.get("priced").asBoolean());
            assertMoney("0", internet.get("fixed_monthly"), "the internet path has no fixed cost");
            assertTrue(internet.get("monthly_total").decimalValue().signum() > 0);
            assertEquals(2, internet.get("provenance").size(), "one egress-rate note per cloud: " + internet);
            JsonNode equinix = path(payload, "equinix_fabric");
            assertTrue(equinix.get("priced").asBoolean());
            assertTrue(equinix.get("fixed_monthly").decimalValue().signum() > 0);
            assertTrue(equinix.get("provenance").size() >= 4,
                    "two connections, two ports, two egress rates: " + equinix);

            // Break-even: present, positive, and consistent with the two totals at 2000 Mbps.
            BigDecimal breakEven = payload.get("break_even_sustained_mbps").decimalValue();
            assertTrue(breakEven.signum() > 0, "break-even: " + breakEven);
            assertMoney(breakEven.divide(BigDecimal.valueOf(2), 1, java.math.RoundingMode.HALF_UP).toPlainString(),
                    payload.get("break_even_each_way_mbps"), "each-way break-even is half the summed rate");
            boolean equinixCheaperAtDeclaredRate = equinix.get("monthly_total").decimalValue()
                    .compareTo(nativePath.get("monthly_total").decimalValue()) < 0;
            assertEquals(BigDecimal.valueOf(2000).compareTo(breakEven) < 0, equinixCheaperAtDeclaredRate,
                    "below the break-even the Equinix path must be the cheaper private path, and vice versa");
            assertFalse(payload.get("break_even_exceeds_link_capacity").asBoolean());

            assertEquals(0, payload.get("unpriced_components").size(),
                    "everything is priced, including the A-side router: " + payload.get("unpriced_components"));
            String recommendation = payload.get("recommendation").asText();
            assertTrue(recommendation.contains("2000 Mbps") && recommendation.contains(breakEven.toPlainString()),
                    recommendation);
            assertNotNull(payload.get("lowest_cost_path"));

            // The catalog environment for the pair, with source and as-of.
            JsonNode environment = payload.get("environment");
            assertTrue(environment.get("listed").asBoolean());
            assertEquals("GA", environment.get("status").asText());
            assertTrue(environment.get("bandwidth_listed").asBoolean());
            assertEquals("2026-09-21", environment.get("as_of").asText());
            assertTrue(environment.get("sources").get(0).asText().startsWith("https://docs.aws.amazon.com/"));

            assertFalse(payload.get("disclaimer").asText().isBlank());
            assertTrue(payload.get("limits").size() >= 3, "the comparison states what it does not model");
        }

        @Test
        @DisplayName("an unpublished size leaves that side null with a reason: never 0, never rounded to a neighbouring size")
        void unpricedSideIsNullWithReason() throws Exception {
            // AWS publishes no 1 Gbps rate; Google publishes 3.50 USD/h for 1 Gbps in North America.
            ObjectNode payload = call("design_compare_cloud_to_cloud", """
                    {"a": {"cloud": "aws", "region": "us-east-1"},
                     "z": {"cloud": "gcp", "region": "us-east4"},
                     "bandwidth_mbps": 1000, "sustained_mbps": 500, "cloud_router_package": "STANDARD"}""");

            JsonNode nativePath = path(payload, "native_multicloud_link");
            assertFalse(nativePath.get("priced").asBoolean());
            assertTrue(nativePath.get("monthly_total").isNull(), "an unpriced path has no total: " + nativePath);
            assertTrue(nativePath.get("fixed_monthly").isNull(),
                    "one priced side is not presented as the link's fixed cost: " + nativePath);
            assertTrue(nativePath.get("total_over_term").isNull());

            JsonNode awsSide = side(nativePath, "AWS");
            assertFalse(awsSide.get("priced").asBoolean());
            assertTrue(awsSide.get("monthly_recurring").isNull(), "unpriced is null, not 0: " + awsSide);
            assertTrue(awsSide.get("unpriced_reason").asText().contains("1000 Mbps"), awsSide.toString());

            JsonNode gcpSide = side(nativePath, "GOOGLE_CLOUD");
            assertTrue(gcpSide.get("priced").asBoolean(), "the priced side is still reported on its own");
            assertMoney("2555.00", gcpSide.get("monthly_recurring"), "Google side, 3.50 USD/h x 730 h");

            List<String> unpriced = texts(payload.get("unpriced_components"));
            assertTrue(unpriced.stream().anyMatch(u -> u.startsWith("native_multicloud_link: the AWS side is unpriced")),
                    "the unpriced component is named: " + unpriced);
            assertTrue(payload.get("break_even_sustained_mbps").isNull(),
                    "no break-even without both private paths priced");
            assertFalse(payload.has("break_even_each_way_mbps"));
            String recommendation = payload.get("recommendation").asText();
            assertTrue(recommendation.startsWith("No recommendation"), recommendation);
            assertTrue(recommendation.contains("AWS side"), recommendation);

            // The Equinix path is unaffected and still priced.
            assertTrue(path(payload, "equinix_fabric").get("priced").asBoolean());
        }

        @Test
        @DisplayName("a provider with no bundled price (Oracle Cloud) is unpriced on every figure it feeds")
        void oracleSideUnpriced() throws Exception {
            ObjectNode payload = call("design_compare_cloud_to_cloud", """
                    {"a": {"cloud": "aws", "region": "us-east-1"},
                     "z": {"cloud": "oci", "region": "us-ashburn-1"},
                     "bandwidth_mbps": 10000}""");

            JsonNode oracle = side(path(payload, "native_multicloud_link"), "ORACLE_CLOUD");
            assertFalse(oracle.get("priced").asBoolean());
            assertTrue(oracle.get("monthly_recurring").isNull());
            assertFalse(oracle.get("unpriced_reason").asText().isBlank());
            // The AWS side at 10 Gbps is published and stays visible.
            assertMoney("9000.90", side(path(payload, "native_multicloud_link"), "AWS").get("monthly_recurring"),
                    "AWS side");
            assertTrue(payload.get("recommendation").asText().contains("ORACLE_CLOUD side"),
                    payload.get("recommendation").asText());
            // The pair is in the catalog as GA even though it cannot be priced.
            assertEquals("GA", payload.get("environment").get("status").asText());
        }

        @Test
        @DisplayName("without cloud_router_package the missing Equinix A-side is named as an unpriced component")
        void missingRouterIsNamed() throws Exception {
            ObjectNode payload = call("design_compare_cloud_to_cloud", AWS_GCP_10G + "}");
            List<String> unpriced = texts(payload.get("unpriced_components"));
            assertTrue(unpriced.stream().anyMatch(u -> u.startsWith("equinix_fabric: no Equinix A-side is priced")),
                    unpriced.toString());
            assertTrue(unpriced.stream().anyMatch(u -> u.contains("break_even_sustained_mbps is overstated")),
                    "the effect on the break-even is stated: " + unpriced);
        }

        @Test
        @DisplayName("without sustained_mbps: volume 0, no lowest_cost_path, and the recommendation is stated against the break-even")
        void noSustainedRate() throws Exception {
            ObjectNode payload = call("design_compare_cloud_to_cloud",
                    AWS_GCP_10G + ", \"cloud_router_package\": \"STANDARD\"}");

            assertFalse(payload.get("traffic").get("sustained_mbps_supplied").asBoolean());
            assertTrue(payload.get("traffic").get("sustained_mbps").isNull());
            assertMoney("0", payload.get("traffic").get("a_to_z_gb_per_month"), "volume");
            assertFalse(payload.has("lowest_cost_path"),
                    "at zero volume the internet path is trivially cheapest, so no path is named");
            String recommendation = payload.get("recommendation").asText();
            assertTrue(recommendation.startsWith("No sustained_mbps was given"), recommendation);
            assertTrue(recommendation.contains(payload.get("break_even_sustained_mbps").decimalValue().toPlainString()),
                    recommendation);
            // Fixed cost only.
            JsonNode equinix = path(payload, "equinix_fabric");
            assertEquals(0, equinix.get("fixed_monthly").decimalValue()
                    .compareTo(equinix.get("monthly_total").decimalValue()));
        }

        @Test
        @DisplayName("term_months changes total_over_term only")
        void termMonthsScope() throws Exception {
            ObjectNode twelve = call("design_compare_cloud_to_cloud", AWS_GCP_10G + ", \"term_months\": 12}");
            ObjectNode thirtySix = call("design_compare_cloud_to_cloud", AWS_GCP_10G + ", \"term_months\": 36}");

            JsonNode native12 = path(twelve, "native_multicloud_link");
            JsonNode native36 = path(thirtySix, "native_multicloud_link");
            assertEquals(0, native12.get("monthly_total").decimalValue()
                    .compareTo(native36.get("monthly_total").decimalValue()), "monthly figures do not move");
            assertMoney("274450.80", native12.get("total_over_term"), "22870.90 x 12");
            assertMoney("823352.40", native36.get("total_over_term"), "22870.90 x 36");
            assertEquals(36, thirtySix.get("term_months").asInt());

            assertTrue(rejected("design_compare_cloud_to_cloud", AWS_GCP_10G + ", \"term_months\": 7}")
                    .getMessage().contains("1, 12, 24, 36"));
        }

        @Test
        @DisplayName("path_tier is an input: tier 4 prices the AWS side at the published tier-4 rate")
        void pathTier() throws Exception {
            ObjectNode payload = call("design_compare_cloud_to_cloud", AWS_GCP_10G + ", \"path_tier\": 4}");
            assertEquals(4, payload.get("path_tier").asInt());
            assertMoney("37799.40", side(path(payload, "native_multicloud_link"), "AWS").get("monthly_recurring"),
                    "AWS side, 51.78 USD/h x 730 h");
            assertTrue(rejected("design_compare_cloud_to_cloud", AWS_GCP_10G + ", \"path_tier\": 9}")
                    .getMessage().contains("'path_tier' must be between 1 and 5"));
        }

        @Test
        @DisplayName("the AWS free tier is opt-in: a known price of 0 on the AWS side, while the unpublished Google size stays null")
        void awsFreeTierOptIn() throws Exception {
            String args = """
                    {"a": {"cloud": "aws", "region": "us-east-1"},
                     "z": {"cloud": "gcp", "region": "us-east4"},
                     "bandwidth_mbps": 500""";
            JsonNode optedIn = path(call("design_compare_cloud_to_cloud", args + ", \"use_aws_free_tier\": true}"),
                    "native_multicloud_link");
            JsonNode awsFree = side(optedIn, "AWS");
            assertTrue(awsFree.get("priced").asBoolean());
            assertMoney("0", awsFree.get("monthly_recurring"), "free tier applied on request");
            assertTrue(awsFree.get("note").asText().toLowerCase(java.util.Locale.ROOT).contains("free"),
                    awsFree.toString());
            assertTrue(side(optedIn, "GOOGLE_CLOUD").get("monthly_recurring").isNull(),
                    "Google publishes no 500 Mbps transport price");

            JsonNode notOptedIn = path(call("design_compare_cloud_to_cloud", args + "}"), "native_multicloud_link");
            assertFalse(side(notOptedIn, "AWS").get("priced").asBoolean(),
                    "the free tier is never inferred: without the flag the 500 Mbps AWS side is unpriced");

            assertTrue(rejected("design_compare_cloud_to_cloud", """
                    {"a": {"cloud": "gcp", "region": "us-east4"}, "z": {"cloud": "azure", "region": "eastus"},
                     "bandwidth_mbps": 1000, "use_aws_free_tier": true}""")
                    .getMessage().contains("'use_aws_free_tier' requires AWS"));
        }

        @Test
        @DisplayName("a PREVIEW pair and a pair absent from the catalog are both reported as such")
        void catalogCaveats() throws Exception {
            ObjectNode preview = call("design_compare_cloud_to_cloud", """
                    {"a": {"cloud": "aws", "region": "us-east-1"}, "z": {"cloud": "azure", "region": "eastus"},
                     "bandwidth_mbps": 1000}""");
            assertEquals("PREVIEW", preview.get("environment").get("status").asText());
            assertTrue(preview.get("recommendation").asText().contains("PREVIEW"),
                    preview.get("recommendation").asText());

            ObjectNode absent = call("design_compare_cloud_to_cloud", """
                    {"a": {"cloud": "aws", "region": "eu-west-1"}, "z": {"cloud": "gcp", "region": "europe-west1"},
                     "bandwidth_mbps": 10000}""");
            assertFalse(absent.get("environment").get("listed").asBoolean());
            assertTrue(absent.get("environment").get("note").asText().contains("not in this copy"),
                    absent.get("environment").toString());
            assertTrue(absent.get("recommendation").asText().contains("lists no native environment"),
                    absent.get("recommendation").asText());
        }

        @Test
        @DisplayName("argument errors name the tool's own fields")
        void argumentErrors() {
            assertTrue(rejected("design_compare_cloud_to_cloud", """
                    {"a": {"cloud": "aws", "region": "us-east-1"}, "z": {"cloud": "aws", "region": "us-west-2"},
                     "bandwidth_mbps": 1000}""").getMessage().contains("must be different providers"));
            assertTrue(rejected("design_compare_cloud_to_cloud", """
                    {"a": {"cloud": "aws"}, "z": {"cloud": "gcp", "region": "us-east4"}, "bandwidth_mbps": 1000}""")
                    .getMessage().contains("'a.region' is required"));
            assertTrue(rejected("design_compare_cloud_to_cloud", """
                    {"z": {"cloud": "gcp", "region": "us-east4"}, "bandwidth_mbps": 1000}""")
                    .getMessage().contains("'a' is required"));
            assertTrue(rejected("design_compare_cloud_to_cloud", """
                    {"a": {"cloud": "aws", "region": "us-east-1"}, "z": {"cloud": "gcp", "region": "us-east4"}}""")
                    .getMessage().contains("'bandwidth_mbps' is required"));
            assertTrue(rejected("design_compare_cloud_to_cloud", AWS_GCP_10G + ", \"sustained_mbps\": 20001}")
                    .getMessage().contains("20000 Mbps summed over both directions"));
            assertTrue(rejected("design_compare_cloud_to_cloud", AWS_GCP_10G + ", \"sustained_mbps\": -1}")
                    .getMessage().contains("'sustained_mbps' must be a finite number >= 0"));
        }

        @Test
        @DisplayName("a live provider card is used for egress rates and reported per provider")
        void liveCardUsed() throws Exception {
            ServerContext live = contextWithLiveCards(
                    (provider, ctx) -> Optional.of(cannedEgressCard(provider)), Map.of());
            ObjectNode payload = call("design_compare_cloud_to_cloud", live,
                    AWS_GCP_10G + ", \"sustained_mbps\": 2000}");

            // 328,500 GB each way x 0.09 USD/GB x 2 directions.
            assertMoney("59130.00", path(payload, "public_internet").get("monthly_total"), "internet path");
            for (JsonNode entry : payload.get("live_pricing")) {
                assertTrue(entry.get("attempted").asBoolean(), entry.toString());
                assertFalse(entry.get("degraded").asBoolean(), entry.toString());
            }
            // The live cards do not price the native link: its fees still come from reference data.
            assertEquals("REFERENCE",
                    side(path(payload, "native_multicloud_link"), "AWS").get("price_source").asText());
            assertMoney("0", path(payload, "native_multicloud_link").get("data_transfer_monthly"),
                    "the native per-GB rate comes from reference data, not from the live card");
        }

        @Test
        @Timeout(60)
        @DisplayName("a stalled live card is cut off by the pricing timeout, named, and replaced by reference rates")
        void stalledLiveCardDegrades() throws Exception {
            ServerContext stalled = contextWithLiveCards(
                    (provider, ctx) -> provider == CloudProviderType.AWS
                            ? Optional.of(stalledCard()) : Optional.empty(),
                    Map.of(ServerContext.ENV_PRICING_TIMEOUT_MS, "200"));

            long start = System.nanoTime();
            ObjectNode payload = call("design_compare_cloud_to_cloud", stalled,
                    AWS_GCP_10G + ", \"sustained_mbps\": 2000}");
            long elapsedMs = (System.nanoTime() - start) / 1_000_000;
            assertTrue(elapsedMs < 30_000, "every lookup is bounded: " + elapsedMs + " ms");

            JsonNode awsLive = payload.get("live_pricing").get(0);
            assertEquals("AWS", awsLive.get("provider").asText());
            assertTrue(awsLive.get("degraded").asBoolean());
            assertTrue(awsLive.get("failures").get(0).asText().contains("AWS"), awsLive.toString());
            assertTrue(awsLive.get("note").asText().contains("reference rates"));
            assertFalse(payload.get("live_pricing").get(1).get("attempted").asBoolean());
            assertTrue(path(payload, "public_internet").get("priced").asBoolean(),
                    "reference rates fill in for the stalled provider");
        }

        @Test
        @DisplayName("the schema states units, the null-not-zero rule, and what term_months does")
        void schemaContract() {
            ToolRegistration compare = tool("design_compare_cloud_to_cloud");
            assertTrue(compare.isReadOnly());
            String description = compare.getDescription();
            assertTrue(description.startsWith("Beta."), description);
            assertTrue(description.contains("never 0"), description);
            assertTrue(description.contains("no rounding up, interpolation or extrapolation"), description);
            assertTrue(description.contains("not in this dated copy"), description);

            Map<String, Object> props = properties(compare.getInputSchema());
            assertEquals(List.of("a", "z", "bandwidth_mbps"), compare.getInputSchema().get("required"));
            assertEquals(List.of(1, 12, 24, 36), child(props, "term_months").get("enum"));
            assertTrue(String.valueOf(child(props, "term_months").get("description")).contains("ONLY for"),
                    "term_months must not read as a pricing lever");
            assertTrue(String.valueOf(child(props, "sustained_mbps").get("description"))
                    .contains("summed over both directions"));
            assertTrue(String.valueOf(child(props, "use_aws_free_tier").get("description")).contains("never inferred"));
            assertEquals(List.of("cloud", "region"), child(props, "a").get("required"));
        }
    }

    // ── design_list_multicloud_environments ─────────────────────────────────

    @Nested
    @DisplayName("design_list_multicloud_environments")
    class ListEnvironments {

        @Test
        @DisplayName("no filter returns the whole bundled catalog, each entry with status, sources and as-of")
        void wholeCatalog() throws Exception {
            ObjectNode payload = call("design_list_multicloud_environments", "{}");

            // Bundled catalog as of 2026-09-21: 8 AWS-Google pairs and 1 AWS-Oracle pair (GA),
            // 4 AWS-Azure pairs (PREVIEW).
            assertEquals(13, payload.get("count").asInt());
            assertEquals(13, payload.get("environments").size());
            assertEquals(13, payload.get("catalog_size").asInt());
            assertEquals("2026-09-21", payload.get("catalog_as_of").asText());
            int ga = 0;
            int preview = 0;
            for (JsonNode env : payload.get("environments")) {
                assertEquals(2, env.get("sites").size(), env.toString());
                assertTrue(env.get("sources").size() >= 1, "every bundled entry cites a source: " + env);
                assertEquals("2026-09-21", env.get("as_of").asText());
                assertTrue(env.get("id_scope").asText().contains("not accepted by any provider API"));
                if ("GA".equals(env.get("status").asText())) {
                    ga++;
                }
                else if ("PREVIEW".equals(env.get("status").asText())) {
                    preview++;
                }
            }
            assertEquals(9, ga);
            assertEquals(4, preview);
            assertTrue(payload.get("staleness_note").asText().contains("not in this copy"));
            assertFalse(payload.get("disclaimer").asText().isBlank());
        }

        @Test
        @DisplayName("a cloud pair is unordered; the AWS and Google Cloud sizes are the four with a published price")
        void pairFilterIsUnordered() throws Exception {
            ObjectNode awsGcp = call("design_list_multicloud_environments",
                    "{\"a\": {\"cloud\": \"aws\"}, \"z\": {\"cloud\": \"gcp\"}}");
            ObjectNode gcpAws = call("design_list_multicloud_environments",
                    "{\"a\": {\"cloud\": \"gcp\"}, \"z\": {\"cloud\": \"aws\"}}");

            assertEquals(8, awsGcp.get("count").asInt());
            assertEquals(awsGcp.get("environments"), gcpAws.get("environments"));
            JsonNode first = awsGcp.get("environments").get(0);
            assertEquals("aws-us-east-1--gcp-us-east4", first.get("id").asText());
            assertEquals(List.of("1000", "5000", "10000", "100000"),
                    texts(first.get("supported_connection_sizes_mbps")));
            assertEquals("GA", first.get("status").asText());
        }

        @Test
        @DisplayName("one cloud, and one cloud plus region (trimmed, case-insensitive)")
        void singleCloudAndRegion() throws Exception {
            ObjectNode oracle = call("design_list_multicloud_environments", "{\"a\": {\"cloud\": \"oci\"}}");
            assertEquals(1, oracle.get("count").asInt());
            assertEquals("aws-us-east-1--oci-us-ashburn-1", oracle.get("environments").get(0).get("id").asText());
            assertFalse(oracle.get("filter").get("a").has("region"));

            ObjectNode byRegion = call("design_list_multicloud_environments",
                    "{\"z\": {\"cloud\": \"aws\", \"region\": \" US-EAST-1 \"}}");
            assertEquals(3, byRegion.get("count").asInt(), "Google Cloud, Oracle Cloud and Azure pair with us-east-1");
            for (JsonNode env : byRegion.get("environments")) {
                assertTrue(env.get("id").asText().startsWith("aws-us-east-1--"), env.get("id").asText());
            }

            ObjectNode none = call("design_list_multicloud_environments", "{\"a\": {\"cloud\": \"ibm_cloud\"}}");
            assertEquals(0, none.get("count").asInt());
            assertTrue(none.get("staleness_note").asText().contains("not 'not offered'"));
        }

        @Test
        @DisplayName("an injected catalog replaces the bundled one for every cloud-to-cloud tool")
        void injectedCatalog() throws Exception {
            MulticloudEnvironment custom = MulticloudEnvironment.of(
                    CloudProviderType.AWS, "eu-west-1", CloudProviderType.GOOGLE_CLOUD, "europe-west1",
                    MulticloudEnvironmentStatus.GA, BandwidthTier.of(10000), "https://example.com/regions",
                    "2026-10-01");
            ServerContext custCtx = ServerContext.builder()
                    .fabric(fabric)
                    .environment(Map.of())
                    .providerRateCardFactory((provider, ctx) -> Optional.empty())
                    .multicloudEnvironments(MulticloudEnvironmentCatalog.of(List.of(custom)))
                    .build();

            ObjectNode listed = call("design_list_multicloud_environments", custCtx, "{}");
            assertEquals(1, listed.get("count").asInt());
            assertEquals("https://example.com/regions", listed.get("environments").get(0).get("sources").get(0).asText());
            assertTrue(listed.get("catalog_as_of").isNull(), "a caller-built catalog has no catalog-level date");

            ObjectNode compared = call("design_compare_cloud_to_cloud", custCtx, """
                    {"a": {"cloud": "aws", "region": "eu-west-1"}, "z": {"cloud": "gcp", "region": "europe-west1"},
                     "bandwidth_mbps": 10000}""");
            assertTrue(compared.get("environment").get("listed").asBoolean());
            assertEquals("2026-10-01", compared.get("environment").get("as_of").asText());
        }

        @Test
        @DisplayName("the same cloud at both ends is rejected; the tool is closed-world and read-only")
        void rejectsSameCloud() {
            assertTrue(rejected("design_list_multicloud_environments",
                    "{\"a\": {\"cloud\": \"aws\"}, \"z\": {\"cloud\": \"aws\"}}")
                    .getMessage().contains("must differ"));
            ToolRegistration list = tool("design_list_multicloud_environments");
            assertTrue(list.isReadOnly());
            assertFalse(list.isOpenWorld(), "it reads the bundled catalog and calls nothing");
            assertTrue(list.getDescription().contains("calls no cloud-provider API"));
        }
    }

    // ── design_plan_deployment: cloud_to_cloud_strategy ─────────────────────

    @Nested
    @DisplayName("design_plan_deployment cloud-to-cloud flows")
    class PlanDeployment {

        private static final String TWO_CLOUD_PLAN = """
                {"optimization": {
                    "workloads": [{"label": "Replication", "type": "general_compute", "bandwidth_mbps": 10000,
                                   "requires_clouds": ["aws", "gcp"]}],
                    "require_clouds": ["aws", "gcp"],
                    "constraints": {"max_metros": 1}},
                 "deployment": {"notifications": ["noc@example.com"]%s}}""";

        private ObjectNode plan(String extraDeploymentFields) throws Exception {
            return call("design_plan_deployment", TWO_CLOUD_PLAN.formatted(extraDeploymentFields));
        }

        @Test
        @DisplayName("default strategy is compare: the Equinix path is planned and a priced native ALTERNATIVE is attached")
        void defaultIsCompare() throws Exception {
            ObjectNode payload = plan("");

            assertEquals(List.of("FCR-DC-to-aws", "FCR-DC-to-gcp"),
                    texts(MAPPER.valueToTree(payload.get("provider_connections").findValuesAsText("name"))),
                    "the Equinix connections are planned unchanged");
            assertTrue(payload.get("validation").get("valid").asBoolean(), payload.get("validation").toString());

            JsonNode links = payload.get("native_multicloud_links");
            assertNotNull(links, "a GA catalog pair at the planned regions yields a native link: "
                    + payload.toPrettyString());
            assertEquals(1, links.size());
            JsonNode link = links.get(0);
            assertEquals("ALTERNATIVE", link.get("role").asText());
            assertEquals("COMPARE", link.get("strategy").asText());
            assertEquals("AWS", link.get("a").get("cloud").asText());
            assertEquals("us-east-1", link.get("a").get("region").asText());
            assertEquals("GOOGLE_CLOUD", link.get("z").get("cloud").asText());
            assertEquals("us-east4", link.get("z").get("region").asText());
            assertEquals(10000, link.get("covering_tier_mbps").asInt());
            assertEquals("GA", link.get("environment").get("status").asText());
            assertEquals(List.of("Replication"), texts(link.get("workloads")));
            assertFalse(link.get("provisioned_by_this_server").asBoolean());

            JsonNode pricing = link.get("pricing");
            assertTrue(pricing.get("native_priced").asBoolean());
            assertMoney("22870.90", pricing.get("native_monthly"), "native monthly, both sides");
            assertEquals(2, pricing.get("native_sides").size());
            assertTrue(pricing.get("equinix_fixed_monthly").decimalValue().signum() > 0);
            assertTrue(pricing.get("break_even_sustained_mbps").decimalValue().signum() > 0);
            assertFalse(link.get("recommendation").asText().isBlank());

            // The create-then-accept steps come through verbatim, and the activation key in them is
            // kept apart from this server's confirm token.
            List<String> recipe = texts(link.get("create_then_accept_recipe"));
            assertTrue(recipe.size() >= 3, recipe.toString());
            assertTrue(recipe.stream().anyMatch(step -> step.contains("activation key")), recipe.toString());
            assertTrue(link.get("recipe_scope").asText().contains("unrelated to this server's"),
                    link.get("recipe_scope").asText());

            // Native fees are reported beside, never inside, the Equinix totals.
            JsonNode planPricing = payload.get("pricing");
            assertMoney("22870.90", planPricing.get("native_alternative_monthly"), "native alternative monthly");
            assertTrue(planPricing.get("native_replacement_monthly").isNull(), "nothing was replaced");
            BigDecimal categories = planPricing.get("router_monthly").decimalValue()
                    .add(planPricing.get("provider_connection_monthly").decimalValue())
                    .add(planPricing.get("backbone_monthly").decimalValue());
            assertEquals(0, categories.compareTo(planPricing.get("monthly_total").decimalValue()),
                    "monthly_total is the Equinix categories only: " + planPricing);
            assertTrue(payload.get("native_multicloud_scope").asText().contains("never provisioned"));
        }

        @Test
        @DisplayName("equinix_only passes through: no native links, no native pricing fields")
        void equinixOnly() throws Exception {
            ObjectNode payload = plan(", \"cloud_to_cloud_strategy\": \"equinix_only\"");
            assertFalse(payload.has("native_multicloud_links"));
            assertFalse(payload.has("native_multicloud_scope"));
            assertFalse(payload.get("pricing").has("native_alternative_monthly"));
            assertEquals(2, payload.get("provider_connections").size());
        }

        @Test
        @DisplayName("native_when_available passes through: the link records the strategy and why each connection was kept")
        void nativeWhenAvailable() throws Exception {
            JsonNode link = plan(", \"cloud_to_cloud_strategy\": \"native_when_available\"")
                    .get("native_multicloud_links").get(0);
            assertEquals("NATIVE_WHEN_AVAILABLE", link.get("strategy").asText());
            // Through this tool a connection exists only for a request-level cloud, and the replacement
            // rule keeps those, so the link stays an ALTERNATIVE and says so.
            assertEquals("ALTERNATIVE", link.get("role").asText());
            assertEquals(0, link.get("replaced_connections").size());
            assertTrue(texts(link.get("reasoning")).stream()
                            .anyMatch(r -> r.contains("Kept FCR-DC-to-aws") && r.contains("request-level")),
                    link.get("reasoning").toString());
        }

        @Test
        @DisplayName("native_only with no catalog environment for the planned regions invalidates the plan with an UNAVAILABLE link")
        void nativeOnlyWithoutEnvironment() throws Exception {
            ServerContext emptyCatalog = ServerContext.builder()
                    .fabric(fabric)
                    .metroRegistry(MetroRegistry.load(metros))
                    .environment(Map.of())
                    .providerRateCardFactory((provider, ctx) -> Optional.empty())
                    .multicloudEnvironments(MulticloudEnvironmentCatalog.empty())
                    .build();
            ObjectNode payload = call("design_plan_deployment", emptyCatalog,
                    TWO_CLOUD_PLAN.formatted(", \"cloud_to_cloud_strategy\": \"native_only\""));

            assertFalse(payload.get("validation").get("valid").asBoolean(), payload.get("validation").toString());
            JsonNode link = payload.get("native_multicloud_links").get(0);
            assertEquals("UNAVAILABLE", link.get("role").asText());
            assertEquals("NATIVE_ONLY", link.get("strategy").asText());
            assertTrue(link.get("environment").isNull());
            assertTrue(link.get("pricing").isNull(), "an unavailable link carries no price, not a zero price");
        }

        @Test
        @DisplayName("multicloud_path_tier reaches the native link's AWS side; bad values are rejected in the tool's terms")
        void pathTierPassthrough() throws Exception {
            JsonNode sides = plan(", \"multicloud_path_tier\": 4").get("native_multicloud_links").get(0)
                    .get("pricing").get("native_sides");
            assertMoney("37799.40", sides.get(0).get("monthly_recurring"), "AWS side at tier 4");

            assertTrue(assertThrows(IllegalArgumentException.class, () -> plan(", \"multicloud_path_tier\": 0"))
                    .getMessage().contains("'deployment.multicloud_path_tier' must be between 1 and 5"));
            assertTrue(assertThrows(IllegalArgumentException.class,
                    () -> plan(", \"cloud_to_cloud_strategy\": \"native_first\""))
                    .getMessage().contains("equinix_only, native_when_available, native_only, compare"));
        }

        @Test
        @DisplayName("a single-cloud plan is unchanged: no native keys anywhere in the payload")
        void singleCloudPlanUnchanged() throws Exception {
            ObjectNode payload = call("design_plan_deployment", """
                    {"optimization": {
                        "workloads": [{"label": "Web Tier", "type": "general_compute", "bandwidth_mbps": 1000}],
                        "require_clouds": ["aws"],
                        "constraints": {"max_metros": 1}},
                     "deployment": {"notifications": ["noc@example.com"]}}""");
            assertFalse(payload.has("native_multicloud_links"));
            assertFalse(payload.toString().contains("native_"), "no native_* key leaks into a single-cloud plan");
        }

        @Test
        @DisplayName("the schema offers the four strategies, names compare as the default, and states the tool's replacement limit")
        void schemaContract() {
            ToolRegistration planTool = tool("design_plan_deployment");
            Map<String, Object> deployment = child(properties(planTool.getInputSchema()), "deployment");
            Map<String, Object> strategy = child(properties(deployment), "cloud_to_cloud_strategy");
            assertEquals(List.of("equinix_only", "native_when_available", "native_only", "compare"),
                    strategy.get("enum"));
            String description = String.valueOf(strategy.get("description"));
            assertTrue(description.contains("Default compare"), description);
            assertTrue(description.contains("never provisions"), description);
            assertTrue(description.contains("REPLACEMENT arises only through the SDK"), description);
            assertTrue(planTool.getDescription().contains("CLOUD-TO-CLOUD FLOWS (Beta)"));
            assertTrue(planTool.getDescription().contains("never part of pricing.monthly_total"));
        }

        @Test
        @DisplayName("an elicited profile pick rebuilds the plan without dropping its native links")
        void profilePickKeepsNativeLinks() {
            ProfileCandidate def = ProfileCandidate.builder().serviceProfileUuid("sp-default")
                    .sellerRegions(List.of("us-east-1")).coveringTierMbps(10000)
                    .supportedBandwidths(List.of(10000)).build();
            ProfileCandidate alt = ProfileCandidate.builder().serviceProfileUuid("sp-alt")
                    .sellerRegions(List.of("us-east-1")).coveringTierMbps(10000)
                    .supportedBandwidths(List.of(10000)).build();
            PlannedConnection connection = PlannedConnection.builder()
                    .name("FCR-DC-to-aws").connectionType(ConnectionType.IP_VC).purpose(ConnectionPurpose.PROVIDER)
                    .bandwidthMbps(10000)
                    .profileSelection(ProfileSelection.builder().requestedMbps(10000)
                            .selectedProfileUuid("sp-default").selectedSellerRegion("us-east-1")
                            .selectedTierMbps(10000).alternatives(List.of(def, alt)).build())
                    .aSideMetro(MetroId.of("DC")).aSideRouterName("FCR-DC")
                    .zSideProviderLabel("AWS").zSideSellerRegion("us-east-1")
                    .zSideServiceProfileUuid("sp-default").build();
            PlannedMulticloudInterconnect link = PlannedMulticloudInterconnect.builder()
                    .name("aws-gcp-DC").providerA(CloudProviderType.AWS).regionA("us-east-1")
                    .providerZ(CloudProviderType.GOOGLE_CLOUD).regionZ("us-east4")
                    .role(MulticloudLinkRole.ALTERNATIVE).requestedMbps(10000).coveringTierMbps(10000).build();
            DeploymentPlan plan = DeploymentPlan.builder()
                    .cloudRouters(List.of()).providerConnections(List.of(connection)).backboneLinks(List.of())
                    .multicloudLinks(List.of(link)).valid(true).build();

            DesignToolFactory.ProfileChoices choices =
                    DesignToolFactory.applyProfileChoices(plan, context, StubExchanges.accepts("sp-alt"));

            assertTrue(choices.changed());
            assertEquals("sp-alt", choices.plan().getProviderConnections().get(0).getZSideServiceProfileUuid());
            assertEquals(List.of(link), choices.plan().multicloudLinksOrEmpty(),
                    "the rebuilt plan carries every field of the original, native links included");
        }
    }

    // ── design_estimate_tco: peer cloud ─────────────────────────────────────

    @Nested
    @DisplayName("design_estimate_tco with a peer cloud")
    class EstimateTco {

        @Test
        @DisplayName("peer_cloud makes the comparison two-sided and adds the native archetype with provenance")
        void peerCloud() throws Exception {
            ObjectNode payload = call("design_estimate_tco", """
                    {"monthly_egress_gb": 328500, "cloud": "aws", "region": "us-east-1",
                     "peer_cloud": "gcp", "peer_region": "us-east4", "bandwidth_mbps": 10000, "metro_code": "DC"}""");

            JsonNode nativeBreakdown = null;
            for (JsonNode b : payload.get("breakdowns")) {
                if ("NATIVE_MULTICLOUD_INTERCONNECT".equals(b.get("archetype").asText())) {
                    nativeBreakdown = b;
                }
            }
            assertNotNull(nativeBreakdown, "the native archetype joins the default set: " + payload.toPrettyString());
            assertTrue(nativeBreakdown.get("fully_priced").asBoolean());
            assertMoney("22870.90", nativeBreakdown.get("monthly_total"), "native archetype monthly");
            List<String> provenance = texts(nativeBreakdown.get("provenance"));
            assertTrue(provenance.stream().anyMatch(p -> p.contains("12.33 USD/h x 730 h/month")), provenance.toString());
            assertTrue(provenance.stream().anyMatch(p -> p.contains("https://aws.amazon.com/interconnect/multicloud/pricing/")),
                    provenance.toString());
            assertTrue(payload.get("traffic_note").asText().contains("GOOGLE_CLOUD"),
                    payload.get("traffic_note").asText());
        }

        @Test
        @DisplayName("an unpriced native side leaves the archetype partially priced, and it is never the recommendation")
        void partiallyPricedNativeIsNotRecommended() throws Exception {
            ObjectNode payload = call("design_estimate_tco", """
                    {"monthly_egress_gb": 1000, "cloud": "aws", "region": "us-east-1",
                     "peer_cloud": "gcp", "peer_region": "us-east4", "bandwidth_mbps": 1000}""");
            for (JsonNode b : payload.get("breakdowns")) {
                if ("NATIVE_MULTICLOUD_INTERCONNECT".equals(b.get("archetype").asText())) {
                    assertFalse(b.get("fully_priced").asBoolean(), "AWS publishes no 1 Gbps rate: " + b);
                    assertTrue(b.get("note").asText().contains("AWS"), b.get("note").asText());
                }
            }
            assertFalse("NATIVE_MULTICLOUD_INTERCONNECT".equals(payload.get("recommended_archetype").asText()));
        }

        @Test
        @DisplayName("without peer_cloud the payload carries no cloud-to-cloud keys, and the dependent inputs are rejected by name")
        void singleCloudUnchangedAndLeversValidated() throws Exception {
            ObjectNode single = call("design_estimate_tco", """
                    {"monthly_egress_gb": 50000, "cloud": "aws", "region": "us-east-1", "bandwidth_mbps": 1000}""");
            assertFalse(single.has("traffic_note"));
            for (JsonNode b : single.get("breakdowns")) {
                assertFalse(b.has("provenance"), "a single-cloud breakdown is unchanged: " + b);
                assertFalse("NATIVE_MULTICLOUD_INTERCONNECT".equals(b.get("archetype").asText()));
            }

            String base = "{\"monthly_egress_gb\": 1000, \"cloud\": \"aws\", \"region\": \"us-east-1\"";
            assertTrue(rejected("design_estimate_tco", base + ", \"path_tier\": 2}")
                    .getMessage().contains("'path_tier' requires 'peer_cloud'"));
            assertTrue(rejected("design_estimate_tco", base + ", \"peer_region\": \"us-east4\"}")
                    .getMessage().contains("'peer_region' requires 'peer_cloud'"));
            assertTrue(rejected("design_estimate_tco",
                    base + ", \"archetypes\": [\"native_multicloud_interconnect\"]}")
                    .getMessage().contains("requires 'peer_cloud'"));
            assertTrue(rejected("design_estimate_tco", base + ", \"peer_cloud\": \"aws\"}")
                    .getMessage().contains("'peer_cloud' must differ from 'cloud'"));
            assertTrue(rejected("design_estimate_tco", base + ", \"peer_cloud\": \"gcp\", \"path_tier\": 6}")
                    .getMessage().contains("'path_tier' must be between 1 and 5"));
        }
    }

    // ── fixtures ────────────────────────────────────────────────────────────

    /** A live card that answers internet and private egress for its own provider only. */
    private static RateCard cannedEgressCard(CloudProviderType owner) {
        return new RateCard() {
            @Override
            public Optional<PriceQuote> connection(ConnectionType type, int bandwidthMbps, MetroCode metro, Term term) {
                return Optional.empty();
            }

            @Override
            public Optional<PriceQuote> cloudRouter(String packageCode, MetroCode metro, Term term) {
                return Optional.empty();
            }

            @Override
            public Optional<EgressRate> egress(CloudProviderType provider, String region, EgressPath path, Term term) {
                if (provider != owner || path == EgressPath.MULTICLOUD_INTERCONNECT) {
                    return Optional.empty();
                }
                BigDecimal rate = path == EgressPath.INTERNET ? new BigDecimal("0.09") : new BigDecimal("0.02");
                return Optional.of(EgressRate.of(rate, Currency.getInstance("USD"), PriceSource.PROVIDER_API));
            }

            @Override
            public PriceSource source() {
                return PriceSource.PROVIDER_API;
            }
        };
    }

    /** A live card whose egress lookup blocks until interrupted. */
    private static RateCard stalledCard() {
        return new RateCard() {
            @Override
            public Optional<PriceQuote> connection(ConnectionType type, int bandwidthMbps, MetroCode metro, Term term) {
                return Optional.empty();
            }

            @Override
            public Optional<PriceQuote> cloudRouter(String packageCode, MetroCode metro, Term term) {
                return Optional.empty();
            }

            @Override
            public Optional<EgressRate> egress(CloudProviderType provider, String region, EgressPath path, Term term) {
                try {
                    Thread.sleep(60_000);
                }
                catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return Optional.empty();
            }

            @Override
            public PriceSource source() {
                return PriceSource.PROVIDER_API;
            }
        };
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> properties(Map<String, Object> schema) {
        Map<String, Object> props = (Map<String, Object>) schema.get("properties");
        assertNotNull(props, "not an object schema: " + schema);
        return props;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> child(Map<String, Object> properties, String name) {
        Object next = properties.get(name);
        assertNotNull(next, "no '" + name + "' property in " + properties.keySet());
        return (Map<String, Object>) next;
    }

    private static Metro metro(String code, String name, double lat, double lon) throws Exception {
        Metro m = mock(Metro.class);
        when(m.metroId()).thenReturn(MetroId.of(code));
        when(m.getCode()).thenReturn(MetroCode.fromCode(code));
        when(m.getName()).thenReturn(name);
        when(m.getRegion()).thenReturn(Region.AMER);
        when(m.geoCoordinates()).thenReturn(MAPPER.readValue(
                "{\"latitude\":" + lat + ",\"longitude\":" + lon + "}", GeoCoordinate.class));
        when(m.getConnectedMetros()).thenReturn(List.<ConnectedMetro>of());
        return m;
    }

    private static ServiceProfile profile(String uuid, String name, ServiceProfileMetro... profileMetros) {
        ServiceProfile p = mock(ServiceProfile.class);
        when(p.getUuid()).thenReturn(uuid);
        when(p.getName()).thenReturn(name);
        when(p.metros()).thenReturn(List.of(profileMetros));
        return p;
    }

    private static ServiceProfileMetro serviceProfileMetro(String code, String sellerRegion) throws Exception {
        return MAPPER.readValue("{\"code\":\"" + code + "\",\"name\":\"" + code
                + "\",\"sellerRegions\":{\"" + sellerRegion + "\":\"" + sellerRegion + "\"}}",
                ServiceProfileMetro.class);
    }
}
