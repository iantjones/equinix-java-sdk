package com.eqixiac.equinix.core;

import com.eqixiac.equinix.core.enums.RequestType;
import com.eqixiac.equinix.core.http.RequestAssembler;
import com.eqixiac.equinix.core.http.response.Page;
import com.eqixiac.equinix.core.internal.Constants;
import com.eqixiac.equinix.fabric.model.json.CloudRouterJson;
import com.eqixiac.equinix.fabric.model.json.ServiceProfileJson;
import com.fasterxml.jackson.databind.JavaType;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Validates that response types <em>derived</em> from a JSON class via
 * {@link RequestAssembler#deriveResponseType} deserialize real API fixtures correctly — the basis for
 * removing the 209 hand-declared {@code TypeReference} fields across the JSON models.
 */
class DerivedTypeTest {

    @Test
    void derivedPagedType_deserializesItemsAsJsonClass() throws Exception {
        JavaType paged = RequestAssembler.deriveResponseType(RequestType.PAGINATED, CloudRouterJson.class);

        // Page has exactly one type parameter (the item/JSON type) since the 2.0 generics collapse.
        assertEquals(1, paged.containedTypeCount());
        assertEquals(CloudRouterJson.class, paged.containedType(0).getRawClass());

        try (InputStream is = getClass().getResourceAsStream("/json/fabric/paginated_cloud_routers.json")) {
            assertNotNull(is, "paginated_cloud_routers.json fixture missing");
            Page<?> page = Constants.mapper().readValue(is, paged);

            assertNotNull(page.getItems());
            assertEquals(2, page.getItems().size());
            assertInstanceOf(CloudRouterJson.class, page.getItems().get(0));
            assertNotNull(page.getPagination());
        }
    }

    @Test
    void derivedSingleType_deserializesSingleFixture() throws Exception {
        JavaType single = RequestAssembler.deriveResponseType(RequestType.SINGLE, ServiceProfileJson.class);

        try (InputStream is = getClass().getResourceAsStream("/json/fabric/service_profile_response.json")) {
            assertNotNull(is, "service_profile_response.json fixture missing");
            Object profile = Constants.mapper().readValue(is, single);
            assertInstanceOf(ServiceProfileJson.class, profile);
        }
    }
}
