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

package com.eqixiac.equinix.design.value;

import com.eqixiac.equinix.core.model.multicloud.ProviderRef;
import com.eqixiac.equinix.design.value.ratecard.MulticloudLinkQuote;
import com.eqixiac.equinix.design.value.ratecard.MulticloudLinkRequest;
import com.eqixiac.equinix.design.value.ratecard.PriceQuote;
import com.eqixiac.equinix.design.value.ratecard.PriceSource;
import com.eqixiac.equinix.design.value.ratecard.Term;
import com.eqixiac.equinix.fabric.model.implementation.cloud.CloudProviderType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The two-sided native-link value types: {@link MulticloudLinkQuote} (unit conversion, per-side
 * optionality, the single-currency combined figure, side merging) and
 * {@link MulticloudLinkRequest} (validation, defaults, provider-id mapping).
 */
class MulticloudLinkQuoteTest {

    private static final Currency USD = Currency.getInstance("USD");
    private static final Currency EUR = Currency.getInstance("EUR");

    private static PriceQuote monthly(String amount, Currency currency) {
        return PriceQuote.monthly(new BigDecimal(amount), currency, PriceSource.CUSTOM);
    }

    private static MulticloudLinkQuote.MulticloudLinkQuoteBuilder awsToGoogle() {
        return MulticloudLinkQuote.builder()
                .providerA(CloudProviderType.AWS).providerZ(CloudProviderType.GOOGLE_CLOUD)
                .bandwidthMbps(10_000).pathTier(1);
    }

    // ── Unit conversion ──

    @Test
    void hoursPerMonthIsTheNamedConstant730() {
        assertEquals(730, MulticloudLinkQuote.HOURS_PER_MONTH, "365 x 24 / 12");
    }

    @Test
    void monthlyFromHourlyReproducesTheAwsPublishedExample() {
        // AWS pricing page (retrieved 2026-09-21): "$12.33 x 730 hours" = "$9,000.90".
        assertEquals(new BigDecimal("9000.90"), MulticloudLinkQuote.monthlyFromHourly(new BigDecimal("12.33")));
        // ...and "$51.78 x 730 hours" = "$37,799.40".
        assertEquals(new BigDecimal("37799.40"), MulticloudLinkQuote.monthlyFromHourly(new BigDecimal("51.78")));
    }

    @Test
    void monthlyFromHourlyRejectsNullAndNegative() {
        assertThrows(IllegalArgumentException.class, () -> MulticloudLinkQuote.monthlyFromHourly(null));
        assertThrows(IllegalArgumentException.class,
                () -> MulticloudLinkQuote.monthlyFromHourly(new BigDecimal("-0.01")));
        assertEquals(0, BigDecimal.ZERO.compareTo(MulticloudLinkQuote.monthlyFromHourly(BigDecimal.ZERO)));
    }

    // ── Sides and the combined figure ──

    @Test
    void combinedMonthlySumsTwoSidesInOneCurrency() {
        MulticloudLinkQuote quote = awsToGoogle()
                .sideA(monthly("9000.90", USD)).sideZ(monthly("13870.00", USD)).build();

        assertTrue(quote.isFullyPriced());
        assertFalse(quote.isMixedCurrency());
        assertEquals(0, new BigDecimal("22870.90").compareTo(quote.combinedMonthly().orElseThrow()));
        assertEquals("USD", quote.combinedCurrency().orElseThrow());
        assertEquals(0, BigDecimal.ZERO.compareTo(quote.combinedSetup().orElseThrow()));
        assertTrue(quote.unpricedSummary().isEmpty());
    }

    @Test
    void anUnpricedSideIsEmptyNotZeroAndWithholdsTheCombinedFigure() {
        MulticloudLinkQuote quote = awsToGoogle()
                .sideZ(monthly("2555.00", USD))
                .sideAUnpricedReason("no published rate for 1000 Mbps")
                .build();

        assertTrue(quote.getSideA().isEmpty(), "unpriced is absent, never a zero quote");
        assertTrue(quote.getSideZ().isPresent());
        assertFalse(quote.isFullyPriced());
        assertTrue(quote.combinedMonthly().isEmpty(), "a one-sided figure is not the link price");
        assertTrue(quote.combinedCurrency().isEmpty());
        String summary = quote.unpricedSummary().orElseThrow();
        assertTrue(summary.contains("AWS") && summary.contains("no published rate for 1000 Mbps"), summary);
    }

    @Test
    void sideLooksUpByProvider() {
        MulticloudLinkQuote quote = awsToGoogle()
                .sideA(monthly("1", USD)).sideZ(monthly("2", USD)).build();

        assertEquals(0, BigDecimal.ONE.compareTo(quote.side(CloudProviderType.AWS).orElseThrow().getMonthlyRecurring()));
        assertEquals(0, new BigDecimal("2").compareTo(
                quote.side(CloudProviderType.GOOGLE_CLOUD).orElseThrow().getMonthlyRecurring()));
        assertTrue(quote.side(CloudProviderType.AZURE).isEmpty(), "not an end of this link");
        assertTrue(quote.side(null).isEmpty());
    }

    // ── Side merging (the rule LayeredRateCard applies) ──

    @Test
    void fillUnpricedSidesKeepsPricedSidesAndTakesTheRestFromTheOtherQuote() {
        MulticloudLinkQuote negotiated = awsToGoogle()
                .sideA(monthly("8000.00", USD)).sideZUnpricedReason("no custom rate").note("n1").build();
        MulticloudLinkQuote reference = awsToGoogle()
                .sideA(monthly("9000.90", USD)).sideZ(monthly("13870.00", USD)).note("n1").note("n2").build();

        MulticloudLinkQuote merged = negotiated.fillUnpricedSidesFrom(reference);

        assertEquals(0, new BigDecimal("8000.00").compareTo(merged.getSideA().orElseThrow().getMonthlyRecurring()),
                "the earlier quote's priced side is kept");
        assertEquals(0, new BigDecimal("13870.00").compareTo(merged.getSideZ().orElseThrow().getMonthlyRecurring()));
        assertNull(merged.getSideZUnpricedReason(), "a filled side has no unpriced reason");
        assertEquals(List.of("n1", "n2"), merged.getNotes(), "notes are concatenated without duplicates");
        assertTrue(negotiated.getSideZ().isEmpty(), "the receiver is immutable");
    }

    @Test
    void fillUnpricedSidesMatchesByProviderWhenTheOtherQuoteIsReversed() {
        MulticloudLinkQuote forward = awsToGoogle().sideA(monthly("1.00", USD)).build();
        MulticloudLinkQuote reversed = MulticloudLinkQuote.builder()
                .providerA(CloudProviderType.GOOGLE_CLOUD).providerZ(CloudProviderType.AWS)
                .bandwidthMbps(10_000).pathTier(1)
                .sideA(monthly("2.00", USD)).sideZ(monthly("99.00", USD)).build();

        MulticloudLinkQuote merged = forward.fillUnpricedSidesFrom(reversed);

        assertEquals(0, new BigDecimal("1.00").compareTo(merged.side(CloudProviderType.AWS).orElseThrow()
                .getMonthlyRecurring()), "the AWS side was already priced and is kept");
        assertEquals(0, new BigDecimal("2.00").compareTo(merged.side(CloudProviderType.GOOGLE_CLOUD).orElseThrow()
                .getMonthlyRecurring()), "the Google side comes from the reversed quote's side A");
    }

    @Test
    void fillUnpricedSidesKeepsBothReasonsWhenNeitherQuotePricesASide() {
        MulticloudLinkQuote first = awsToGoogle().sideAUnpricedReason("custom: none declared").build();
        MulticloudLinkQuote second = awsToGoogle().sideAUnpricedReason("reference: not published").build();

        MulticloudLinkQuote merged = first.fillUnpricedSidesFrom(second);

        assertTrue(merged.getSideA().isEmpty());
        assertEquals("custom: none declared; reference: not published", merged.getSideAUnpricedReason());
        assertSame(first, first.fillUnpricedSidesFrom(null));
    }

    @Test
    void fillUnpricedSidesRejectsADifferentProviderPair() {
        MulticloudLinkQuote awsGoogle = awsToGoogle().build();
        MulticloudLinkQuote awsOracle = MulticloudLinkQuote.builder()
                .providerA(CloudProviderType.AWS).providerZ(CloudProviderType.ORACLE_CLOUD)
                .bandwidthMbps(10_000).pathTier(1).build();

        assertThrows(IllegalArgumentException.class, () -> awsGoogle.fillUnpricedSidesFrom(awsOracle));
    }

    // ── Request ──

    @Test
    void requestDefaultsToPathTierOneAndNoFreeTier() {
        MulticloudLinkRequest request = MulticloudLinkRequest.builder()
                .providerA(CloudProviderType.AWS).regionA("  ")
                .providerZ(CloudProviderType.GOOGLE_CLOUD).regionZ(" us-east4 ")
                .bandwidthMbps(10_000)
                .build();

        assertEquals(1, request.getPathTier());
        assertEquals(MulticloudLinkQuote.DEFAULT_PATH_TIER, request.getPathTier());
        assertFalse(request.isUseAwsFreeTier(), "the free tier is an opt-in, never a default");
        assertNull(request.getRegionA(), "a blank region is 'not stated'");
        assertEquals("us-east4", request.getRegionZ());
        assertNull(request.getTerm());
        assertTrue(request.involves(CloudProviderType.AWS));
        assertFalse(request.involves(CloudProviderType.AZURE));
    }

    @Test
    void requestBuilderFailsFastOnInvalidInput() {
        assertThrows(IllegalArgumentException.class, () -> MulticloudLinkRequest.builder()
                .providerZ(CloudProviderType.GOOGLE_CLOUD).bandwidthMbps(1000).build(), "providerA missing");
        assertThrows(IllegalArgumentException.class, () -> MulticloudLinkRequest.builder()
                .providerA(CloudProviderType.AWS).bandwidthMbps(1000).build(), "providerZ missing");
        assertThrows(IllegalArgumentException.class, () -> MulticloudLinkRequest.builder()
                .providerA(CloudProviderType.AWS).providerZ(CloudProviderType.AWS).bandwidthMbps(1000).build(),
                "a link joins two different providers");
        assertThrows(IllegalArgumentException.class, () -> MulticloudLinkRequest.builder()
                .providerA(CloudProviderType.AWS).providerZ(CloudProviderType.GOOGLE_CLOUD).build(),
                "bandwidth is required");
        assertThrows(IllegalArgumentException.class, () -> MulticloudLinkRequest.builder()
                .providerA(CloudProviderType.AWS).providerZ(CloudProviderType.GOOGLE_CLOUD)
                .bandwidthMbps(1000).pathTier(0).build());
        assertThrows(IllegalArgumentException.class, () -> MulticloudLinkRequest.builder()
                .providerA(CloudProviderType.AWS).providerZ(CloudProviderType.GOOGLE_CLOUD)
                .bandwidthMbps(1000).pathTier(6).build());
    }

    @Test
    void requestToBuilderRoundTrips() {
        MulticloudLinkRequest request = MulticloudLinkRequest.builder()
                .providerA(CloudProviderType.AWS).regionA("us-east-1")
                .providerZ(CloudProviderType.GOOGLE_CLOUD).regionZ("us-east4")
                .bandwidthMbps(10_000).pathTier(4).term(Term.MONTH_36).useAwsFreeTier(true)
                .build();

        assertEquals(request, request.toBuilder().build());
        assertEquals(5, request.toBuilder().pathTier(5).build().getPathTier());
    }

    @Test
    void providerRefMappingCoversTheFourCloudsAndNothingElse() {
        assertEquals(Optional.of(CloudProviderType.AWS), MulticloudLinkRequest.cloudProviderOf(ProviderRef.AWS));
        assertEquals(Optional.of(CloudProviderType.GOOGLE_CLOUD), MulticloudLinkRequest.cloudProviderOf(ProviderRef.GCP));
        assertEquals(Optional.of(CloudProviderType.ORACLE_CLOUD), MulticloudLinkRequest.cloudProviderOf(ProviderRef.OCI));
        assertEquals(Optional.of(CloudProviderType.AZURE), MulticloudLinkRequest.cloudProviderOf(ProviderRef.AZURE));
        assertTrue(MulticloudLinkRequest.cloudProviderOf(ProviderRef.EQUINIX).isEmpty(),
                "Equinix is not a cloud the value layer prices egress for");
        assertTrue(MulticloudLinkRequest.cloudProviderOf(ProviderRef.of("example")).isEmpty());
        assertTrue(MulticloudLinkRequest.cloudProviderOf(null).isEmpty());

        for (CloudProviderType cloud : CloudProviderType.values()) {
            MulticloudLinkRequest.providerRefOf(cloud).ifPresent(ref ->
                    assertEquals(Optional.of(cloud), MulticloudLinkRequest.cloudProviderOf(ref),
                            "the two mappings are inverses for " + cloud));
        }
        assertTrue(MulticloudLinkRequest.providerRefOf(CloudProviderType.IBM_CLOUD).isEmpty());
        assertTrue(MulticloudLinkRequest.providerRefOf(null).isEmpty());
    }

    // The two-currency cases live in MulticloudCurrencyMixingTest.
    @Test
    void mixedCurrencyFlagIsFalseForOneCurrency() {
        assertFalse(awsToGoogle().sideA(monthly("1", EUR)).sideZ(monthly("2", EUR)).build().isMixedCurrency());
    }
}
