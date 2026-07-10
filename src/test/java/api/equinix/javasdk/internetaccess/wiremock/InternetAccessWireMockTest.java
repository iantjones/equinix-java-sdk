package api.equinix.javasdk.internetaccess.wiremock;

import api.equinix.javasdk.InternetAccess;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.core.exception.*;
import api.equinix.javasdk.internetaccess.enums.ExportPolicy;
import api.equinix.javasdk.internetaccess.enums.ServiceTypeV2;
import api.equinix.javasdk.internetaccess.model.json.creators.BgpRoutingProtocolRequest;
import api.equinix.javasdk.internetaccess.model.json.creators.ChangeOperationUpdate;
import api.equinix.javasdk.internetaccess.model.json.creators.ServiceSearchRequest;
import org.junit.jupiter.api.*;

import java.util.List;

import static api.equinix.javasdk.core.ResponseStubs.*;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * WireMock-based error-mapping tests for the Internet Access (EIA) v2 services lifecycle:
 * create ({@code POST /internetAccess/v2/services}), read
 * ({@code GET /internetAccess/v2/services/{serviceId}}), update
 * ({@code PATCH /internetAccess/v2/services/{serviceId}}), delete
 * ({@code DELETE /internetAccess/v2/services/{serviceId}}) and search
 * ({@code POST /internetAccess/v2/services/search}). Every HTTP error status the SDK maps to a
 * typed exception (400/401/403/404/409/429/5xx) is exercised on the create path, and the
 * destructive/lookup ops pin the statuses whose semantics matter for them (404/409 on
 * update/delete, 401/404 on get, 400/403/429 on search). Error bodies use the EIA wire shape:
 * a JSON array of {@code {"errorCode": ..., "errorMessage": ...}} objects.
 */
class InternetAccessWireMockTest extends WireMockTestBase {

    private static final String SERVICE_ID = "919ac898-a4b9-4f9d-b684-aa09ddc65b1b";
    private static final String SERVICES_PATH = "/internetAccess/v2/services";
    private static final String SERVICE_PATH = SERVICES_PATH + "/" + SERVICE_ID;
    private static final String SEARCH_PATH = SERVICES_PATH + "/search";

    static InternetAccess internetAccess;

    @BeforeAll
    static void setUp() {
        internetAccess = new InternetAccess(testCredentials());
        redirectToWireMock(internetAccess);
        internetAccess.authenticate();
    }

    @AfterAll
    static void tearDown() throws Exception {
        if (internetAccess != null) internetAccess.close();
    }

    @BeforeEach
    void resetBeforeEach() {
        resetStubs();
    }

    private void createBgpService() {
        internetAccess.services().define()
                .name("WebServers")
                .type(ServiceTypeV2.SINGLE)
                .connection("9b8c5042-b553-4d5e-a2ac-c73bf6d4fd81")
                .routingProtocol(BgpRoutingProtocolRequest.builder()
                        .customerAsn(16220L)
                        .exportPolicy(ExportPolicy.FULL)
                        .build())
                .create();
    }

    @Nested
    @DisplayName("create() - Error handling (full status matrix)")
    class ServicesErrors {

        @Test
        @DisplayName("400 throws EquinixServiceException")
        void badRequest() {
            stubErrorInline(wireMock, SERVICES_PATH,
                    400, "[{\"errorCode\":\"ERR-400\",\"errorMessage\":\"Invalid request\"}]");

            assertThrows(EquinixServiceException.class, InternetAccessWireMockTest.this::createBgpService);
        }

        @Test
        @DisplayName("401 throws EquinixAuthenticationException")
        void unauthorized() {
            stubErrorInline(wireMock, SERVICES_PATH,
                    401, "[{\"errorCode\":\"ERR-401\",\"errorMessage\":\"Authentication failed\"}]");

            assertThrows(EquinixAuthenticationException.class, InternetAccessWireMockTest.this::createBgpService);
        }

        @Test
        @DisplayName("403 throws EquinixAuthorizationException")
        void forbidden() {
            stubErrorInline(wireMock, SERVICES_PATH,
                    403, "[{\"errorCode\":\"ERR-403\",\"errorMessage\":\"Access denied\"}]");

            assertThrows(EquinixAuthorizationException.class, InternetAccessWireMockTest.this::createBgpService);
        }

        @Test
        @DisplayName("404 throws EquinixNotFoundException")
        void notFound() {
            stubErrorInline(wireMock, SERVICES_PATH,
                    404, "[{\"errorCode\":\"ERR-404\",\"errorMessage\":\"Connection not found\"}]");

            assertThrows(EquinixNotFoundException.class, InternetAccessWireMockTest.this::createBgpService);
        }

        @Test
        @DisplayName("409 throws EquinixConflictException")
        void conflict() {
            stubErrorInline(wireMock, SERVICES_PATH,
                    409, "[{\"errorCode\":\"ERR-409\",\"errorMessage\":\"Service already exists on this connection\"}]");

            assertThrows(EquinixConflictException.class, InternetAccessWireMockTest.this::createBgpService);
        }

        @Test
        @DisplayName("429 throws EquinixRateLimitException")
        void rateLimited() {
            stubErrorInline(wireMock, SERVICES_PATH,
                    429, "[{\"errorCode\":\"ERR-429\",\"errorMessage\":\"Too many requests\"}]");

            assertThrows(EquinixRateLimitException.class, InternetAccessWireMockTest.this::createBgpService);
        }

        @Test
        @DisplayName("500 throws EquinixServerException")
        void serverError() {
            stubErrorInline(wireMock, SERVICES_PATH,
                    500, "[{\"errorCode\":\"ERR-500\",\"errorMessage\":\"Internal server error\"}]");

            assertThrows(EquinixServerException.class, InternetAccessWireMockTest.this::createBgpService);
        }
    }

    @Nested
    @DisplayName("getByUuid() - Error handling")
    class GetByUuidErrors {

        @Test
        @DisplayName("404 throws EquinixNotFoundException")
        void notFound() {
            stubErrorInline(wireMock, SERVICE_PATH,
                    404, "[{\"errorCode\":\"ERR-404\",\"errorMessage\":\"Service not found\"}]");

            assertThrows(EquinixNotFoundException.class,
                    () -> internetAccess.services().getByUuid(SERVICE_ID));
        }

        @Test
        @DisplayName("401 throws EquinixAuthenticationException")
        void unauthorized() {
            stubErrorInline(wireMock, SERVICE_PATH,
                    401, "[{\"errorCode\":\"ERR-401\",\"errorMessage\":\"Authentication failed\"}]");

            assertThrows(EquinixAuthenticationException.class,
                    () -> internetAccess.services().getByUuid(SERVICE_ID));
        }
    }

    @Nested
    @DisplayName("update() - Error handling")
    class UpdateErrors {

        private void updateBandwidth() {
            internetAccess.services().update(SERVICE_ID,
                    List.of(ChangeOperationUpdate.replace("/bandwidth", "2000")));
        }

        @Test
        @DisplayName("404 throws EquinixNotFoundException")
        void notFound() {
            stubErrorInline(wireMock, SERVICE_PATH,
                    404, "[{\"errorCode\":\"ERR-404\",\"errorMessage\":\"Service not found\"}]");

            assertThrows(EquinixNotFoundException.class, this::updateBandwidth);
        }

        @Test
        @DisplayName("409 throws EquinixConflictException")
        void conflict() {
            stubErrorInline(wireMock, SERVICE_PATH,
                    409, "[{\"errorCode\":\"ERR-409\",\"errorMessage\":\"Service has a pending change\"}]");

            assertThrows(EquinixConflictException.class, this::updateBandwidth);
        }
    }

    @Nested
    @DisplayName("delete() - Error handling")
    class DeleteErrors {

        @Test
        @DisplayName("404 throws EquinixNotFoundException")
        void notFound() {
            stubErrorInline(wireMock, SERVICE_PATH,
                    404, "[{\"errorCode\":\"ERR-404\",\"errorMessage\":\"Service not found\"}]");

            assertThrows(EquinixNotFoundException.class,
                    () -> internetAccess.services().delete(SERVICE_ID));
        }

        @Test
        @DisplayName("409 throws EquinixConflictException")
        void conflict() {
            stubErrorInline(wireMock, SERVICE_PATH,
                    409, "[{\"errorCode\":\"ERR-409\",\"errorMessage\":\"Service is not in a deletable state\"}]");

            assertThrows(EquinixConflictException.class,
                    () -> internetAccess.services().delete(SERVICE_ID));
        }
    }

    @Nested
    @DisplayName("search() - Error handling")
    class SearchErrors {

        private void search() {
            internetAccess.services().search(new ServiceSearchRequest().equals("/state", "ACTIVE"));
        }

        @Test
        @DisplayName("400 throws EquinixServiceException")
        void badRequest() {
            stubErrorInline(wireMock, SEARCH_PATH,
                    400, "[{\"errorCode\":\"ERR-400\",\"errorMessage\":\"Invalid filter property\"}]");

            assertThrows(EquinixServiceException.class, this::search);
        }

        @Test
        @DisplayName("403 throws EquinixAuthorizationException")
        void forbidden() {
            stubErrorInline(wireMock, SEARCH_PATH,
                    403, "[{\"errorCode\":\"ERR-403\",\"errorMessage\":\"Access denied\"}]");

            assertThrows(EquinixAuthorizationException.class, this::search);
        }

        @Test
        @DisplayName("429 throws EquinixRateLimitException")
        void rateLimited() {
            stubErrorInline(wireMock, SEARCH_PATH,
                    429, "[{\"errorCode\":\"ERR-429\",\"errorMessage\":\"Too many requests\"}]");

            assertThrows(EquinixRateLimitException.class, this::search);
        }
    }
}
