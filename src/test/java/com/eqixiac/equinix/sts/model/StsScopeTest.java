package com.eqixiac.equinix.sts.model;

import com.eqixiac.equinix.sts.model.json.creators.TokenRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** The four scope forms the live STS accepts (2026-11-11). */
class StsScopeTest {

    @Test
    void factoriesBuildTheLiveForms() {
        assertEquals("roleassignments:org-1", StsScope.roleAssignments("org-1").getValue());
        assertEquals("projectpolicies:abc-123", StsScope.projectPolicies("project:abc-123").getValue());
        assertEquals("principalpolicy:abc-123", StsScope.principalPolicy("abc-123").getValue());
        assertEquals("ern:eqix:equinix/access:global:abc-123:AccessPolicy:my-policy",
                StsScope.accessPolicy("project:abc-123", "accesspolicy:my-policy").getValue());
        assertEquals("ern:a:b/c:d:e:f:g", StsScope.accessPolicyErn("ern:a:b/c:d:e:f:g").getValue());
    }

    @Test
    void rejectsMalformedInput() {
        assertThrows(IllegalArgumentException.class, () -> StsScope.accessPolicyErn("roles"));
        assertThrows(IllegalArgumentException.class, () -> StsScope.projectPolicies(" "));
        assertThrows(IllegalArgumentException.class, () -> StsScope.principalPolicy("a:b"));
    }

    @Test
    void tokenRequestCarriesTheTypedScope() {
        TokenRequest request = new TokenRequest().scope(StsScope.principalPolicy("p1"));
        assertEquals("principalpolicy:p1", request.toFormFields().get("scope"));
    }
}
