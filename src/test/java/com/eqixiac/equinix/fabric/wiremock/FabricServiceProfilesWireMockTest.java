package com.eqixiac.equinix.fabric.wiremock;

import com.eqixiac.equinix.Fabric;
import com.eqixiac.equinix.core.WireMockTestBase;
import com.eqixiac.equinix.core.exception.*;
import com.eqixiac.equinix.core.http.response.PaginatedFilteredList;
import com.eqixiac.equinix.core.http.response.PaginatedList;
import com.eqixiac.equinix.fabric.enums.EnvironmentActionState;
import com.eqixiac.equinix.fabric.enums.EnvironmentActionType;
import com.eqixiac.equinix.fabric.enums.ProviderEnvironmentType;
import com.eqixiac.equinix.fabric.enums.ServiceProfileType;
import com.eqixiac.equinix.fabric.enums.ServiceProfileVisibility;
import com.eqixiac.equinix.fabric.model.EnvironmentActionResponse;
import com.eqixiac.equinix.fabric.model.ServiceProfile;
import com.eqixiac.equinix.fabric.model.ServiceProfileAction;
import com.eqixiac.equinix.fabric.model.implementation.ActivationKeyDetails;
import com.eqixiac.equinix.fabric.model.implementation.ProviderEnvironment;
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

    /**
     * Beta catalog operation {@code getServiceProfileEnvironmentsByUuid}. The fixture is the
     * catalog's {@code ServiceProfileEnvironmentsResponse} example (fetched 2026-09-21),
     * transcribed from YAML to JSON without changes.
     */
    @Nested
    @DisplayName("getEnvironments() [Beta]")
    class GetEnvironments {

        private static final String SP_ID = "23ad37d0-6b1c-4d1f-a4eb-9f3d68fbe4fd";
        private static final String URL = "/fabric/v4/serviceProfiles/" + SP_ID + "/environments";

        @Test
        @DisplayName("GETs {uuid}/environments and maps the catalog example")
        void returnsCatalogExample() {
            wireMock.stubFor(get(urlPathEqualTo(URL))
                    .willReturn(okJson(loadFixture("/json/fabric/service_profile_environments_response.json"))));

            List<ProviderEnvironment> environments = fabric.serviceProfiles().getEnvironments(SP_ID);

            assertEquals(2, environments.size());
            ProviderEnvironment west = environments.get(0);
            assertEquals("6ad498b5-d929-44ac-a199-ce9f0d31d9ac", west.getUuid());
            assertEquals("https://api.equinix.com/fabric/v4/serviceProfiles/" + SP_ID
                    + "/environments/6ad498b5-d929-44ac-a199-ce9f0d31d9ac", west.getHref());
            assertEquals(ProviderEnvironmentType.IC_ENV, west.getType());
            // The catalog example separates the words with U+2013 (en dash), not a hyphen.
            assertEquals("US West 1 \u2013 Production", west.getName());
            assertEquals("Primary production environment", west.getDescription());
            assertEquals("us-west-1", west.getRegion());
            assertEquals(List.of(1000, 10000, 100000), west.getSupportedBandwidths());
            assertEquals(2, west.getMetros().size());
            assertEquals("CH", west.getMetros().get(0).metroId().code());
            assertEquals("Chicago", west.getMetros().get(0).getDisplayName());
            assertEquals("DC", west.getMetros().get(1).metroId().code());

            wireMock.verify(1, getRequestedFor(urlPathEqualTo(URL)));
        }

        @Test
        @DisplayName("reads the example's second metros shape ({href, metroCode, type}) through the code alias")
        void readsMetroReferenceShape() {
            wireMock.stubFor(get(urlPathEqualTo(URL))
                    .willReturn(okJson(loadFixture("/json/fabric/service_profile_environments_response.json"))));

            ProviderEnvironment east = fabric.serviceProfiles().getEnvironments(SP_ID).get(1);

            assertEquals("us-east-1", east.getRegion());
            assertEquals(1, east.getMetros().size());
            ServiceMetro metro = east.getMetros().get(0);
            assertEquals("DC", metro.metroId().code(),
                    "metroCode must populate the metro id; the schema key is 'code' but the example sends 'metroCode'");
            assertEquals("https://api.equinix.com/fabric/v4/metros/DC", metro.getHref());
            assertNull(metro.getDisplayName());
        }

        @Test
        @DisplayName("properties the example omits (supportedFeatures, changeLog) read as null")
        void omittedPropertiesAreNull() {
            wireMock.stubFor(get(urlPathEqualTo(URL))
                    .willReturn(okJson(loadFixture("/json/fabric/service_profile_environments_response.json"))));

            ProviderEnvironment west = fabric.serviceProfiles().getEnvironments(SP_ID).get(0);

            assertNull(west.getSupportedFeatures());
            assertNull(west.getChangeLog());
        }

        @Test
        @DisplayName("follows server pagination: one GET per page, offset advanced, all pages returned")
        void loadsEveryPage() {
            wireMock.stubFor(get(urlPathEqualTo(URL))
                    .willReturn(okJson("""
                            {"data":[{"uuid":"ENV_PAGE_1","type":"IC_ENV","region":"us-west-1"}],
                             "pagination":{"offset":0,"limit":1,"total":2}}""")));
            wireMock.stubFor(get(urlPathEqualTo(URL))
                    .withQueryParam("offset", equalTo("1"))
                    .willReturn(okJson("""
                            {"data":[{"uuid":"ENV_PAGE_2","type":"IC_ENV","region":"us-east-1"}],
                             "pagination":{"offset":1,"limit":1,"total":2}}""")));

            List<ProviderEnvironment> environments = fabric.serviceProfiles().getEnvironments(SP_ID);

            assertEquals(2, environments.size());
            assertEquals("ENV_PAGE_1", environments.get(0).getUuid());
            assertEquals("ENV_PAGE_2", environments.get(1).getUuid());
            assertEquals("us-east-1", environments.get(1).getRegion());
            wireMock.verify(2, getRequestedFor(urlPathEqualTo(URL)));
            wireMock.verify(1, getRequestedFor(urlPathEqualTo(URL)).withQueryParam("offset", equalTo("1")));
        }

        @Test
        @DisplayName("doc contract: a failure on a later page (catalog example sp-500) propagates; no partial list is returned")
        void laterPageFailurePropagates() {
            wireMock.stubFor(get(urlPathEqualTo(URL))
                    .willReturn(okJson("""
                            {"data":[{"uuid":"ENV_PAGE_1","type":"IC_ENV"}],
                             "pagination":{"offset":0,"limit":1,"total":2}}""")));
            wireMock.stubFor(get(urlPathEqualTo(URL))
                    .withQueryParam("offset", equalTo("1"))
                    .willReturn(aResponse().withStatus(500)
                            .withHeader("Content-Type", "application/json")
                            .withBody("[{\"errorCode\":\"EQ-3001206\",\"errorMessage\":\"Internal Server Error\"}]")));

            assertThrows(EquinixServerException.class, () -> fabric.serviceProfiles().getEnvironments(SP_ID));
        }

        @Test
        @DisplayName("doc contract: the returned list is an unmodifiable snapshot")
        void listIsUnmodifiable() {
            wireMock.stubFor(get(urlPathEqualTo(URL))
                    .willReturn(okJson(loadFixture("/json/fabric/service_profile_environments_response.json"))));

            List<ProviderEnvironment> environments = fabric.serviceProfiles().getEnvironments(SP_ID);

            assertThrows(UnsupportedOperationException.class, () -> environments.remove(0));
        }

        @Test
        @DisplayName("an empty data array returns an empty list")
        void emptyPage() {
            wireMock.stubFor(get(urlPathEqualTo(URL))
                    .willReturn(okJson("{\"data\":[],\"pagination\":{\"offset\":0,\"limit\":20,\"total\":0}}")));

            assertTrue(fabric.serviceProfiles().getEnvironments(SP_ID).isEmpty());
        }

        @Test
        @DisplayName("403 (catalog example sp-403-read) throws EquinixAuthorizationException")
        void forbidden() {
            stubErrorInline(wireMock, URL, 403,
                    "[{\"errorCode\":\"EQ-3001033\",\"errorMessage\":"
                            + "\"You are not authorized to execute the requested action on the resource\"}]");

            assertThrows(EquinixAuthorizationException.class, () -> fabric.serviceProfiles().getEnvironments(SP_ID));
        }

        @Test
        @DisplayName("a null or blank service profile uuid is rejected before any request")
        void rejectsBlankUuid() {
            assertThrows(IllegalArgumentException.class, () -> fabric.serviceProfiles().getEnvironments(null));
            assertThrows(IllegalArgumentException.class, () -> fabric.serviceProfiles().getEnvironments(" "));
            wireMock.verify(0, getRequestedFor(urlPathMatching("/fabric/v4/serviceProfiles/.*")));
        }
    }

    /**
     * Beta catalog operation {@code serviceProfileEnvironmentAction}. The request body is the
     * catalog's {@code ValidateActivationKey} example. The catalog publishes NO response example
     * (fetched 2026-09-21): the response fixture holds only properties for which the
     * {@code EnvironmentActionResponse} / {@code ActivationKeyDetails} schemas give a
     * property-level example ({@code type}, {@code uuid}, {@code state}, {@code keyDetails.value}).
     */
    @Nested
    @DisplayName("validateActivationKey() [Beta]")
    class ValidateActivationKey {

        private static final String SP_ID = "23ad37d0-6b1c-4d1f-a4eb-9f3d68fbe4fd";
        private static final String ENV_ID = "6ad498b5-d929-44ac-a199-ce9f0d31d9ac";
        private static final String URL = "/fabric/v4/serviceProfiles/" + SP_ID + "/environments/" + ENV_ID + "/actions";
        private static final String RESPONSE = "/json/fabric/service_profile_environment_action_response.json";

        private void stubCreated(String body) {
            // The catalog documents 201 for this operation.
            wireMock.stubFor(post(urlPathEqualTo(URL))
                    .willReturn(aResponse().withStatus(201)
                            .withHeader("Content-Type", "application/json")
                            .withBody(body)));
        }

        @Test
        @DisplayName("POSTs exactly the catalog's ValidateActivationKey example body")
        void postsCatalogExampleBody() {
            stubCreated(loadFixture(RESPONSE));

            fabric.serviceProfiles().validateActivationKey(SP_ID, ENV_ID, "key_here");

            wireMock.verify(1, postRequestedFor(urlPathEqualTo(URL))
                    .withHeader("Content-Type", containing("application/json"))
                    .withRequestBody(equalToJson(
                            "{\"type\":\"VALIDATE_ACTIVATION_KEY\",\"keyDetails\":{\"value\":\"key_here\"}}",
                            false, false)));
        }

        @Test
        @DisplayName("maps the response to a typed EnvironmentActionResponse; ACTIVE means the key is already used")
        void mapsResponse() {
            stubCreated(loadFixture(RESPONSE));

            EnvironmentActionResponse response =
                    fabric.serviceProfiles().validateActivationKey(SP_ID, ENV_ID, "key_here");

            assertEquals(EnvironmentActionType.VALIDATE_ACTIVATION_KEY, response.getType());
            assertEquals("3a58dd05-f46d-4b1d-a154-2e85c396ea62", response.getUuid());
            assertEquals(EnvironmentActionState.ACTIVE, response.getState());
            assertFalse(response.isKeyUnused(), "ACTIVE is documented as 'already used'");
            assertEquals("key_here", response.getKeyDetails().getValue());
            assertNull(response.getHref());
            assertNull(response.getChangeLog());
        }

        @Test
        @DisplayName("INACTIVE means the key is not used")
        void inactiveMeansUnused() {
            stubCreated(loadFixture(RESPONSE).replace("\"ACTIVE\"", "\"INACTIVE\""));

            EnvironmentActionResponse response =
                    fabric.serviceProfiles().validateActivationKey(SP_ID, ENV_ID, "key_here");

            assertEquals(EnvironmentActionState.INACTIVE, response.getState());
            assertTrue(response.isKeyUnused());
        }

        @Test
        @DisplayName("a state or type this SDK does not define reads as UNKNOWN and is not treated as unused")
        void unknownEnumValues() {
            stubCreated(loadFixture(RESPONSE)
                    .replace("\"ACTIVE\"", "\"EXPIRED\"")
                    .replace("\"VALIDATE_ACTIVATION_KEY\"", "\"ROTATE_ACTIVATION_KEY\""));

            EnvironmentActionResponse response =
                    fabric.serviceProfiles().validateActivationKey(SP_ID, ENV_ID, "key_here");

            assertEquals(EnvironmentActionState.UNKNOWN, response.getState());
            assertEquals(EnvironmentActionType.UNKNOWN, response.getType());
            assertFalse(response.isKeyUnused());
        }

        @Test
        @DisplayName("maps every ActivationKeyDetails and response property when the service sends them (synthetic body)")
        void mapsAllSchemaProperties() {
            // Synthetic: values are placeholders chosen for this test, not catalog examples.
            stubCreated("""
                    {
                      "href": "https://api.equinix.com/synthetic/action-href",
                      "type": "VALIDATE_ACTIVATION_KEY",
                      "uuid": "3a58dd05-f46d-4b1d-a154-2e85c396ea62",
                      "state": "INACTIVE",
                      "keyDetails": {
                        "value": "key_here",
                        "providerId": "synthetic-provider-id",
                        "accountId": "synthetic-account-id",
                        "bandwidth": 1000,
                        "region": "us-west-1"
                      },
                      "changeLog": {"createdBy": "adminuser", "createdDateTime": "2026-03-04T10:30:00Z"}
                    }""");

            EnvironmentActionResponse response =
                    fabric.serviceProfiles().validateActivationKey(SP_ID, ENV_ID, "key_here");

            assertEquals("https://api.equinix.com/synthetic/action-href", response.getHref());
            ActivationKeyDetails details = response.getKeyDetails();
            assertEquals("synthetic-provider-id", details.getProviderId());
            assertEquals("synthetic-account-id", details.getAccountId());
            assertEquals(1000, details.getBandwidth());
            assertEquals("us-west-1", details.getRegion());
            assertEquals("adminuser", response.getChangeLog().getCreatedBy());
            assertNotNull(response.getChangeLog().getCreatedDateTime());
        }

        @Test
        @DisplayName("the ActivationKeyDetails overload sends the set properties and omits null ones")
        void keyDetailsOverload() {
            stubCreated(loadFixture(RESPONSE));

            fabric.serviceProfiles().validateActivationKey(SP_ID, ENV_ID,
                    ActivationKeyDetails.builder().value("key_here").bandwidth(1000).region("us-west-1").build());

            wireMock.verify(postRequestedFor(urlPathEqualTo(URL))
                    .withRequestBody(equalToJson("""
                            {"type":"VALIDATE_ACTIVATION_KEY",
                             "keyDetails":{"value":"key_here","bandwidth":1000,"region":"us-west-1"}}""",
                            false, false)));
        }

        @Test
        @DisplayName("400 (first entry of catalog example sp-400) throws EquinixServiceException carrying the status code")
        void badRequest() {
            stubErrorInline(wireMock, URL, 400,
                    "[{\"errorCode\":\"EQ-3001015\",\"errorMessage\":\"Access point is not accessible\"}]");

            EquinixServiceException e = assertThrows(EquinixServiceException.class,
                    () -> fabric.serviceProfiles().validateActivationKey(SP_ID, ENV_ID, "key_here"));
            assertEquals(400, e.getStatusCode());
        }

        @Test
        @DisplayName("null or blank arguments are rejected before any request")
        void rejectsBlankArguments() {
            assertThrows(IllegalArgumentException.class,
                    () -> fabric.serviceProfiles().validateActivationKey(null, ENV_ID, "key_here"));
            assertThrows(IllegalArgumentException.class,
                    () -> fabric.serviceProfiles().validateActivationKey(SP_ID, " ", "key_here"));
            assertThrows(IllegalArgumentException.class,
                    () -> fabric.serviceProfiles().validateActivationKey(SP_ID, ENV_ID, (String) null));
            assertThrows(IllegalArgumentException.class,
                    () -> fabric.serviceProfiles().validateActivationKey(SP_ID, ENV_ID, ""));
            assertThrows(NullPointerException.class,
                    () -> fabric.serviceProfiles().validateActivationKey(SP_ID, ENV_ID, (ActivationKeyDetails) null));
            wireMock.verify(0, postRequestedFor(urlPathMatching("/fabric/v4/serviceProfiles/.*")));
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
