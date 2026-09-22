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

package com.eqixiac.equinix.design.optimizer;

import com.eqixiac.equinix.core.model.multicloud.BandwidthTier;
import com.eqixiac.equinix.core.model.multicloud.ProviderRef;
import com.eqixiac.equinix.design.optimizer.enums.MulticloudEnvironmentStatus;
import com.eqixiac.equinix.design.optimizer.model.MulticloudEnvironment;
import com.eqixiac.equinix.design.optimizer.model.MulticloudEnvironmentCatalog;
import com.eqixiac.equinix.fabric.model.implementation.cloud.CloudProviderType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.eqixiac.equinix.fabric.model.implementation.cloud.CloudProviderType.AWS;
import static com.eqixiac.equinix.fabric.model.implementation.cloud.CloudProviderType.AZURE;
import static com.eqixiac.equinix.fabric.model.implementation.cloud.CloudProviderType.GOOGLE_CLOUD;
import static com.eqixiac.equinix.fabric.model.implementation.cloud.CloudProviderType.ORACLE_CLOUD;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The bundled multicloud environment catalog and the catalog API.
 *
 * <p>The bundled-data assertions encode what the providers' documentation listed on 2026-09-21:
 * {@code https://docs.aws.amazon.com/interconnect/latest/userguide/region-availability.html}
 * (eight Google Cloud pairs, four Microsoft Azure pairs under a "Preview" heading, one Oracle
 * Cloud pair), confirmed against Google's paired-locations page and Oracle's Interconnect for AWS
 * page. When a provider adds a pair, the bundled JSON and these expectations change together.</p>
 */
@DisplayName("MulticloudEnvironmentCatalog")
class MulticloudEnvironmentCatalogTest {

    private static final MulticloudEnvironmentCatalog STANDARD = MulticloudEnvironmentCatalog.standard();

    // ── Bundled data: observed provider documentation ──

    @Test
    @DisplayName("bundled catalog holds the 8 AWS-Google, 4 AWS-Azure and 1 AWS-Oracle pairs listed by AWS on 2026-09-21")
    void bundledPairCounts() {
        assertEquals("2026-09-21", STANDARD.asOf());
        assertEquals(13, STANDARD.environments().size());
        assertEquals(8, STANDARD.regionsFor(AWS, GOOGLE_CLOUD).size());
        assertEquals(4, STANDARD.regionsFor(AWS, AZURE).size());
        assertEquals(1, STANDARD.regionsFor(AWS, ORACLE_CLOUD).size());
        assertTrue(STANDARD.regionsFor(GOOGLE_CLOUD, AZURE).isEmpty(), "no Google-Azure pair is documented");
    }

    @Test
    @DisplayName("the AWS-Google pairs are the region pairs on the AWS page, all GA")
    void awsGooglePairs() {
        List<String> expected = List.of(
                "us-east-1|us-east4", "us-west-1|us-west2", "us-west-2|us-west1", "eu-west-2|europe-west2",
                "eu-central-1|europe-west3", "eu-north-1|europe-north2", "ap-southeast-1|asia-southeast1",
                "ap-southeast-2|australia-southeast1");
        List<String> actual = STANDARD.regionsFor(AWS, GOOGLE_CLOUD).stream()
                .map(e -> e.regionOf(AWS).orElseThrow() + "|" + e.regionOf(GOOGLE_CLOUD).orElseThrow())
                .collect(Collectors.toList());
        assertEquals(expected, actual);
        assertTrue(STANDARD.regionsFor(AWS, GOOGLE_CLOUD).stream()
                .allMatch(e -> e.getStatus() == MulticloudEnvironmentStatus.GA));
    }

    @Test
    @DisplayName("AWS us-east-1 <-> OCI us-ashburn-1 is GA with Oracle's eight provisioned bandwidth options")
    void awsOraclePair() {
        MulticloudEnvironment env = STANDARD.find(AWS, "us-east-1", ORACLE_CLOUD, "us-ashburn-1").orElseThrow();
        assertEquals(MulticloudEnvironmentStatus.GA, env.getStatus());
        assertEquals(List.of(500, 1000, 2000, 5000, 10000, 20000, 50000, 100000), env.sizesMbps().toList());
        assertEquals("aws-us-east-1--oci-us-ashburn-1", env.getEnvironment().getEnvironmentId());
        assertTrue(env.getNote().contains("500M is charged at the 1G usage rate"));
    }

    @Test
    @DisplayName("the AWS-Azure pairs are PREVIEW and limited to 1000 Mbps, as AWS states for preview providers")
    void awsAzurePairsArePreview() {
        for (MulticloudEnvironment env : STANDARD.regionsFor(AWS, AZURE)) {
            assertEquals(MulticloudEnvironmentStatus.PREVIEW, env.getStatus(), env.describe());
            assertEquals(List.of(1000), env.sizesMbps().toList(), env.describe());
        }
        assertTrue(STANDARD.find(AWS, "us-east-1", AZURE, "eastus").isPresent());
        assertTrue(STANDARD.find(AWS, "eu-central-1", AZURE, "germanywestcentral").isPresent());
    }

    @Test
    @DisplayName("every bundled entry carries an https source URL and an ISO as-of date not after the catalog date")
    void everyEntryIsSourcedAndDated() {
        LocalDate catalogDate = LocalDate.parse(STANDARD.asOf());
        for (MulticloudEnvironment env : STANDARD.environments()) {
            assertFalse(env.getSourceUrls().isEmpty(), env.describe());
            assertTrue(env.getSourceUrls().stream().allMatch(u -> u.startsWith("https://")), env.describe());
            assertTrue(env.primarySourceUrl().orElseThrow().contains("docs.aws.amazon.com"), env.describe());
            assertNotNull(env.getAsOf(), env.describe());
            assertFalse(LocalDate.parse(env.getAsOf()).isAfter(catalogDate), env.describe());
            assertFalse(env.sizesMbps().isEmpty(), env.describe());
            assertNotNull(env.getSizesNote(), env.describe());
        }
        assertNotNull(STANDARD.disclaimer());
    }

    // ── Lookup rules ──

    @Test
    @DisplayName("find() is order-insensitive and ignores region case and surrounding whitespace")
    void findIsOrderInsensitive() {
        MulticloudEnvironment forward = STANDARD.find(AWS, "us-east-1", GOOGLE_CLOUD, "us-east4").orElseThrow();
        MulticloudEnvironment reverse = STANDARD.find(GOOGLE_CLOUD, "us-east4", AWS, "us-east-1").orElseThrow();
        assertSame(forward, reverse);
        assertSame(forward, STANDARD.find(AWS, " US-EAST-1 ", GOOGLE_CLOUD, "US-East4").orElseThrow());
    }

    @Test
    @DisplayName("find() pairs each region with its own cloud: swapping the regions between the clouds matches nothing")
    void findDoesNotCrossRegions() {
        assertTrue(STANDARD.find(AWS, "us-east4", GOOGLE_CLOUD, "us-east-1").isEmpty());
    }

    @Test
    @DisplayName("find() is empty for an uncatalogued pair and for null arguments")
    void findEmptyCases() {
        assertTrue(STANDARD.find(AWS, "eu-west-1", GOOGLE_CLOUD, "europe-west1").isEmpty());
        assertTrue(STANDARD.find(null, "us-east-1", GOOGLE_CLOUD, "us-east4").isEmpty());
        assertTrue(STANDARD.find(AWS, null, GOOGLE_CLOUD, "us-east4").isEmpty());
        assertTrue(STANDARD.find(AWS, "us-east-1", CloudProviderType.OTHER, "x").isEmpty());
    }

    @Test
    @DisplayName("regionsFor() is order-insensitive; the same cloud twice or a null cloud yields an empty list")
    void regionsForIsOrderInsensitive() {
        assertEquals(STANDARD.regionsFor(AWS, GOOGLE_CLOUD), STANDARD.regionsFor(GOOGLE_CLOUD, AWS));
        assertTrue(STANDARD.regionsFor(AWS, AWS).isEmpty());
        assertTrue(STANDARD.regionsFor(null, AWS).isEmpty());
        assertThrows(UnsupportedOperationException.class,
                () -> STANDARD.regionsFor(AWS, GOOGLE_CLOUD).add(null));
    }

    @Test
    @DisplayName("sizes round up through BandwidthTier: 3000 Mbps on the Oracle pair covers at 5000, 200000 has no covering size")
    void coveringSize() {
        BandwidthTier sizes = STANDARD.find(AWS, "us-east-1", ORACLE_CLOUD, "us-ashburn-1").orElseThrow().sizesMbps();
        assertEquals(5000, sizes.coveringTier(3000).orElseThrow());
        assertTrue(sizes.coveringTier(200_000).isEmpty());
    }

    // ── Extension: the bundled catalog goes stale ──

    @Test
    @DisplayName("with() adds a region pair to a new catalog and leaves the original unchanged")
    void withAddsEntry() {
        MulticloudEnvironment added = MulticloudEnvironment.of(AWS, "eu-west-1", GOOGLE_CLOUD, "europe-west1",
                MulticloudEnvironmentStatus.GA, BandwidthTier.of(1000, 10000), "https://example.com/launch", "2026-12-01");

        MulticloudEnvironmentCatalog extended = STANDARD.with(added);

        assertEquals(14, extended.environments().size());
        assertSame(added, extended.find(GOOGLE_CLOUD, "europe-west1", AWS, "eu-west-1").orElseThrow());
        assertEquals(13, STANDARD.environments().size(), "the bundled catalog is immutable");
        assertTrue(STANDARD.find(AWS, "eu-west-1", GOOGLE_CLOUD, "europe-west1").isEmpty());
        assertEquals(STANDARD.asOf(), extended.asOf(), "the catalog date still describes the bundled entries");
    }

    @Test
    @DisplayName("with() replaces the entry for a region pair already present, in place, whichever order the sides are given in")
    void withReplacesSamePair() {
        MulticloudEnvironment promoted = MulticloudEnvironment.of(AZURE, "eastus", AWS, "us-east-1",
                MulticloudEnvironmentStatus.GA, BandwidthTier.of(1000, 10000), "https://example.com/azure-ga", "2027-01-15");

        MulticloudEnvironmentCatalog updated = STANDARD.with(promoted);

        assertEquals(13, updated.environments().size());
        assertEquals(MulticloudEnvironmentStatus.GA,
                updated.find(AWS, "us-east-1", AZURE, "eastus").orElseThrow().getStatus());
        assertEquals(MulticloudEnvironmentStatus.PREVIEW,
                STANDARD.find(AWS, "us-east-1", AZURE, "eastus").orElseThrow().getStatus());
        assertEquals(STANDARD.environments().indexOf(STANDARD.find(AWS, "us-east-1", AZURE, "eastus").orElseThrow()),
                updated.environments().indexOf(promoted));
    }

    @Test
    @DisplayName("of() builds a caller-supplied catalog; empty() matches nothing")
    void ofAndEmpty() {
        MulticloudEnvironment only = MulticloudEnvironment.of(AWS, "sa-east-1", GOOGLE_CLOUD, "southamerica-east1",
                null, null, null, null);
        MulticloudEnvironmentCatalog custom = MulticloudEnvironmentCatalog.of(List.of(only));

        assertEquals(1, custom.environments().size());
        assertNull(custom.asOf());
        assertEquals(MulticloudEnvironmentStatus.UNVERIFIED, only.getStatus(), "no status defaults to UNVERIFIED");
        assertTrue(only.sizesMbps().isEmpty());
        assertTrue(only.primarySourceUrl().isEmpty());

        assertTrue(MulticloudEnvironmentCatalog.empty().isEmpty());
        assertTrue(MulticloudEnvironmentCatalog.empty().find(AWS, "us-east-1", GOOGLE_CLOUD, "us-east4").isEmpty());
        assertThrows(NullPointerException.class, () -> MulticloudEnvironmentCatalog.of(null));
    }

    // ── Loading ──

    @Test
    @DisplayName("load() reads the documented file format; a missing or unrecognized status loads as UNVERIFIED")
    void loadReadsDocumentedFormat() {
        String json = "{ \"asOf\": \"2027-02-01\", \"disclaimer\": \"d\", \"environments\": ["
                + "{ \"sites\": [ {\"provider\": \"providers/aws\", \"site\": \"us-east-2\"},"
                + "              {\"provider\": \"gcp\", \"site\": \"us-east5\", \"displayName\": \"Columbus\"} ],"
                + "  \"status\": \"rumoured\", \"supportedConnectionSizeMbps\": [10000, 0, 1000],"
                + "  \"sources\": [\"https://example.com/a\"], \"asOf\": \"2027-01-31\" },"
                + "{ \"sites\": [ {\"provider\": \"oci\", \"site\": \"us-phoenix-1\"},"
                + "              {\"provider\": \"aws\", \"site\": \"us-west-2\"} ] } ] }";

        MulticloudEnvironmentCatalog loaded = MulticloudEnvironmentCatalog.load(stream(json));

        assertEquals("2027-02-01", loaded.asOf());
        assertEquals("d", loaded.disclaimer());
        MulticloudEnvironment first = loaded.find(GOOGLE_CLOUD, "us-east5", AWS, "us-east-2").orElseThrow();
        assertEquals(MulticloudEnvironmentStatus.UNVERIFIED, first.getStatus());
        assertEquals(List.of(1000, 10000), first.sizesMbps().toList(), "sizes are sorted and the 0 placeholder is dropped");
        assertEquals("aws-us-east-2--gcp-us-east5", first.getEnvironment().getEnvironmentId());
        assertEquals(Optional.of("us-east5"), first.getEnvironment().siteOf(ProviderRef.GCP));
        MulticloudEnvironment second = loaded.find(AWS, "us-west-2", ORACLE_CLOUD, "us-phoenix-1").orElseThrow();
        assertEquals(MulticloudEnvironmentStatus.UNVERIFIED, second.getStatus());
        assertNull(second.getAsOf());
    }

    @Test
    @DisplayName("load() rejects a malformed entry and names its index")
    void loadNamesMalformedEntry() {
        String oneSite = "{ \"environments\": [ { \"sites\": [ {\"provider\": \"aws\", \"site\": \"us-east-1\"},"
                + " {\"provider\": \"gcp\", \"site\": \"us-east4\"} ] },"
                + " { \"sites\": [ {\"provider\": \"aws\", \"site\": \"us-east-1\"} ] } ] }";
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> MulticloudEnvironmentCatalog.load(stream(oneSite)));
        assertTrue(e.getMessage().contains("entry 1"), e.getMessage());

        String sameProvider = "{ \"environments\": [ { \"sites\": [ {\"provider\": \"aws\", \"site\": \"us-east-1\"},"
                + " {\"provider\": \"aws\", \"site\": \"us-west-2\"} ] } ] }";
        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> MulticloudEnvironmentCatalog.load(stream(sameProvider))).getMessage().contains("entry 0"));
    }

    @Test
    @DisplayName("a failed load throws and is not retained: standard() afterwards is the same intact bundled catalog")
    void failedLoadIsNotRetained() {
        assertThrows(UncheckedIOException.class, () -> MulticloudEnvironmentCatalog.load(stream("not json {")));
        assertThrows(IllegalArgumentException.class, () -> MulticloudEnvironmentCatalog.load(stream("[1, 2]")));

        assertSame(STANDARD, MulticloudEnvironmentCatalog.standard(), "standard() is loaded once and shared");
        assertEquals(13, MulticloudEnvironmentCatalog.standard().environments().size());
    }

    // ── MulticloudEnvironment ──

    @Test
    @DisplayName("MulticloudEnvironment.of() is order-insensitive and validates its arguments")
    void environmentFactory() {
        MulticloudEnvironment one = MulticloudEnvironment.of(AWS, "us-east-1", GOOGLE_CLOUD, "us-east4",
                MulticloudEnvironmentStatus.GA, BandwidthTier.of(10000), "https://example.com", "2026-09-21");
        MulticloudEnvironment swapped = MulticloudEnvironment.of(GOOGLE_CLOUD, "us-east4", AWS, "us-east-1",
                MulticloudEnvironmentStatus.GA, BandwidthTier.of(10000), "https://example.com", "2026-09-21");
        assertEquals(one, swapped);
        assertEquals("aws us-east-1 <-> gcp us-east4 (GA, as of 2026-09-21)", one.describe());
        assertTrue(one.joins(GOOGLE_CLOUD, AWS));
        assertFalse(one.joins(AWS, AZURE));
        assertEquals(List.of(Optional.of(AWS), Optional.of(GOOGLE_CLOUD)), one.cloudProviders());

        assertThrows(IllegalArgumentException.class, () -> MulticloudEnvironment.of(
                AWS, "us-east-1", AWS, "us-west-2", null, null, null, null));
        assertThrows(IllegalArgumentException.class, () -> MulticloudEnvironment.of(
                AWS, "us-east-1", CloudProviderType.OTHER, "x", null, null, null, null));
        assertThrows(IllegalArgumentException.class, () -> MulticloudEnvironment.of(
                AWS, " ", GOOGLE_CLOUD, "us-east4", null, null, null, null));
        assertThrows(IllegalArgumentException.class, () -> MulticloudEnvironment.of(
                AWS, "us-east-1", GOOGLE_CLOUD, "us-east4", null, null, null, "21/09/2026"));
    }

    @Test
    @DisplayName("provider mapping follows CloudProviderType.shortCode(): aws, azure, gcp, oci, ibm, alibaba; equinix maps to no cloud")
    void providerMapping() {
        assertEquals(ProviderRef.AWS, MulticloudEnvironment.providerRefOf(AWS));
        assertEquals(ProviderRef.GCP, MulticloudEnvironment.providerRefOf(GOOGLE_CLOUD));
        assertEquals(ProviderRef.OCI, MulticloudEnvironment.providerRefOf(ORACLE_CLOUD));
        assertEquals(ProviderRef.AZURE, MulticloudEnvironment.providerRefOf(AZURE));
        assertEquals("ibm", MulticloudEnvironment.providerRefOf(CloudProviderType.IBM_CLOUD).id());
        assertEquals(Optional.of(CloudProviderType.ALIBABA_CLOUD),
                MulticloudEnvironment.cloudProviderOf(ProviderRef.of("alibaba")));
        assertEquals(Optional.empty(), MulticloudEnvironment.cloudProviderOf(ProviderRef.EQUINIX));
        assertEquals(Optional.empty(), MulticloudEnvironment.cloudProviderOf(null));
    }

    private static InputStream stream(String json) {
        return new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
    }
}
