package com.eqixiac.equinix.fabric.wiremock;

import com.eqixiac.equinix.Fabric;
import com.eqixiac.equinix.core.WireMockTestBase;
import com.eqixiac.equinix.core.http.response.PaginatedFilteredList;
import com.eqixiac.equinix.fabric.enums.StreamAssetAttachmentStatus;
import com.eqixiac.equinix.fabric.enums.StreamAssetType;
import com.eqixiac.equinix.fabric.enums.StreamSubscriptionSinkCredentialType;
import com.eqixiac.equinix.fabric.enums.StreamSubscriptionSinkType;
import com.eqixiac.equinix.fabric.enums.StreamSubscriptionType;
import com.eqixiac.equinix.fabric.model.Stream;
import com.eqixiac.equinix.fabric.model.StreamAsset;
import com.eqixiac.equinix.fabric.model.StreamSubscription;
import com.eqixiac.equinix.fabric.model.implementation.filter.Filter;
import com.eqixiac.equinix.fabric.model.implementation.sort.Sort;
import org.junit.jupiter.api.*;

import java.util.List;

import static com.eqixiac.equinix.core.ResponseStubs.*;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Streams GA additions from spec 4.34.0: the three stream searches, Network Edge device streaming
 * ({@code /streams/{streamId}/devices/{deviceId}}) and the Dynatrace sink with an {@code API_TOKEN}
 * credential. Fixtures are the spec's response examples.
 */
class FabricStreamSearchAndDevicesWireMockTest extends WireMockTestBase {

    static Fabric fabric;

    private static final String STREAM = "e684aa26-8276-48b7-bb42-a6d9def0a419";
    private static final String DEVICE = "8a9b7c6d-1e2f-4a3b-9c4d-5e6f7a8b9c0d";
    private static final String DEVICE_URL = "/fabric/v4/streams/" + STREAM + "/devices/" + DEVICE;

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
    @DisplayName("streams().search POSTs /streams/search with filter and sort")
    void searchStreams() {
        stubPaginatedPost(wireMock, "/fabric/v4/streams/search", "/json/fabric/streams_search_response.json");

        PaginatedFilteredList<Stream> streams = fabric.streams().search(
                Filter.filter().and().equals("/state", "PROVISIONED"), Sort.sort().desc("/changeLog/createdDateTime"));

        assertEquals(1, streams.size());
        assertEquals("12f4d4a1-c7cb-4def-9f77-5ce93c19d999", streams.get(0).getUuid());
        wireMock.verify(postRequestedFor(urlPathEqualTo("/fabric/v4/streams/search"))
                .withRequestBody(matchingJsonPath("$.filter.and[0].property", equalTo("/state")))
                .withRequestBody(matchingJsonPath("$.sort[0].direction", equalTo("DESC"))));
    }

    @Test
    @DisplayName("streamAssets().search POSTs /streamAssets/search and reads streamUuid/projectId")
    void searchStreamAssets() {
        stubPaginatedPost(wireMock, "/fabric/v4/streamAssets/search", "/json/fabric/stream_assets_search_response.json");

        PaginatedFilteredList<StreamAsset> assets = fabric.streamAssets().search(
                Filter.filter().and().equals("/streamUuid", "e784aa26-8276-48b7-bb42-a6d9def0a424"), null);

        assertEquals(1, assets.size());
        StreamAsset a = assets.get(0);
        assertEquals(StreamAssetType.XF_PORT, a.getType());
        assertEquals(StreamAssetAttachmentStatus.ATTACHED, a.getAttachmentStatus());
        assertEquals("e784aa26-8276-48b7-bb42-a6d9def0a424", a.getStreamUuid());
        assertEquals("dadd3ab6-c0af-430c-8216-43d44f08c1c5", a.getProjectId());
    }

    @Test
    @DisplayName("streamSubscriptions().search POSTs /streamSubscriptions/search and requires a filter")
    void searchStreamSubscriptions() {
        stubPaginatedPost(wireMock, "/fabric/v4/streamSubscriptions/search",
                "/json/fabric/stream_subscriptions_search_response.json");

        PaginatedFilteredList<StreamSubscription> subs = fabric.streamSubscriptions().search(
                Filter.filter().and().equals("/streamUuid", "12f4d4a1-c7cb-4def-9f77-5ce93c19d999"), null);

        assertEquals(1, subs.size());
        assertEquals("demo-subscription", subs.get(0).getName());
        assertThrows(IllegalArgumentException.class, () -> fabric.streamSubscriptions().search(null, null));
        wireMock.verify(1, postRequestedFor(urlPathEqualTo("/fabric/v4/streamSubscriptions/search")));
    }

    @Test
    @DisplayName("getDevice reads a VIRTUAL_DEVICE asset with its interfaces")
    void getDevice() {
        stubSingleton(wireMock, DEVICE_URL, "/json/fabric/stream_device_response.json");

        StreamAsset device = fabric.streamAssets().getDevice(STREAM, DEVICE);

        assertEquals(StreamAssetType.VIRTUAL_DEVICE, device.getType());
        assertEquals(DEVICE, device.getUuid());
        assertEquals(2, device.getInterfaces().size());
        assertEquals(3, device.getInterfaces().get(0).getId());
        assertEquals("DATA", device.getInterfaces().get(0).getType());
    }

    @Test
    @DisplayName("attachDevice PUTs the spec StreamDevicePutRequest; detachDevice DELETEs")
    void attachAndDetachDevice() {
        wireMock.stubFor(put(urlPathEqualTo(DEVICE_URL)).willReturn(aResponse().withStatus(202)
                .withHeader("Content-Type", "application/json")
                .withBody(loadFixture("/json/fabric/stream_device_attaching_response.json"))));
        wireMock.stubFor(delete(urlPathEqualTo(DEVICE_URL)).willReturn(aResponse().withStatus(202)
                .withHeader("Content-Type", "application/json")
                .withBody(loadFixture("/json/fabric/stream_device_detaching_response.json"))));

        StreamAsset attaching = fabric.streamAssets().attachDevice(STREAM, DEVICE, List.of(3, 5));
        StreamAsset detaching = fabric.streamAssets().detachDevice(STREAM, DEVICE);

        assertEquals(StreamAssetAttachmentStatus.ATTACHING, attaching.getAttachmentStatus());
        assertEquals(StreamAssetAttachmentStatus.DETACHING, detaching.getAttachmentStatus());
        wireMock.verify(putRequestedFor(urlPathEqualTo(DEVICE_URL))
                .withRequestBody(equalToJson("{\"metricsEnabled\":true,\"interfaces\":[{\"id\":3},{\"id\":5}]}")));
        wireMock.verify(deleteRequestedFor(urlPathEqualTo(DEVICE_URL)));
        assertThrows(IllegalArgumentException.class, () -> fabric.streamAssets().attachDevice(STREAM, DEVICE, List.of()));
    }

    @Test
    @DisplayName("a Dynatrace sink sends type DYNATRACE and an API_TOKEN credential with apiToken")
    void dynatraceSink() {
        String url = "/fabric/v4/streams/" + STREAM + "/subscriptions";
        wireMock.stubFor(post(urlPathEqualTo(url))
                .willReturn(okJson(loadFixture("/json/fabric/stream_subscription_response.json"))));

        fabric.streamSubscriptions().define(STREAM)
                .withType(StreamSubscriptionType.STREAM_SUBSCRIPTION)
                .withName("dynatrace-sub")
                .withEnabled(true)
                .withSinkType(StreamSubscriptionSinkType.DYNATRACE)
                .withSinkUri("https://abc123.live.dynatrace.com")
                .withApiToken("dt0c01.example")
                .create();

        wireMock.verify(postRequestedFor(urlPathEqualTo(url))
                .withRequestBody(matchingJsonPath("$.sink.type", equalTo("DYNATRACE")))
                .withRequestBody(matchingJsonPath("$.sink.credential.type", equalTo("API_TOKEN")))
                .withRequestBody(matchingJsonPath("$.sink.credential.apiToken", equalTo("dt0c01.example"))));
        assertEquals(StreamSubscriptionSinkType.DYNATRACE, StreamSubscriptionSinkType.fromString("DYNATRACE"));
        assertEquals(StreamSubscriptionSinkCredentialType.API_TOKEN, StreamSubscriptionSinkCredentialType.fromString("API_TOKEN"));
    }
}
