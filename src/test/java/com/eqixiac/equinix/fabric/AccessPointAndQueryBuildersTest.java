package com.eqixiac.equinix.fabric;

import com.eqixiac.equinix.core.internal.Constants;
import com.eqixiac.equinix.fabric.enums.AccessPointType;
import com.eqixiac.equinix.fabric.enums.ConnectionOperationType;
import com.eqixiac.equinix.fabric.model.implementation.AccessPoint;
import com.eqixiac.equinix.fabric.model.implementation.AccessPointAddress;
import com.eqixiac.equinix.fabric.model.implementation.ProductOffering;
import com.eqixiac.equinix.fabric.model.implementation.SimpleAccessPoint;
import com.eqixiac.equinix.fabric.model.implementation.filter.CloudEventQuery;
import com.eqixiac.equinix.fabric.model.implementation.filter.ConnectionSearchField;
import com.eqixiac.equinix.fabric.model.implementation.filter.FilterOperator;
import com.eqixiac.equinix.fabric.model.implementation.filter.FilterPropertyList;
import com.eqixiac.equinix.fabric.model.implementation.filter.MetricQuery;
import com.eqixiac.equinix.fabric.model.json.GatewayPackageJson;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Spec 4.34.0 access-point properties, the IC builder, the router-package shape and the metric /
 * cloud-event query builders.
 */
class AccessPointAndQueryBuildersTest {

    private static final ObjectMapper MAPPER = Constants.mapper();

    @Test
    @DisplayName("providerIdentifier, productOffering, bandwidthCommit and address serialize as in Colo2Sp-LMaaS-Primary")
    void newAccessPointPropertiesSerialize() throws Exception {
        SimpleAccessPoint ap = SimpleAccessPoint.define(AccessPointType.SP)
                .metro("SV")
                .providerIdentifier("prov-1")
                .productOffering(new ProductOffering("po-1", "LMaaS", 12))
                .bandwidthCommit(50)
                .address(AccessPointAddress.builder().streetNr("1").streetName("Main").city("San Jose")
                        .stateOrProvince("CA").postCode("95002").country("US").build())
                .create();

        JsonNode n = MAPPER.readTree(MAPPER.writeValueAsString(ap));
        assertEquals("prov-1", n.at("/providerIdentifier").asText());
        assertEquals(12, n.at("/productOffering/billingTerm").asInt());
        assertEquals(50, n.at("/bandwidthCommit").asInt());
        assertEquals("San Jose", n.at("/address/city").asText());
        assertFalse(n.has("hideAssetInfo"));
        assertFalse(n.has("interconnection"));
    }

    @Test
    @DisplayName("spec ranges are enforced: billingTerm values, bandwidthCommit 0..100000")
    void rangesEnforced() {
        assertThrows(IllegalArgumentException.class, () -> new ProductOffering("p", null, 7));
        assertThrows(IllegalArgumentException.class, () -> SimpleAccessPoint.define(AccessPointType.SP).bandwidthCommit(100001));
    }

    @Test
    @DisplayName("an IC access point serializes type IC with interconnect.uuid; reads back providerIdentifier")
    void interconnectAccessPoint() throws Exception {
        SimpleAccessPoint ap = SimpleAccessPoint.define(AccessPointType.IC).interconnect("ic-1").create();
        JsonNode n = MAPPER.readTree(MAPPER.writeValueAsString(ap));
        assertEquals("IC", n.at("/type").asText());
        assertEquals("ic-1", n.at("/interconnect/uuid").asText());

        AccessPoint read = MAPPER.readValue("{\"type\":\"SP\",\"providerIdentifier\":\"x\",\"bandwidthCommit\":10}", AccessPoint.class);
        assertEquals("x", read.getProviderIdentifier());
        assertEquals(10, read.getBandwidthCommit());
        assertEquals("/aSide/accessPoint/interconnect/uuid", ConnectionSearchField.A_SIDE_INTERCONNECT_UUID.path());
    }

    @Test
    @DisplayName("router packages read vcCountMax, crCountMax and vcBandwidthMax (live shape 2026-11-11)")
    void gatewayPackageShape() throws Exception {
        GatewayPackageJson pkg = MAPPER.readValue("{\"code\":\"LAB\",\"totalIPv4RoutesMax\":50,\"routeFilterSupported\":true,"
                + "\"vcCountMax\":10,\"crCountMax\":3,\"vcBandwidthMax\":50}", GatewayPackageJson.class);
        assertEquals(10, pkg.getVcCountMax());
        assertEquals(3, pkg.getCrCountMax());
        assertEquals(50, pkg.getVcBandwidthMax());
        assertEquals(ConnectionOperationType.CONNECTION_PROVIDER_STATUS_UPDATE_REQUEST,
                ConnectionOperationType.valueOf("CONNECTION_PROVIDER_STATUS_UPDATE_REQUEST"));
    }

    @Test
    @DisplayName("MetricQuery builds subject (=), names (IN) and the BETWEEN range; validate rejects incomplete filters")
    void metricQuery() {
        FilterPropertyList f = MetricQuery.forConnection("c-1").names("a", "b")
                .between(LocalDateTime.of(2026, 1, 1, 0, 0), LocalDateTime.of(2026, 1, 2, 0, 0)).interval("PT1H").toFilter();
        assertTrue(f.hasProperty("/subject", FilterOperator.EQUALS));
        assertTrue(f.hasProperty("/name", FilterOperator.IN));
        assertTrue(f.hasProperty("/dataPoints/endDateTime", FilterOperator.BETWEEN));
        assertTrue(f.hasProperty("/interval"));
        assertDoesNotThrow(() -> MetricQuery.validate(f));
        assertThrows(IllegalArgumentException.class, () -> MetricQuery.forPort(" "));
        assertThrows(IllegalArgumentException.class, () -> MetricQuery.validate(null));
    }

    @Test
    @DisplayName("CloudEventQuery scopes by subject (IN), project or organization (=)")
    void cloudEventQuery() {
        assertTrue(CloudEventQuery.forAsset("ports", "p-1").toFilter().hasProperty("/subject", FilterOperator.IN));
        assertTrue(CloudEventQuery.forProject("p").toFilter().hasProperty("/equinixproject", FilterOperator.EQUALS));
        FilterPropertyList f = CloudEventQuery.forOrganization("o")
                .between(LocalDateTime.of(2026, 1, 1, 0, 0), LocalDateTime.of(2026, 1, 2, 0, 0)).toFilter();
        assertTrue(f.hasProperty("/equinixorganization"));
        assertTrue(f.hasProperty("/time", FilterOperator.BETWEEN));
        assertThrows(IllegalArgumentException.class, CloudEventQuery::forSubjects);
    }
}
