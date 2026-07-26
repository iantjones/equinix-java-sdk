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

import com.eqixiac.equinix.fabric.model.implementation.cloud.CloudProviderType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Locks down {@link CloudProviderType#matchesServiceProfileName(String)}, the lookup the Metro
 * Optimizer resolves a {@code requireProvider(CloudProviderType)} requirement through.
 *
 * <p><strong>Regression context.</strong> The engine used to match a provider requirement against
 * the <em>corporate</em> name only ({@code CloudProviderType.AWS.getProviderName()} ==
 * "Amazon Web Services"). Real Fabric marketplace profiles are named after the <em>product</em>
 * ("AWS Direct Connect", "Azure ExpressRoute"), so the lookup resolved to nothing, the
 * provider-to-metro index came back empty, and every metro was then dropped by the
 * required-provider filter — observed live as "Analyzed 67 metros, 0 met constraints" with zero
 * recommendations.</p>
 *
 * <p><strong>Second regression.</strong> The first fix bought that false negative back with
 * uncharacterized false positives: bare generic aliases ("direct connect", "direct link",
 * "microsoft") plus <em>bidirectional</em> matching meant "Microsoft 365" resolved to AZURE,
 * "&lt;NSP&gt; Direct Connect" resolved to AWS, and sub-phrases such as "Web Services" or
 * "Cloud Platform" resolved to AWS and GOOGLE_CLOUD. A false positive is the worse error here: it
 * silently claims a cloud is reachable from a metro where it is not, and the whole recommendation
 * is then built on that claim. Matching is now one-directional and restricted to terms distinctive
 * to the provider.</p>
 *
 * <p>This suite asserts three things: every realistic profile name still resolves to exactly its
 * own provider (including the seller-prefixed and region-suffixed shapes Fabric actually emits),
 * nothing else resolves, and each specific false positive the reviewers found is now an asserted
 * negative. The engine-level consequences are covered by
 * {@link MetroOptimizerProviderResolutionTest}.</p>
 */
@DisplayName("CloudProviderType service-profile name matching")
class CloudProviderProfileMatchingTest {

    /** Every provider constant that claims the given profile name. */
    private static List<CloudProviderType> matching(String profileName) {
        return Arrays.stream(CloudProviderType.values())
                .filter(p -> p.matchesServiceProfileName(profileName))
                .collect(Collectors.toList());
    }

    // ══════════════════════════════════════════════════════════════════
    //  1. True positives — real profile names resolve, exclusively
    // ══════════════════════════════════════════════════════════════════

    @ParameterizedTest(name = "\"{0}\" resolves to {1} and to no other provider")
    @CsvSource({
            "'AWS Direct Connect',                AWS",
            "'Azure ExpressRoute',                AZURE",
            "'Google Cloud Partner Interconnect', GOOGLE_CLOUD",
            "'Oracle FastConnect',                ORACLE_CLOUD",
            "'IBM Cloud Direct Link',             IBM_CLOUD",
            "'Alibaba Express Connect',           ALIBABA_CLOUD"
    })
    @DisplayName("a product-named profile resolves to exactly its own provider")
    void productNamedProfilesResolveToTheirProvider(String profileName, CloudProviderType expected) {
        // Before the first fix every one of these returned no match at all: none of them contains
        // the corporate name the matcher used to compare against.
        assertEquals(List.of(expected), matching(profileName),
                "'" + profileName + "' must resolve to " + expected + " and nothing else");
    }

    @ParameterizedTest(name = "\"{0}\" still resolves to {1}")
    @CsvSource({
            "'Amazon Web Services Direct Connect', AWS",
            "'Microsoft Azure',                    AZURE",
            "'Google Cloud Platform',              GOOGLE_CLOUD",
            "'Oracle Cloud Infrastructure',        ORACLE_CLOUD",
            "'IBM Cloud',                          IBM_CLOUD",
            "'Alibaba Cloud',                      ALIBABA_CLOUD"
    })
    @DisplayName("corporate-named profiles keep resolving: the alias fix is additive, not a swap")
    void corporateNamedProfilesStillResolve(String profileName, CloudProviderType expected) {
        assertEquals(List.of(expected), matching(profileName),
                "'" + profileName + "' must still resolve to " + expected + " and nothing else");
    }

    @Test
    @DisplayName("the fixture spelling 'Amazon Web Services Direct Connect' resolves to AWS")
    void fixtureProfileNameResolvesToAws() {
        // Pinned separately because three pre-existing optimizer/MCP suites stub the AWS service
        // profile with exactly this name. Tightening the alias set must never reach it: it matches
        // on the corporate token "amazon", which survives every narrowing in this file.
        assertEquals(List.of(CloudProviderType.AWS), matching("Amazon Web Services Direct Connect"));
        assertTrue(CloudProviderType.AWS.matchesServiceProfileName("Amazon Web Services Direct Connect"));
    }

    @ParameterizedTest(name = "\"{0}\" resolves to {1} despite case and punctuation")
    @CsvSource({
            "'aws direct connect',                       AWS",
            "'AWS-Direct-Connect',                       AWS",
            "'AWS   Direct   Connect',                   AWS",
            "'Azure ExpressRoute (Seller Hosted)',       AZURE",
            "'Azure Express Route',                      AZURE",
            "'Google Cloud Dedicated Interconnect',      GOOGLE_CLOUD",
            "'Oracle Cloud Infrastructure -FastConnect', ORACLE_CLOUD"
    })
    @DisplayName("matching is case-insensitive and collapses punctuation into token boundaries")
    void matchingNormalizesCaseAndPunctuation(String profileName, CloudProviderType expected) {
        assertEquals(List.of(expected), matching(profileName));
    }

    @ParameterizedTest(name = "\"{0}\" resolves to {1}")
    @CsvSource({
            "'Equinix AWS Direct Connect',                    AWS",
            "'AWS Direct Connect - Sydney',                   AWS",
            "'AWS Direct Connect (Hosted)',                   AWS",
            "'Microsoft Azure ExpressRoute',                  AZURE",
            "'Azure ExpressRoute - Silicon Valley',           AZURE",
            "'Google Cloud Partner Interconnect Zone 1',      GOOGLE_CLOUD",
            "'Oracle Cloud Infrastructure FastConnect - DC1', ORACLE_CLOUD",
            "'IBM Cloud Direct Link 2.0',                     IBM_CLOUD",
            "'Alibaba Cloud Express Connect',                 ALIBABA_CLOUD"
    })
    @DisplayName("seller prefixes and region/zone suffixes do not break resolution")
    void sellerPrefixedAndSuffixedNamesResolve(String profileName, CloudProviderType expected) {
        // Fabric profile names are decorated in the marketplace. Narrowing the alias set must not
        // narrow it to exact-name equality.
        assertEquals(List.of(expected), matching(profileName),
                "'" + profileName + "' must resolve to " + expected + " and nothing else");
    }

    @ParameterizedTest
    @EnumSource(value = CloudProviderType.class, mode = EnumSource.Mode.EXCLUDE, names = "OTHER")
    @DisplayName("every provider matches its own product name, corporate name, and constant name")
    void everyProviderMatchesItsOwnNames(CloudProviderType provider) {
        assertFalse(provider.getMatchTerms().isEmpty(), provider + " must carry match terms");
        assertTrue(provider.matchesServiceProfileName(provider.getDisplayName()),
                provider + " must match its Fabric product name '" + provider.getDisplayName() + "'");
        assertTrue(provider.matchesServiceProfileName(provider.getProviderName()),
                provider + " must match its corporate name '" + provider.getProviderName() + "'");
        assertTrue(provider.matchesServiceProfileName(provider.name()),
                provider + " must match its own constant name");
    }

    @ParameterizedTest
    @EnumSource(value = CloudProviderType.class, mode = EnumSource.Mode.EXCLUDE, names = "OTHER")
    @DisplayName("no provider's own product name is claimed by a second provider")
    void productNamesAreUnambiguousAcrossProviders(CloudProviderType provider) {
        assertEquals(List.of(provider), matching(provider.getDisplayName()),
                provider.getDisplayName() + " must be claimed by " + provider + " alone");
        assertEquals(List.of(provider), matching(provider.getProviderName()),
                provider.getProviderName() + " must be claimed by " + provider + " alone");
    }

    // ── OTHER is a placeholder, never a nameable provider ──

    @ParameterizedTest
    @ValueSource(strings = {"Custom Provider", "Other", "OTHER", "AWS Direct Connect", "Bespoke Seller Cloud"})
    @DisplayName("OTHER matches nothing at all - not even its own display name")
    void otherMatchesNothing(String profileName) {
        assertFalse(CloudProviderType.OTHER.matchesServiceProfileName(profileName),
                "OTHER is a placeholder and must never claim '" + profileName + "'");
        assertTrue(CloudProviderType.OTHER.getMatchTerms().isEmpty());
    }

    @ParameterizedTest
    @EnumSource(CloudProviderType.class)
    @DisplayName("null, empty, and punctuation-only names never match any provider")
    void degenerateNamesNeverMatch(CloudProviderType provider) {
        assertFalse(provider.matchesServiceProfileName(null));
        assertFalse(provider.matchesServiceProfileName(""));
        assertFalse(provider.matchesServiceProfileName("   -  "));
    }

    // ══════════════════════════════════════════════════════════════════
    //  2. False negatives that must stay fixed: no mid-word matching
    // ══════════════════════════════════════════════════════════════════

    @ParameterizedTest
    @ValueSource(strings = {
            "Warsaw",                       // contains "aws" mid-word
            "Warsaw Data Center",           // ditto, multi-token
            "Kawasaki",                     // ditto
            "Sawstone Networks",            // contains "aws" mid-word
            "Associated Carrier Ethernet",  // contains "oci" mid-word
            "Gcpnet Global Transit",        // contains "gcp" mid-word
            "Digital Realty Interconnect",  // "interconnect" alone is not a term
            "Megaport Cloud Router",
            "Colt IP Access",
            "Equinix Fabric Cloud Router"
    })
    @DisplayName("names that merely contain a provider alias mid-word resolve to no provider")
    void aliasesMatchOnWholeTokensOnly(String profileName) {
        // Matching is token-aware, not a naive substring test: the Warsaw (WA) metro and any
        // profile named after it must not satisfy a required-AWS constraint.
        assertTrue(matching(profileName).isEmpty(),
                "'" + profileName + "' must resolve to no provider, got " + matching(profileName));
    }

    @ParameterizedTest
    @CsvSource({
            "'IBM Cloud Direct Link',             AWS",           // "direct link" != "direct connect"
            "'Alibaba Express Connect',           AZURE",         // "express connect" != "express route"
            "'Google Cloud Partner Interconnect', ORACLE_CLOUD",
            "'AWS Direct Connect',                IBM_CLOUD",
            "'Oracle FastConnect',                GOOGLE_CLOUD"
    })
    @DisplayName("near-miss product names do not cross-match a neighbouring provider")
    void nearMissProductNamesDoNotCrossMatch(String profileName, CloudProviderType wrongProvider) {
        assertFalse(wrongProvider.matchesServiceProfileName(profileName),
                "'" + profileName + "' must not resolve to " + wrongProvider);
    }

    // ══════════════════════════════════════════════════════════════════
    //  3. False positives the adversarial review found — now negatives
    // ══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("a generic name is never evidence that a cloud is reachable")
    class NoGenericFalsePositives {

        @ParameterizedTest
        @ValueSource(strings = {
                "Microsoft 365",
                "Microsoft 365 - Seller Hosted",
                "Microsoft Teams",
                "Microsoft Peering Service"
        })
        @DisplayName("the bare corporate token 'Microsoft' does not resolve to AZURE")
        void bareMicrosoftDoesNotResolveToAzure(String profileName) {
            // Microsoft's Fabric presence is wider than ExpressRoute. "Microsoft 365" is its own
            // profile and reaching it says nothing about an Azure on-ramp in that metro.
            assertFalse(CloudProviderType.AZURE.matchesServiceProfileName(profileName),
                    "'" + profileName + "' must not resolve to AZURE");
            assertTrue(matching(profileName).isEmpty(),
                    "'" + profileName + "' must resolve to no provider, got " + matching(profileName));
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "Direct Connect",
                "Megaport Direct Connect",
                "Lumen Direct Connect",
                "PacketFabric Direct Connect",
                "Console Connect Direct Connect"
        })
        @DisplayName("a seller-branded 'Direct Connect' does not resolve to AWS")
        void sellerBrandedDirectConnectDoesNotResolveToAws(String profileName) {
            // "Direct Connect" is how many network service providers brand their own on-ramp.
            // Treating it as AWS evidence would report AWS reachable in metros where it is not.
            assertFalse(CloudProviderType.AWS.matchesServiceProfileName(profileName),
                    "'" + profileName + "' must not resolve to AWS");
            assertTrue(matching(profileName).isEmpty(),
                    "'" + profileName + "' must resolve to no provider, got " + matching(profileName));
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "Direct Link",
                "Colt Direct Link",
                "Console Connect Direct Link"
        })
        @DisplayName("a seller-branded 'Direct Link' does not resolve to IBM_CLOUD")
        void sellerBrandedDirectLinkDoesNotResolveToIbm(String profileName) {
            assertFalse(CloudProviderType.IBM_CLOUD.matchesServiceProfileName(profileName),
                    "'" + profileName + "' must not resolve to IBM_CLOUD");
            assertTrue(matching(profileName).isEmpty(),
                    "'" + profileName + "' must resolve to no provider, got " + matching(profileName));
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "Cloud Interconnect",
                "Partner Interconnect",
                "Dedicated Interconnect",
                "Equinix Partner Interconnect"
        })
        @DisplayName("generic interconnection wording does not resolve to GOOGLE_CLOUD")
        void genericInterconnectWordingDoesNotResolveToGoogle(String profileName) {
            assertFalse(CloudProviderType.GOOGLE_CLOUD.matchesServiceProfileName(profileName),
                    "'" + profileName + "' must not resolve to GOOGLE_CLOUD");
            assertTrue(matching(profileName).isEmpty(),
                    "'" + profileName + "' must resolve to no provider, got " + matching(profileName));
        }

        @ParameterizedTest
        @ValueSource(strings = {"Express Connect", "Seller Express Connect"})
        @DisplayName("a bare 'Express Connect' does not resolve to ALIBABA_CLOUD")
        void bareExpressConnectDoesNotResolveToAlibaba(String profileName) {
            assertFalse(CloudProviderType.ALIBABA_CLOUD.matchesServiceProfileName(profileName),
                    "'" + profileName + "' must not resolve to ALIBABA_CLOUD");
            assertTrue(matching(profileName).isEmpty(),
                    "'" + profileName + "' must resolve to no provider, got " + matching(profileName));
        }
    }

    @Test
    @DisplayName("matching is one-directional: a sub-phrase of a provider term does not resolve")
    void subPhrasesOfProviderTermsDoNotResolve() {
        // Matching used to run in BOTH directions, so any multi-token name that was itself a
        // sub-phrase of one of a provider's terms resolved to that provider. These four were the
        // documented consequences; all are now negatives. The profile name must contain a
        // distinctive term, never the other way round.
        assertTrue(matching("Web Services").isEmpty(),
                "'Web Services' is a sub-phrase of 'Amazon Web Services', not evidence of AWS");
        assertTrue(matching("Cloud Platform").isEmpty(),
                "'Cloud Platform' is a sub-phrase of 'Google Cloud Platform', not evidence of GCP");
        assertTrue(matching("Cloud Infrastructure").isEmpty(),
                "'Cloud Infrastructure' is a sub-phrase of 'Oracle Cloud Infrastructure', not evidence of OCI");
        assertTrue(matching("Direct Connect").isEmpty(),
                "'Direct Connect' is a sub-phrase of 'AWS Direct Connect', not evidence of AWS");

        // Single-token names were already safe and stay so.
        assertTrue(matching("Cloud").isEmpty());
        assertTrue(matching("Services").isEmpty());
        assertTrue(matching("Connect").isEmpty());
        assertTrue(matching("Interconnect").isEmpty());
    }

    @ParameterizedTest
    @EnumSource(CloudProviderType.class)
    @DisplayName("no provider carries a generic industry phrase as a standalone match term")
    void noProviderCarriesAGenericTermStandalone(CloudProviderType provider) {
        // Structural guard: the alias lists are hand-curated, so this pins the curation rule rather
        // than any one spelling. A term here is only ever a whole normalized phrase, so an exact
        // comparison is the right test — "aws direct connect" is fine, a bare "direct connect" is
        // not, because it would match every seller profile carrying those two words.
        List<String> generic = List.of(
                "direct connect", "direct link", "express connect", "cloud interconnect",
                "partner interconnect", "dedicated interconnect", "interconnect", "microsoft",
                "cloud", "connect", "web services", "cloud platform", "cloud infrastructure");
        for (String term : provider.getMatchTerms()) {
            assertFalse(generic.contains(term),
                    provider + " must not carry the generic term '" + term + "': it would claim "
                            + "any seller profile containing those words");
        }
    }
}
