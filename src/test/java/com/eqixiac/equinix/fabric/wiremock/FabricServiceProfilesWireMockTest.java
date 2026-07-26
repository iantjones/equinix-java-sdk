package com.eqixiac.equinix.fabric.wiremock;

import com.eqixiac.equinix.Fabric;
import com.eqixiac.equinix.core.WireMockTestBase;
import com.eqixiac.equinix.core.exception.*;
import com.eqixiac.equinix.core.http.response.PaginatedFilteredList;
import com.eqixiac.equinix.core.http.response.PaginatedList;
import com.eqixiac.equinix.fabric.enums.ServiceProfileType;
import com.eqixiac.equinix.fabric.enums.ServiceProfileVisibility;
import com.eqixiac.equinix.fabric.model.ServiceProfile;
import com.eqixiac.equinix.fabric.model.ServiceProfileAction;
import com.eqixiac.equinix.fabric.model.implementation.ServiceMetro;
import com.eqixiac.equinix.fabric.model.implementation.filter.Filter;
import com.eqixiac.equinix.fabric.model.implementation.filter.FilterPropertyList;
import com.eqixiac.equinix.fabric.model.implementation.sort.Sort;
import com.eqixiac.equinix.fabric.model.implementation.sort.SortPropertyList;
import org.junit.jupiter.api.*;

import java.util.List;

import static com.eqixiac.equinix.core.ResponseStubs.*;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * WireMock-based API tests for Fabric Service Profiles.
 */
class FabricServiceProfilesWireMockTest extends WireMockTestBase {

    static Fabric fabric;

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
    @DisplayName("getByUuid()")
    class GetByUuid {

        @Test
        @DisplayName("returns service profile for valid UUID")
        void returnsServiceProfile() {
            stubSingleton(wireMock, "/fabric/v4/serviceProfiles/.*",
                    "/json/fabric/service_profile_response.json");

            ServiceProfile profile = fabric.serviceProfiles().getByUuid("f6a7b8c9-d0e1-2345-fabc-567890123def");
            assertNotNull(profile);
            assertEquals("f6a7b8c9-d0e1-2345-fabc-567890123def", profile.getUuid());
            assertEquals("AWS Direct Connect - Production", profile.getName());
        }

        @Test
        @DisplayName("404 throws EquinixNotFoundException")
        void notFound() {
            stubErrorInline(wireMock, "/fabric/v4/serviceProfiles/.*",
                    404, "[{\"errorCode\":\"ERR-404\",\"errorMessage\":\"Service profile not found\"}]");

            assertThrows(EquinixNotFoundException.class,
                    () -> fabric.serviceProfiles().getByUuid("invalid-uuid"));
        }
    }

    @Nested
    @DisplayName("list()")
    class ListProfiles {

        @Test
        @DisplayName("GETs /serviceProfiles and returns a paginated list")
        void returnsPaginatedList() {
            stubPaginatedGet(wireMock, "/fabric/v4/serviceProfiles",
                    "/json/fabric/paginated_service_profiles.json");

            PaginatedList<ServiceProfile> profiles = fabric.serviceProfiles().list();

            assertNotNull(profiles);
            assertEquals(2, profiles.size());
            assertEquals("f6a7b8c9-d0e1-2345-fabc-567890123def", profiles.get(0).getUuid());
            assertEquals("AWS Direct Connect - Production", profiles.get(0).getName());
            assertEquals("a1b2c3d4-e5f6-7890-abcd-ef1234567890", profiles.get(1).getUuid());

            wireMock.verify(getRequestedFor(urlPathEqualTo("/fabric/v4/serviceProfiles")));
        }
    }

    @Nested
    @DisplayName("search()")
    class Search {

        private static final String SEARCH_URL = "/fabric/v4/serviceProfiles/search";

        @Test
        @DisplayName("no-arg search POSTs the default body to /serviceProfiles/search and returns a filtered list")
        void searchNoArg() {
            stubPaginatedPost(wireMock, SEARCH_URL, "/json/fabric/paginated_service_profiles.json");

            PaginatedFilteredList<ServiceProfile> profiles = fabric.serviceProfiles().search();

            assertNotNull(profiles);
            assertEquals(2, profiles.size());
            assertEquals("f6a7b8c9-d0e1-2345-fabc-567890123def", profiles.get(0).getUuid());

            // Default no-arg search sends an (empty) filter, no sort, with pagination.
            wireMock.verify(postRequestedFor(urlPathEqualTo(SEARCH_URL))
                    .withHeader("Content-Type", containing("application/json"))
                    .withRequestBody(matchingJsonPath("$.pagination")));
        }

        @Test
        @DisplayName("search(filter) carries the filter predicate in the POST body")
        void searchWithFilter() {
            stubPaginatedPost(wireMock, SEARCH_URL, "/json/fabric/paginated_service_profiles.json");

            FilterPropertyList filter = Filter.filter().and()
                    .equals("/name", "AWS Direct Connect - Production")
                    .equals("/visibility", "PUBLIC");

            PaginatedFilteredList<ServiceProfile> profiles = fabric.serviceProfiles().search(filter);

            assertNotNull(profiles);
            assertEquals(2, profiles.size());

            wireMock.verify(postRequestedFor(urlPathEqualTo(SEARCH_URL))
                    .withRequestBody(matchingJsonPath("$.filter.and[0].property", equalTo("/name")))
                    .withRequestBody(matchingJsonPath("$.filter.and[0].values[0]", equalTo("AWS Direct Connect - Production")))
                    .withRequestBody(matchingJsonPath("$.filter.and[1].property", equalTo("/visibility")))
                    .withRequestBody(matchingJsonPath("$.filter.and[1].values[0]", equalTo("PUBLIC"))));
        }

        @Test
        @DisplayName("search(sort) carries the sort directive in the POST body")
        void searchWithSort() {
            stubPaginatedPost(wireMock, SEARCH_URL, "/json/fabric/paginated_service_profiles.json");

            SortPropertyList sort = Sort.sort().desc("/changeLog/updatedDateTime");

            PaginatedFilteredList<ServiceProfile> profiles = fabric.serviceProfiles().search(sort);

            assertNotNull(profiles);
            wireMock.verify(postRequestedFor(urlPathEqualTo(SEARCH_URL))
                    .withRequestBody(matchingJsonPath("$.sort[0].property", equalTo("/changeLog/updatedDateTime")))
                    .withRequestBody(matchingJsonPath("$.sort[0].direction", equalTo("DESC"))));
        }

        @Test
        @DisplayName("search(filter, sort) carries both filter and sort in the POST body")
        void searchWithFilterAndSort() {
            stubPaginatedPost(wireMock, SEARCH_URL, "/json/fabric/paginated_service_profiles.json");

            FilterPropertyList filter = Filter.filter().and()
                    .equals("/state", "ACTIVE");
            SortPropertyList sort = Sort.sort().asc("/name");

            PaginatedFilteredList<ServiceProfile> profiles = fabric.serviceProfiles().search(filter, sort);

            assertNotNull(profiles);
            wireMock.verify(postRequestedFor(urlPathEqualTo(SEARCH_URL))
                    .withRequestBody(matchingJsonPath("$.filter.and[0].property", equalTo("/state")))
                    .withRequestBody(matchingJsonPath("$.filter.and[0].values[0]", equalTo("ACTIVE")))
                    .withRequestBody(matchingJsonPath("$.sort[0].property", equalTo("/name")))
                    .withRequestBody(matchingJsonPath("$.sort[0].direction", equalTo("ASC"))));
        }
    }

    @Nested
    @DisplayName("getMetros()")
    class GetMetros {

        @Test
        @DisplayName("GETs {uuid}/metros and returns the list of service metros")
        void returnsMetros() {
            stubPaginatedGet(wireMock, "/fabric/v4/serviceProfiles/.*/metros",
                    "/json/fabric/service_profile_metros_response.json");

            List<ServiceMetro> metros = fabric.serviceProfiles().getMetros("f6a7b8c9-d0e1-2345-fabc-567890123def");

            assertNotNull(metros);
            assertEquals(2, metros.size());
            assertEquals("Silicon Valley (SV)", metros.get(0).getDisplayName());
            assertEquals(10000, metros.get(0).getVcBandwidthMax());
            assertTrue(metros.get(0).getInTrail());

            wireMock.verify(getRequestedFor(urlPathMatching(
                    "/fabric/v4/serviceProfiles/f6a7b8c9-d0e1-2345-fabc-567890123def/metros")));
        }
    }

    @Nested
    @DisplayName("define() / create()")
    class Create {

        @Test
        @DisplayName("POSTs a new service profile with the configured body")
        void createsServiceProfile() {
            stubCreate(wireMock, "/fabric/v4/serviceProfiles",
                    "/json/fabric/service_profile_response.json");

            ServiceProfile created = fabric.serviceProfiles()
                    .define(ServiceProfileType.L2_PROFILE)
                    .name("AWS Direct Connect - Production")
                    .description("AWS Direct Connect service profile for production workloads with low-latency connectivity")
                    .visibility(ServiceProfileVisibility.PUBLIC)
                    .allowedEmail("partner-onboard@example.com")
                    .tag("cloud")
                    .create();

            assertNotNull(created);
            assertEquals("f6a7b8c9-d0e1-2345-fabc-567890123def", created.getUuid());

            wireMock.verify(postRequestedFor(urlPathEqualTo("/fabric/v4/serviceProfiles"))
                    .withHeader("Content-Type", containing("application/json"))
                    .withRequestBody(matchingJsonPath("$.type", equalTo("L2_PROFILE")))
                    .withRequestBody(matchingJsonPath("$.name", equalTo("AWS Direct Connect - Production")))
                    .withRequestBody(matchingJsonPath("$.visibility", equalTo("PUBLIC")))
                    .withRequestBody(matchingJsonPath("$.allowedEmails[0]", equalTo("partner-onboard@example.com")))
                    .withRequestBody(matchingJsonPath("$.tags[0]", equalTo("cloud"))));
        }
    }

    @Nested
    @DisplayName("update() / save()")
    class Update {

        @Test
        @DisplayName("PATCHes a JSON Patch array as application/json-patch+json")
        void savePatchesNameAndDescription() {
            wireMock.stubFor(patch(urlPathMatching("/fabric/v4/serviceProfiles/.*"))
                    .willReturn(okJson(loadFixture("/json/fabric/service_profile_response.json"))));

            ServiceProfile updated = fabric.serviceProfiles()
                    .update("f6a7b8c9-d0e1-2345-fabc-567890123def")
                    .name("Renamed-Profile")
                    .description("Updated description")
                    .save();

            assertNotNull(updated);
            wireMock.verify(patchRequestedFor(urlPathEqualTo("/fabric/v4/serviceProfiles/f6a7b8c9-d0e1-2345-fabc-567890123def"))
                    .withHeader("Content-Type", containing("application/json-patch+json"))
                    .withRequestBody(equalToJson(
                            "[{\"op\":\"replace\",\"path\":\"/name\",\"value\":\"Renamed-Profile\"},"
                            + "{\"op\":\"replace\",\"path\":\"/description\",\"value\":\"Updated description\"}]")));
        }

        @Test
        @DisplayName("save() with no changes throws and makes no request")
        void emptyUpdateThrows() {
            assertThrows(IllegalStateException.class,
                    () -> fabric.serviceProfiles().update("f6a7b8c9-d0e1-2345-fabc-567890123def").save());
            wireMock.verify(0, patchRequestedFor(urlPathMatching("/fabric/v4/serviceProfiles/.*")));
        }
    }

    @Nested
    @DisplayName("createAction()")
    class CreateAction {

        @Test
        @DisplayName("POSTs {uuid}/actions with the action type and description")
        void postsAction() {
            wireMock.stubFor(post(urlPathMatching("/fabric/v4/serviceProfiles/.*/actions"))
                    .willReturn(okJson(loadFixture("/json/fabric/service_profile_action_response.json"))));

            ServiceProfileAction action = fabric.serviceProfiles().createAction(
                    "f6a7b8c9-d0e1-2345-fabc-567890123def",
                    "PROFILE_UPDATE_ACCEPTANCE",
                    "Approved by network team");

            assertNotNull(action);
            assertEquals("PROFILE_UPDATE_ACCEPTANCE", action.getType());

            wireMock.verify(postRequestedFor(urlPathEqualTo(
                    "/fabric/v4/serviceProfiles/f6a7b8c9-d0e1-2345-fabc-567890123def/actions"))
                    .withHeader("Content-Type", containing("application/json"))
                    .withRequestBody(matchingJsonPath("$.type", equalTo("PROFILE_UPDATE_ACCEPTANCE")))
                    .withRequestBody(matchingJsonPath("$.description", equalTo("Approved by network team"))));
        }

        @Test
        @DisplayName("omits null description from the request body")
        void omitsNullDescription() {
            wireMock.stubFor(post(urlPathMatching("/fabric/v4/serviceProfiles/.*/actions"))
                    .willReturn(okJson(loadFixture("/json/fabric/service_profile_action_response.json"))));

            fabric.serviceProfiles().createAction(
                    "f6a7b8c9-d0e1-2345-fabc-567890123def",
                    "PROFILE_UPDATE_REJECTION",
                    null);

            wireMock.verify(postRequestedFor(urlPathEqualTo(
                    "/fabric/v4/serviceProfiles/f6a7b8c9-d0e1-2345-fabc-567890123def/actions"))
                    .withRequestBody(matchingJsonPath("$.type", equalTo("PROFILE_UPDATE_REJECTION")))
                    .withRequestBody(notContaining("description")));
        }
    }

    @Nested
    @DisplayName("Error handling")
    class Errors {

        @Test
        @DisplayName("500 throws EquinixServerException")
        void serverError() {
            stubErrorInline(wireMock, "/fabric/v4/serviceProfiles/.*",
                    500, "[{\"errorCode\":\"ERR-500\",\"errorMessage\":\"Internal server error\"}]");

            assertThrows(EquinixServerException.class,
                    () -> fabric.serviceProfiles().getByUuid("test-uuid"));
        }
    }

    @Nested
    @DisplayName("Wrapper refresh()")
    class WrapperRefresh {

        private static final String SP_ID = "f6a7b8c9-d0e1-2345-fabc-567890123def";
        private static final String URL = "/fabric/v4/serviceProfiles/" + SP_ID;

        @Test
        @DisplayName("re-GETs /serviceProfiles/{uuid} and swaps the wrapper's state in place")
        void refreshReloadsInPlace() {
            wireMock.stubFor(get(urlPathEqualTo(URL))
                    .inScenario("sp-refresh")
                    .whenScenarioStateIs(com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED)
                    .willReturn(okJson(loadFixture("/json/fabric/service_profile_response.json")))
                    .willSetStateTo("renamed"));
            wireMock.stubFor(get(urlPathEqualTo(URL))
                    .inScenario("sp-refresh")
                    .whenScenarioStateIs("renamed")
                    .willReturn(okJson(loadFixture("/json/fabric/service_profile_response.json")
                            .replace("AWS Direct Connect - Production", "AWS Direct Connect - Renamed"))));

            ServiceProfile profile = fabric.serviceProfiles().getByUuid(SP_ID);
            assertEquals("AWS Direct Connect - Production", profile.getName());

            profile.refresh();

            assertEquals("AWS Direct Connect - Renamed", profile.getName(),
                    "refresh() must swap the wrapper's backing state in place");
            wireMock.verify(2, getRequestedFor(urlPathEqualTo(URL)));
        }
    }

    @Nested
    @DisplayName("Wrapper delete()")
    class WrapperDelete {

        private static final String SP_ID = "f6a7b8c9-d0e1-2345-fabc-567890123def";
        private static final String URL = "/fabric/v4/serviceProfiles/" + SP_ID;

        @Test
        @DisplayName("DELETEs /serviceProfiles/{uuid} and returns true")
        void deletesServiceProfile() {
            wireMock.stubFor(get(urlPathEqualTo(URL))
                    .willReturn(okJson(loadFixture("/json/fabric/service_profile_response.json"))));
            // deleteOne() reads the deleted resource from the response body, so the stub returns one.
            wireMock.stubFor(delete(urlPathEqualTo(URL))
                    .willReturn(okJson(loadFixture("/json/fabric/service_profile_response.json"))));

            ServiceProfile profile = fabric.serviceProfiles().getByUuid(SP_ID);
            Boolean deleted = profile.delete();

            assertEquals(Boolean.TRUE, deleted);
            wireMock.verify(deleteRequestedFor(urlPathEqualTo(URL)));
        }
    }

    @Nested
    @DisplayName("Multi-page search paging")
    class Paging {

        private static final String PAGE_1 = """
                {
                  "pagination": { "offset": 0, "limit": 100, "total": 150 },
                  "data": [ { "uuid": "PAGE1_PROFILE" } ]
                }
                """;

        private static final String PAGE_2 = """
                {
                  "pagination": { "offset": 100, "limit": 100, "total": 150 },
                  "data": [ { "uuid": "PAGE2_PROFILE" } ]
                }
                """;

        @Test
        @DisplayName("loadAll() re-POSTs the search with the body's pagination offset advanced to page 2")
        void loadAllFetchesSecondPage() {
            wireMock.stubFor(post(urlPathEqualTo("/fabric/v4/serviceProfiles/search"))
                    .withRequestBody(matchingJsonPath("$.pagination.offset", equalTo("0")))
                    .willReturn(okJson(PAGE_1)));
            wireMock.stubFor(post(urlPathEqualTo("/fabric/v4/serviceProfiles/search"))
                    .withRequestBody(matchingJsonPath("$.pagination.offset", equalTo("100")))
                    .willReturn(okJson(PAGE_2)));

            PaginatedFilteredList<ServiceProfile> profiles = fabric.serviceProfiles().search();
            assertEquals(1, profiles.size());
            assertTrue(profiles.hasNextPage());

            profiles.loadAll();

            assertEquals(2, profiles.size());
            assertEquals("PAGE1_PROFILE", profiles.get(0).getUuid());
            assertEquals("PAGE2_PROFILE", profiles.get(1).getUuid());
            assertFalse(profiles.hasNextPage());

            wireMock.verify(1, postRequestedFor(urlPathEqualTo("/fabric/v4/serviceProfiles/search"))
                    .withRequestBody(matchingJsonPath("$.pagination.offset", equalTo("100"))));
        }
    }
}
