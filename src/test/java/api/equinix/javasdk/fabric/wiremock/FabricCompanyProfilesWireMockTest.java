package api.equinix.javasdk.fabric.wiremock;

import api.equinix.javasdk.Fabric;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.fabric.model.CompanyProfile;
import org.junit.jupiter.api.*;

import static api.equinix.javasdk.core.ResponseStubs.*;
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
                            + "\"contactUrl\":\"https://acme.example.com/contact\"}", true, true)));
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
}
