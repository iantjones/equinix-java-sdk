package api.equinix.javasdk.ibxsmartview.wiremock;

import api.equinix.javasdk.IBXSmartView;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.core.exception.*;
import api.equinix.javasdk.ibxsmartview.enums.ChannelType;
import api.equinix.javasdk.ibxsmartview.model.StreamingSubscription;
import api.equinix.javasdk.ibxsmartview.model.implementation.Channel;
import api.equinix.javasdk.ibxsmartview.model.implementation.MessageType;
import api.equinix.javasdk.ibxsmartview.model.implementation.PowerMessageType;
import org.junit.jupiter.api.*;

import java.util.List;

import static api.equinix.javasdk.core.ResponseStubs.*;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * WireMock-based API tests for IBX SmartView Streaming Subscriptions.
 */
class IBXSmartViewStreamingSubscriptionsWireMockTest extends WireMockTestBase {

    static IBXSmartView ibxSmartView;

    @BeforeAll
    static void setUp() {
        ibxSmartView = new IBXSmartView(testCredentials());
        redirectToWireMock(ibxSmartView);
        ibxSmartView.authenticate();
    }

    @AfterAll
    static void tearDown() throws Exception {
        if (ibxSmartView != null) ibxSmartView.close();
    }

    @BeforeEach
    void resetBeforeEach() {
        resetStubs();
    }

    @Nested
    @DisplayName("getByUuid()")
    class GetByUuid {

        @Test
        @DisplayName("returns streaming subscription for valid UUID")
        void returnsSubscription() {
            stubSingleton(wireMock, "/smartview/v2/streaming/subscriptions/.*",
                    "/json/ibxsmartview/streaming_subscription_response.json");

            StreamingSubscription subscription = ibxSmartView.streamingSubscriptions().getByUuid("sub-12345-abcde");
            assertNotNull(subscription);
            assertEquals("sub-12345-abcde", subscription.getId());
            assertNotNull(subscription.getChannel());
        }

        @Test
        @DisplayName("404 throws EquinixNotFoundException")
        void notFound() {
            stubErrorInline(wireMock, "/smartview/v2/streaming/subscriptions/.*",
                    404, "[{\"errorCode\":\"ERR-404\",\"errorMessage\":\"Subscription not found\"}]");

            assertThrows(EquinixNotFoundException.class,
                    () -> ibxSmartView.streamingSubscriptions().getByUuid("invalid-uuid"));
        }
    }

    @Nested
    @DisplayName("define().create()")
    class Create {

        @Test
        @DisplayName("reads the new id from the 201 Location header and re-fetches the subscription")
        void createsAndReFetches() {
            // POST returns 201 with only a Location header (Mongo ObjectId, not a dashed UUID).
            wireMock.stubFor(post(urlPathEqualTo("/smartview/v2/streaming/subscriptions"))
                    .willReturn(aResponse().withStatus(201)
                            .withHeader("Location",
                                    "/smartview/v2/streaming/subscriptions/607460b4e4a78360425bca56")));
            // The create flow then GETs the new subscription by that id.
            stubSingleton(wireMock, "/smartview/v2/streaming/subscriptions/607460b4e4a78360425bca56",
                    "/json/ibxsmartview/streaming_subscription_response.json");

            StreamingSubscription created = ibxSmartView.streamingSubscriptions().define()
                    .withMessageType(MessageType.builder()
                            .power(List.of(PowerMessageType.builder()
                                    .accountNumber("123456").ibx(List.of("SV5")).build()))
                            .build())
                    .withChannel(Channel.builder().channelType(ChannelType.WEBHOOK).build())
                    .create();

            assertNotNull(created);
            assertNotNull(created.getChannel());

            wireMock.verify(postRequestedFor(urlPathEqualTo("/smartview/v2/streaming/subscriptions"))
                    .withRequestBody(matchingJsonPath("$.channel.channelType", equalTo("WEBHOOK")))
                    .withRequestBody(matchingJsonPath("$.messageType.power[0].ibx[0]", equalTo("SV5"))));
            wireMock.verify(getRequestedFor(
                    urlPathEqualTo("/smartview/v2/streaming/subscriptions/607460b4e4a78360425bca56")));
        }
    }

    @Nested
    @DisplayName("delete()")
    class Delete {

        @Test
        @DisplayName("returns true on a 204 No Content response")
        void deletesOn204() {
            stubSingleton(wireMock, "/smartview/v2/streaming/subscriptions/sub-12345-abcde",
                    "/json/ibxsmartview/streaming_subscription_response.json");
            stubDeleteNoContent(wireMock, "/smartview/v2/streaming/subscriptions/sub-12345-abcde");

            StreamingSubscription subscription =
                    ibxSmartView.streamingSubscriptions().getByUuid("sub-12345-abcde");

            assertTrue(subscription.delete());
            wireMock.verify(deleteRequestedFor(
                    urlPathEqualTo("/smartview/v2/streaming/subscriptions/sub-12345-abcde")));
        }
    }

    @Nested
    @DisplayName("Error handling")
    class Errors {

        @Test
        @DisplayName("500 throws EquinixServerException")
        void serverError() {
            stubErrorInline(wireMock, "/smartview/v2/streaming/subscriptions/.*",
                    500, "[{\"errorCode\":\"ERR-500\",\"errorMessage\":\"Internal server error\"}]");

            assertThrows(EquinixServerException.class,
                    () -> ibxSmartView.streamingSubscriptions().getByUuid("test-uuid"));
        }
    }
}
