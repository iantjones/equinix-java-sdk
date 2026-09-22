/*
 * Copyright 2021 Ian Jones. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
 * OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package com.eqixiac.equinix.core.model.multicloud;

import com.eqixiac.equinix.core.internal.Constants;
import com.eqixiac.equinix.fabric.model.implementation.cloud.CloudProviderType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link ProviderRef}. {@code aws} and {@code gcp} are the provider ids written in the
 * specification's {@code docs/Protocols.md} (commit {@code bbfc763}).
 */
@DisplayName("core.model.multicloud.ProviderRef")
class ProviderRefTest {

    @Test
    @DisplayName("of normalizes case, whitespace and the providers/ resource prefix")
    void normalizes() {
        assertEquals("aws", ProviderRef.of("aws").id());
        assertEquals("aws", ProviderRef.of(" AWS ").id());
        assertEquals("aws", ProviderRef.of("providers/aws").id());
        assertEquals("aws", ProviderRef.of("/providers/aws").id());
        assertEquals("aws", ProviderRef.of("Providers/AWS").id());
        assertEquals("my-provider", ProviderRef.of("providers/my-provider").id());
    }

    @Test
    @DisplayName("equals and hashCode follow the normalized id; toString is the id")
    void valueSemantics() {
        assertEquals(ProviderRef.AWS, ProviderRef.of("providers/AWS"));
        assertEquals(ProviderRef.AWS.hashCode(), ProviderRef.of("providers/AWS").hashCode());
        assertNotEquals(ProviderRef.AWS, ProviderRef.GCP);
        assertNotEquals(ProviderRef.AWS, "aws");
        assertEquals("gcp", ProviderRef.GCP.toString());
        assertEquals(ProviderRef.GCP.id(), ProviderRef.GCP.toString());
    }

    @Test
    @DisplayName("resourceName is providers/{id}")
    void resourceName() {
        assertEquals("providers/aws", ProviderRef.AWS.resourceName());
        assertEquals("providers/gcp", ProviderRef.GCP.resourceName());
        assertEquals("providers/equinix", ProviderRef.EQUINIX.resourceName());
        assertEquals(ProviderRef.OCI, ProviderRef.of(ProviderRef.OCI.resourceName()));
    }

    @Test
    @DisplayName("well-known constants carry the documented ids")
    void constants() {
        assertEquals("aws", ProviderRef.AWS.id());
        assertEquals("gcp", ProviderRef.GCP.id());
        assertEquals("oci", ProviderRef.OCI.id());
        assertEquals("azure", ProviderRef.AZURE.id());
        assertEquals("equinix", ProviderRef.EQUINIX.id());
        for (ProviderRef wellKnown : List.of(ProviderRef.AWS, ProviderRef.GCP, ProviderRef.OCI,
                ProviderRef.AZURE, ProviderRef.EQUINIX)) {
            assertTrue(wellKnown.isWellKnown());
        }
        assertFalse(ProviderRef.of("some-nsp").isWellKnown());
    }

    @Test
    @DisplayName("of rejects null, blank, multi-segment names and embedded whitespace")
    void rejectsInvalid() {
        assertThrows(NullPointerException.class, () -> ProviderRef.of(null));
        assertThrows(IllegalArgumentException.class, () -> ProviderRef.of("   "));
        assertThrows(IllegalArgumentException.class, () -> ProviderRef.of("providers/"));
        assertThrows(IllegalArgumentException.class, () -> ProviderRef.of("providers/aws/environments/x"));
        assertThrows(IllegalArgumentException.class, () -> ProviderRef.of("a ws"));
    }

    @Test
    @DisplayName("compareTo orders by id")
    void ordering() {
        List<ProviderRef> refs = new ArrayList<>(List.of(ProviderRef.GCP, ProviderRef.EQUINIX, ProviderRef.AWS));
        Collections.sort(refs);
        assertEquals(List.of(ProviderRef.AWS, ProviderRef.EQUINIX, ProviderRef.GCP), refs);
    }

    @Test
    @DisplayName("JSON form is the id string; both id and resource name deserialize")
    void json() throws Exception {
        assertEquals("\"aws\"", Constants.mapper().writeValueAsString(ProviderRef.AWS));
        assertEquals(ProviderRef.AWS, Constants.mapper().readValue("\"aws\"", ProviderRef.class));
        assertEquals(ProviderRef.GCP, Constants.mapper().readValue("\"providers/gcp\"", ProviderRef.class));
    }

    @Test
    @DisplayName("doc contract: CloudProviderType.shortCode() bridges to ProviderRef as the class javadoc states")
    void cloudProviderTypeBridge() {
        assertEquals(ProviderRef.AWS, ProviderRef.of(CloudProviderType.AWS.shortCode()));
        assertEquals(ProviderRef.AZURE, ProviderRef.of(CloudProviderType.AZURE.shortCode()));
        assertEquals(ProviderRef.GCP, ProviderRef.of(CloudProviderType.GOOGLE_CLOUD.shortCode()));
        assertEquals(ProviderRef.OCI, ProviderRef.of(CloudProviderType.ORACLE_CLOUD.shortCode()));
        // "The last three produce a valid ProviderRef for which isWellKnown() is false".
        for (CloudProviderType notInTable : List.of(CloudProviderType.IBM_CLOUD, CloudProviderType.ALIBABA_CLOUD,
                CloudProviderType.OTHER)) {
            assertFalse(ProviderRef.of(notInTable.shortCode()).isWellKnown(), notInTable.name());
        }
        assertEquals("cloud", ProviderRef.of(CloudProviderType.OTHER.shortCode()).id());
        // Every constant bridges without throwing; a new CloudProviderType constant is covered too.
        for (CloudProviderType type : CloudProviderType.values()) {
            assertEquals(type.shortCode(), ProviderRef.of(type.shortCode()).id());
        }
    }
}
