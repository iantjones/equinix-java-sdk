package api.equinix.javasdk.projects.wiremock;

import api.equinix.javasdk.Projects;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.core.exception.*;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.projects.model.Project;
import org.junit.jupiter.api.*;

import static api.equinix.javasdk.core.ResponseStubs.*;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * WireMock-based API tests for the read-only Projects domain
 * ({@code GET /resourceManager/v2/projects}).
 */
class ProjectsWireMockTest extends WireMockTestBase {

    static Projects projects;

    @BeforeAll
    static void setUp() {
        projects = new Projects(testCredentials());
        redirectToWireMock(projects);
        projects.authenticate();
    }

    @AfterAll
    static void tearDown() throws Exception {
        if (projects != null) projects.close();
    }

    @BeforeEach
    void resetBeforeEach() {
        resetStubs();
    }

    @Nested
    @DisplayName("list()")
    class ListProjects {

        @Test
        @DisplayName("returns a paginated list of projects")
        void returnsProjects() {
            stubPaginatedGet(wireMock, "/resourceManager/v2/projects",
                    "/json/projects/project_response.json");

            PaginatedList<Project> projectList = projects.projects().list();

            assertNotNull(projectList);
            assertEquals(2, projectList.size());
            Project first = projectList.get(0);
            assertEquals("1234", first.getProjectId());
            assertEquals("Default project", first.getProjectName());
            assertTrue(first.getInboxResource());
            assertEquals("5678", first.getParentOrganizationId());
            assertEquals("Network Edge", first.getLabels().get("application"));
            assertEquals(1, first.getPermissions().size());
            assertEquals("L2_CONNECTION", first.getPermissions().get(0).getResourceType());
            wireMock.verify(getRequestedFor(urlPathEqualTo("/resourceManager/v2/projects")));
        }

        @Test
        @DisplayName("passes includePermissions and includeInbox query params")
        void passesQueryParams() {
            stubPaginatedGet(wireMock, "/resourceManager/v2/projects",
                    "/json/projects/project_response.json");

            projects.projects().list(true, false);

            wireMock.verify(getRequestedFor(urlPathEqualTo("/resourceManager/v2/projects"))
                    .withQueryParam("includePermissions", equalTo("true"))
                    .withQueryParam("includeInbox", equalTo("false")));
        }
    }

    @Nested
    @DisplayName("Multi-page list paging")
    class Paging {

        // resourceManager/v2 paginates via offset/limit query parameters: the first request always
        // carries offset=0&limit=100 (the SDK's PAGE_LIMIT default), and page 2 must advance the
        // offset from the SERVER-reported pagination (offset + limit).
        private static final String PAGE_1 = """
                {
                  "pagination": { "offset": 0, "limit": 100, "total": 150 },
                  "data": [ { "projectId": "1111", "projectName": "PAGE1_PROJECT" } ]
                }
                """;

        private static final String PAGE_2 = """
                {
                  "pagination": { "offset": 100, "limit": 100, "total": 150 },
                  "data": [ { "projectId": "2222", "projectName": "PAGE2_PROJECT" } ]
                }
                """;

        @Test
        @DisplayName("loadAll() fetches page 2 by advancing the offset query param")
        void loadAllFetchesSecondPage() {
            wireMock.stubFor(get(urlPathEqualTo("/resourceManager/v2/projects"))
                    .withQueryParam("offset", equalTo("0"))
                    .willReturn(okJson(PAGE_1)));
            wireMock.stubFor(get(urlPathEqualTo("/resourceManager/v2/projects"))
                    .withQueryParam("offset", equalTo("100"))
                    .willReturn(okJson(PAGE_2)));

            PaginatedList<Project> projectList = projects.projects().list();
            assertEquals(1, projectList.size());
            assertTrue(projectList.hasNextPage());

            projectList.loadAll();

            assertEquals(2, projectList.size());
            assertEquals("PAGE1_PROJECT", projectList.get(0).getProjectName());
            assertEquals("PAGE2_PROJECT", projectList.get(1).getProjectName());
            assertFalse(projectList.hasNextPage());

            // Page 2 request: offset advanced from the server-reported pagination, limit carried.
            wireMock.verify(1, getRequestedFor(urlPathEqualTo("/resourceManager/v2/projects"))
                    .withQueryParam("offset", equalTo("100"))
                    .withQueryParam("limit", equalTo("100")));
        }
    }

    @Nested
    @DisplayName("Error handling")
    class Errors {

        @Test
        @DisplayName("400 throws EquinixServiceException")
        void badRequest() {
            stubErrorInline(wireMock, "/resourceManager/v2/projects.*",
                    400, "[{\"errorCode\":\"ERR-400\",\"errorMessage\":\"Invalid request\"}]");

            assertThrows(EquinixServiceException.class,
                    () -> projects.projects().list());
        }

        @Test
        @DisplayName("401 throws EquinixAuthenticationException")
        void unauthorized() {
            stubErrorInline(wireMock, "/resourceManager/v2/projects.*",
                    401, "[{\"errorCode\":\"ERR-401\",\"errorMessage\":\"Authentication failed\"}]");

            assertThrows(EquinixAuthenticationException.class,
                    () -> projects.projects().list());
        }

        @Test
        @DisplayName("403 throws EquinixAuthorizationException")
        void forbidden() {
            stubErrorInline(wireMock, "/resourceManager/v2/projects.*",
                    403, "[{\"errorCode\":\"ERR-403\",\"errorMessage\":\"Access denied\"}]");

            assertThrows(EquinixAuthorizationException.class,
                    () -> projects.projects().list());
        }

        @Test
        @DisplayName("404 throws EquinixNotFoundException")
        void notFound() {
            stubErrorInline(wireMock, "/resourceManager/v2/projects.*",
                    404, "[{\"errorCode\":\"ERR-404\",\"errorMessage\":\"Not found\"}]");

            assertThrows(EquinixNotFoundException.class,
                    () -> projects.projects().list());
        }

        @Test
        @DisplayName("429 throws EquinixRateLimitException")
        void rateLimited() {
            stubErrorInline(wireMock, "/resourceManager/v2/projects.*",
                    429, "[{\"errorCode\":\"ERR-429\",\"errorMessage\":\"Too many requests\"}]");

            assertThrows(EquinixRateLimitException.class,
                    () -> projects.projects().list());
        }

        @Test
        @DisplayName("500 throws EquinixServerException")
        void serverError() {
            stubErrorInline(wireMock, "/resourceManager/v2/projects.*",
                    500, "[{\"errorCode\":\"ERR-500\",\"errorMessage\":\"Internal server error\"}]");

            assertThrows(EquinixServerException.class,
                    () -> projects.projects().list());
        }
    }
}
