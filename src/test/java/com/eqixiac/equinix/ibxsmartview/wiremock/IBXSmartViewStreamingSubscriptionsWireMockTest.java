package com.eqixiac.equinix.ibxsmartview.wiremock;

import com.eqixiac.equinix.IBXSmartView;
import com.eqixiac.equinix.core.WireMockTestBase;
import com.eqixiac.equinix.core.exception.*;
import com.eqixiac.equinix.ibxsmartview.enums.ChannelType;
import com.eqixiac.equinix.ibxsmartview.enums.SubscriptionStatus;
import com.eqixiac.equinix.ibxsmartview.model.StreamingSubscription;
import com.eqixiac.equinix.ibxsmartview.model.SubscriptionCertificate;
import com.eqixiac.equinix.ibxsmartview.model.SubscriptionData;
import com.eqixiac.equinix.ibxsmartview.model.implementation.Channel;
import com.eqixiac.equinix.ibxsmartview.model.implementation.MessageType;
import com.eqixiac.equinix.ibxsmartview.model.implementation.PowerMessageType;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import org.junit.jupiter.api.*;

import java.util.List;

import static com.eqixiac.equinix.core.ResponseStubs.*;
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
    @DisplayName("refresh()")
    class Refresh {

        static final String PATH = "/smartview/v2/streaming/subscriptions/sub-12345-abcde";

        @Test
        @DisplayName("re-GETs the subscription and updates the wrapper's state in place")
        void refreshesInPlace() {
            // First GET returns the original state (ACTIVE, updatedBy user-b); the second GET —
            // triggered by wrapper.refresh() — returns a DIFFERENT payload (FAILED, updatedBy user-c).
            wireMock.stubFor(get(urlPathEqualTo(PATH))
                    .inScenario("subscription-refresh")
                    .whenScenarioStateIs(Scenario.STARTED)
                    .willReturn(okJson(loadFixture(
                            "/json/ibxsmartview/streaming_subscription_response.json")))
                    .willSetStateTo("state-changed"));
            wireMock.stubFor(get(urlPathEqualTo(PATH))
                    .inScenario("subscription-refresh")
                    .whenScenarioStateIs("state-changed")
                    .willReturn(okJson(loadFixture(
                            "/json/ibxsmartview/streaming_subscription_response_refreshed.json"))));

            StreamingSubscription subscription =
                    ibxSmartView.streamingSubscriptions().getByUuid("sub-12345-abcde");
            assertEquals(SubscriptionStatus.ACTIVE, subscription.getStatus());
            assertEquals("user-b", subscription.getUpdatedBy());
            assertEquals("2024-01-15T12:00:00Z", subscription.getUpdatedDateTime());

            subscription.refresh();

            // The same wrapper instance now reflects the re-fetched server state.
            assertEquals(SubscriptionStatus.FAILED, subscription.getStatus());
            assertEquals("user-c", subscription.getUpdatedBy());
            assertEquals("2024-01-20T09:30:00Z", subscription.getUpdatedDateTime());
            assertEquals("sub-12345-abcde", subscription.getId());

            wireMock.verify(2, getRequestedFor(urlPathEqualTo(PATH)));
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
    @DisplayName("list()")
    class ListSubscriptions {

        @Test
        @DisplayName("GETs /streaming/subscriptions and maps the bare JSON array")
        void listsSubscriptions() {
            stubPaginatedGet(wireMock, "/smartview/v2/streaming/subscriptions",
                    "/json/ibxsmartview/streaming_subscriptions_list.json");

            List<StreamingSubscription> subscriptions = ibxSmartView.streamingSubscriptions().list();

            assertNotNull(subscriptions);
            assertEquals(2, subscriptions.size());
            assertEquals("sub-12345-abcde", subscriptions.get(0).getId());
            assertEquals("sub-67890-fghij", subscriptions.get(1).getId());
            assertNotNull(subscriptions.get(0).getChannel());

            wireMock.verify(getRequestedFor(
                    urlPathEqualTo("/smartview/v2/streaming/subscriptions")));
        }
    }

    @Nested
    @DisplayName("getSubscriptionData()")
    class GetSubscriptionData {

        @Test
        @DisplayName("plain overload GETs /streaming/subscriptionData/{id} with no query params")
        void getsDataNoFilters() {
            stubSingleton(wireMock, "/smartview/v2/streaming/subscriptionData/sub-12345-abcde",
                    "/json/ibxsmartview/subscription_data_response.json");

            SubscriptionData data =
                    ibxSmartView.streamingSubscriptions().getSubscriptionData("sub-12345-abcde");

            assertNotNull(data);
            assertNotNull(data.getPowerMessageData());
            assertEquals(1, data.getPowerMessageData().size());
            assertEquals("POWER", data.getPowerMessageData().get(0).getType());
            assertNotNull(data.getPagination());
            assertEquals(2, data.getPagination().getTotal());

            wireMock.verify(getRequestedFor(
                    urlPathEqualTo("/smartview/v2/streaming/subscriptionData/sub-12345-abcde"))
                    .withUrl("/smartview/v2/streaming/subscriptionData/sub-12345-abcde"));
        }

        @Test
        @DisplayName("filtered overload GETs the same path carrying ibxs/messageTypes/streamIds/offset/limit query params")
        void getsDataWithFilters() {
            stubSingleton(wireMock, "/smartview/v2/streaming/subscriptionData/sub-12345-abcde",
                    "/json/ibxsmartview/subscription_data_response.json");

            SubscriptionData data = ibxSmartView.streamingSubscriptions().getSubscriptionData(
                    "sub-12345-abcde",
                    List.of("SV5", "DC6"),
                    List.of("POWER", "ENVIRONMENTAL"),
                    List.of("stream-1"),
                    10, 25);

            assertNotNull(data);

            wireMock.verify(getRequestedFor(
                    urlPathEqualTo("/smartview/v2/streaming/subscriptionData/sub-12345-abcde"))
                    .withQueryParam("ibxs", equalTo("SV5"))
                    .withQueryParam("ibxs", equalTo("DC6"))
                    .withQueryParam("messageTypes", equalTo("POWER"))
                    .withQueryParam("messageTypes", equalTo("ENVIRONMENTAL"))
                    .withQueryParam("streamIds", equalTo("stream-1"))
                    .withQueryParam("offset", equalTo("10"))
                    .withQueryParam("limit", equalTo("25")));
        }

        @Test
        @DisplayName("null filters/pagination emit no query params")
        void getsDataNullFiltersOmitted() {
            stubSingleton(wireMock, "/smartview/v2/streaming/subscriptionData/sub-12345-abcde",
                    "/json/ibxsmartview/subscription_data_response.json");

            ibxSmartView.streamingSubscriptions().getSubscriptionData(
                    "sub-12345-abcde", null, null, null, null, null);

            wireMock.verify(getRequestedFor(
                    urlPathEqualTo("/smartview/v2/streaming/subscriptionData/sub-12345-abcde"))
                    .withUrl("/smartview/v2/streaming/subscriptionData/sub-12345-abcde"));
        }
    }

    @Nested
    @DisplayName("getCertificate()")
    class GetCertificate {

        @Test
        @DisplayName("GETs /streaming/subscriptions/certificate with the channelType query param")
        void getsCertificate() {
            stubSingleton(wireMock, "/smartview/v2/streaming/subscriptions/certificate",
                    "/json/ibxsmartview/subscription_certificate_response.json");

            SubscriptionCertificate certificate = ibxSmartView.streamingSubscriptions()
                    .getCertificate(ChannelType.AWS_IOT_CORE.name());

            assertNotNull(certificate);
            assertEquals(ChannelType.AWS_IOT_CORE, certificate.getChannelType());
            assertNotNull(certificate.getCertificateBase64());
            assertEquals("2025-12-31T23:59:59Z", certificate.getExpiryDate());

            wireMock.verify(getRequestedFor(
                    urlPathEqualTo("/smartview/v2/streaming/subscriptions/certificate"))
                    .withQueryParam("channelType", equalTo("AWS_IOT_CORE")));
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
