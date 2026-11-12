package com.eqixiac.equinix.design.value.ratecard;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * The answer to one rate-card lookup: the figure, or the reason there is none, plus which card
 * answered and why earlier cards declined. It is the explainable form of the {@code Optional}
 * the plain {@link RateCard} methods return, for callers that must tell a reader why an item is
 * unpriced.
 *
 * @param value          the figure, or {@code null} when unpriced
 * @param unpricedReason why there is no figure; {@code null} when {@code value} is present
 * @param answeredBy     the card that produced the figure (its {@link RateCard#cardName()}), or
 *                       {@code null} when unpriced
 * @param trail          what each consulted card said, in order (one entry per card for a layered
 *                       card; empty for a single card)
 * @param <T>            the figure's type ({@link PriceQuote} or {@link EgressRate})
 */
public record PriceLookup<T>(T value, String unpricedReason, String answeredBy, List<String> trail) {

    /** Requires a reason when there is no value and copies the trail. */
    public PriceLookup {
        if (value == null && (unpricedReason == null || unpricedReason.isBlank())) {
            throw new IllegalArgumentException("An unpriced lookup needs a reason");
        }
        if (value != null) {
            unpricedReason = null;
        }
        trail = trail == null ? List.of() : List.copyOf(trail);
    }

    /**
     * @param value      the figure
     * @param answeredBy the card that produced it
     * @param <T>        the figure's type
     * @return a priced lookup
     */
    public static <T> PriceLookup<T> priced(T value, String answeredBy) {
        if (value == null) {
            throw new IllegalArgumentException("A priced lookup needs a value");
        }
        return new PriceLookup<>(value, null, answeredBy, List.of());
    }

    /**
     * @param reason why the item cannot be priced
     * @param <T>    the figure's type
     * @return an unpriced lookup
     */
    public static <T> PriceLookup<T> unpriced(String reason) {
        return new PriceLookup<>(null, reason, null, List.of());
    }

    /**
     * Adapts a plain {@code Optional} result.
     *
     * @param result     the plain result
     * @param answeredBy the card's name
     * @param reason     the reason to state when {@code result} is empty
     * @param <T>        the figure's type
     * @return the lookup
     */
    public static <T> PriceLookup<T> of(Optional<T> result, String answeredBy, String reason) {
        return result.isPresent() ? priced(result.get(), answeredBy) : unpriced(reason);
    }

    /** @return {@code true} when there is a figure */
    public boolean isPriced() {
        return value != null;
    }

    /** @return the figure as an {@code Optional} */
    public Optional<T> toOptional() {
        return Optional.ofNullable(value);
    }

    /** @return the reason there is no figure, empty when priced */
    public Optional<String> reason() {
        return Optional.ofNullable(unpricedReason);
    }

    /**
     * @param mapper transforms the figure
     * @param <R>    the new type
     * @return a lookup with the mapped figure and the same reason, card and trail
     */
    public <R> PriceLookup<R> map(Function<T, R> mapper) {
        return new PriceLookup<>(value == null ? null : mapper.apply(value), unpricedReason, answeredBy, trail);
    }

    /**
     * @param entries the trail to attach
     * @return a copy with the trail replaced
     */
    public PriceLookup<T> withTrail(List<String> entries) {
        return new PriceLookup<>(value, unpricedReason, answeredBy, entries);
    }

    /**
     * Combines the lookups of a layered card: the first priced one wins, each consulted card's
     * answer goes into the trail, and when none prices the reason lists every card's reason.
     *
     * @param results  one lookup per card, in precedence order (cards after the winner are not consulted)
     * @param names    the cards' names, same order
     * @param <T>      the figure's type
     * @return the combined lookup
     */
    static <T> PriceLookup<T> firstPriced(List<PriceLookup<T>> results, List<String> names) {
        List<String> trail = new ArrayList<>();
        for (int i = 0; i < results.size(); i++) {
            PriceLookup<T> r = results.get(i);
            String name = names.get(i);
            if (r.isPriced()) {
                trail.add("layer " + (i + 1) + " " + name + ": answered");
                return new PriceLookup<>(r.value(), null, r.answeredBy() != null ? r.answeredBy() : name, trail);
            }
            trail.add("layer " + (i + 1) + " " + name + ": declined (" + r.unpricedReason() + ")");
        }
        String reason = results.isEmpty() ? "the layered rate card holds no cards"
                : "no layer could price it: " + String.join("; ", trail);
        return new PriceLookup<>(null, reason, null, trail);
    }
}
