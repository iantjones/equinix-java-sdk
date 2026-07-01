package api.equinix.javasdk.customerportal.wiremock;

import api.equinix.javasdk.CustomerPortal;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.customerportal.model.SupportPlan;
import org.junit.jupiter.api.*;

import java.util.List;

import static api.equinix.javasdk.core.ResponseStubs.*;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * WireMock-based API tests for the CustomerPortal Support Plans (v2) client.
 *
 * <p>All read operations map to {@code GET /colocations/v2/supportPlans}. The resource declares an
 * {@code overrideUriFormat} of {@code {$rootUri}/{$requestUri}} in apiParams (dropping the functional
 * area version prefix) and the {@code ListSupportPlans} endpoint has no {@code requestUri}, so the
 * effective path is exactly {@code /colocations/v2/supportPlans}.</p>
 *
 * <p>These tests exercise the three public {@code list(...)} overloads and assert the verb, path and
 * the {@code accountNumbers}/{@code ibxs}/{@code planIds}/{@code sorts} query-parameter forwarding.</p>
 */
class CustomerPortalSupportPlansWireMockTest extends WireMockTestBase {

    private static final String LIST_PATH = "/colocations/v2/supportPlans";
    private static final String FIXTURE = "/json/customerportal/paginated_support_plans.json";

    static CustomerPortal customerPortal;

    @BeforeAll
    static void setUp() {
        customerPortal = new CustomerPortal(testCredentials());
        redirectToWireMock(customerPortal);
        customerPortal.authenticate();
    }

    @AfterAll
    static void tearDown() throws Exception {
        if (customerPortal != null) customerPortal.close();
    }

    @BeforeEach
    void resetBeforeEach() {
        resetStubs();
    }

    @Nested
    @DisplayName("list()")
    class ListAll {

        @Test
        @DisplayName("GETs /colocations/v2/supportPlans with no query params and maps the page")
        void listAll_getsPage() {
            stubPaginatedGet(wireMock, LIST_PATH, FIXTURE);

            PaginatedList<SupportPlan> plans = customerPortal.supportPlans().list();

            assertNotNull(plans);
            assertEquals(2, plans.size());
            SupportPlan first = plans.get(0);
            assertEquals("SP-100023", first.getId());
            assertEquals("128745", first.getAccountNumber());
            assertEquals("Smart Hands Premium", first.getPlanName());
            assertEquals(List.of("SV5", "DC11"), first.getIbxs());
            assertEquals(430, first.getRemainingMinutes());

            wireMock.verify(getRequestedFor(urlPathEqualTo(LIST_PATH))
                    .withQueryParam("accountNumbers", absent())
                    .withQueryParam("ibxs", absent())
                    .withQueryParam("planIds", absent())
                    .withQueryParam("sorts", absent()));
        }
    }

    @Nested
    @DisplayName("list(accountNumbers, ibxs, planIds)")
    class ListFiltered {

        @Test
        @DisplayName("forwards accountNumbers, ibxs and planIds as query params")
        void listFiltered_forwardsFilters() {
            stubPaginatedGet(wireMock, LIST_PATH, FIXTURE);

            PaginatedList<SupportPlan> plans = customerPortal.supportPlans().list(
                    List.of("128745"), List.of("SV5"), List.of("SP-100023"));

            assertNotNull(plans);
            assertEquals(2, plans.size());

            wireMock.verify(getRequestedFor(urlPathEqualTo(LIST_PATH))
                    .withQueryParam("accountNumbers", equalTo("128745"))
                    .withQueryParam("ibxs", equalTo("SV5"))
                    .withQueryParam("planIds", equalTo("SP-100023"))
                    .withQueryParam("sorts", absent()));
        }

        @Test
        @DisplayName("forwards each element of a multi-valued filter as a repeated query param")
        void listFiltered_forwardsRepeatedValues() {
            stubPaginatedGet(wireMock, LIST_PATH, FIXTURE);

            customerPortal.supportPlans().list(
                    List.of("128745", "998877"), null, null);

            wireMock.verify(getRequestedFor(urlPathEqualTo(LIST_PATH))
                    .withQueryParam("accountNumbers", havingExactly("128745", "998877"))
                    .withQueryParam("ibxs", absent())
                    .withQueryParam("planIds", absent()));
        }

        @Test
        @DisplayName("omits empty/null filters entirely")
        void listFiltered_omitsEmptyFilters() {
            stubPaginatedGet(wireMock, LIST_PATH, FIXTURE);

            customerPortal.supportPlans().list(null, List.of("DC11"), null);

            wireMock.verify(getRequestedFor(urlPathEqualTo(LIST_PATH))
                    .withQueryParam("ibxs", equalTo("DC11"))
                    .withQueryParam("accountNumbers", absent())
                    .withQueryParam("planIds", absent()));
        }
    }

    @Nested
    @DisplayName("list(accountNumbers, ibxs, planIds, sorts)")
    class ListSorted {

        @Test
        @DisplayName("forwards filters plus the sorts query param")
        void listSorted_forwardsSorts() {
            stubPaginatedGet(wireMock, LIST_PATH, FIXTURE);

            PaginatedList<SupportPlan> plans = customerPortal.supportPlans().list(
                    List.of("128745"), List.of("SV5"), List.of("SP-100023"), List.of("-startDate"));

            assertNotNull(plans);
            assertEquals(2, plans.size());

            wireMock.verify(getRequestedFor(urlPathEqualTo(LIST_PATH))
                    .withQueryParam("accountNumbers", equalTo("128745"))
                    .withQueryParam("ibxs", equalTo("SV5"))
                    .withQueryParam("planIds", equalTo("SP-100023"))
                    .withQueryParam("sorts", equalTo("-startDate")));
        }

        @Test
        @DisplayName("sorts-only (null filters) forwards just the sorts query param")
        void listSorted_sortsOnly() {
            stubPaginatedGet(wireMock, LIST_PATH, FIXTURE);

            customerPortal.supportPlans().list(null, null, null, List.of("planName", "-startDate"));

            wireMock.verify(getRequestedFor(urlPathEqualTo(LIST_PATH))
                    .withQueryParam("sorts", havingExactly("planName", "-startDate"))
                    .withQueryParam("accountNumbers", absent())
                    .withQueryParam("ibxs", absent())
                    .withQueryParam("planIds", absent()));
        }
    }
}
