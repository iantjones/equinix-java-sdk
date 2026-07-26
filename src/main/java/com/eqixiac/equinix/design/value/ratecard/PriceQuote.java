package com.eqixiac.equinix.design.value.ratecard;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Objects;

/**
 * An immutable price for a single resource, split into its monthly-recurring
 * (MRC) and non-recurring (one-time setup, NRC) components, tagged with the
 * {@link PriceSource} it was resolved from.
 *
 * <p>All monetary values use {@link BigDecimal} to avoid floating-point drift,
 * consistent with the rest of the SDK's billing models. Quotes are additive via
 * {@link #plus(PriceQuote)} so an engine can aggregate per-resource quotes into a
 * deployment total while preserving provenance.</p>
 */
@Value
@Builder
public class PriceQuote {

    /** The monthly recurring charge (MRC), never {@code null} on quotes built via the factories. */
    BigDecimal monthlyRecurring;

    /** The one-time setup charge (NRC), never {@code null} on quotes built via the factories. */
    BigDecimal nonRecurring;

    /** The currency both amounts are expressed in. */
    Currency currency;

    /** Where this quote came from — see {@link PriceSource} for the trust spectrum. */
    PriceSource source;

    /**
     * Optional human-readable provenance detail — e.g. the Equinix price code, a
     * {@code "STANDARD substituted for PREMIUM"} label, or an {@code EXTRAPOLATED}
     * tag on a figure derived beyond the tabulated data. Engines surface it in
     * reports so a substituted or extrapolated figure is never mistaken for an
     * exact one. May be {@code null}; dropped by {@link #plus(PriceQuote)}.
     */
    String note;

    /**
     * Creates a quote with explicit MRC and NRC.
     *
     * @param monthlyRecurring the monthly recurring charge (defaults to zero if null)
     * @param nonRecurring     the one-time setup charge (defaults to zero if null)
     * @param currency         the currency of both amounts
     * @param source           the provenance of this quote
     * @return a new quote
     */
    public static PriceQuote of(BigDecimal monthlyRecurring, BigDecimal nonRecurring,
                                Currency currency, PriceSource source) {
        return PriceQuote.builder()
                .monthlyRecurring(monthlyRecurring == null ? BigDecimal.ZERO : monthlyRecurring)
                .nonRecurring(nonRecurring == null ? BigDecimal.ZERO : nonRecurring)
                .currency(currency)
                .source(source)
                .build();
    }

    /**
     * Creates a monthly-only quote (no one-time setup charge).
     *
     * @param monthlyRecurring the monthly recurring charge (defaults to zero if null)
     * @param currency         the currency
     * @param source           the provenance of this quote
     * @return a new quote with a zero NRC
     */
    public static PriceQuote monthly(BigDecimal monthlyRecurring, Currency currency, PriceSource source) {
        return of(monthlyRecurring, BigDecimal.ZERO, currency, source);
    }

    /**
     * Creates a genuinely-free quote (zero MRC and NRC). This asserts a known price of
     * zero — distinct from a rate card returning {@link java.util.Optional#empty()},
     * which means the item could not be priced at all.
     *
     * @param currency the currency
     * @param source   the provenance of this quote
     * @return a new zero-valued quote
     */
    public static PriceQuote zero(Currency currency, PriceSource source) {
        return of(BigDecimal.ZERO, BigDecimal.ZERO, currency, source);
    }

    /**
     * Returns a copy of this quote with the note replaced (this instance is immutable
     * and unchanged).
     *
     * @param note the provenance note to attach (replaces any existing note)
     * @return a new quote identical to this one but carrying the given note
     */
    public PriceQuote withNote(String note) {
        return PriceQuote.builder()
                .monthlyRecurring(monthlyRecurring)
                .nonRecurring(nonRecurring)
                .currency(currency)
                .source(source)
                .note(note)
                .build();
    }

    /**
     * The first-year total: {@code MRC × 12 + NRC}.
     *
     * @return the twelve-month cost including the one-time setup charge
     */
    public BigDecimal annualizedTotal() {
        return monthlyRecurring.multiply(BigDecimal.valueOf(12)).add(nonRecurring);
    }

    /**
     * The total cost over a commitment term: {@code MRC × term.months() + NRC}. This is
     * the figure the TCO comparison ranks archetypes by, so one-time setup charges are
     * never ignored in a recommendation.
     *
     * @param term the commitment term
     * @return the full-term cost including the one-time setup charge
     */
    public BigDecimal totalOverTerm(Term term) {
        return monthlyRecurring.multiply(BigDecimal.valueOf(term.months())).add(nonRecurring);
    }

    /**
     * Sums this quote with another. Both must share the same currency — this class never
     * fabricates an FX conversion. The resulting source is preserved when both quotes
     * agree, otherwise it is reported as {@link PriceSource#COMPOSITE}. The per-quote
     * notes are <em>dropped</em> on aggregation (a merged note would misattribute
     * provenance); callers that need them must read the operands. A {@code null}
     * argument returns this quote unchanged.
     *
     * @param other the quote to add (may be {@code null})
     * @return a new aggregated quote without a note
     * @throws IllegalArgumentException if the currencies differ
     */
    public PriceQuote plus(PriceQuote other) {
        if (other == null) {
            return this;
        }
        if (!Objects.equals(currency, other.currency)) {
            throw new IllegalArgumentException(
                    "Cannot add quotes in different currencies: " + currency + " vs " + other.currency);
        }
        PriceSource combined = (source == other.source) ? source : PriceSource.COMPOSITE;
        return PriceQuote.builder()
                .monthlyRecurring(monthlyRecurring.add(other.monthlyRecurring))
                .nonRecurring(nonRecurring.add(other.nonRecurring))
                .currency(currency)
                .source(combined)
                .build();
    }
}
