package api.equinix.javasdk.ibxsmartview.wiremock;

import api.equinix.javasdk.IBXSmartView;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.ibxsmartview.model.EnvironmentData;
import api.equinix.javasdk.ibxsmartview.model.EnvironmentDataForArray;
import api.equinix.javasdk.ibxsmartview.model.TrendingEnvironmentData;
import org.junit.jupiter.api.*;

import java.util.List;

import static api.equinix.javasdk.core.ResponseStubs.*;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * WireMock-based tests for the legacy IBX SmartView environmental API
 * ({@link api.equinix.javasdk.ibxsmartview.client.LegacyEnvironmentals}).
 *
 * <p>All three read ops are GETs against the legacy {@code /environment/v1/*} endpoints
 * (empty {@code rootUri}, {@code overrideUriFormat} {@code environment/v{version}/{requestUri}},
 * {@code defaultVersion} 1) and pass their inputs entirely as query parameters:</p>
 * <ul>
 *   <li>{@code getCurrent} -> GET {@code /environment/v1/current}</li>
 *   <li>{@code listCurrent} -> GET {@code /environment/v1/listCurrent} (unwraps {@code payLoad.data})</li>
 *   <li>{@code getTrending} -> GET {@code /environment/v1/trending}</li>
 * </ul>
 */
class IBXSmartViewLegacyEnvironmentalsWireMockTest extends WireMockTestBase {

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
    @DisplayName("getCurrent()")
    class GetCurrent {

        @Test
        @DisplayName("GETs /environment/v1/current with all inputs as query params")
        void getsCurrent() {
            stubSingleton(wireMock, "/environment/v1/current",
                    "/json/ibxsmartview/environment_data_response.json");

            EnvironmentData data = ibxSmartView.legacyEnvironmentals()
                    .getCurrent("123456", "SV5", "cabinet", "SV5:01:001100:0105");

            assertNotNull(data);
            assertNotNull(data.getPayLoad());
            assertEquals("SV5", data.getPayLoad().getIbx());
            assertEquals("123456", data.getPayLoad().getAccountNo());
            assertEquals("21.8", data.getPayLoad().getTemperature());
            assertEquals("42.3", data.getPayLoad().getHumidity());

            wireMock.verify(getRequestedFor(urlPathEqualTo("/environment/v1/current"))
                    .withQueryParam("accountNo", equalTo("123456"))
                    .withQueryParam("ibx", equalTo("SV5"))
                    .withQueryParam("levelType", equalTo("cabinet"))
                    .withQueryParam("levelValue", equalTo("SV5:01:001100:0105")));
        }
    }

    @Nested
    @DisplayName("listCurrent()")
    class ListCurrent {

        @Test
        @DisplayName("GETs /environment/v1/listCurrent and unwraps payLoad.data")
        void listsCurrent() {
            stubSingleton(wireMock, "/environment/v1/listCurrent",
                    "/json/ibxsmartview/environment_list_current_response.json");

            List<EnvironmentDataForArray> data = ibxSmartView.legacyEnvironmentals()
                    .listCurrent("123456", "SV5", "cage");

            assertNotNull(data);
            assertEquals(2, data.size());
            assertEquals("SV5", data.get(0).getIbx());
            assertEquals("SV5:01:001100:0105", data.get(0).getCabinet());
            assertEquals("22.4", data.get(1).getTemperature());

            wireMock.verify(getRequestedFor(urlPathEqualTo("/environment/v1/listCurrent"))
                    .withQueryParam("accountNo", equalTo("123456"))
                    .withQueryParam("ibx", equalTo("SV5"))
                    .withQueryParam("levelType", equalTo("cage")));
        }

        @Test
        @DisplayName("returns an empty list when payLoad has no data array")
        void returnsEmptyListWhenNoData() {
            wireMock.stubFor(get(urlPathEqualTo("/environment/v1/listCurrent"))
                    .willReturn(okJson("{\"payLoad\":{},\"status\":{\"type\":\"INFO\",\"statuscode\":1000}}")));

            List<EnvironmentDataForArray> data = ibxSmartView.legacyEnvironmentals()
                    .listCurrent("123456", "SV5", "cage");

            assertNotNull(data);
            assertTrue(data.isEmpty());

            wireMock.verify(getRequestedFor(urlPathEqualTo("/environment/v1/listCurrent"))
                    .withQueryParam("levelType", equalTo("cage")));
        }
    }

    @Nested
    @DisplayName("getTrending()")
    class GetTrending {

        @Test
        @DisplayName("GETs /environment/v1/trending with all eight inputs as query params")
        void getsTrending() {
            stubSingleton(wireMock, "/environment/v1/trending",
                    "/json/ibxsmartview/environment_trending_response.json");

            TrendingEnvironmentData data = ibxSmartView.legacyEnvironmentals()
                    .getTrending("123456", "SV5", "temperature", "cabinet",
                            "SV5:01:001100:0105", "hourly",
                            "2024-01-15T00:00:00Z", "2024-01-15T23:59:59Z");

            assertNotNull(data);
            assertNotNull(data.getPayLoad());
            assertEquals("SV5", data.getPayLoad().getIbx());
            assertEquals("temperature", data.getPayLoad().getDatapoint());
            assertEquals("hourly", data.getPayLoad().getInterval());
            assertNotNull(data.getPayLoad().getSeries());
            assertEquals(2, data.getPayLoad().getSeries().size());
            assertEquals("21.5", data.getPayLoad().getSeries().get(0).getValue());

            wireMock.verify(getRequestedFor(urlPathEqualTo("/environment/v1/trending"))
                    .withQueryParam("accountNo", equalTo("123456"))
                    .withQueryParam("ibx", equalTo("SV5"))
                    .withQueryParam("dataPoint", equalTo("temperature"))
                    .withQueryParam("levelType", equalTo("cabinet"))
                    .withQueryParam("levelValue", equalTo("SV5:01:001100:0105"))
                    .withQueryParam("interval", equalTo("hourly"))
                    .withQueryParam("fromDate", equalTo("2024-01-15T00:00:00Z"))
                    .withQueryParam("toDate", equalTo("2024-01-15T23:59:59Z")));
        }
    }
}
