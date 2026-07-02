package api.equinix.javasdk.ibxsmartview.wiremock;

import api.equinix.javasdk.IBXSmartView;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.ibxsmartview.enums.PowerLevelType;
import api.equinix.javasdk.ibxsmartview.model.PowerData;
import api.equinix.javasdk.ibxsmartview.model.PowerDataIBX;
import api.equinix.javasdk.ibxsmartview.model.TrendingPowerData;
import api.equinix.javasdk.ibxsmartview.model.json.creators.PowerCurrentPostRequest;
import org.junit.jupiter.api.*;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * WireMock-based tests for the legacy IBX SmartView power API
 * ({@link api.equinix.javasdk.ibxsmartview.client.LegacyPower}).
 *
 * <p>Covers the {@code postCurrent(PowerCurrentPostRequest)} action, which POSTs to the
 * legacy {@code /power/v1/current} endpoint (empty rootUri, {@code overrideUriFormat}
 * {@code power/v{version}/{requestUri}}, {@code defaultVersion} 1) and unwraps the
 * {@code payLoad.data} array from the {@code PowerDataResponse_IBX} envelope.</p>
 */
class IBXSmartViewLegacyPowerWireMockTest extends WireMockTestBase {

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
    @DisplayName("postCurrent()")
    class PostCurrent {

        @Test
        @DisplayName("POSTs the typed body to /power/v1/current and unwraps payLoad.data")
        void postsAndUnwrapsPayload() {
            wireMock.stubFor(post(urlPathEqualTo("/power/v1/current"))
                    .willReturn(okJson(loadFixture("/json/ibxsmartview/power_current_ibx_response.json"))));

            PowerCurrentPostRequest request =
                    new PowerCurrentPostRequest("123456", "SV5", PowerLevelType.CAGE);

            List<PowerDataIBX> data = ibxSmartView.legacyPower().postCurrent(request);

            assertNotNull(data);
            assertEquals(2, data.size());
            assertEquals("SV5", data.get(0).getIbx());
            assertEquals("123456", data.get(0).getAccountNo());
            assertEquals(PowerLevelType.CAGE, data.get(0).getLevelType());
            assertEquals(3.42, data.get(0).getKva());
            assertEquals("true", data.get(1).getIsAlarm());

            // Verify the request path, verb and the serialized JSON body.
            wireMock.verify(postRequestedFor(urlPathEqualTo("/power/v1/current"))
                    .withRequestBody(matchingJsonPath("$.accountNo", equalTo("123456")))
                    .withRequestBody(matchingJsonPath("$.ibx", equalTo("SV5")))
                    // PowerLevelType serializes to its lower-case wire form via @JsonValue.
                    .withRequestBody(matchingJsonPath("$.levelType", equalTo("cage"))));
        }

        @Test
        @DisplayName("returns an empty list when the payLoad has no data")
        void returnsEmptyListWhenNoData() {
            wireMock.stubFor(post(urlPathEqualTo("/power/v1/current"))
                    .willReturn(okJson("{\"payLoad\":{},\"status\":{\"type\":\"SUCCESS\",\"statuscode\":200}}")));

            PowerCurrentPostRequest request =
                    new PowerCurrentPostRequest("123456", "SV5", PowerLevelType.IBX);

            List<PowerDataIBX> data = ibxSmartView.legacyPower().postCurrent(request);

            assertNotNull(data);
            assertTrue(data.isEmpty());

            wireMock.verify(postRequestedFor(urlPathEqualTo("/power/v1/current"))
                    .withRequestBody(matchingJsonPath("$.levelType", equalTo("ibx"))));
        }
    }

    @Nested
    @DisplayName("getCurrent()")
    class GetCurrent {

        @Test
        @DisplayName("GETs /power/v1/current with the level query params and unwraps payLoad")
        void getsCurrentPowerWithQueryParams() {
            wireMock.stubFor(get(urlPathEqualTo("/power/v1/current"))
                    .willReturn(okJson(loadFixture("/json/ibxsmartview/power_current_get_response.json"))));

            PowerData result = ibxSmartView.legacyPower()
                    .getCurrent("123456", "SV5", "cage", "SV5:01:001000");

            assertNotNull(result);
            assertNotNull(result.getPayLoad());
            assertEquals("SV5", result.getPayLoad().getIbx());
            assertEquals("123456", result.getPayLoad().getAccountNo());
            assertEquals(PowerLevelType.CAGE, result.getPayLoad().getLevelType());
            assertEquals("SV5:01:001000", result.getPayLoad().getLevelValue());
            assertEquals(3.42, result.getPayLoad().getKva());
            assertEquals("false", result.getPayLoad().getIsAlarm());
            assertNotNull(result.getStatus());
            assertEquals(200, result.getStatus().getStatuscode());

            // Verify exact path, verb and every query parameter.
            wireMock.verify(getRequestedFor(urlPathEqualTo("/power/v1/current"))
                    .withQueryParam("accountNo", equalTo("123456"))
                    .withQueryParam("ibx", equalTo("SV5"))
                    .withQueryParam("levelType", equalTo("cage"))
                    .withQueryParam("levelValue", equalTo("SV5:01:001000")));
        }
    }

    @Nested
    @DisplayName("getTrending()")
    class GetTrending {

        @Test
        @DisplayName("GETs /power/v1/trending with all query params and unwraps the trend series")
        void getsTrendingPowerWithQueryParams() {
            wireMock.stubFor(get(urlPathEqualTo("/power/v1/trending"))
                    .willReturn(okJson(loadFixture("/json/ibxsmartview/power_trending_response.json"))));

            TrendingPowerData result = ibxSmartView.legacyPower()
                    .getTrending("123456", "SV5", "cage", "SV5:01:001000",
                            "hourly", "2026-06-30T00:00:00Z", "2026-06-30T23:59:59Z");

            assertNotNull(result);
            assertNotNull(result.getPayLoad());
            assertEquals("123456", result.getPayLoad().getAccountNumber());
            assertEquals("SV5", result.getPayLoad().getIbx());
            assertEquals(PowerLevelType.CAGE, result.getPayLoad().getLevelType());
            assertEquals("hourly", result.getPayLoad().getInterval());
            assertNotNull(result.getPayLoad().getData());
            assertEquals(3, result.getPayLoad().getData().size());
            assertEquals("3.42", result.getPayLoad().getData().get(1).getValue());
            assertEquals("2026-06-30T11:00:00Z", result.getPayLoad().getData().get(1).getDatetime());

            // Verify exact path, verb and every query parameter.
            wireMock.verify(getRequestedFor(urlPathEqualTo("/power/v1/trending"))
                    .withQueryParam("accountNo", equalTo("123456"))
                    .withQueryParam("ibx", equalTo("SV5"))
                    .withQueryParam("levelType", equalTo("cage"))
                    .withQueryParam("levelValue", equalTo("SV5:01:001000"))
                    .withQueryParam("interval", equalTo("hourly"))
                    .withQueryParam("fromDate", equalTo("2026-06-30T00:00:00Z"))
                    .withQueryParam("toDate", equalTo("2026-06-30T23:59:59Z")));
        }
    }
}
