package api.equinix.javasdk.core;

import api.equinix.javasdk.core.enums.RequestType;
import api.equinix.javasdk.core.http.Utils;
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.core.internal.Constants;
import api.equinix.javasdk.fabric.model.json.CloudRouterJson;
import api.equinix.javasdk.fabric.model.json.ServiceProfileJson;
import com.fasterxml.jackson.databind.JavaType;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Validates that response types <em>derived</em> from a JSON class via
 * {@link Utils#deriveResponseType} deserialize real API fixtures correctly — the basis for
 * removing the 209 hand-declared {@code TypeReference} fields across the JSON models.
 */
class DerivedTypeTest {

    @Test
    void derivedPagedType_deserializesItemsAsJsonClass() throws Exception {
        JavaType paged = Utils.deriveResponseType(RequestType.PAGINATED, CloudRouterJson.class);

        try (InputStream is = getClass().getResourceAsStream("/json/fabric/paginated_cloud_routers.json")) {
            assertNotNull(is, "paginated_cloud_routers.json fixture missing");
            Page<?, ?> page = Constants.objectMapper.readValue(is, paged);

            assertNotNull(page.getItems());
            assertEquals(2, page.getItems().size());
            assertInstanceOf(CloudRouterJson.class, page.getItems().get(0));
            assertNotNull(page.getPagination());
        }
    }

    @Test
    void derivedSingleType_deserializesSingleFixture() throws Exception {
        JavaType single = Utils.deriveResponseType(RequestType.SINGLE, ServiceProfileJson.class);

        try (InputStream is = getClass().getResourceAsStream("/json/fabric/service_profile_response.json")) {
            assertNotNull(is, "service_profile_response.json fixture missing");
            Object profile = Constants.objectMapper.readValue(is, single);
            assertInstanceOf(ServiceProfileJson.class, profile);
        }
    }
}
