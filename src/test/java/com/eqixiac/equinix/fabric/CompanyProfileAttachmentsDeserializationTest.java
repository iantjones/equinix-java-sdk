package com.eqixiac.equinix.fabric;

import com.eqixiac.equinix.core.internal.Constants;
import com.eqixiac.equinix.fabric.model.json.CompanyServiceProfileListResponseJson;
import com.eqixiac.equinix.fabric.model.json.PrivateServiceListResponseJson;
import com.eqixiac.equinix.fabric.model.json.TagListResponseJson;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Deserialization tests for the company-profile attachment listing responses
 * (service profiles, tags, private services), which wrap their entries in a
 * {@code { "data": [...] }} envelope.
 */
class CompanyProfileAttachmentsDeserializationTest {

    private static final ObjectMapper objectMapper = Constants.mapper();

    @Test
    void serviceProfiles_areDeserialized() throws Exception {
        InputStream is = getClass().getResourceAsStream("/json/fabric/company_profile_service_profiles_response.json");
        assertNotNull(is, "company_profile_service_profiles_response.json fixture not found on classpath");

        CompanyServiceProfileListResponseJson response = objectMapper.readValue(is, CompanyServiceProfileListResponseJson.class);
        assertNotNull(response.getData());
        assertEquals(2, response.getData().size());
        assertEquals(2, response.getServiceProfiles().size());
        assertEquals("423af68b-42f0-4f2e-9c5c-2fbd44b4b387", response.getData().get(0).getUuid());
        assertNotNull(response.getData().get(0).getHref());
    }

    @Test
    void tags_areDeserialized() throws Exception {
        InputStream is = getClass().getResourceAsStream("/json/fabric/company_profile_tags_response.json");
        assertNotNull(is, "company_profile_tags_response.json fixture not found on classpath");

        TagListResponseJson response = objectMapper.readValue(is, TagListResponseJson.class);
        assertNotNull(response.getData());
        assertEquals(1, response.getData().size());
        assertEquals(1, response.getTags().size());
        assertEquals("environment", response.getData().get(0).getName());
        assertEquals("Environment", response.getData().get(0).getDisplayName());
        assertEquals(10000, response.getData().get(0).getWeight());
    }

    @Test
    void privateServices_areDeserialized() throws Exception {
        InputStream is = getClass().getResourceAsStream("/json/fabric/company_profile_private_services_response.json");
        assertNotNull(is, "company_profile_private_services_response.json fixture not found on classpath");

        PrivateServiceListResponseJson response = objectMapper.readValue(is, PrivateServiceListResponseJson.class);
        assertNotNull(response.getData());
        assertEquals(1, response.getData().size());
        assertEquals(1, response.getPrivateServices().size());
        assertEquals("460af68b-42f0-4f2e-9c5c-2fbd44b4b387", response.getData().get(0).getUuid());
        assertNotNull(response.getData().get(0).getHref());
    }
}
