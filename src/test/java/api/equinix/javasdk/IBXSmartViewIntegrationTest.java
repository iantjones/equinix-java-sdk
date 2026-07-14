package api.equinix.javasdk;

import api.equinix.javasdk.core.IntegrationTestBase;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.ibxsmartview.enums.ChannelType;
import api.equinix.javasdk.ibxsmartview.enums.PowerLevelType;
import api.equinix.javasdk.ibxsmartview.model.*;
import api.equinix.javasdk.ibxsmartview.model.implementation.*;
import api.equinix.javasdk.ibxsmartview.model.json.creators.*;
import org.junit.jupiter.api.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Live integration tests for the IBX SmartView domain, catalog-complete against the
 * safe-operation inventory of {@code smartviewv2.yaml} (all GET reads and read-only
 * POST search/batch-read variants).
 *
 * <p>Every test runs through {@code requireEntitled}: a 401/403 (credential not entitled to
 * SmartView) skips, while any other failure — deserialization crash, 5xx, unmapped enum,
 * 404 on a collection URL — fails. This is the spec-vs-reality contract of the read-only
 * tier. Item reads that need a live identifier (sensor, subscription, asset, tag point)
 * discover one from the corresponding list/search first and skip when the account has none.</p>
 *
 * <h3>Usage</h3>
 * <pre>
 * mvn test -Pintegration-readonly -DaccessKey=ID -DsecretKey=SECRET
 *          [-DtestIbxCode=DC2] [-DtestAccountNo=123456]
 * </pre>
 *
 * <p>{@code -DtestIbxCode} selects the IBX under test (default {@code DC2}).
 * {@code -DtestAccountNo} supplies the Equinix account number passed to the legacy
 * environment/power and asset endpoints; when omitted, {@code null} is sent and the API
 * resolves the caller's default account (house precedent from the original live suite).</p>
 *
 * <p>The SmartView safe inventory contains no documented dry-run/validate operations, so this
 * class has no {@code integration-dryrun} tier. Mutations (streaming-subscription create/delete,
 * power-alert-configuration lifecycle) belong to {@code integration-full} and are not covered here.</p>
 */
@Tag("integration-readonly")
@DisplayName("IBX SmartView Integration Tests")
class IBXSmartViewIntegrationTest extends IntegrationTestBase {

    private static final String CLASSIFICATION_ELECTRICAL = "Electrical";
    private static final String CLASSIFICATION_MECHANICAL = "Mechanical";

    static IBXSmartView client;
    static String testIbxCode;
    static String testAccountNo;

    // Lazily discovered live identifiers, shared across asset-dependent tests.
    private static boolean assetDiscoveryAttempted;
    private static String discoveredAssetId;
    private static String discoveredAssetClassification;
    private static boolean tagDiscoveryAttempted;
    private static String discoveredTagId;

    @BeforeAll
    static void setUp() {
        client = new IBXSmartView(testCredentials());
        client.authenticate();
        testIbxCode = System.getProperty("testIbxCode", "DC2");
        testAccountNo = System.getProperty("testAccountNo");
    }

    // ── Discovery helpers (skip via Assumptions when the account has no data) ──

    /**
     * Discovers a live asset identifier from the asset list (Electrical first, then Mechanical),
     * caching the result. Skips the calling test when the account has no monitored assets.
     */
    static String requireDiscoveredAssetId() {
        if (!assetDiscoveryAttempted) {
            assetDiscoveryAttempted = true;
            for (String classification : List.of(CLASSIFICATION_ELECTRICAL, CLASSIFICATION_MECHANICAL)) {
                AssetsList assetsList = requireEntitled("IBXSmartView", "getAsset", "AssetsList", "GET",
                        () -> client.smartViewAssets().list(testAccountNo, testIbxCode, classification, null));
                String assetId = firstAssetId(assetsList);
                if (assetId != null) {
                    discoveredAssetId = assetId;
                    discoveredAssetClassification = classification;
                    break;
                }
            }
        }
        Assumptions.assumeTrue(discoveredAssetId != null,
                "No SmartView assets found in " + testIbxCode + "; skipping asset-dependent test");
        return discoveredAssetId;
    }

    /**
     * Discovers a live tag-point identifier from the discovered asset's details, caching the
     * result. Skips the calling test when no asset or no tag points exist.
     */
    static String requireDiscoveredTagId() {
        String assetId = requireDiscoveredAssetId();
        if (!tagDiscoveryAttempted) {
            tagDiscoveryAttempted = true;
            AssetDetail detail = requireEntitled("IBXSmartView", "getAssetDetails", "AssetDetail", "GET",
                    () -> client.smartViewAssets().getAssetDetails(
                            testAccountNo, testIbxCode, discoveredAssetClassification, assetId));
            if (detail != null && detail.getPayLoad() != null && detail.getPayLoad().getTags() != null) {
                for (TagPointDataArray tag : detail.getPayLoad().getTags()) {
                    if (tag.getTagId() != null) {
                        discoveredTagId = tag.getTagId();
                        break;
                    }
                }
            }
        }
        Assumptions.assumeTrue(discoveredTagId != null,
                "No tag points found on asset " + assetId + "; skipping tag-point test");
        return discoveredTagId;
    }

    private static String firstAssetId(AssetsList assetsList) {
        if (assetsList == null || assetsList.getPayLoad() == null
                || assetsList.getPayLoad().getCategories() == null) {
            return null;
        }
        for (Category category : assetsList.getPayLoad().getCategories()) {
            if (category.getTemplates() == null) {
                continue;
            }
            for (Template template : category.getTemplates()) {
                if (template.getAssets() == null) {
                    continue;
                }
                for (AssetForAssetAPI asset : template.getAssets()) {
                    if (asset.getAssetId() != null) {
                        return asset.getAssetId();
                    }
                }
            }
        }
        return null;
    }

    // ════════════════════════════════════════════════════════════════════
    //  READONLY TESTS — one test per safe inventory operation
    // ════════════════════════════════════════════════════════════════════

    /**
     * Environmental sensor readings ({@code /smartview/v2/environmental/ibxs/...}):
     * inventory ops {@code getSensorReadings} and {@code getSingleSensorReadings}.
     */
    @Nested
    @Tag("integration-readonly")
    @DisplayName("Environmental Sensors")
    class EnvironmentalSensorTests {

        @Test
        @DisplayName("sensors_list - List environmental sensor readings for IBX [getSensorReadings]")
        void sensors_list() {
            PaginatedList<SensorReading> readings = requireEntitled("IBXSmartView", "list", "SensorReading", "GET",
                    () -> client.environmentals().list(testIbxCode));
            assertNotNull(readings);
            if (!readings.isEmpty()) {
                SensorReading first = readings.get(0);
                assertNotNull(first.getSensorId(), "sensorId should be populated");
                assertNotNull(first.getIbx(), "ibx should be populated");
                // Touch the typed readings to force deserialization of the nested value/unit pairs.
                first.getTemperature();
                first.getHumidity();
            }
        }

        @Test
        @DisplayName("sensors_getSingle - Get readings for one sensor (if any exist) [getSingleSensorReadings]")
        void sensors_getSingle() {
            PaginatedList<SensorReading> readings = requireEntitled("IBXSmartView", "list", "SensorReading", "GET",
                    () -> client.environmentals().list(testIbxCode));
            Assumptions.assumeTrue(!readings.isEmpty(),
                    "No environmental sensors found in " + testIbxCode + "; skipping single-sensor test");

            String sensorId = readings.get(0).getSensorId();
            SensorReading reading = requireEntitled("IBXSmartView", "getSensorReading", "SensorReading", "GET",
                    () -> client.environmentals().getSensorReading(testIbxCode, sensorId));
            assertNotNull(reading);
            assertEquals(sensorId, reading.getSensorId());
        }
    }

    /**
     * Streaming subscriptions ({@code /smartview/v2/streaming/...}): inventory ops
     * {@code getAllSubscriptions}, {@code getSubscriptionById}, {@code getSubscriptionData}
     * and {@code getCertificate}.
     */
    @Nested
    @Tag("integration-readonly")
    @DisplayName("Streaming Subscriptions")
    class StreamingSubscriptionTests {

        @Test
        @DisplayName("subscriptions_list - List streaming subscriptions [getAllSubscriptions]")
        void subscriptions_list() {
            List<StreamingSubscription> subscriptions = requireEntitled(
                    "IBXSmartView", "list", "StreamingSubscription", "GET",
                    () -> client.streamingSubscriptions().list());
            assertNotNull(subscriptions);
            if (!subscriptions.isEmpty()) {
                StreamingSubscription first = subscriptions.get(0);
                assertNotNull(first.getId(), "subscription id should be populated");
                first.getStatus();
                first.getChannel();
            }
        }

        @Test
        @DisplayName("subscriptions_getByUuid - Get subscription by id (if any exist) [getSubscriptionById]")
        void subscriptions_getByUuid() {
            List<StreamingSubscription> subscriptions = requireEntitled(
                    "IBXSmartView", "list", "StreamingSubscription", "GET",
                    () -> client.streamingSubscriptions().list());
            Assumptions.assumeTrue(!subscriptions.isEmpty(),
                    "No streaming subscriptions found; skipping get-by-id test");

            String id = subscriptions.get(0).getId();
            StreamingSubscription subscription = requireEntitled(
                    "IBXSmartView", "getByUuid", "StreamingSubscription", "GET",
                    () -> client.streamingSubscriptions().getByUuid(id));
            assertNotNull(subscription);
            assertEquals(id, subscription.getId());
        }

        @Test
        @DisplayName("subscriptions_getData - Get near real-time subscription data (if any exist) [getSubscriptionData]")
        void subscriptions_getData() {
            List<StreamingSubscription> subscriptions = requireEntitled(
                    "IBXSmartView", "list", "StreamingSubscription", "GET",
                    () -> client.streamingSubscriptions().list());
            Assumptions.assumeTrue(!subscriptions.isEmpty(),
                    "No streaming subscriptions found; skipping subscription-data test");

            String id = subscriptions.get(0).getId();
            SubscriptionData data = requireEntitled(
                    "IBXSmartView", "getSubscriptionData", "SubscriptionData", "GET",
                    () -> client.streamingSubscriptions().getSubscriptionData(id));
            assertNotNull(data);
            // Message-type buckets are nullable; touching them forces deserialization when present.
            data.getEnvironmentMessageData();
            data.getPowerMessageData();
            data.getPagination();
        }

        @Test
        @DisplayName("subscriptions_getCertificate - Get channel certificate for a provisioned channel type [getCertificate]")
        void subscriptions_getCertificate() {
            List<StreamingSubscription> subscriptions = requireEntitled(
                    "IBXSmartView", "list", "StreamingSubscription", "GET",
                    () -> client.streamingSubscriptions().list());

            // The certificate endpoint only serves AWS_IOT_CORE/WEBHOOK channels; ground the
            // channelType in a live subscription so we never ask for a never-provisioned channel.
            ChannelType channelType = null;
            for (StreamingSubscription subscription : subscriptions) {
                Channel channel = subscription.getChannel();
                if (channel != null && (channel.getChannelType() == ChannelType.AWS_IOT_CORE
                        || channel.getChannelType() == ChannelType.WEBHOOK)) {
                    channelType = channel.getChannelType();
                    break;
                }
            }
            Assumptions.assumeTrue(channelType != null,
                    "No AWS_IOT_CORE/WEBHOOK streaming subscription found; skipping certificate test");

            final ChannelType resolvedChannelType = channelType;
            SubscriptionCertificate certificate = requireEntitled(
                    "IBXSmartView", "getCertificate", "SubscriptionCertificate", "GET",
                    () -> client.streamingSubscriptions().getCertificate(resolvedChannelType.name()));
            assertNotNull(certificate);
            assertEquals(resolvedChannelType, certificate.getChannelType());
            assertNotNull(certificate.getCertificateBase64(), "certificate content should be populated");
        }
    }

    /**
     * Hierarchy trees ({@code /smartview/v1/hierarchy/...}): inventory ops
     * {@code getLocationHierarchy} and {@code getPowerHierarchy}.
     */
    @Nested
    @Tag("integration-readonly")
    @DisplayName("Hierarchy")
    class HierarchyTests {

        @Test
        @DisplayName("hierarchy_location - Get location hierarchy for IBX [getLocationHierarchy]")
        void hierarchy_location() {
            List<HierarchyNode> hierarchy = requireEntitled(
                    "IBXSmartView", "getLocationHierarchy", "HierarchyNode", "GET",
                    () -> client.hierarchy().getLocationHierarchy(testAccountNo, testIbxCode));
            assertNotNull(hierarchy);
            if (!hierarchy.isEmpty()) {
                HierarchyNode first = hierarchy.get(0);
                assertNotNull(first.getLevelType(), "levelType should be populated");
                first.getLevelValue();
                first.getChildren();
            }
        }

        @Test
        @DisplayName("hierarchy_power - Get power hierarchy for IBX [getPowerHierarchy]")
        void hierarchy_power() {
            List<PowerHierarchyNode> hierarchy = requireEntitled(
                    "IBXSmartView", "getPowerHierarchy", "PowerHierarchyNode", "GET",
                    () -> client.hierarchy().getPowerHierarchy(testAccountNo, testIbxCode));
            assertNotNull(hierarchy);
            if (!hierarchy.isEmpty()) {
                PowerHierarchyNode first = hierarchy.get(0);
                assertNotNull(first.getLevelType(), "levelType should be populated");
                first.getLevelValue();
                first.getChildren();
            }
        }
    }

    /**
     * SmartView assets ({@code /smartview/v1/asset/...}): inventory ops {@code getAsset},
     * {@code searchAsset}, {@code getAssetDetails}, {@code postAssetDetails},
     * {@code getAffectedAsset}, {@code getCurrentTagPoint} and {@code postCurrentTagPoint}.
     */
    @Nested
    @Tag("integration-readonly")
    @DisplayName("SmartView Assets")
    class AssetTests {

        @Test
        @DisplayName("assets_list - List assets as category/template/asset hierarchy [getAsset]")
        void assets_list() {
            AssetsList assets = requireEntitled("IBXSmartView", "list", "AssetsList", "GET",
                    () -> client.smartViewAssets().list(
                            testAccountNo, testIbxCode, CLASSIFICATION_ELECTRICAL, null));
            assertNotNull(assets);
            if (assets.getPayLoad() != null) {
                assets.getPayLoad().getClassification();
                assets.getPayLoad().getCategories();
            }
        }

        @Test
        @DisplayName("assets_search - Wildcard search matching a live asset id [searchAsset]")
        void assets_search() {
            String assetId = requireDiscoveredAssetId();
            Assets results = requireEntitled("IBXSmartView", "search", "Assets", "GET",
                    () -> client.smartViewAssets().search(testAccountNo, testIbxCode, assetId));
            assertNotNull(results);
            if (results.getPayLoad() != null && results.getPayLoad().getAssetsList() != null
                    && !results.getPayLoad().getAssetsList().isEmpty()) {
                AssetsArray first = results.getPayLoad().getAssetsList().get(0);
                assertNotNull(first.getAssetId(), "assetId should be populated on search hits");
                first.getAssetClassification();
            }
        }

        @Test
        @DisplayName("assets_getDetails - Get details (incl. tag points) for one asset [getAssetDetails]")
        void assets_getDetails() {
            String assetId = requireDiscoveredAssetId();
            AssetDetail detail = requireEntitled("IBXSmartView", "getAssetDetails", "AssetDetail", "GET",
                    () -> client.smartViewAssets().getAssetDetails(
                            testAccountNo, testIbxCode, discoveredAssetClassification, assetId));
            assertNotNull(detail);
            assertNotNull(detail.getPayLoad(), "asset details payload should be present for a live asset id");
            detail.getPayLoad().getAssetType();
            detail.getPayLoad().getTags();
        }

        @Test
        @DisplayName("assets_postDetails - Batch-read details for multiple asset ids [postAssetDetails]")
        void assets_postDetails() {
            String assetId = requireDiscoveredAssetId();
            AssetDetailsRequest request = new AssetDetailsRequest(
                    testAccountNo, testIbxCode, discoveredAssetClassification, List.of(assetId));
            AssetDetailsResponse response = requireEntitled(
                    "IBXSmartView", "getMultipleAssetDetails", "AssetDetailsResponse", "POST",
                    () -> client.smartViewAssets().getMultipleAssetDetails(request));
            assertNotNull(response);
            if (response.getPayLoad() != null && response.getPayLoad().getAssetDetails() != null
                    && !response.getPayLoad().getAssetDetails().isEmpty()) {
                AssetDetailsPayload first = response.getPayLoad().getAssetDetails().get(0);
                assertNotNull(first.getAssetId(), "assetId should be populated in batch details");
                first.getTags();
            }
        }

        @Test
        @DisplayName("assets_affectedAssets - Get affected customer assets for one asset [getAffectedAsset]")
        void assets_affectedAssets() {
            String assetId = requireDiscoveredAssetId();
            HierarchyNodeForAssetAPI affected = requireEntitled(
                    "IBXSmartView", "getAffectedAssets", "HierarchyNodeForAssetAPI", "GET",
                    () -> client.smartViewAssets().getAffectedAssets(
                            testAccountNo, testIbxCode, assetId, discoveredAssetClassification));
            assertNotNull(affected);
            if (affected.getPayLoad() != null) {
                affected.getPayLoad().getCages();
                affected.getPayLoad().getCircuits();
            }
        }

        @Test
        @DisplayName("assets_currentTagPoint - Get current value for one tag point [getCurrentTagPoint]")
        void assets_currentTagPoint() {
            String tagId = requireDiscoveredTagId();
            TagPointData data = requireEntitled(
                    "IBXSmartView", "getCurrentTagPoint", "TagPointData", "GET",
                    () -> client.smartViewAssets().getCurrentTagPoint(testAccountNo, testIbxCode, tagId));
            assertNotNull(data);
            if (data.getPayLoad() != null && !data.getPayLoad().isEmpty()) {
                TagPointDataArrayCurrent first = data.getPayLoad().get(0);
                assertNotNull(first.getTagId(), "tagId should be populated");
                first.getValue();
                first.getReadingTime();
            }
        }

        @Test
        @DisplayName("assets_postCurrentTagPoints - Batch-read current values for multiple tag points [postCurrentTagPoint]")
        void assets_postCurrentTagPoints() {
            String tagId = requireDiscoveredTagId();
            CurrentTagPointRequest request = new CurrentTagPointRequest(
                    testAccountNo, List.of(tagId), testIbxCode);
            TagPointData data = requireEntitled(
                    "IBXSmartView", "getMultipleCurrentTagPoints", "TagPointData", "POST",
                    () -> client.smartViewAssets().getMultipleCurrentTagPoints(request));
            assertNotNull(data);
            if (data.getPayLoad() != null && !data.getPayLoad().isEmpty()) {
                assertNotNull(data.getPayLoad().get(0).getTagId(), "tagId should be populated");
            }
        }
    }

    /**
     * Legacy environmental data ({@code /environment/v1/...}): inventory ops
     * {@code getCurrentEnvironmentData}, {@code listCurrentEnvironmentData} and
     * {@code getTrendingEnvironmentData}.
     */
    @Nested
    @Tag("integration-readonly")
    @DisplayName("Legacy Environmentals")
    class LegacyEnvironmentalTests {

        @Test
        @DisplayName("legacyEnv_getCurrent - Current environment data at IBX level [getCurrentEnvironmentData]")
        void legacyEnv_getCurrent() {
            EnvironmentData data = requireEntitled(
                    "IBXSmartView", "getCurrent", "EnvironmentData", "GET",
                    () -> client.legacyEnvironmentals().getCurrent(
                            testAccountNo, testIbxCode, "ibx", testIbxCode));
            assertNotNull(data);
            if (data.getPayLoad() != null) {
                data.getPayLoad().getIbx();
                data.getPayLoad().getTemperature();
                data.getPayLoad().getHumidity();
            }
        }

        @Test
        @DisplayName("legacyEnv_listCurrent - Current environment data for all zones [listCurrentEnvironmentData]")
        void legacyEnv_listCurrent() {
            List<EnvironmentDataForArray> data = requireEntitled(
                    "IBXSmartView", "listCurrent", "EnvironmentDataForArray", "GET",
                    () -> client.legacyEnvironmentals().listCurrent(testAccountNo, testIbxCode, "zone"));
            assertNotNull(data);
            if (!data.isEmpty()) {
                EnvironmentDataForArray first = data.get(0);
                first.getIbx();
                first.getZone();
                first.getTemperature();
            }
        }

        @Test
        @DisplayName("legacyEnv_getTrending - Trending temperature over the last 24h [getTrendingEnvironmentData]")
        void legacyEnv_getTrending() {
            String toDate = Instant.now().truncatedTo(ChronoUnit.SECONDS).toString();
            String fromDate = Instant.now().minus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS).toString();
            TrendingEnvironmentData data = requireEntitled(
                    "IBXSmartView", "getTrending", "TrendingEnvironmentData", "GET",
                    () -> client.legacyEnvironmentals().getTrending(
                            testAccountNo, testIbxCode, "temperature",
                            "ibx", testIbxCode, "hourly", fromDate, toDate));
            assertNotNull(data);
            if (data.getPayLoad() != null) {
                data.getPayLoad().getDatapoint();
                data.getPayLoad().getSeries();
            }
        }
    }

    /**
     * Legacy power data ({@code /power/v1/...}): inventory ops {@code getCurrentPowerData},
     * {@code postCurrentPowerData} and {@code getTrendingPowerData}.
     */
    @Nested
    @Tag("integration-readonly")
    @DisplayName("Legacy Power")
    class LegacyPowerTests {

        @Test
        @DisplayName("legacyPower_getCurrent - Current power data at IBX level [getCurrentPowerData]")
        void legacyPower_getCurrent() {
            PowerData data = requireEntitled(
                    "IBXSmartView", "getCurrent", "PowerData", "GET",
                    () -> client.legacyPower().getCurrent(testAccountNo, testIbxCode, "ibx", testIbxCode));
            assertNotNull(data);
            if (data.getPayLoad() != null) {
                data.getPayLoad().getIbx();
                data.getPayLoad().getLevelType();
                data.getPayLoad().getKva();
            }
        }

        @Test
        @DisplayName("legacyPower_postCurrent - Bulk current power data for all cages [postCurrentPowerData]")
        void legacyPower_postCurrent() {
            PowerCurrentPostRequest request = new PowerCurrentPostRequest(
                    testAccountNo, testIbxCode, PowerLevelType.CAGE);
            List<PowerDataIBX> data = requireEntitled(
                    "IBXSmartView", "postCurrent", "PowerDataIBX", "POST",
                    () -> client.legacyPower().postCurrent(request));
            assertNotNull(data);
            if (!data.isEmpty()) {
                PowerDataIBX first = data.get(0);
                first.getIbx();
                first.getLevelType();
                first.getKva();
            }
        }

        @Test
        @DisplayName("legacyPower_getTrending - Trending power draw over the last 24h [getTrendingPowerData]")
        void legacyPower_getTrending() {
            String toDate = Instant.now().truncatedTo(ChronoUnit.SECONDS).toString();
            String fromDate = Instant.now().minus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS).toString();
            TrendingPowerData data = requireEntitled(
                    "IBXSmartView", "getTrending", "TrendingPowerData", "GET",
                    () -> client.legacyPower().getTrending(
                            testAccountNo, testIbxCode, "ibx", testIbxCode, "hourly", fromDate, toDate));
            assertNotNull(data);
            if (data.getPayLoad() != null) {
                data.getPayLoad().getIbx();
                data.getPayLoad().getInterval();
                data.getPayLoad().getData();
            }
        }
    }

    /**
     * System alerts ({@code /smartview/v2/systemAlerts/search}). The spec's operationIds are
     * swapped on this path: the GET variant carries operationId {@code postAlerts} and the POST
     * variant carries {@code getAlerts} — both are pure searches.
     */
    @Nested
    @Tag("integration-readonly")
    @DisplayName("System Alerts")
    class SystemAlertTests {

        @Test
        @DisplayName("systemAlerts_searchGet - Search alerts via query params [postAlerts (GET; spec ids swapped)]")
        void systemAlerts_searchGet() {
            PaginatedList<SystemAlert> alerts = requireEntitled(
                    "IBXSmartView", "search", "SystemAlert", "GET",
                    () -> client.systemAlerts().search("ACTIVE", null, null, 0, 10));
            assertNotNull(alerts);
            if (!alerts.isEmpty()) {
                SystemAlert first = alerts.get(0);
                assertNotNull(first.getAlertUid(), "alertUid should be populated");
                first.getStatus();
                first.getAsset();
            }
        }

        @Test
        @DisplayName("systemAlerts_searchPost - Search alerts via typed filter body [getAlerts (POST; spec ids swapped)]")
        void systemAlerts_searchPost() {
            SearchRequest request = new SearchRequest(
                    new SearchFilter(
                            List.of(new SearchCondition("status", "EQUALS", List.of("ACTIVE"))),
                            null),
                    new SearchPagination(0L, 10),
                    List.of(new SearchSort("DESC", "id")));
            PaginatedList<SystemAlert> alerts = requireEntitled(
                    "IBXSmartView", "searchPost", "SystemAlert", "POST",
                    () -> client.systemAlerts().searchPost(request));
            assertNotNull(alerts);
            if (!alerts.isEmpty()) {
                assertNotNull(alerts.get(0).getAlertUid(), "alertUid should be populated");
            }
        }
    }

    /**
     * Power events ({@code /dcim/v3/powerEvents/...}): inventory ops {@code getPowerEvents}
     * and {@code searchAlertConfigurations}. Alert-configuration create/update/pause/resume/
     * delete are mutations and stay in the full tier.
     */
    @Nested
    @Tag("integration-readonly")
    @DisplayName("Power Events")
    class PowerEventTests {

        @Test
        @DisplayName("powerEvents_search - Search power events for IBX [getPowerEvents]")
        void powerEvents_search() {
            PaginatedList<PowerEvent> events = requireEntitled(
                    "IBXSmartView", "search", "PowerEvent", "GET",
                    () -> client.powerEvents().search(List.of(testIbxCode), null, null, 0, 10));
            assertNotNull(events);
            if (!events.isEmpty()) {
                PowerEvent first = events.get(0);
                assertNotNull(first.getAlertUid(), "alertUid should be populated");
                first.getStatus();
                first.getAsset();
            }
        }

        @Test
        @DisplayName("powerEvents_searchAlertConfigurations - Search power alert configurations [searchAlertConfigurations]")
        void powerEvents_searchAlertConfigurations() {
            PaginatedList<PowerAlertConfiguration> configurations = requireEntitled(
                    "IBXSmartView", "searchAlertConfigurations", "PowerAlertConfiguration", "GET",
                    () -> client.powerEvents().searchAlertConfigurations(List.of(testIbxCode), null, 0, 10));
            assertNotNull(configurations);
            if (!configurations.isEmpty()) {
                PowerAlertConfiguration first = configurations.get(0);
                assertNotNull(first.getAlertConfigurationUid(), "alertConfigurationUid should be populated");
                first.getIbx();
                first.getState();
            }
        }
    }
}
