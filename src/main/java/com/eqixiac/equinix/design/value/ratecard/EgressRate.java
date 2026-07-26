package com.eqixiac.equinix.design.value.ratecard;

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
 *
 * <p>The price is per <em>decimal</em> (SI) gigabyte — 1&nbsp;TB = 1000&nbsp;GB —
 * matching {@code DataUnit} and how the engines express volume. A provider list
 * price quoted per Gi<em>bi</em>byte must be converted before being supplied here
 * (the bundled GCP adapter divides its per-GiB figure by 1.073741824 and records
 * the original in the note).</p>
 */
@Value
@Builder
public class EgressRate {

    /** The price per decimal (SI) gigabyte of egress. */
    BigDecimal pricePerGb;

    /** The currency the per-GB price is expressed in. */
    Currency currency;

    /** Where this rate came from — see {@link PriceSource} for the trust spectrum. */
    PriceSource source;

    /**
     * Optional human-readable provenance detail — e.g. the source meter/SKU name, the
     * pricing tier the figure represents, or an original per-GiB list price before
     * conversion. May be {@code null}.
     */
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

    /**
     * Returns a copy of this rate with the note replaced (this instance is immutable and
     * unchanged). The provider adapters use it to record where a figure came from — e.g.
     * the meter name or the original per-GiB price a converted rate was derived from.
     *
     * @param note the provenance note to attach (replaces any existing note)
     * @return a new rate identical to this one but carrying the given note
     */
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
