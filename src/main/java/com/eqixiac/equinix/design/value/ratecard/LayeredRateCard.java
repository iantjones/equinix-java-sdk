package com.eqixiac.equinix.design.value.ratecard;

import com.eqixiac.equinix.core.enums.MetroCode;
import com.eqixiac.equinix.fabric.enums.ConnectionType;
import com.eqixiac.equinix.fabric.model.implementation.cloud.CloudProviderType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A {@link RateCard} that delegates to an ordered list of underlying cards and
 * returns the first non-empty quote. This is how the SDK blends caller-supplied
 * rates, live Equinix pricing, and bundled reference figures into one resolver
 * with a clear precedence: the earliest card that can price an item wins.
 *
 * <p>{@code multicloudLink(...)} applies the same precedence per side, because each side of a
 * native multicloud link is a separate price from a separate provider: the earliest card that
 * prices a side supplies it, and a side no card prices stays empty with every card's reason.
 * A caller can therefore declare one negotiated side on a {@code CustomRateCard} and take the
 * other side from the reference card.</p>
 *
 * <p>Instances are created via {@link RateCard#layered(RateCard...)}.</p>
 */
final class LayeredRateCard implements RateCard {

    private final List<RateCard> cards;

    LayeredRateCard(List<RateCard> cards) {
        this.cards = new ArrayList<>();
        if (cards != null) {
            for (RateCard card : cards) {
                if (card != null) {
                    this.cards.add(card);
                }
            }
        }
    }

    @Override
    public Optional<PriceQuote> connection(ConnectionType type, int bandwidthMbps, MetroCode metro, Term term) {
        for (RateCard card : cards) {
            Optional<PriceQuote> quote = card.connection(type, bandwidthMbps, metro, term);
            if (quote.isPresent()) {
                return quote;
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<PriceQuote> cloudRouter(String packageCode, MetroCode metro, Term term) {
        for (RateCard card : cards) {
            Optional<PriceQuote> quote = card.cloudRouter(packageCode, metro, term);
            if (quote.isPresent()) {
                return quote;
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<EgressRate> egress(CloudProviderType provider, String region, EgressPath path, Term term) {
        for (RateCard card : cards) {
            Optional<EgressRate> rate = card.egress(provider, region, path, term);
            if (rate.isPresent()) {
                return rate;
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<PriceQuote> colocation(ColocationItem item, MetroCode metro, Term term) {
        for (RateCard card : cards) {
            Optional<PriceQuote> quote = card.colocation(item, metro, term);
            if (quote.isPresent()) {
                return quote;
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<MulticloudLinkQuote> multicloudLink(MulticloudLinkRequest request) {
        MulticloudLinkQuote merged = null;
        for (RateCard card : cards) {
            Optional<MulticloudLinkQuote> quote = card.multicloudLink(request);
            if (quote.isEmpty()) {
                continue;
            }
            merged = merged == null ? quote.get() : merged.fillUnpricedSidesFrom(quote.get());
            if (merged.isFullyPriced()) {
                break;
            }
        }
        return Optional.ofNullable(merged);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Consults each layer's {@code connectionLookup} in order; the trail says which layer
     * answered and why each earlier layer declined.</p>
     */
    @Override
    public PriceLookup<PriceQuote> connectionLookup(ConnectionType type, int bandwidthMbps, MetroCode metro,
                                                    Term term) {
        return layered(card -> card.connectionLookup(type, bandwidthMbps, metro, term));
    }

    /** {@inheritDoc} Same layering and trail as {@link #connectionLookup}. */
    @Override
    public PriceLookup<PriceQuote> cloudRouterLookup(String packageCode, MetroCode metro, Term term) {
        return layered(card -> card.cloudRouterLookup(packageCode, metro, term));
    }

    /** {@inheritDoc} Same layering and trail as {@link #connectionLookup}. */
    @Override
    public PriceLookup<EgressRate> egressLookup(CloudProviderType provider, String region, EgressPath path,
                                                Term term) {
        return layered(card -> card.egressLookup(provider, region, path, term));
    }

    /** {@inheritDoc} Same layering and trail as {@link #connectionLookup}. */
    @Override
    public PriceLookup<PriceQuote> colocationLookup(ColocationItem item, MetroCode metro, Term term) {
        return layered(card -> card.colocationLookup(item, metro, term));
    }

    @Override
    public String cardName() {
        List<String> names = new ArrayList<>();
        for (RateCard card : cards) {
            names.add(card.cardName());
        }
        return "Layered[" + String.join(" > ", names) + "]";
    }

    private <T> PriceLookup<T> layered(java.util.function.Function<RateCard, PriceLookup<T>> lookup) {
        List<PriceLookup<T>> results = new ArrayList<>();
        List<String> names = new ArrayList<>();
        for (RateCard card : cards) {
            PriceLookup<T> result = lookup.apply(card);
            results.add(result);
            names.add(card.cardName());
            if (result.isPriced()) {
                break;
            }
        }
        return PriceLookup.firstPriced(results, names);
    }

    @Override
    public PriceSource source() {
        return PriceSource.COMPOSITE;
    }
}
