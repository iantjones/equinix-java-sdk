package com.eqixiac.equinix.internetaccess.wiremock;

import com.eqixiac.equinix.InternetAccess;
import com.eqixiac.equinix.core.WireMockTestBase;
import com.eqixiac.equinix.internetaccess.model.InternetAccessService;
import com.eqixiac.equinix.internetaccess.model.json.creators.ChangeOperationUpdate;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.absent;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.patch;
import static com.github.tomakehurst.wiremock.client.WireMock.patchRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * WireMock-backed tests for the Equinix Internet Access (EIA) v2 service update
 * ({@code PATCH /internetAccess/v2/services/{serviceId}}) and delete
 * ({@code DELETE /internetAccess/v2/services/{serviceId}}) operations, including the
 * validate-only {@code dryRun} query-parameter overloads.
 */
class InternetAccessServiceLifecycleWireMockTest extends WireMockTestBase {

    private static final String SERVICE_ID = "919ac898-a4b9-4f9d-b684-aa09ddc65b1b";
    private static final String SERVICE_PATH = "/internetAccess/v2/services/" + SERVICE_ID;

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

    @Test
    void delete_withoutDryRun_omitsQueryParameter() {
        wireMock.stubFor(delete(urlPathEqualTo(SERVICE_PATH))
                .willReturn(aResponse().withStatus(202)));

        assertTrue(internetAccess.services().delete(SERVICE_ID));

        wireMock.verify(deleteRequestedFor(urlPathEqualTo(SERVICE_PATH))
                .withQueryParam("dryRun", absent()));
    }

    @Test
    void delete_withDryRun_addsQueryParameter() {
        wireMock.stubFor(delete(urlPathEqualTo(SERVICE_PATH))
                .willReturn(aResponse().withStatus(202)));

        assertTrue(internetAccess.services().delete(SERVICE_ID, true));

        wireMock.verify(deleteRequestedFor(urlPathEqualTo(SERVICE_PATH))
                .withQueryParam("dryRun", equalTo("true")));
    }

    @Test
    void update_withDryRun_addsQueryParameterAndDeserializesResponse() {
        wireMock.stubFor(patch(urlPathEqualTo(SERVICE_PATH))
                .willReturn(aResponse()
                        .withStatus(202)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{ \"uuid\": \"" + SERVICE_ID + "\", \"type\": \"SINGLE\", \"bandwidth\": 2000, \"state\": \"PROVISIONING\" }")));

        List<ChangeOperationUpdate> ops = List.of(ChangeOperationUpdate.replace("/bandwidth", "2000"));
        InternetAccessService service = internetAccess.services().update(SERVICE_ID, ops, true);

        assertEquals(Long.valueOf(2000), service.getBandwidth());

        wireMock.verify(patchRequestedFor(urlPathEqualTo(SERVICE_PATH))
                .withQueryParam("dryRun", equalTo("true")));
    }

    @Test
    void update_withoutDryRun_omitsQueryParameter() {
        wireMock.stubFor(patch(urlPathEqualTo(SERVICE_PATH))
                .willReturn(aResponse()
                        .withStatus(202)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{ \"uuid\": \"" + SERVICE_ID + "\", \"type\": \"SINGLE\", \"state\": \"PROVISIONING\" }")));

        internetAccess.services().update(SERVICE_ID, List.of(ChangeOperationUpdate.replace("/bandwidth", "2000")));

        wireMock.verify(patchRequestedFor(urlPathEqualTo(SERVICE_PATH))
                .withQueryParam("dryRun", absent()));
    }
}
