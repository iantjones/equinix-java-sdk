package api.equinix.javasdk.design.value.ratecard;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.util.Currency;

/**
 * A usage-based data-egress price expressed per gigabyte, tagged with its
 * {@link PriceSource}. Unlike {@link PriceQuote} (which models recurring and
 * one-time charges for provisioned resources), an egress rate is multiplied by a
 * monthly data volume to produce a cost, so it is the unit that drives the
 * internet-vs-private savings calculation.
 */
@Value
@Builder
public class EgressRate {

    /** Price per gigabyte of egress. */
    BigDecimal pricePerGb;

    /** Currency of {@link #pricePerGb}. */
    Currency currency;

    /** Where this rate came from. */
    PriceSource source;

    /** Optional note (e.g. the source tier or a staleness caveat). */
    String note;

    /**
     * Creates an egress rate.
     *
     * @param pricePerGb the per-GB price (defaults to zero if null)
     * @param currency   the currency
     * @param source     the provenance
     * @return a new egress rate
     */
    public static EgressRate of(BigDecimal pricePerGb, Currency currency, PriceSource source) {
        return EgressRate.builder()
                .pricePerGb(pricePerGb == null ? BigDecimal.ZERO : pricePerGb)
                .currency(currency)
                .source(source)
                .build();
    }

    /** Returns a copy of this rate with the supplied note attached. */
    public EgressRate withNote(String note) {
        return EgressRate.builder()
                .pricePerGb(pricePerGb)
                .currency(currency)
                .source(source)
                .note(note)
                .build();
    }

    /**
     * Computes the egress cost for a given monthly volume.
     *
     * @param gigabytes the monthly egress volume in GB
     * @return {@code pricePerGb × gigabytes}
     */
    public BigDecimal costFor(BigDecimal gigabytes) {
        return pricePerGb.multiply(gigabytes == null ? BigDecimal.ZERO : gigabytes);
    }
}
