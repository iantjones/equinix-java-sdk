package api.equinix.javasdk.internetaccess.wiremock;

import api.equinix.javasdk.InternetAccess;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.internetaccess.model.OperationalUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * WireMock-backed tests for the Equinix Internet Access (EIA) v1 operational-units lookup
 * ({@code GET /internetAccess/v1/operationalUnits}, the {@code OperationalUnitsV1} group in
 * {@code apiParams_InternetAccess.json}, {@code defaultVersion: 1}). Confirms the v1 URI/version
 * routing, the {@code location.ibx} filter placement, and the read-only {@link OperationalUnit}
 * deserialization (the JSON model implements the interface directly, so there is no wrapper).
 */
class InternetAccessOperationalUnitsWireMockTest extends WireMockTestBase {

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

    private static String page(String dataJson) {
        return "{ \"pagination\": { \"offset\": 0, \"limit\": 50, \"total\": 1 }, \"data\": [" + dataJson + "] }";
    }

    @Test
    void list_routesToV1WithIbxQueryParam() {
        wireMock.stubFor(get(urlPathEqualTo("/internetAccess/v1/operationalUnits"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(page("{ \"name\": \"Equinix (Poland) Sp. z o.o.\", "
                                + "\"address\": { \"street\": \"ul. Konstruktorska 13\", \"city\": \"Warsaw\", "
                                + "\"countryCode\": \"PL\", \"postalCode\": \"02-673\" }, "
                                + "\"location\": { \"ibx\": \"WA1\", \"metroCode\": \"WA\" } }"))));

        PaginatedList<OperationalUnit> units = internetAccess.operationalUnits().list("WA1");

        assertEquals(1, units.size());
        assertEquals("Equinix (Poland) Sp. z o.o.", units.get(0).getName());
        assertNotNull(units.get(0).getAddress());
        assertEquals("Warsaw", units.get(0).getAddress().getCity());
        assertNotNull(units.get(0).getLocation());
        assertEquals("WA1", units.get(0).getLocation().getIbx());
        assertEquals("WA", units.get(0).getLocation().getMetroCode());

        wireMock.verify(getRequestedFor(urlPathEqualTo("/internetAccess/v1/operationalUnits"))
                .withQueryParam("location.ibx", equalTo("WA1")));
    }
}
