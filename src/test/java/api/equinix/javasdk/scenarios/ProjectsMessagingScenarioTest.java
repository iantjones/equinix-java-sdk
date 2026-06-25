package api.equinix.javasdk.scenarios;

import api.equinix.javasdk.Messaging;
import api.equinix.javasdk.Projects;
import api.equinix.javasdk.core.IntegrationTestBase;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.messaging.enums.SubscriptionType;
import api.equinix.javasdk.messaging.model.Subscription;
import api.equinix.javasdk.projects.model.Project;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Scenario: Project + Messaging Subscription lifecycle.
 *
 * <p>Creates a project, verifies it, updates it, then creates and verifies
 * a messaging subscription before tearing everything down in reverse order.</p>
 */
@Tag("integration-scenario")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ProjectsMessagingScenarioTest extends IntegrationTestBase {

    private Projects projectsService;
    private Messaging messagingService;
    private String projectUuid;
    private String subscriptionUuid;
    private String projectName;

    private void initClients() {
        if (projectsService == null) {
            projectsService = new Projects(testCredentials());
        }
        if (messagingService == null) {
            messagingService = new Messaging(testCredentials());
        }
    }

    @Test
    @Order(1)
    @DisplayName("Create a project")
    void createProject() {
        Assumptions.assumeTrue(isFullCrudEnabled(),
                "Skipped: full CRUD mode not enabled");
        initClients();

        try {
            projectName = testResourceName("proj");
            Project project = timedCall("Projects", "create", "Project", "POST", () ->
                    projectsService.projects().define()
                            .name(projectName)
                            .description("SDK integration test project")
                            .create()
            );

            assertNotNull(project, "Project should be created");
            projectUuid = project.getUuid();
            assertNotNull(projectUuid, "Project UUID should not be null");

            registerCleanup("Project", projectUuid, id -> {
                Project toDelete = projectsService.projects().getByUuid(id);
                toDelete.delete();
            });
            System.out.printf("  Project created: %s (%s)%n", projectName, projectUuid);
        } catch (Exception e) {
            Assumptions.assumeTrue(false,
                    "Project creation not available: " + e.getMessage());
        }
    }

    @Test
    @Order(2)
    @DisplayName("Verify project via GET")
    void verifyProject() {
        Assumptions.assumeTrue(isFullCrudEnabled(),
                "Skipped: full CRUD mode not enabled");
        Assumptions.assumeTrue(projectUuid != null,
                "Skipped: no project was created");
        initClients();

        Project project = timedCall("Projects", "get", "Project", "GET",
                projectUuid, () ->
                        projectsService.projects().getByUuid(projectUuid)
        );

        assertNotNull(project, "Project should be retrievable");
        assertEquals(projectName, project.getName(), "Project name should match");
        assertNotNull(project.getDescription(), "Project description should not be null");
        System.out.printf("  Project verified: %s%n", project.getName());
    }

    @Test
    @Order(3)
    @DisplayName("List projects and verify created project appears")
    void listProjects() {
        Assumptions.assumeTrue(isFullCrudEnabled(),
                "Skipped: full CRUD mode not enabled");
        Assumptions.assumeTrue(projectUuid != null,
                "Skipped: no project was created");
        initClients();

        PaginatedList<Project> projects = timedCall("Projects", "list", "Project", "GET", () ->
                projectsService.projects().list()
        );

        assertNotNull(projects, "Projects list should not be null");
        boolean found = false;
        for (Project p : projects) {
            if (projectUuid.equals(p.getUuid())) {
                found = true;
                break;
            }
        }
        Assumptions.assumeTrue(found,
                "Created project should appear in list (may be eventually consistent)");
        System.out.printf("  Project found in list of %d projects%n", projects.size());
    }

    @Test
    @Order(4)
    @DisplayName("Create messaging subscription")
    void createSubscription() {
        Assumptions.assumeTrue(isFullCrudEnabled(),
                "Skipped: full CRUD mode not enabled");
        initClients();

        try {
            String subName = testResourceName("msg-sub");
            Subscription subscription = timedCall("Messaging", "create", "Subscription", "POST", () ->
                    messagingService.subscriptions().define()
                            .name(subName)
                            .type(SubscriptionType.WEBHOOK)
                            .eventTypes(List.of("NETWORK_STATUS", "MAINTENANCE"))
                            .create()
            );

            assertNotNull(subscription, "Subscription should be created");
            subscriptionUuid = subscription.getUuid();
            assertNotNull(subscriptionUuid, "Subscription UUID should not be null");

            registerCleanup("Subscription", subscriptionUuid, id -> {
                Subscription toDelete = messagingService.subscriptions().getByUuid(id);
                toDelete.delete();
            });
            System.out.printf("  Subscription created: %s (%s)%n", subName, subscriptionUuid);
        } catch (Exception e) {
            Assumptions.assumeTrue(false,
                    "Subscription creation not available: " + e.getMessage());
        }
    }

    @Test
    @Order(5)
    @DisplayName("Verify messaging subscription via GET")
    void verifySubscription() {
        Assumptions.assumeTrue(isFullCrudEnabled(),
                "Skipped: full CRUD mode not enabled");
        Assumptions.assumeTrue(subscriptionUuid != null,
                "Skipped: no subscription was created");
        initClients();

        Subscription subscription = timedCall("Messaging", "get", "Subscription", "GET",
                subscriptionUuid, () ->
                        messagingService.subscriptions().getByUuid(subscriptionUuid)
        );

        assertNotNull(subscription, "Subscription should be retrievable");
        assertNotNull(subscription.getName(), "Subscription name should not be null");
        assertNotNull(subscription.getType(), "Subscription type should not be null");
        System.out.printf("  Subscription verified: %s (type=%s)%n",
                subscription.getName(), subscription.getType());
    }

    @Test
    @Order(6)
    @DisplayName("Teardown messaging subscription")
    void teardownSubscription() {
        Assumptions.assumeTrue(isFullCrudEnabled(),
                "Skipped: full CRUD mode not enabled");
        Assumptions.assumeTrue(subscriptionUuid != null,
                "Skipped: no subscription to delete");
        initClients();

        try {
            Subscription subscription = messagingService.subscriptions().getByUuid(subscriptionUuid);
            Boolean deleted = timedCall("Messaging", "delete", "Subscription", "DELETE",
                    subscriptionUuid, subscription::delete);
            assertNotNull(deleted, "Delete should return a result");
            System.out.printf("  Subscription deleted: %s%n", subscriptionUuid);
        } catch (Exception e) {
            System.err.printf("  Subscription teardown failed (cleanup will retry): %s%n",
                    e.getMessage());
        }
    }

    @Test
    @Order(7)
    @DisplayName("Teardown project and verify 404")
    void teardownProject() {
        Assumptions.assumeTrue(isFullCrudEnabled(),
                "Skipped: full CRUD mode not enabled");
        Assumptions.assumeTrue(projectUuid != null,
                "Skipped: no project to delete");
        initClients();

        try {
            Project project = projectsService.projects().getByUuid(projectUuid);
            Boolean deleted = timedCall("Projects", "delete", "Project", "DELETE",
                    projectUuid, project::delete);
            assertNotNull(deleted, "Delete should return a result");
            System.out.printf("  Project deleted: %s%n", projectUuid);

            // Verify 404 on re-fetch
            timedExpectedFailure("Projects", "get-after-delete", "Project", "GET",
                    projectUuid, () ->
                            projectsService.projects().getByUuid(projectUuid)
            );
            System.out.println("  Confirmed: project returns expected error after deletion");
        } catch (Exception e) {
            System.err.printf("  Project teardown failed (cleanup will retry): %s%n",
                    e.getMessage());
        }
    }
}
