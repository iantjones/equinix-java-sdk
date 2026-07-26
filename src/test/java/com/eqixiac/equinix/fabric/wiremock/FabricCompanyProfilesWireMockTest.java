package com.eqixiac.equinix.fabric.wiremock;

import com.eqixiac.equinix.Fabric;
import com.eqixiac.equinix.core.WireMockTestBase;
import com.eqixiac.equinix.core.exception.*;
import com.eqixiac.equinix.core.http.response.PaginatedFilteredList;
import com.eqixiac.equinix.fabric.model.CompanyProfile;
import com.eqixiac.equinix.fabric.model.CompanyServiceProfile;
import com.eqixiac.equinix.fabric.model.PrivateService;
import com.eqixiac.equinix.fabric.model.Tag;
import com.eqixiac.equinix.fabric.enums.NotificationType;
import com.eqixiac.equinix.fabric.model.implementation.Notification;
import com.eqixiac.equinix.fabric.model.implementation.filter.Filter;
import com.eqixiac.equinix.fabric.model.implementation.filter.FilterPropertyList;
import com.eqixiac.equinix.fabric.model.implementation.sort.Sort;
import com.eqixiac.equinix.fabric.model.implementation.sort.SortPropertyList;
import org.junit.jupiter.api.*;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static com.eqixiac.equinix.core.ResponseStubs.*;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * WireMock-based API tests for Fabric Company Profiles.
 * Closes mutation/action coverage: define().create(), deleteLogo(), and the
 * service-profile / tag / private-service attach + detach actions.
 */
class FabricCompanyProfilesWireMockTest extends WireMockTestBase {

    static Fabric fabric;

    static final String CP_ID = "9c0f8e2a-2b1d-4c3e-9a4f-1d2c3b4a5e6f";
    static final String SP_ID = "3a58dd05-f46d-4b1d-a154-2e85c396ea85";
    static final String TAG_ID = "18a127ad-9d0c-46e2-a66d-8ed85d1858b0";
    static final String PS_ID = "b7c1e2d3-4f5a-6b7c-8d9e-0a1b2c3d4e5f";
    static final String LOGO_ID = "c4d5e6f7-8a9b-0c1d-2e3f-4a5b6c7d8e9f";

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
    @DisplayName("define(type).create()")
    class Create {

        @Test
        @DisplayName("POSTs the company-profile creator body to /companyProfiles")
        void createPostsCreatorBody() {
            stubCreate(wireMock, "/fabric/v4/companyProfiles",
                    "/json/fabric/company_profile_response.json");

            CompanyProfile created = fabric.companyProfiles()
                    .define("COMPANY_PROFILE")
                    .name("Acme Networks")
                    .summary("Global interconnection provider")
                    .description("Acme Networks offers low-latency interconnection across major metros.")
                    .webUrl("https://acme.example.com")
                    .contactUrl("https://acme.example.com/contact")
                    .notifications(List.of(
                            new Notification(NotificationType.NOTIFICATION, List.of("example@example.com"))))
                    .create();

            assertNotNull(created);
            assertEquals(CP_ID, created.getUuid());
            assertEquals("Acme Networks", created.getName());

            wireMock.verify(postRequestedFor(urlPathEqualTo("/fabric/v4/companyProfiles"))
                    .withHeader("Content-Type", containing("application/json"))
                    .withRequestBody(equalToJson(
                            "{\"type\":\"COMPANY_PROFILE\","
                            + "\"name\":\"Acme Networks\","
                            + "\"summary\":\"Global interconnection provider\","
                            + "\"description\":\"Acme Networks offers low-latency interconnection across major metros.\","
                            + "\"webUrl\":\"https://acme.example.com\","
                            + "\"contactUrl\":\"https://acme.example.com/contact\","
                            + "\"notifications\":[{\"type\":\"NOTIFICATION\",\"emails\":[\"example@example.com\"]}]}",
                            true, true)));
        }
    }

    @Nested
    @DisplayName("deleteLogo(uuid)")
    class DeleteLogo {

        @Test
        @DisplayName("DELETEs /logos/{uuid} (overrideRootUri, not under companyProfiles)")
        void deleteLogoHitsLogosPath() {
            stubDeleteNoContent(wireMock, "/fabric/v4/logos/" + LOGO_ID);

            Boolean result = fabric.companyProfiles().deleteLogo(LOGO_ID);

            assertTrue(result);
            wireMock.verify(deleteRequestedFor(urlPathEqualTo("/fabric/v4/logos/" + LOGO_ID)));
        }
    }

    @Nested
    @DisplayName("Service-profile attach / detach")
    class ServiceProfileAttachment {

        @Test
        @DisplayName("attachServiceProfile PUTs /companyProfiles/{cpId}/serviceProfiles/{spId}")
        void attachServiceProfile() {
            wireMock.stubFor(put(urlPathEqualTo(
                    "/fabric/v4/companyProfiles/" + CP_ID + "/serviceProfiles/" + SP_ID))
                    .willReturn(noContent()));

            Boolean result = fabric.companyProfiles().attachServiceProfile(CP_ID, SP_ID);

            assertTrue(result);
            wireMock.verify(putRequestedFor(urlPathEqualTo(
                    "/fabric/v4/companyProfiles/" + CP_ID + "/serviceProfiles/" + SP_ID)));
        }

        @Test
        @DisplayName("detachServiceProfile DELETEs /companyProfiles/{cpId}/serviceProfiles/{spId}")
        void detachServiceProfile() {
            stubDeleteNoContent(wireMock,
                    "/fabric/v4/companyProfiles/" + CP_ID + "/serviceProfiles/" + SP_ID);

            Boolean result = fabric.companyProfiles().detachServiceProfile(CP_ID, SP_ID);

            assertTrue(result);
            wireMock.verify(deleteRequestedFor(urlPathEqualTo(
                    "/fabric/v4/companyProfiles/" + CP_ID + "/serviceProfiles/" + SP_ID)));
        }
    }

    @Nested
    @DisplayName("Tag attach / detach")
    class TagAttachment {

        @Test
        @DisplayName("attachTag PUTs /companyProfiles/{cpId}/tags/{tagId}")
        void attachTag() {
            wireMock.stubFor(put(urlPathEqualTo(
                    "/fabric/v4/companyProfiles/" + CP_ID + "/tags/" + TAG_ID))
                    .willReturn(noContent()));

            Boolean result = fabric.companyProfiles().attachTag(CP_ID, TAG_ID);

            assertTrue(result);
            wireMock.verify(putRequestedFor(urlPathEqualTo(
                    "/fabric/v4/companyProfiles/" + CP_ID + "/tags/" + TAG_ID)));
        }

        @Test
        @DisplayName("detachTag DELETEs /companyProfiles/{cpId}/tags/{tagId}")
        void detachTag() {
            stubDeleteNoContent(wireMock,
                    "/fabric/v4/companyProfiles/" + CP_ID + "/tags/" + TAG_ID);

            Boolean result = fabric.companyProfiles().detachTag(CP_ID, TAG_ID);

            assertTrue(result);
            wireMock.verify(deleteRequestedFor(urlPathEqualTo(
                    "/fabric/v4/companyProfiles/" + CP_ID + "/tags/" + TAG_ID)));
        }
    }

    @Nested
    @DisplayName("Private-service attach / detach")
    class PrivateServiceAttachment {

        @Test
        @DisplayName("attachPrivateService PUTs /companyProfiles/{cpId}/privateServices/{psId}")
        void attachPrivateService() {
            wireMock.stubFor(put(urlPathEqualTo(
                    "/fabric/v4/companyProfiles/" + CP_ID + "/privateServices/" + PS_ID))
                    .willReturn(noContent()));

            Boolean result = fabric.companyProfiles().attachPrivateService(CP_ID, PS_ID);

            assertTrue(result);
            wireMock.verify(putRequestedFor(urlPathEqualTo(
                    "/fabric/v4/companyProfiles/" + CP_ID + "/privateServices/" + PS_ID)));
        }

        @Test
        @DisplayName("detachPrivateService DELETEs /companyProfiles/{cpId}/privateServices/{psId}")
        void detachPrivateService() {
            stubDeleteNoContent(wireMock,
                    "/fabric/v4/companyProfiles/" + CP_ID + "/privateServices/" + PS_ID);

            Boolean result = fabric.companyProfiles().detachPrivateService(CP_ID, PS_ID);

            assertTrue(result);
            wireMock.verify(deleteRequestedFor(urlPathEqualTo(
                    "/fabric/v4/companyProfiles/" + CP_ID + "/privateServices/" + PS_ID)));
        }
    }

    @Nested
    @DisplayName("search()")
    class Search {

        private static final String SEARCH_URL = "/fabric/v4/companyProfiles/search";

        @Test
        @DisplayName("no-arg search POSTs the default body to /companyProfiles/search and returns a filtered list")
        void searchNoArg() {
            stubPaginatedPost(wireMock, SEARCH_URL, "/json/fabric/paginated_company_profiles.json");

            PaginatedFilteredList<CompanyProfile> profiles = fabric.companyProfiles().search();

            assertNotNull(profiles);
            assertEquals(2, profiles.size());
            assertEquals(CP_ID, profiles.get(0).getUuid());

            // Default no-arg search sends an (empty) filter and pagination, no sort.
            wireMock.verify(postRequestedFor(urlPathEqualTo(SEARCH_URL))
                    .withHeader("Content-Type", containing("application/json"))
                    .withRequestBody(matchingJsonPath("$.pagination")));
        }

        @Test
        @DisplayName("search(filter) carries the filter predicate in the POST body")
        void searchWithFilter() {
            stubPaginatedPost(wireMock, SEARCH_URL, "/json/fabric/paginated_company_profiles.json");

            FilterPropertyList filter = Filter.filter().and()
                    .equals("/name", "Acme Networks")
                    .equals("/state", "ACTIVE");

            PaginatedFilteredList<CompanyProfile> profiles = fabric.companyProfiles().search(filter);

            assertNotNull(profiles);
            assertEquals(2, profiles.size());

            wireMock.verify(postRequestedFor(urlPathEqualTo(SEARCH_URL))
                    .withRequestBody(matchingJsonPath("$.filter.and[0].property", equalTo("/name")))
                    .withRequestBody(matchingJsonPath("$.filter.and[0].values[0]", equalTo("Acme Networks")))
                    .withRequestBody(matchingJsonPath("$.filter.and[1].property", equalTo("/state")))
                    .withRequestBody(matchingJsonPath("$.filter.and[1].values[0]", equalTo("ACTIVE"))));
        }

        @Test
        @DisplayName("search(sort) carries the sort directive in the POST body")
        void searchWithSort() {
            stubPaginatedPost(wireMock, SEARCH_URL, "/json/fabric/paginated_company_profiles.json");

            SortPropertyList sort = Sort.sort().desc("/changeLog/createdDateTime");

            PaginatedFilteredList<CompanyProfile> profiles = fabric.companyProfiles().search(sort);

            assertNotNull(profiles);
            wireMock.verify(postRequestedFor(urlPathEqualTo(SEARCH_URL))
                    .withRequestBody(matchingJsonPath("$.sort.property", equalTo("/changeLog/createdDateTime")))
                    .withRequestBody(matchingJsonPath("$.sort.direction", equalTo("DESC"))));
        }

        @Test
        @DisplayName("search(filter, sort) carries both filter and sort in the POST body")
        void searchWithFilterAndSort() {
            stubPaginatedPost(wireMock, SEARCH_URL, "/json/fabric/paginated_company_profiles.json");

            FilterPropertyList filter = Filter.filter().and()
                    .equals("/type", "COMPANY_PROFILE");
            SortPropertyList sort = Sort.sort().asc("/name");

            PaginatedFilteredList<CompanyProfile> profiles = fabric.companyProfiles().search(filter, sort);

            assertNotNull(profiles);
            wireMock.verify(postRequestedFor(urlPathEqualTo(SEARCH_URL))
                    .withRequestBody(matchingJsonPath("$.filter.and[0].property", equalTo("/type")))
                    .withRequestBody(matchingJsonPath("$.filter.and[0].values[0]", equalTo("COMPANY_PROFILE")))
                    .withRequestBody(matchingJsonPath("$.sort.property", equalTo("/name")))
                    .withRequestBody(matchingJsonPath("$.sort.direction", equalTo("ASC"))));
        }
    }

    @Nested
    @DisplayName("getByUuid()")
    class GetByUuid {

        @Test
        @DisplayName("GETs /companyProfiles/{uuid} and returns the profile")
        void returnsProfile() {
            stubSingleton(wireMock, "/fabric/v4/companyProfiles/.*",
                    "/json/fabric/company_profile_response.json");

            CompanyProfile profile = fabric.companyProfiles().getByUuid(CP_ID);

            assertNotNull(profile);
            assertEquals(CP_ID, profile.getUuid());
            assertEquals("Acme Networks", profile.getName());

            assertNotNull(profile.getTags());
            assertEquals(1, profile.getTags().size());
            assertEquals("260af68b-42f0-4f2e-9c5c-2fbd44b4b387", profile.getTags().get(0).getUuid());
            assertEquals("equinix.fabric.spotlight.category.featured", profile.getTags().get(0).getName());

            assertNotNull(profile.getServiceProfiles());
            assertEquals(1, profile.getServiceProfiles().size());
            assertEquals("423af68b-42f0-4f2e-9c5c-2fbd44b4b387", profile.getServiceProfiles().get(0).getUuid());

            assertNotNull(profile.getChangeLog());
            assertEquals("user1234", profile.getChangeLog().getCreatedBy());
            assertEquals("user5678", profile.getChangeLog().getUpdatedBy());

            wireMock.verify(getRequestedFor(urlPathEqualTo("/fabric/v4/companyProfiles/" + CP_ID)));
        }
    }

    @Nested
    @DisplayName("getLogo()")
    class GetLogo {

        @Test
        @DisplayName("GETs /logos/{uuid} (overrideRootUri, not under companyProfiles) and returns the raw bytes")
        void returnsLogoBytes() {
            byte[] payload = "PNGDATA".getBytes(StandardCharsets.UTF_8);
            wireMock.stubFor(get(urlPathEqualTo("/fabric/v4/logos/" + LOGO_ID))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "image/png")
                            .withBody(payload)));

            byte[] logo = fabric.companyProfiles().getLogo(LOGO_ID);

            assertNotNull(logo);
            assertArrayEquals(payload, logo);

            wireMock.verify(getRequestedFor(urlPathEqualTo("/fabric/v4/logos/" + LOGO_ID)));
        }
    }

    @Nested
    @DisplayName("getServiceProfiles() / getTags() / getPrivateServices() [list]")
    class Lists {

        @Test
        @DisplayName("getServiceProfiles(cpId) GETs /companyProfiles/{cpId}/serviceProfiles")
        void listServiceProfiles() {
            stubPaginatedGet(wireMock, "/fabric/v4/companyProfiles/.*/serviceProfiles",
                    "/json/fabric/company_profile_service_profiles_response.json");

            List<CompanyServiceProfile> serviceProfiles = fabric.companyProfiles().getServiceProfiles(CP_ID);

            assertNotNull(serviceProfiles);
            assertEquals(2, serviceProfiles.size());
            assertEquals("423af68b-42f0-4f2e-9c5c-2fbd44b4b387", serviceProfiles.get(0).getUuid());

            wireMock.verify(getRequestedFor(urlPathEqualTo(
                    "/fabric/v4/companyProfiles/" + CP_ID + "/serviceProfiles")));
        }

        @Test
        @DisplayName("getTags(cpId) GETs /companyProfiles/{cpId}/tags")
        void listTags() {
            stubPaginatedGet(wireMock, "/fabric/v4/companyProfiles/.*/tags",
                    "/json/fabric/company_profile_tags_response.json");

            List<Tag> tags = fabric.companyProfiles().getTags(CP_ID);

            assertNotNull(tags);
            assertEquals(1, tags.size());
            assertEquals(TAG_ID, tags.get(0).getUuid());
            assertEquals("environment", tags.get(0).getName());

            wireMock.verify(getRequestedFor(urlPathEqualTo(
                    "/fabric/v4/companyProfiles/" + CP_ID + "/tags")));
        }

        @Test
        @DisplayName("getPrivateServices(cpId) GETs /companyProfiles/{cpId}/privateServices")
        void listPrivateServices() {
            stubPaginatedGet(wireMock, "/fabric/v4/companyProfiles/.*/privateServices",
                    "/json/fabric/company_profile_private_services_response.json");

            List<PrivateService> privateServices = fabric.companyProfiles().getPrivateServices(CP_ID);

            assertNotNull(privateServices);
            assertEquals(1, privateServices.size());
            assertEquals("460af68b-42f0-4f2e-9c5c-2fbd44b4b387", privateServices.get(0).getUuid());

            wireMock.verify(getRequestedFor(urlPathEqualTo(
                    "/fabric/v4/companyProfiles/" + CP_ID + "/privateServices")));
        }
    }

    @Nested
    @DisplayName("Wrapper refresh()")
    class WrapperRefresh {

        @Test
        @DisplayName("re-GETs /companyProfiles/{uuid} and swaps the wrapper's state in place")
        void refreshReloadsInPlace() {
            String url = "/fabric/v4/companyProfiles/" + CP_ID;
            wireMock.stubFor(get(urlPathEqualTo(url))
                    .inScenario("cp-refresh")
                    .whenScenarioStateIs(com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED)
                    .willReturn(okJson(loadFixture("/json/fabric/company_profile_response.json")))
                    .willSetStateTo("renamed"));
            wireMock.stubFor(get(urlPathEqualTo(url))
                    .inScenario("cp-refresh")
                    .whenScenarioStateIs("renamed")
                    .willReturn(okJson(loadFixture("/json/fabric/company_profile_response.json")
                            .replace("Acme Networks", "Acme Networks Renamed"))));

            CompanyProfile profile = fabric.companyProfiles().getByUuid(CP_ID);
            assertEquals("Acme Networks", profile.getName());

            profile.refresh();

            assertEquals("Acme Networks Renamed", profile.getName(),
                    "refresh() must swap the wrapper's backing state in place");
            wireMock.verify(2, getRequestedFor(urlPathEqualTo(url)));
        }
    }

    @Nested
    @DisplayName("Wrapper delete()")
    class WrapperDelete {

        @Test
        @DisplayName("DELETEs /companyProfiles/{uuid} (the profile itself, not just its logo) and returns true")
        void deletesCompanyProfile() {
            stubSingleton(wireMock, "/fabric/v4/companyProfiles/" + CP_ID,
                    "/json/fabric/company_profile_response.json");
            // deleteOne() reads the deleted resource from the response body, so the stub returns one.
            wireMock.stubFor(delete(urlPathEqualTo("/fabric/v4/companyProfiles/" + CP_ID))
                    .willReturn(okJson(loadFixture("/json/fabric/company_profile_response.json"))));

            CompanyProfile profile = fabric.companyProfiles().getByUuid(CP_ID);
            Boolean deleted = profile.delete();

            assertEquals(Boolean.TRUE, deleted);
            wireMock.verify(deleteRequestedFor(urlPathEqualTo("/fabric/v4/companyProfiles/" + CP_ID)));
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
            wireMock.stubFor(post(urlPathEqualTo("/fabric/v4/companyProfiles/search"))
                    .withRequestBody(matchingJsonPath("$.pagination.offset", equalTo("0")))
                    .willReturn(okJson(PAGE_1)));
            wireMock.stubFor(post(urlPathEqualTo("/fabric/v4/companyProfiles/search"))
                    .withRequestBody(matchingJsonPath("$.pagination.offset", equalTo("100")))
                    .willReturn(okJson(PAGE_2)));

            PaginatedFilteredList<CompanyProfile> profiles = fabric.companyProfiles().search();
            assertEquals(1, profiles.size());
            assertTrue(profiles.hasNextPage());

            profiles.loadAll();

            assertEquals(2, profiles.size());
            assertEquals("PAGE1_PROFILE", profiles.get(0).getUuid());
            assertEquals("PAGE2_PROFILE", profiles.get(1).getUuid());
            assertFalse(profiles.hasNextPage());

            wireMock.verify(1, postRequestedFor(urlPathEqualTo("/fabric/v4/companyProfiles/search"))
                    .withRequestBody(matchingJsonPath("$.pagination.offset", equalTo("100"))));
        }
    }

    @Nested
    @DisplayName("Error handling")
    class Errors {

        @Test
        @DisplayName("404 throws EquinixNotFoundException")
        void notFound() {
            stubErrorInline(wireMock, "/fabric/v4/companyProfiles/.*",
                    404, "[{\"errorCode\":\"ERR-404\",\"errorMessage\":\"Company profile not found\"}]");

            assertThrows(EquinixNotFoundException.class,
                    () -> fabric.companyProfiles().getByUuid("invalid-uuid"));
        }

        @Test
        @DisplayName("401 throws EquinixAuthenticationException")
        void unauthorized() {
            stubErrorInline(wireMock, "/fabric/v4/companyProfiles/.*",
                    401, "[{\"errorCode\":\"ERR-401\",\"errorMessage\":\"Unauthorized\"}]");

            assertThrows(EquinixAuthenticationException.class,
                    () -> fabric.companyProfiles().getByUuid("test-uuid"));
        }

        @Test
        @DisplayName("500 throws EquinixServerException")
        void serverError() {
            stubErrorInline(wireMock, "/fabric/v4/companyProfiles/.*",
                    500, "[{\"errorCode\":\"ERR-500\",\"errorMessage\":\"Internal server error\"}]");

            assertThrows(EquinixServerException.class,
                    () -> fabric.companyProfiles().getByUuid("test-uuid"));
        }
    }
}
