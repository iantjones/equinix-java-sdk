package api.equinix.javasdk.design.value.ratecard;

import api.equinix.javasdk.core.enums.MetroCode;
import api.equinix.javasdk.fabric.enums.ConnectionType;
import api.equinix.javasdk.fabric.model.implementation.cloud.CloudProviderType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A {@link RateCard} that delegates to an ordered list of underlying cards and
 * returns the first non-empty quote. This is how the SDK blends caller-supplied
 * rates, live Equinix pricing, and bundled reference figures into one resolver
 * with a clear precedence: the earliest card that can price an item wins.
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
    public PriceSource source() {
        return PriceSource.COMPOSITE;
    }
}
