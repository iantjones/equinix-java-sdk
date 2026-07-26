package com.eqixiac.equinix;

import com.eqixiac.equinix.core.IntegrationTestBase;
import com.eqixiac.equinix.core.http.response.PaginatedList;
import com.eqixiac.equinix.projects.model.Project;
import com.eqixiac.equinix.sts.model.Jwks;
import com.eqixiac.equinix.sts.model.OidcProvider;
import com.eqixiac.equinix.sts.model.OpenIdConfiguration;
import com.eqixiac.equinix.sts.model.json.GrantedAccessPolicyPage;
import com.eqixiac.equinix.sts.model.json.OidcProviderPage;
import com.eqixiac.equinix.sts.model.json.creators.ListPoliciesGrantedRequest;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Live integration tests for the STS domain of the Equinix Java SDK, catalog-complete against the
 * safe operations of the {@code stsv1.yaml} spec: the pre-auth discovery reads
 * ({@code getJwks} / {@code getOpenIdConfiguration}), the OIDC provider listing
 * ({@code pageOidcProviders}) and the granted-access-policy search
 * ({@code POST /v1/accessPoliciesGranted}, operationId {@code listAccessPoliciesGranted} — a pure
 * filter+pagination POST; nothing is created server-side).
 *
 * <p>Spec-vs-reality contract: every call runs through
 * {@code IntegrationTestBase.requireEntitled}, which skips only on a 401/403 entitlement gap and
 * fails on any other defect (deserialization crash, 5xx, unmapped enum). Note the spec serves this
 * API from {@code https://sts.eqix.equinix.com} while the SDK routes through the unified
 * {@code api.equinix.com} gateway — a gateway that does not front the root-level discovery paths
 * would surface here as a failure, which is exactly the signal this tier exists to produce.</p>
 *
 * <h3>Test inputs</h3>
 * <ul>
 *     <li><b>projectId</b> — {@code -DstsProjectId=project:...} when supplied (used verbatim);
 *         otherwise derived from the first project returned by the Projects domain
 *         ({@code getAllProjects}), normalized to the typed-id form ({@code project:} prefix).
 *         The spec scopes OIDC providers to a <em>root</em> project, so a non-root default may
 *         require the override.</li>
 *     <li><b>subject token</b> — {@code listAccessPoliciesGranted} requires a real OIDC ID (or
 *         access) token as its {@code subjectToken} input; supply it via
 *         {@code -DstsSubjectToken=...} (and optionally
 *         {@code -DstsSubjectTokenType=urn:ietf:params:oauth:token-type:access_token}; the default
 *         is the id_token type). The test skips when no token is supplied.</li>
 * </ul>
 *
 * <h3>Usage</h3>
 * <pre>
 * mvn test -Pintegration-readonly -DaccessKey=ID -DsecretKey=SECRET \
 *     [-DstsProjectId=project:abc-123] [-DstsSubjectToken=eyJ...]
 * </pre>
 */
@Tag("integration-readonly")
@DisplayName("STS Integration Tests")
class STSIntegrationTest extends IntegrationTestBase {

    private static final String DEFAULT_SUBJECT_TOKEN_TYPE = "urn:ietf:params:oauth:token-type:id_token";

    static STS sts;
    static String projectId;
    static String projectDiscoveryNote;

    @BeforeAll
    static void setUpSts() {
        sts = new STS(testCredentials());
        projectId = resolveProjectId();
    }

    /**
     * Resolves the project id used by the project-scoped tests: the {@code -DstsProjectId} system
     * property verbatim when supplied, otherwise the first project discovered through the Projects
     * domain, normalized to the typed-id form ({@code project:} prefix).
     */
    private static String resolveProjectId() {
        String explicit = System.getProperty("stsProjectId");
        if (explicit != null && !explicit.isBlank()) {
            return explicit;
        }
        try {
            PaginatedList<Project> projects = new Projects(testCredentials()).projects().list();
            if (projects != null && !projects.isEmpty()) {
                String raw = projects.get(0).getProjectId();
                if (raw != null && !raw.isBlank()) {
                    return raw.startsWith("project:") ? raw : "project:" + raw;
                }
            }
            projectDiscoveryNote = "the project list returned no usable projectId";
        } catch (Exception e) {
            projectDiscoveryNote = "project discovery via the Projects domain failed: " + e.getMessage();
        }
        return null;
    }

    static void assumeProjectAvailable() {
        Assumptions.assumeTrue(projectId != null,
                "No projectId available for project-scoped STS tests (" +
                        (projectDiscoveryNote != null ? projectDiscoveryNote : "no project discovered") +
                        "); supply -DstsProjectId to run them");
    }

    // ════════════════════════════════════════════════════════════════════
    //  READONLY TESTS - Safe GET/list/search operations
    // ════════════════════════════════════════════════════════════════════

    @Nested
    @Tag("integration-readonly")
    @DisplayName("Discovery Read-Only Tests")
    class DiscoveryTests {

        @Test
        @DisplayName("discovery_getJwks - Get the JSON Web Key Set (getJwks)")
        void discovery_getJwks() {
            Jwks jwks = requireEntitled("STS", "getJwks", "Jwks", "GET",
                    () -> sts.discovery().getJwks());
            assertNotNull(jwks);
            assertNotNull(jwks.getKeys(), "getJwks returned a key set without a keys array");
        }

        @Test
        @DisplayName("discovery_getOpenIdConfiguration - Get the OIDC discovery document (getOpenIdConfiguration)")
        void discovery_getOpenIdConfiguration() {
            OpenIdConfiguration configuration = requireEntitled("STS", "getOpenIdConfiguration",
                    "OpenIdConfiguration", "GET",
                    () -> sts.discovery().getOpenIdConfiguration());
            assertNotNull(configuration);
            assertNotNull(configuration.getIssuer(), "getOpenIdConfiguration returned no issuer");
            configuration.getJwksUri();
            configuration.getTokenEndpoint();
            configuration.getIdTokenSigningAlgValuesSupported();
        }
    }

    @Nested
    @Tag("integration-readonly")
    @DisplayName("OIDC Provider Read-Only Tests")
    class OidcProviderTests {

        @Test
        @DisplayName("oidcProviders_list - Page the OIDC providers of a root project (pageOidcProviders)")
        void oidcProviders_list() {
            assumeProjectAvailable();

            OidcProviderPage providers = requireEntitled("STS", "pageOidcProviders", "OidcProvider", "GET",
                    () -> sts.oidcProviders().list(projectId));
            assertNotNull(providers);

            if (providers.getList() != null && !providers.getList().isEmpty()) {
                OidcProvider first = providers.getList().get(0);
                assertNotNull(first.getIdpId(), "pageOidcProviders returned a provider without an idpId");
                first.getName();
                first.getIssuerUri();
                first.getStatus();
                first.getTrustedClientIds();
            }
        }

        @Test
        @DisplayName("oidcProviders_list_includeSuspended - Page providers including suspended (pageOidcProviders)")
        void oidcProviders_list_includeSuspended() {
            assumeProjectAvailable();

            OidcProviderPage providers = requireEntitled("STS", "pageOidcProviders", "OidcProvider", "GET",
                    () -> sts.oidcProviders().list(projectId, true, null, null));
            assertNotNull(providers);

            if (providers.getList() != null && !providers.getList().isEmpty()) {
                OidcProvider first = providers.getList().get(0);
                assertNotNull(first.getIdpId(), "pageOidcProviders returned a provider without an idpId");
                first.getStatus();
            }
        }
    }

    @Nested
    @Tag("integration-readonly")
    @DisplayName("Granted Access Policy Search Tests")
    class GrantedAccessPolicyTests {

        @Test
        @DisplayName("accessPoliciesGranted_list - List access policies granted to a subject (listAccessPoliciesGranted)")
        void accessPoliciesGranted_list() {
            assumeProjectAvailable();

            String subjectToken = System.getProperty("stsSubjectToken");
            Assumptions.assumeTrue(subjectToken != null && !subjectToken.isBlank(),
                    "listAccessPoliciesGranted requires a real OIDC subject token; " +
                            "supply -DstsSubjectToken to run this test");
            String subjectTokenType = System.getProperty("stsSubjectTokenType", DEFAULT_SUBJECT_TOKEN_TYPE);

            ListPoliciesGrantedRequest request = new ListPoliciesGrantedRequest()
                    .projectId(projectId)
                    .subjectToken(subjectToken)
                    .subjectTokenType(subjectTokenType)
                    .pageSize(10);

            GrantedAccessPolicyPage page = requireEntitled("STS", "listAccessPoliciesGranted",
                    "GrantedAccessPolicy", "POST",
                    () -> sts.tokens().listAccessPoliciesGranted(request));
            assertNotNull(page);

            if (page.getList() != null && !page.getList().isEmpty()) {
                assertNotNull(page.getList().get(0).getAccessPolicyId(),
                        "listAccessPoliciesGranted returned an entry without an accessPolicyId");
            }
        }
    }
}
