package com.eqixiac.equinix.customerportal;

import com.eqixiac.equinix.core.internal.Constants;
import com.eqixiac.equinix.customerportal.enums.PlanFrequency;
import com.eqixiac.equinix.customerportal.enums.SupportPlanAssignmentStatus;
import com.eqixiac.equinix.customerportal.enums.SupportPlanStatus;
import com.eqixiac.equinix.customerportal.model.json.SupportPlanJson;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

class SupportPlanDeserializationTest {

    private static SupportPlanJson plan;

    @BeforeAll
    static void setUp() throws Exception {
        ObjectMapper objectMapper = Constants.mapper();
        InputStream is = SupportPlanDeserializationTest.class.getResourceAsStream("/json/customerportal/support_plan_response.json");
        assertNotNull(is, "support_plan_response.json fixture not found on classpath");
        plan = objectMapper.readValue(is, SupportPlanJson.class);
    }

    @Test
    void id_isDeserialized() {
        assertEquals("SP-100023", plan.getId());
    }

    @Test
    void scalarFields_areDeserialized() {
        assertEquals("128745", plan.getAccountNumber());
        assertEquals("Smart Hands Premium", plan.getPlanName());
        assertEquals("SMARTHANDS_PREMIUM", plan.getProductCode());
        assertEquals(Boolean.TRUE, plan.getIbxSpecific());
        assertEquals("2026-01-01", plan.getStartDate());
        assertEquals("2026-12-31", plan.getEndDate());
    }

    @Test
    void ibxs_isDeserialized() {
        assertEquals(2, plan.getIbxs().size());
        assertTrue(plan.getIbxs().contains("SV5"));
    }

    @Test
    void minutes_areDeserialized() {
        assertEquals(600, plan.getPurchasedMinutes());
        assertEquals(550, plan.getAssignedMinutes());
        assertEquals(120, plan.getConsumedMinutes());
        assertEquals(430, plan.getRemainingMinutes());
        assertEquals(60, plan.getPreviousConsumedMinutes());
        assertEquals(60, plan.getCurrentConsumedMinutes());
        assertEquals(0, plan.getPrepaidConsumedMinutes());
        assertEquals(0, plan.getTransitionMinutes());
    }

    @Test
    void enums_areDeserialized() {
        assertEquals(PlanFrequency.MONTHLY_ROLLOVER, plan.getPlanFrequency());
        assertEquals(SupportPlanStatus.ACTIVE, plan.getStatus());
    }

    @Test
    void assignment_isDeserialized() {
        assertNotNull(plan.getAssignment());
        assertEquals("Acme Corp", plan.getAssignment().getName());
        assertEquals("998877", plan.getAssignment().getAccountNumber());
        assertEquals(SupportPlanAssignmentStatus.ASSIGNED, plan.getAssignment().getStatus());
        assertEquals(Boolean.TRUE, plan.getAssignment().getEligible());
    }
}
