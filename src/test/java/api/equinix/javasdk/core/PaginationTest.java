package api.equinix.javasdk.core;

import api.equinix.javasdk.core.http.response.Pagination;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link Pagination} logic (page number, first/last page calculations).
 * Uses Jackson to populate the Lombok @Setter(PRIVATE) fields.
 */
class PaginationTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    private Pagination createPagination(int offset, int limit, int total) throws Exception {
        String json = String.format(
                "{\"offset\":%d,\"limit\":%d,\"total\":%d}", offset, limit, total);
        return objectMapper.readValue(json, Pagination.class);
    }

    @Test
    void getPageNumber_offsetZero_returnsPageZero() throws Exception {
        Pagination pagination = createPagination(0, 20, 100);
        assertEquals(0, pagination.getPageNumber());
    }

    @Test
    void getPageNumber_offsetEqualsLimit_returnsPageOne() throws Exception {
        Pagination pagination = createPagination(20, 20, 100);
        assertEquals(1, pagination.getPageNumber());
    }

    @Test
    void getPageNumber_offsetTwiceLimit_returnsPageTwo() throws Exception {
        Pagination pagination = createPagination(40, 20, 100);
        assertEquals(2, pagination.getPageNumber());
    }

    @Test
    void getPageSize_returnsLimit() throws Exception {
        Pagination pagination = createPagination(0, 25, 100);
        assertEquals(25, pagination.getPageSize());
    }

    @Test
    void getIsFirstPage_offsetZero_returnsTrue() throws Exception {
        Pagination pagination = createPagination(0, 20, 100);
        assertTrue(pagination.getIsFirstPage());
    }

    @Test
    void getIsFirstPage_offsetNonZero_returnsFalse() throws Exception {
        Pagination pagination = createPagination(20, 20, 100);
        assertFalse(pagination.getIsFirstPage());
    }

    @Test
    void getIsLastPage_singlePage_returnsTrue() throws Exception {
        Pagination pagination = createPagination(0, 20, 5);
        assertTrue(pagination.getIsLastPage());
    }

    @Test
    void getIsLastPage_exactlyFull_returnsTrue() throws Exception {
        Pagination pagination = createPagination(0, 20, 20);
        assertTrue(pagination.getIsLastPage());
    }

    @Test
    void getIsLastPage_notLastPage_returnsFalse() throws Exception {
        Pagination pagination = createPagination(0, 20, 100);
        assertFalse(pagination.getIsLastPage());
    }

    @Test
    void getIsLastPage_lastPage_returnsTrue() throws Exception {
        Pagination pagination = createPagination(80, 20, 100);
        assertTrue(pagination.getIsLastPage());
    }

    @Test
    void getIsLastPage_middlePage_returnsFalse() throws Exception {
        Pagination pagination = createPagination(40, 20, 100);
        assertFalse(pagination.getIsLastPage());
    }

    @Test
    void previousAndNext_populatedFromJson() throws Exception {
        String json = "{\"offset\":20,\"limit\":20,\"total\":100,"
                + "\"previous\":\"/v4/connections?offset=0&limit=20\","
                + "\"next\":\"/v4/connections?offset=40&limit=20\"}";
        Pagination pagination = objectMapper.readValue(json, Pagination.class);
        assertNotNull(pagination.getPrevious());
        assertNotNull(pagination.getNext());
        assertEquals(20, pagination.getOffset());
        assertEquals(20, pagination.getLimit());
        assertEquals(100, pagination.getTotal());
    }
}
