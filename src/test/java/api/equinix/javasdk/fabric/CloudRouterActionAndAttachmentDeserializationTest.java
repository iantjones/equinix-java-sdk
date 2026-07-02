package api.equinix.javasdk.fabric;
import api.equinix.javasdk.fabric.enums.CloudRouterType;

import api.equinix.javasdk.core.internal.Constants;
import api.equinix.javasdk.fabric.enums.CloudRouterActionState;
import api.equinix.javasdk.fabric.enums.CloudRouterActionType;
import api.equinix.javasdk.fabric.enums.ConnectionAttachmentStatus;
import api.equinix.javasdk.fabric.enums.ConnectionType;
import api.equinix.javasdk.fabric.enums.Direction;
import api.equinix.javasdk.fabric.enums.RouteAggregationType;
import api.equinix.javasdk.fabric.enums.RouteFilterType;
import api.equinix.javasdk.fabric.model.json.CloudRouterActionJson;
import api.equinix.javasdk.fabric.model.json.RouteAggregationAttachmentJson;
import api.equinix.javasdk.fabric.model.json.RouteFilterAttachmentJson;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Deserialization tests for the read-only Fabric Cloud Router action and connection route
 * filter / aggregation attachment models added for the CloudRouters and Connections sub-ops.
 */
class CloudRouterActionAndAttachmentDeserializationTest {

    private static ObjectMapper objectMapper;
    private static CloudRouterActionJson action;
    private static RouteFilterAttachmentJson routeFilterAttachment;
    private static RouteAggregationAttachmentJson routeAggregationAttachment;

    @BeforeAll
    static void setUp() throws Exception {
        objectMapper = Constants.objectMapper;

        action = read("/json/fabric/cloud_router_action_response.json", CloudRouterActionJson.class);
        routeFilterAttachment = read("/json/fabric/connection_route_filter_attachment_response.json", RouteFilterAttachmentJson.class);
        routeAggregationAttachment = read("/json/fabric/connection_route_aggregation_attachment_response.json", RouteAggregationAttachmentJson.class);
    }

    private static <T> T read(String resource, Class<T> type) throws Exception {
        InputStream is = CloudRouterActionAndAttachmentDeserializationTest.class.getResourceAsStream(resource);
        assertNotNull(is, resource + " fixture not found on classpath");
        return objectMapper.readValue(is, type);
    }

    @Test
    void action_scalarFields_areDeserialized() {
        assertEquals("557400f8-d360-11e9-bb65-2a2ae2dbcce4", action.getUuid());
        assertEquals(CloudRouterActionType.ROUTE_TABLE_ENTRY_UPDATE, action.getType());
        assertEquals(CloudRouterActionState.SUCCEEDED, action.getState());
        assertEquals("description", action.getDescription());
        assertNotNull(action.getHref());
    }

    @Test
    void action_connection_isDeserialized() {
        assertNotNull(action.getConnection());
        assertEquals("557400f8-d360-11e9-bb65-2a2ae2dbcce4", action.getConnection().getUuid());
        assertEquals(ConnectionType.IP_VC, action.getConnection().getType());
        assertNotNull(action.getConnection().getOperation());
        assertEquals(6, action.getConnection().getOperation().getBgpIpv4RoutesCount());
        assertEquals(4, action.getConnection().getOperation().getDistinctIpv4PrefixesCount());
    }

    @Test
    void action_router_isDeserialized() {
        assertNotNull(action.getRouter());
        assertEquals("a1c6b7fd-aead-410a-96b4-b1dfa1071700", action.getRouter().getUuid());
        assertEquals(CloudRouterType.XF_ROUTER, action.getRouter().getType());
        assertNotNull(action.getRouter().getOperation());
        assertEquals(12, action.getRouter().getOperation().getBgpIpv4RoutesCount());
    }

    @Test
    void action_changeLog_isDeserialized() {
        assertNotNull(action.getChangeLog());
        assertEquals("testuser", action.getChangeLog().getCreatedBy());
        assertNotNull(action.getChangeLog().getCreatedDateTime());
    }

    @Test
    void routeFilterAttachment_isDeserialized() {
        assertEquals("695a8471-6595-4ac6-a2f4-b3d96ed3a59d", routeFilterAttachment.getUuid());
        assertEquals(RouteFilterType.BGP_IPv4_PREFIX_FILTER, routeFilterAttachment.getType());
        assertEquals(ConnectionAttachmentStatus.ATTACHED, routeFilterAttachment.getAttachmentStatus());
        assertEquals(Direction.INBOUND, routeFilterAttachment.getDirection());
        assertNotNull(routeFilterAttachment.getHref());
        assertNotNull(routeFilterAttachment.getChangeLog());
    }

    @Test
    void routeAggregationAttachment_isDeserialized() {
        assertEquals("695a8471-6595-4ac6-a2f4-b3d96ed3a59d", routeAggregationAttachment.getUuid());
        assertEquals(RouteAggregationType.BGP_IPv4_PREFIX_AGGREGATION, routeAggregationAttachment.getType());
        assertEquals(ConnectionAttachmentStatus.ATTACHED, routeAggregationAttachment.getAttachmentStatus());
        assertNotNull(routeAggregationAttachment.getHref());
        assertNotNull(routeAggregationAttachment.getChangeLog());
    }
}
