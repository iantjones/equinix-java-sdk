package com.eqixiac.equinix.internetaccess;

import com.eqixiac.equinix.core.internal.Constants;
import com.eqixiac.equinix.internetaccess.enums.ChangeType;
import com.eqixiac.equinix.internetaccess.enums.ConnectionType;
import com.eqixiac.equinix.internetaccess.enums.ContactType;
import com.eqixiac.equinix.internetaccess.enums.CustomerAsnRange;
import com.eqixiac.equinix.internetaccess.enums.ExportPolicy;
import com.eqixiac.equinix.internetaccess.enums.PurchaseOrderType;
import com.eqixiac.equinix.internetaccess.enums.Region;
import com.eqixiac.equinix.internetaccess.enums.RoutingProtocolType;
import com.eqixiac.equinix.internetaccess.enums.ServiceBilling;
import com.eqixiac.equinix.internetaccess.enums.ServiceOrderStatus;
import com.eqixiac.equinix.internetaccess.enums.ServiceOrderType;
import com.eqixiac.equinix.internetaccess.enums.ServiceState;
import com.eqixiac.equinix.internetaccess.enums.ServiceTypeV2;
import com.eqixiac.equinix.internetaccess.enums.UseCase;
import com.eqixiac.equinix.internetaccess.model.json.InternetAccessServiceJson;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Deserialization tests for {@link InternetAccessServiceJson}, the read-only {@code ServiceReadModel}
 * response returned by the Equinix Internet Access (EIA) v2 service operations.
 */
class InternetAccessServiceDeserializationTest {

    private static ObjectMapper objectMapper;
    private static InternetAccessServiceJson service;

    @BeforeAll
    static void setUp() throws Exception {
        objectMapper = Constants.mapper();
        InputStream is = InternetAccessServiceDeserializationTest.class
                .getResourceAsStream("/json/internetaccess/internet_access_service_response.json");
        assertNotNull(is, "internet_access_service_response.json fixture not found on classpath");
        service = objectMapper.readValue(is, InternetAccessServiceJson.class);
    }

    @Test
    void scalarFields_areDeserialized() {
        assertEquals("e1f2a3b4-c5d6-4e7f-8091-021324354657", service.getUuid());
        assertEquals("WebServers", service.getName());
        assertEquals("Customer facing web servers", service.getDescription());
        assertEquals(ServiceTypeV2.SINGLE, service.getType());
        assertEquals(Long.valueOf(1000), service.getBandwidth());
        assertEquals(ServiceState.ACTIVE, service.getState());
        assertEquals(UseCase.MAIN, service.getUseCase());
    }

    @Test
    void billingFields_areDeserialized() {
        assertEquals(ServiceBilling.FIXED, service.getBilling());
        assertEquals(Boolean.TRUE, service.getBillingEnabled());
        assertEquals("2026-01-15", service.getBillingStartDate());
    }

    @Test
    void account_isDeserialized() {
        assertNotNull(service.getAccount());
        assertEquals("1234533211", service.getAccount().getAccountNumber());
        assertEquals("Equinix, Inc.", service.getAccount().getOrganizationName());
    }

    @Test
    void project_isDeserialized() {
        assertNotNull(service.getProject());
        assertEquals("9713123422221", service.getProject().getProjectId());
        assertEquals("US Office", service.getProject().getProjectName());
    }

    @Test
    void change_isDeserialized() {
        assertNotNull(service.getChange());
        assertEquals(ChangeType.SERVICE_CREATION, service.getChange().getType());
        assertEquals("COMPLETED", service.getChange().getStatus());
        assertNotNull(service.getChange().getData());
        assertEquals("e1f2a3b4-c5d6-4e7f-8091-021324354657",
                service.getChange().getData().getService().getUuid());
    }

    @Test
    void changeLog_isDeserialized() {
        assertNotNull(service.getChangeLog());
        assertEquals("john.doe@company.com", service.getChangeLog().getCreatedByEmail());
        assertEquals("Ryan Einstein", service.getChangeLog().getUpdatedByFullName());
    }

    @Test
    void order_isDeserialized() {
        assertNotNull(service.getOrder());
        assertEquals(ServiceOrderType.NEW, service.getOrder().getType());
        assertEquals(ServiceOrderStatus.COMPLETED, service.getOrder().getStatus());
        assertEquals("1-9234239473", service.getOrder().getNumber());
        assertEquals("239384723943", service.getOrder().getReferenceNumber());

        assertNotNull(service.getOrder().getPurchaseOrder());
        assertEquals(PurchaseOrderType.STANDARD_PURCHASE_ORDER, service.getOrder().getPurchaseOrder().getType());
        assertEquals("129105284100", service.getOrder().getPurchaseOrder().getNumber());

        assertNotNull(service.getOrder().getContacts());
        assertEquals(1, service.getOrder().getContacts().size());
        assertEquals(ContactType.ORDERING, service.getOrder().getContacts().get(0).getType());
        assertEquals("john.doe@nowhere.com",
                service.getOrder().getContacts().get(0).getDetails().get(0).getValue());
    }

    @Test
    void connections_areDeserialized() {
        assertNotNull(service.getConnections());
        assertEquals(1, service.getConnections().size());
        assertEquals("9b8c5042-b553-4d5e-a2ac-c73bf6d4fd81", service.getConnections().get(0).getUuid());
        assertEquals(ConnectionType.IA_C, service.getConnections().get(0).getType());
        assertEquals("38a1eb68-4daf-4ef0-bd7f-6970727b6fc1",
                service.getConnections().get(0).getASide().getService().getUuid());
    }

    @Test
    void routingProtocol_isDeserialized() {
        assertNotNull(service.getRoutingProtocol());
        assertEquals(RoutingProtocolType.BGP, service.getRoutingProtocol().getType());
        assertEquals(Long.valueOf(16220), service.getRoutingProtocol().getCustomerAsn());
        assertEquals(CustomerAsnRange.BITS_32, service.getRoutingProtocol().getCustomerAsnRange());
        assertEquals(ExportPolicy.FULL, service.getRoutingProtocol().getExportPolicy());
        assertNotNull(service.getRoutingProtocol().getIpv4());
        assertEquals("198.51.100.0/24",
                service.getRoutingProtocol().getIpv4().getCustomerRoutes().get(0).getPrefix());
        assertEquals(Boolean.TRUE,
                service.getRoutingProtocol().getIpv4().getPeerings().get(0).getVrrpEnabled());
    }

    @Test
    void locations_areDeserialized() {
        assertNotNull(service.getLocations());
        assertEquals(1, service.getLocations().size());
        assertEquals("WA1", service.getLocations().get(0).getIbxCode());
        assertEquals(Region.EMEA, service.getLocations().get(0).getRegion());
    }

    @Test
    void tags_areDeserialized() {
        assertNotNull(service.getTags());
        assertEquals(2, service.getTags().size());
        assertTrue(service.getTags().contains("tag1"));
    }
}
