package api.equinix.javasdk.fabric.wiremock;

import api.equinix.javasdk.Fabric;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.core.exception.*;
import api.equinix.javasdk.fabric.model.StreamAsset;
import org.junit.jupiter.api.*;

import static api.equinix.javasdk.core.ResponseStubs.*;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * WireMock-based API tests for Fabric Stream Assets.
 * Covers attach() (PUT with metricsEnabled body), detach() (DELETE), and error handling.
 */
class FabricStreamAssetsWireMockTest extends WireMockTestBase {

    static Fabric fabric;

    static final String STREAM_ID = "d4e5f6a7-b8c9-0123-defa-345678901bcd";
    static final String ASSET = "connections";
    static final String ASSET_ID = "3a58dd05-f46d-4b1d-a154-2e85c396ea85";
    static final String ASSET_PATH =
            "/fabric/v4/streams/" + STREAM_ID + "/" + ASSET + "/" + ASSET_ID;

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
    @DisplayName("attach()")
    class Attach {

        @Test
        @DisplayName("PUTs the asset path with a metricsEnabled body and returns the asset")
        void attachPutsMetricsEnabledBody() {
            wireMock.stubFor(put(urlPathEqualTo(ASSET_PATH))
                    .willReturn(okJson(loadFixture("/json/fabric/stream_asset_response.json"))));

            StreamAsset asset = fabric.streamAssets().attach(STREAM_ID, ASSET, ASSET_ID, true);

            assertNotNull(asset);
            assertEquals(ASSET_ID, asset.getUuid());
            assertEquals("CONNECTION", asset.getType());
            assertEquals(Boolean.TRUE, asset.getMetricsEnabled());
            assertEquals("ATTACHED", asset.getAttachmentStatus());

            wireMock.verify(putRequestedFor(urlPathEqualTo(ASSET_PATH))
                    .withRequestBody(equalToJson("{\"metricsEnabled\":true}", true, true)));
        }

        @Test
        @DisplayName("omits metricsEnabled from the body when null (NON_NULL)")
        void attachOmitsNullMetricsEnabled() {
            wireMock.stubFor(put(urlPathEqualTo(ASSET_PATH))
                    .willReturn(okJson(loadFixture("/json/fabric/stream_asset_response.json"))));

            fabric.streamAssets().attach(STREAM_ID, ASSET, ASSET_ID, null);

            wireMock.verify(putRequestedFor(urlPathEqualTo(ASSET_PATH))
                    .withRequestBody(equalToJson("{}", true, true)));
        }

        @Test
        @DisplayName("sends metricsEnabled=false when explicitly disabled")
        void attachSendsMetricsDisabled() {
            wireMock.stubFor(put(urlPathEqualTo(ASSET_PATH))
                    .willReturn(okJson(loadFixture("/json/fabric/stream_asset_response.json"))));

            fabric.streamAssets().attach(STREAM_ID, ASSET, ASSET_ID, false);

            wireMock.verify(putRequestedFor(urlPathEqualTo(ASSET_PATH))
                    .withRequestBody(matchingJsonPath("$.metricsEnabled", equalTo("false"))));
        }
    }

    @Nested
    @DisplayName("detach()")
    class Detach {

        @Test
        @DisplayName("DELETEs the asset path and returns true")
        void detachDeletesAssetPath() {
            wireMock.stubFor(delete(urlPathEqualTo(ASSET_PATH))
                    .willReturn(noContent()));

            Boolean result = fabric.streamAssets().detach(STREAM_ID, ASSET, ASSET_ID);

            assertEquals(Boolean.TRUE, result);
            wireMock.verify(deleteRequestedFor(urlPathEqualTo(ASSET_PATH)));
        }
    }

    @Nested
    @DisplayName("Error handling")
    class Errors {

        @Test
        @DisplayName("404 on attach throws EquinixNotFoundException")
        void attachNotFound() {
            stubErrorInline(wireMock, ASSET_PATH,
                    404, "[{\"errorCode\":\"ERR-404\",\"errorMessage\":\"Stream not found\"}]");

            assertThrows(EquinixNotFoundException.class,
                    () -> fabric.streamAssets().attach(STREAM_ID, ASSET, ASSET_ID, true));
        }

        @Test
        @DisplayName("404 on detach throws EquinixNotFoundException")
        void detachNotFound() {
            stubErrorInline(wireMock, ASSET_PATH,
                    404, "[{\"errorCode\":\"ERR-404\",\"errorMessage\":\"Asset not attached\"}]");

            assertThrows(EquinixNotFoundException.class,
                    () -> fabric.streamAssets().detach(STREAM_ID, ASSET, ASSET_ID));
        }

        @Test
        @DisplayName("401 throws EquinixAuthenticationException")
        void unauthorized() {
            stubErrorInline(wireMock, ASSET_PATH,
                    401, "[{\"errorCode\":\"ERR-401\",\"errorMessage\":\"Unauthorized\"}]");

            assertThrows(EquinixAuthenticationException.class,
                    () -> fabric.streamAssets().attach(STREAM_ID, ASSET, ASSET_ID, true));
        }

        @Test
        @DisplayName("500 throws EquinixServerException")
        void serverError() {
            stubErrorInline(wireMock, ASSET_PATH,
                    500, "[{\"errorCode\":\"ERR-500\",\"errorMessage\":\"Internal server error\"}]");

            assertThrows(EquinixServerException.class,
                    () -> fabric.streamAssets().detach(STREAM_ID, ASSET, ASSET_ID));
        }
    }
}
