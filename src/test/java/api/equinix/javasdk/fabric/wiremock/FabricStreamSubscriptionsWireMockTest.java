package api.equinix.javasdk.fabric.wiremock;

import api.equinix.javasdk.Fabric;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.core.exception.*;
import api.equinix.javasdk.fabric.enums.StreamSubscriptionSinkCredentialType;
import api.equinix.javasdk.fabric.enums.StreamSubscriptionSinkType;
import api.equinix.javasdk.fabric.enums.StreamSubscriptionType;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.fabric.model.StreamSubscription;
import org.junit.jupiter.api.*;

import static api.equinix.javasdk.core.ResponseStubs.*;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * WireMock-based API tests for Fabric Stream Subscriptions.
 */
class FabricStreamSubscriptionsWireMockTest extends WireMockTestBase {

    static Fabric fabric;

    static final String STREAM_ID = "d4e5f6a7-b8c9-0123-defa-345678901bcd";
    static final String SUBSCRIPTION_ID = "f6a7b8c9-d0e1-2345-fabc-567890123def";

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
    @DisplayName("list(streamId)")
    class ListByStream {

        @Test
        @DisplayName("GETs the stream's subscriptions collection endpoint")
        void listsSubscriptions() {
            stubPaginatedGet(wireMock, "/fabric/v4/streams/.*/subscriptions",
                    "/json/fabric/paginated_stream_subscriptions.json");

            PaginatedList<StreamSubscription> subscriptions = fabric.streamSubscriptions().list(STREAM_ID);

            assertNotNull(subscriptions);
            assertEquals(1, subscriptions.size());
            assertEquals(SUBSCRIPTION_ID, subscriptions.get(0).getUuid());
            assertEquals("Production-Splunk-Subscription", subscriptions.get(0).getName());

            wireMock.verify(getRequestedFor(urlPathEqualTo(
                    "/fabric/v4/streams/" + STREAM_ID + "/subscriptions")));
        }
    }

    @Nested
    @DisplayName("getByUuid()")
    class GetByUuid {

        @Test
        @DisplayName("returns stream subscription for valid UUID")
        void returnsStreamSubscription() {
            stubSingleton(wireMock, "/fabric/v4/streams/.*/subscriptions/.*",
                    "/json/fabric/stream_subscription_response.json");

            StreamSubscription subscription = fabric.streamSubscriptions().getByUuid(STREAM_ID, SUBSCRIPTION_ID);
            assertNotNull(subscription);
            assertEquals(SUBSCRIPTION_ID, subscription.getUuid());
            assertEquals("Production-Splunk-Subscription", subscription.getName());

            assertNotNull(subscription.getMetricSelector());
            assertEquals(java.util.List.of("equinix.fabric.connection.*"),
                    subscription.getMetricSelector().getInclude());
            assertEquals(java.util.List.of("equinix.fabric.connection.bandwidth_rx.usage"),
                    subscription.getMetricSelector().getExcept());
            assertNotNull(subscription.getEventSelector());
            assertEquals(java.util.List.of("equinix.fabric.connection.*"),
                    subscription.getEventSelector().getInclude());
        }

        @Test
        @DisplayName("404 throws EquinixNotFoundException")
        void notFound() {
            stubErrorInline(wireMock, "/fabric/v4/streams/.*/subscriptions/.*",
                    404, "[{\"errorCode\":\"ERR-404\",\"errorMessage\":\"Stream subscription not found\"}]");

            assertThrows(EquinixNotFoundException.class,
                    () -> fabric.streamSubscriptions().getByUuid(STREAM_ID, "invalid-uuid"));
        }
    }

    @Nested
    @DisplayName("define(streamId).create()")
    class Create {

        @Test
        @DisplayName("POSTs the sink + credential body to the stream's subscriptions endpoint")
        void createPostsBody() {
            wireMock.stubFor(post(urlPathEqualTo("/fabric/v4/streams/" + STREAM_ID + "/subscriptions"))
                    .willReturn(okJson(loadFixture("/json/fabric/stream_subscription_response.json"))));

            StreamSubscription created = fabric.streamSubscriptions().define(STREAM_ID)
                    .withType(StreamSubscriptionType.STREAM_SUBSCRIPTION)
                    .withName("Production-Splunk-Subscription")
                    .withDescription("Delivers production telemetry events to the Splunk HEC sink")
                    .withEnabled(true)
                    .withSinkType(StreamSubscriptionSinkType.SPLUNK_HEC)
                    .withSinkUri("https://splunk.example.com:8088/services/collector")
                    .withCredentialType(StreamSubscriptionSinkCredentialType.ACCESS_TOKEN)
                    .withAccessToken("test-access-token")
                    .withIntegrationKey("test-integration-key")
                    .create();

            assertNotNull(created);
            assertEquals(SUBSCRIPTION_ID, created.getUuid());
            assertEquals("Production-Splunk-Subscription", created.getName());

            wireMock.verify(postRequestedFor(urlPathEqualTo(
                    "/fabric/v4/streams/" + STREAM_ID + "/subscriptions"))
                    .withHeader("Content-Type", containing("application/json"))
                    .withRequestBody(matchingJsonPath("$.type", equalTo("STREAM_SUBSCRIPTION")))
                    .withRequestBody(matchingJsonPath("$.name", equalTo("Production-Splunk-Subscription")))
                    .withRequestBody(matchingJsonPath("$.enabled", equalTo("true")))
                    .withRequestBody(matchingJsonPath("$.sink.type", equalTo("SPLUNK_HEC")))
                    .withRequestBody(matchingJsonPath("$.sink.uri",
                            equalTo("https://splunk.example.com:8088/services/collector")))
                    .withRequestBody(matchingJsonPath("$.sink.credential.type", equalTo("ACCESS_TOKEN")))
                    .withRequestBody(matchingJsonPath("$.sink.credential.accessToken", equalTo("test-access-token")))
                    .withRequestBody(matchingJsonPath("$.sink.credential.integrationKey",
                            equalTo("test-integration-key"))));
        }

        @Test
        @DisplayName("create() on a builder targeting an existing subscription throws")
        void createOnUpdateBuilderThrows() {
            stubSingleton(wireMock, "/fabric/v4/streams/.*/subscriptions/.*",
                    "/json/fabric/stream_subscription_response.json");

            StreamSubscription subscription = fabric.streamSubscriptions().getByUuid(STREAM_ID, SUBSCRIPTION_ID);
            assertThrows(IllegalStateException.class,
                    () -> subscription.update(STREAM_ID).withName("X").create());
            wireMock.verify(0, postRequestedFor(urlPathEqualTo(
                    "/fabric/v4/streams/" + STREAM_ID + "/subscriptions")));
        }
    }

    @Nested
    @DisplayName("update() / save()")
    class Update {

        @Test
        @DisplayName("PUTs the full body as application/json with the seeded name")
        void savePutsSeededBody() {
            stubSingleton(wireMock, "/fabric/v4/streams/.*/subscriptions/.*",
                    "/json/fabric/stream_subscription_response.json");
            wireMock.stubFor(put(urlPathMatching("/fabric/v4/streams/.*/subscriptions/.*"))
                    .willReturn(okJson(loadFixture("/json/fabric/stream_subscription_response.json"))));

            StreamSubscription subscription = fabric.streamSubscriptions().getByUuid(STREAM_ID, SUBSCRIPTION_ID);
            StreamSubscription updated = subscription.update(STREAM_ID).withName("Renamed-Subscription").save();

            assertNotNull(updated);
            wireMock.verify(putRequestedFor(urlPathMatching(
                    "/fabric/v4/streams/" + STREAM_ID + "/subscriptions/" + SUBSCRIPTION_ID))
                    .withHeader("Content-Type", containing("application/json"))
                    .withRequestBody(matchingJsonPath("$.name", equalTo("Renamed-Subscription")))
                    .withRequestBody(matchingJsonPath("$.sink.uri",
                            equalTo("https://splunk.example.com:8088/services/collector"))));
        }

        @Test
        @DisplayName("save() on a freshly-defined (non-seeded) builder throws and makes no request")
        void saveWithoutSeedThrows() {
            assertThrows(IllegalStateException.class,
                    () -> fabric.streamSubscriptions().define(STREAM_ID).withName("New").save());
            wireMock.verify(0, putRequestedFor(urlPathMatching("/fabric/v4/streams/.*/subscriptions/.*")));
        }
    }

    @Nested
    @DisplayName("Error handling")
    class Errors {

        @Test
        @DisplayName("500 throws EquinixServerException")
        void serverError() {
            stubErrorInline(wireMock, "/fabric/v4/streams/.*/subscriptions/.*",
                    500, "[{\"errorCode\":\"ERR-500\",\"errorMessage\":\"Internal server error\"}]");

            assertThrows(EquinixServerException.class,
                    () -> fabric.streamSubscriptions().getByUuid(STREAM_ID, "test-uuid"));
        }
    }
}
