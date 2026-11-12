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

package com.eqixiac.equinix.design.cost;

import com.eqixiac.equinix.core.enums.MetroCode;
import com.eqixiac.equinix.design.spi.CurrencyConverter;
import com.eqixiac.equinix.design.value.ratecard.ColocationItem;
import com.eqixiac.equinix.design.value.ratecard.EgressPath;
import com.eqixiac.equinix.design.value.ratecard.EgressRate;
import com.eqixiac.equinix.design.value.ratecard.MulticloudLinkQuote;
import com.eqixiac.equinix.design.value.ratecard.MulticloudLinkRequest;
import com.eqixiac.equinix.design.value.ratecard.PriceLookup;
import com.eqixiac.equinix.design.value.ratecard.PriceQuote;
import com.eqixiac.equinix.design.value.ratecard.PriceSource;
import com.eqixiac.equinix.design.value.ratecard.RateCard;
import com.eqixiac.equinix.design.value.ratecard.Term;
import com.eqixiac.equinix.fabric.enums.ConnectionType;
import com.eqixiac.equinix.fabric.model.implementation.cloud.CloudProviderType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Optional;

/**
 * A {@link RateCard} that answers every lookup in one target currency: a figure quoted in another currency is
 * converted through a {@link CurrencyConverter} and its note says from what, at which rate, from which source
 * and as of when. A figure whose currency no source covers is unpriced with that reason. The TCO and savings
 * models wrap their card in one so they compare in the same currency, with the same rates, as the optimiser's
 * {@link RateCardCostModel}. A figure converted at a bundled reference rate is tagged
 * {@link PriceSource#ESTIMATE}.
 */
public final class ConvertingRateCard implements RateCard {

    private final RateCard card;
    private final String target;
    private final CurrencyConverters.Chain converter;

    private ConvertingRateCard(RateCard card, String target, CurrencyConverters.Chain converter) {
        this.card = card;
        this.target = target;
        this.converter = converter;
    }

    /**
     * @param card      the card to wrap
     * @param target    the ISO 4217 code every answer is given in
     * @param converter the converter
     * @return the converting card
     */
    public static ConvertingRateCard of(RateCard card, String target, CurrencyConverters.Chain converter) {
        if (card == null || target == null || converter == null) {
            throw new IllegalArgumentException("A converting card needs a card, a target currency and a converter");
        }
        return new ConvertingRateCard(card, target, converter);
    }

    /** @return the target currency */
    public String target() {
        return target;
    }

    /** @return the converter (for its fallback notes) */
    public CurrencyConverters.Chain converter() {
        return converter;
    }

    /**
     * Converts a quote to the target currency.
     *
     * @param lookup a lookup in any currency
     * @return the lookup in the target currency, or unpriced when no source has the rate
     */
    public PriceLookup<PriceQuote> toTarget(PriceLookup<PriceQuote> lookup) {
        if (!lookup.isPriced()) {
            return lookup;
        }
        PriceQuote q = lookup.value();
        String from = q.getCurrency().getCurrencyCode();
        if (from.equals(target)) {
            return lookup;
        }
        Optional<CurrencyConverter.Rate> rate = converter.rate(from, target);
        if (rate.isEmpty()) {
            return PriceLookup.unpriced("priced in " + from + " " + q.getMonthlyRecurring().toPlainString()
                    + "/month; " + converter.missingReason(from, target));
        }
        CurrencyConverter.Rate r = rate.get();
        PriceQuote converted = PriceQuote.builder().monthlyRecurring(r.apply(q.getMonthlyRecurring()))
                .nonRecurring(r.apply(q.getNonRecurring())).currency(Currency.getInstance(target))
                .source(r.kind() == CurrencyConverter.Kind.REFERENCE ? PriceSource.ESTIMATE : q.getSource())
                .note((q.getNote() == null ? "" : q.getNote() + "; ") + describe(r, from, q.getMonthlyRecurring()))
                .build();
        return new PriceLookup<>(converted, null, lookup.answeredBy(), lookup.trail());
    }

    /**
     * Converts an amount to the target currency.
     *
     * @param amount   the amount
     * @param currency its ISO 4217 code
     * @return the converted amount, or empty when no source has the rate
     */
    public Optional<BigDecimal> amount(BigDecimal amount, String currency) {
        if (currency.equals(target)) {
            return Optional.of(amount);
        }
        return converter.rate(currency, target).map(r -> r.apply(amount));
    }

    private static String describe(CurrencyConverter.Rate r, String from, BigDecimal original) {
        return "converted from " + from + " " + original.toPlainString() + " at "
                + r.rate().stripTrailingZeros().toPlainString() + " " + r.to() + "/" + from + " (" + r.source()
                + ", as of " + r.asOf() + ")";
    }

    private PriceLookup<EgressRate> egressToTarget(PriceLookup<EgressRate> lookup) {
        if (!lookup.isPriced()) {
            return lookup;
        }
        EgressRate e = lookup.value();
        String from = e.getCurrency().getCurrencyCode();
        if (from.equals(target)) {
            return lookup;
        }
        Optional<CurrencyConverter.Rate> rate = converter.rate(from, target);
        if (rate.isEmpty()) {
            return PriceLookup.unpriced("priced in " + from + " " + e.getPricePerGb().toPlainString() + "/GB; "
                    + converter.missingReason(from, target));
        }
        CurrencyConverter.Rate r = rate.get();
        EgressRate converted = EgressRate.builder()
                .pricePerGb(e.getPricePerGb().multiply(r.rate()).setScale(6, RoundingMode.HALF_UP))
                .currency(Currency.getInstance(target))
                .source(r.kind() == CurrencyConverter.Kind.REFERENCE ? PriceSource.ESTIMATE : e.getSource())
                .note((e.getNote() == null ? "" : e.getNote() + "; ") + "per-GB rate " + describe(r, from,
                        e.getPricePerGb()))
                .build();
        return new PriceLookup<>(converted, null, lookup.answeredBy(), lookup.trail());
    }

    @Override
    public Optional<PriceQuote> connection(ConnectionType type, int bandwidthMbps, MetroCode metro, Term term) {
        return connectionLookup(type, bandwidthMbps, metro, term).toOptional();
    }

    @Override
    public PriceLookup<PriceQuote> connectionLookup(ConnectionType type, int bandwidthMbps, MetroCode metro,
                                                    Term term) {
        return toTarget(card.connectionLookup(type, bandwidthMbps, metro, term));
    }

    @Override
    public Optional<PriceQuote> cloudRouter(String packageCode, MetroCode metro, Term term) {
        return cloudRouterLookup(packageCode, metro, term).toOptional();
    }

    @Override
    public PriceLookup<PriceQuote> cloudRouterLookup(String packageCode, MetroCode metro, Term term) {
        return toTarget(card.cloudRouterLookup(packageCode, metro, term));
    }

    @Override
    public Optional<EgressRate> egress(CloudProviderType provider, String region, EgressPath path, Term term) {
        return egressLookup(provider, region, path, term).toOptional();
    }

    @Override
    public PriceLookup<EgressRate> egressLookup(CloudProviderType provider, String region, EgressPath path,
                                                Term term) {
        return egressToTarget(card.egressLookup(provider, region, path, term));
    }

    @Override
    public Optional<PriceQuote> colocation(ColocationItem item, MetroCode metro, Term term) {
        return colocationLookup(item, metro, term).toOptional();
    }

    @Override
    public PriceLookup<PriceQuote> colocationLookup(ColocationItem item, MetroCode metro, Term term) {
        return toTarget(card.colocationLookup(item, metro, term));
    }

    @Override
    public Optional<MulticloudLinkQuote> multicloudLink(MulticloudLinkRequest request) {
        Optional<MulticloudLinkQuote> found = card.multicloudLink(request);
        if (found.isEmpty()) {
            return found;
        }
        MulticloudLinkQuote q = found.get();
        MulticloudLinkQuote.MulticloudLinkQuoteBuilder b = q.toBuilder();
        List<PriceLookup<PriceQuote>> sides = new ArrayList<>();
        for (PriceQuote side : new PriceQuote[]{q.getSideA().orElse(null), q.getSideZ().orElse(null)}) {
            sides.add(side == null ? null : toTarget(PriceLookup.priced(side, card.cardName())));
        }
        if (sides.get(0) != null) {
            b.sideA(sides.get(0).value()).sideAUnpricedReason(sides.get(0).unpricedReason());
        }
        if (sides.get(1) != null) {
            b.sideZ(sides.get(1).value()).sideZUnpricedReason(sides.get(1).unpricedReason());
        }
        return Optional.of(b.build());
    }

    @Override
    public PriceSource source() {
        return card.source();
    }

    @Override
    public String cardName() {
        return card.cardName() + " in " + target;
    }
}
