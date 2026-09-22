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
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link EnvironmentRef} and {@link ProviderSite}. The fixture is the specification's
 * own example ({@code docs/Protocols.md}, commit {@code bbfc763}): AWS {@code us-east-1} and GCP
 * {@code us-east4}, environment id {@code aws-gcp-us-east}.
 */
@DisplayName("core.model.multicloud.EnvironmentRef / ProviderSite")
class EnvironmentRefTest {

    private static final ProviderSite AWS_SITE = ProviderSite.builder()
            .providerRef(ProviderRef.AWS).site("us-east-1").displayName("US East (N. Virginia)").build();
    private static final ProviderSite GCP_SITE = ProviderSite.of(ProviderRef.GCP, "us-east4");

    private static EnvironmentRef awsFirst() {
        return EnvironmentRef.builder()
                .environmentId("aws-gcp-us-east")
                .providerSite(AWS_SITE)
                .providerSite(GCP_SITE)
                .supportedConnectionSizeMbps(BandwidthTier.of(1000, 10000))
                .visibility(EnvironmentVisibility.ENVIRONMENT_VISIBILITY_PUBLIC)
                .build();
    }

    private static EnvironmentRef gcpFirst() {
        return EnvironmentRef.builder()
                .environmentId("aws-gcp-us-east")
                .providerSite(GCP_SITE)
                .providerSite(AWS_SITE)
                .supportedConnectionSizeMbps(BandwidthTier.of(10000, 1000))
                .visibility(EnvironmentVisibility.ENVIRONMENT_VISIBILITY_PUBLIC)
                .build();
    }

    @Nested
    @DisplayName("order-insensitivity")
    class OrderInsensitivity {

        @Test
        @DisplayName("doc contract: 'two instances built with the sites in either order are equal, have the same hash code and the same toString()'")
        void equalityIgnoresSiteOrder() {
            assertEquals(awsFirst(), gcpFirst());
            assertEquals(awsFirst().hashCode(), gcpFirst().hashCode());
            assertEquals(awsFirst().toString(), gcpFirst().toString());
            assertEquals(EnvironmentRef.of("e", AWS_SITE, GCP_SITE), EnvironmentRef.of("e", GCP_SITE, AWS_SITE));
        }

        @Test
        @DisplayName("providerSites is stored sorted by provider id")
        void canonicalOrder() {
            assertEquals(List.of(AWS_SITE, GCP_SITE), awsFirst().getProviderSites());
            assertEquals(List.of(AWS_SITE, GCP_SITE), gcpFirst().getProviderSites());
        }

        @Test
        @DisplayName("connects accepts its endpoints in either order")
        void connectsEitherOrder() {
            for (EnvironmentRef env : List.of(awsFirst(), gcpFirst())) {
                assertTrue(env.connects(ProviderRef.AWS, "us-east-1", ProviderRef.GCP, "us-east4"));
                assertTrue(env.connects(ProviderRef.GCP, "us-east4", ProviderRef.AWS, "us-east-1"));
            }
        }

        @Test
        @DisplayName("connects pairs each site with its own provider: swapped site names do not match")
        void connectsDoesNotCrossSites() {
            EnvironmentRef env = awsFirst();
            assertFalse(env.connects(ProviderRef.AWS, "us-east4", ProviderRef.GCP, "us-east-1"));
            assertFalse(env.connects(ProviderRef.GCP, "us-east-1", ProviderRef.AWS, "us-east4"));
        }

        @Test
        @DisplayName("connects rejects a wrong site, a wrong provider, the same endpoint twice, and nulls")
        void connectsNegatives() {
            EnvironmentRef env = awsFirst();
            assertFalse(env.connects(ProviderRef.AWS, "us-west-2", ProviderRef.GCP, "us-east4"));
            assertFalse(env.connects(ProviderRef.AWS, "us-east-1", ProviderRef.OCI, "us-east4"));
            assertFalse(env.connects(ProviderRef.AWS, "us-east-1", ProviderRef.AWS, "us-east-1"));
            assertFalse(env.connects(null, "us-east-1", ProviderRef.GCP, "us-east4"));
            assertFalse(env.connects(ProviderRef.AWS, null, ProviderRef.GCP, "us-east4"));
            assertFalse(env.connects(ProviderRef.AWS, "us-east-1", null, "us-east4"));
            assertFalse(env.connects(ProviderRef.AWS, "us-east-1", ProviderRef.GCP, null));
        }

        @Test
        @DisplayName("connects compares site names trimmed and without regard to case")
        void connectsSiteNormalization() {
            assertTrue(awsFirst().connects(ProviderRef.of("AWS"), " US-EAST-1 ", ProviderRef.of("providers/gcp"), "US-East4"));
        }
    }

    @Test
    @DisplayName("siteOf, otherSide and involves answer per provider; a non-member or null yields empty")
    void perProviderLookups() {
        EnvironmentRef env = gcpFirst();
        assertEquals(Optional.of("us-east-1"), env.siteOf(ProviderRef.AWS));
        assertEquals(Optional.of("us-east4"), env.siteOf(ProviderRef.GCP));
        assertEquals(Optional.empty(), env.siteOf(ProviderRef.EQUINIX));
        assertEquals(Optional.empty(), env.siteOf(null));

        assertEquals(Optional.of(ProviderRef.GCP), env.otherSide(ProviderRef.AWS));
        assertEquals(Optional.of(ProviderRef.AWS), env.otherSide(ProviderRef.GCP));
        assertEquals(Optional.empty(), env.otherSide(ProviderRef.AZURE));
        assertEquals(Optional.empty(), env.otherSide(null));

        assertTrue(env.involves(ProviderRef.AWS));
        assertFalse(env.involves(ProviderRef.OCI));
        assertFalse(env.involves(null));
    }

    @Test
    @DisplayName("an environment joins exactly two sites of two different providers")
    void pairInvariant() {
        assertThrows(IllegalArgumentException.class, () -> EnvironmentRef.builder()
                .environmentId("e").build());
        assertThrows(IllegalArgumentException.class, () -> EnvironmentRef.builder()
                .environmentId("e").providerSite(AWS_SITE).build());
        assertThrows(IllegalArgumentException.class, () -> EnvironmentRef.builder()
                .environmentId("e").providerSite(AWS_SITE).providerSite(GCP_SITE)
                .providerSite(ProviderSite.of(ProviderRef.OCI, "us-ashburn-1")).build());
        IllegalArgumentException sameProvider = assertThrows(IllegalArgumentException.class,
                () -> EnvironmentRef.of("e", AWS_SITE, ProviderSite.of(ProviderRef.AWS, "us-west-2")));
        assertTrue(sameProvider.getMessage().contains("aws"), sameProvider.getMessage());
        assertThrows(NullPointerException.class, () -> EnvironmentRef.of("e", AWS_SITE, null));
        assertThrows(NullPointerException.class, () -> EnvironmentRef.builder()
                .environmentId("e").providerSite(AWS_SITE).providerSite(null).build());
    }

    @Test
    @DisplayName("environmentId must be a bare, non-blank id")
    void environmentIdInvariant() {
        assertThrows(NullPointerException.class, () -> EnvironmentRef.of(null, AWS_SITE, GCP_SITE));
        assertThrows(IllegalArgumentException.class, () -> EnvironmentRef.of("  ", AWS_SITE, GCP_SITE));
        assertThrows(IllegalArgumentException.class,
                () -> EnvironmentRef.of("providers/aws/environments/aws-gcp-us-east", AWS_SITE, GCP_SITE));
        assertEquals("aws-gcp-us-east", EnvironmentRef.of(" aws-gcp-us-east ", AWS_SITE, GCP_SITE).getEnvironmentId());
    }

    @Test
    @DisplayName("absent sizes become BandwidthTier.none(); absent visibility becomes UNKNOWN")
    void defaults() {
        EnvironmentRef env = EnvironmentRef.of("aws-gcp-us-east", AWS_SITE, GCP_SITE);
        assertSame(BandwidthTier.none(), env.getSupportedConnectionSizeMbps());
        assertEquals(EnvironmentVisibility.UNKNOWN, env.getVisibility());
        assertEquals(OptionalInt.empty(), env.getSupportedConnectionSizeMbps().coveringTier(1000));
        assertNotEquals(awsFirst(), env);
    }

    @Test
    @DisplayName("providerSites is unmodifiable")
    void providerSitesUnmodifiable() {
        assertThrows(UnsupportedOperationException.class, () -> awsFirst().getProviderSites().remove(0));
    }

    @Test
    @DisplayName("resourceName reproduces the specification's two addresses of one environment")
    void resourceName() {
        EnvironmentRef env = awsFirst();
        assertEquals("providers/aws/environments/aws-gcp-us-east", env.resourceName(ProviderRef.AWS));
        assertEquals("providers/gcp/environments/aws-gcp-us-east", env.resourceName(ProviderRef.GCP));
        assertThrows(IllegalArgumentException.class, () -> env.resourceName(ProviderRef.OCI));
        assertThrows(NullPointerException.class, () -> env.resourceName(null));
    }

    @Test
    @DisplayName("environmentIdOf: the javadoc table")
    void environmentIdOf() {
        assertEquals(Optional.of("aws-gcp-us-east"),
                EnvironmentRef.environmentIdOf("providers/aws/environments/aws-gcp-us-east"));
        assertEquals(Optional.of("aws-gcp-us-east"),
                EnvironmentRef.environmentIdOf("/providers/gcp/environments/aws-gcp-us-east/interconnects/i1"));
        assertEquals(Optional.of("e1"),
                EnvironmentRef.environmentIdOf("https://host/v1/providers/aws/environments/e1?x=1"));
        assertEquals(Optional.of("e1"), EnvironmentRef.environmentIdOf(" providers/aws/environments/e1#frag "));
        assertEquals(Optional.of("e1"), EnvironmentRef.environmentIdOf("providers/aws/environments/e1/"));
        assertEquals(Optional.empty(), EnvironmentRef.environmentIdOf("providers/aws"));
        assertEquals(Optional.empty(), EnvironmentRef.environmentIdOf("environments/"));
        assertEquals(Optional.empty(), EnvironmentRef.environmentIdOf("environments"));
        assertEquals(Optional.empty(), EnvironmentRef.environmentIdOf(""));
        assertEquals(Optional.empty(), EnvironmentRef.environmentIdOf(null));
    }

    @Test
    @DisplayName("matchesUri compares the environment id exactly and ignores the provider prefix")
    void matchesUri() {
        EnvironmentRef env = awsFirst();
        assertTrue(env.matchesUri("providers/aws/environments/aws-gcp-us-east"));
        assertTrue(env.matchesUri("providers/gcp/environments/aws-gcp-us-east"));
        assertFalse(env.matchesUri("providers/aws/environments/AWS-GCP-US-EAST"));
        assertFalse(env.matchesUri("providers/aws/environments/aws-gcp-us-west"));
        assertFalse(env.matchesUri("providers/aws"));
        assertFalse(env.matchesUri(null));
    }

    @Test
    @DisplayName("JSON form: provider id as a string, sizes as an ascending array, wire enum name")
    void jsonShape() throws Exception {
        JsonNode json = Constants.mapper().readTree(Constants.mapper().writeValueAsString(gcpFirst()));
        assertEquals("aws-gcp-us-east", json.get("environmentId").asText());
        assertEquals("aws", json.get("providerSites").get(0).get("providerRef").asText());
        assertEquals("us-east-1", json.get("providerSites").get(0).get("site").asText());
        assertEquals("US East (N. Virginia)", json.get("providerSites").get(0).get("displayName").asText());
        assertEquals("gcp", json.get("providerSites").get(1).get("providerRef").asText());
        assertEquals("[1000,10000]", json.get("supportedConnectionSizeMbps").toString());
        assertEquals("ENVIRONMENT_VISIBILITY_PUBLIC", json.get("visibility").asText());
    }

    @Nested
    @DisplayName("ProviderSite")
    class ProviderSiteTests {

        @Test
        @DisplayName("site is trimmed with case preserved; displayName is optional")
        void construction() {
            ProviderSite site = ProviderSite.of(ProviderRef.EQUINIX, " DC ");
            assertEquals("DC", site.getSite());
            assertEquals(ProviderRef.EQUINIX, site.getProviderRef());
            assertNull(site.getDisplayName());
            assertEquals("US East (N. Virginia)", AWS_SITE.getDisplayName());
        }

        @Test
        @DisplayName("providerRef and site are required; site must not be blank")
        void validation() {
            assertThrows(NullPointerException.class, () -> ProviderSite.of(null, "us-east-1"));
            assertThrows(NullPointerException.class, () -> ProviderSite.of(ProviderRef.AWS, null));
            assertThrows(IllegalArgumentException.class, () -> ProviderSite.of(ProviderRef.AWS, " "));
            assertThrows(NullPointerException.class, () -> ProviderSite.builder().site("us-east-1").build());
        }

        @Test
        @DisplayName("equality is exact on all three fields; matches ignores displayName and site case")
        void equalityVersusMatches() {
            ProviderSite plain = ProviderSite.of(ProviderRef.AWS, "us-east-1");
            assertNotEquals(AWS_SITE, plain, "displayName differs");
            assertNotEquals(plain, ProviderSite.of(ProviderRef.AWS, "US-EAST-1"));
            assertEquals(plain, ProviderSite.of(ProviderRef.of("AWS"), " us-east-1 "));

            assertTrue(AWS_SITE.matches(ProviderRef.AWS, "US-EAST-1"));
            assertTrue(AWS_SITE.matches(ProviderRef.AWS, " us-east-1 "));
            assertFalse(AWS_SITE.matches(ProviderRef.GCP, "us-east-1"));
            assertFalse(AWS_SITE.matches(ProviderRef.AWS, "us-east-2"));
            assertFalse(AWS_SITE.matches(null, "us-east-1"));
            assertFalse(AWS_SITE.matches(ProviderRef.AWS, null));
        }
    }
}
