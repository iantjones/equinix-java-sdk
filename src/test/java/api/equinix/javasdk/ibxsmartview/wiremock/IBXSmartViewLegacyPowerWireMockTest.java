package api.equinix.javasdk.ibxsmartview.wiremock;

import api.equinix.javasdk.IBXSmartView;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.ibxsmartview.enums.PowerLevelType;
import api.equinix.javasdk.ibxsmartview.model.PowerDataIBX;
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
            assertEquals("cage", data.get(0).getLevelType());
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
}
