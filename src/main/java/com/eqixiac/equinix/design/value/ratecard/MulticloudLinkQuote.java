package com.eqixiac.equinix.design.value.ratecard;

import com.eqixiac.equinix.design.value.CurrencyReconciler;
import com.eqixiac.equinix.fabric.model.implementation.cloud.CloudProviderType;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import lombok.Value;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * The price of one native provider-to-provider multicloud link. Each provider bills its own side
 * independently, so the quote is two-sided: {@code getSideA()} is what {@code getProviderA()}
 * charges, {@code getSideZ()} is what {@code getProviderZ()} charges. Either side may be
 * unpriced.
 *
 * <p><b>Beta</b>: models products that reached general availability in 2026; see
 * {@link MulticloudLinkRequest}.</p>
 *
 * <h2>Units</h2>
 * <p>Both providers publish flat <em>hourly</em> rates. The rest of the value layer works in
 * monthly recurring charges, so rate cards convert with the named constant
 * {@link #HOURS_PER_MONTH} ({@value #HOURS_PER_MONTH} h = 365 d x 24 h / 12, the figure AWS uses
 * in its own pricing examples) and state the conversion in the side's
 * {@code PriceQuote.getNote()}. A 31-day month has 744 h, so a real invoice can exceed the
 * modelled figure by up to 1.9 %.</p>
 *
 * <h2>Unpriced sides</h2>
 * <p>A side with no verifiable price is absent ({@link Optional#empty()}), never zero, and
 * {@code getSideAUnpricedReason()} / {@code getSideZUnpricedReason()} names what is missing.
 * A present side with a zero amount is a known price of zero (for example the AWS free tier,
 * which a card applies only on request).</p>
 *
 * <h2>Combined figure</h2>
 * <p>{@link #combinedMonthly()} is present only when both sides are priced and share one known
 * currency. It runs through {@link CurrencyReconciler}: sides in different currencies are never
 * summed and no FX rate is applied. {@link #monthlySubtotalsByCurrency()} exposes the
 * per-currency figures for reporting when the combined figure is withheld. A side whose
 * monthly and one-time charges are both zero does not take part in the currency check, because
 * zero is the same amount in every currency: a free-tier side quoted in USD combines with a
 * negotiated side quoted in EUR.</p>
 */
@Value
@Builder(toBuilder = true)
public class MulticloudLinkQuote {

    /**
     * Hours per month used to convert an hourly rate to a monthly recurring charge:
     * 365 x 24 / 12 = 730.
     */
    public static final int HOURS_PER_MONTH = 730;

    /** The path tier assumed when a request does not state one: 1, local connectivity scope. */
    public static final int DEFAULT_PATH_TIER = 1;

    /** The lowest AWS connectivity-scope tier. */
    public static final int MIN_PATH_TIER = 1;

    /** The highest AWS connectivity-scope tier. */
    public static final int MAX_PATH_TIER = 5;

    /** The provider billing {@code getSideA()}. */
    CloudProviderType providerA;

    /** {@code providerA}'s region as requested, or {@code null}. */
    String regionA;

    /** The provider billing {@code getSideZ()}. */
    CloudProviderType providerZ;

    /** {@code providerZ}'s region as requested, or {@code null}. */
    String regionZ;

    /** The link size the quote was resolved for, in Mbps. */
    int bandwidthMbps;

    /** The AWS connectivity-scope tier (1-5) the quote was resolved for. */
    int pathTier;

    @Getter(AccessLevel.NONE)
    PriceQuote sideA;

    @Getter(AccessLevel.NONE)
    PriceQuote sideZ;

    /** Why {@code getSideA()} is empty; {@code null} when that side is priced. */
    String sideAUnpricedReason;

    /** Why {@code getSideZ()} is empty; {@code null} when that side is priced. */
    String sideZUnpricedReason;

    /**
     * Quote-level notes: assumptions that apply to the whole link (tier input, hourly-to-monthly
     * conversion, a declined free-tier request). Per-side provenance is on each side's
     * {@code PriceQuote.getNote()}.
     */
    @Singular
    List<String> notes;

    /**
     * The monthly recurring and one-time charge billed by {@code getProviderA()}.
     *
     * @return the side's quote, or empty when it could not be priced
     */
    public Optional<PriceQuote> getSideA() {
        return Optional.ofNullable(sideA);
    }

    /**
     * The monthly recurring and one-time charge billed by {@code getProviderZ()}.
     *
     * @return the side's quote, or empty when it could not be priced
     */
    public Optional<PriceQuote> getSideZ() {
        return Optional.ofNullable(sideZ);
    }

    /**
     * The side billed by the given provider.
     *
     * @param provider one of the link's two providers
     * @return that provider's side; empty when the provider is not an end of this link or its
     *         side is unpriced
     */
    public Optional<PriceQuote> side(CloudProviderType provider) {
        if (provider == null) {
            return Optional.empty();
        }
        if (provider == providerA) {
            return getSideA();
        }
        return provider == providerZ ? getSideZ() : Optional.empty();
    }

    /**
     * Whether both sides are priced. A fully priced quote can still have no
     * {@link #combinedMonthly()} when the two sides are in different currencies.
     *
     * @return {@code true} when neither side is empty
     */
    public boolean isFullyPriced() {
        return sideA != null && sideZ != null;
    }

    /**
     * The link's total monthly recurring charge: side A plus side Z.
     *
     * @return the sum, or empty when a side is unpriced, a side has no currency, or the two
     *         sides are in different currencies
     */
    public Optional<BigDecimal> combinedMonthly() {
        CurrencyReconciler recon = reconcile();
        return recon == null ? Optional.empty() : recon.monthlyTotal();
    }

    /**
     * The link's total one-time charge: side A plus side Z.
     *
     * @return the sum, or empty under the same conditions as {@link #combinedMonthly()}
     */
    public Optional<BigDecimal> combinedSetup() {
        CurrencyReconciler recon = reconcile();
        return recon == null ? Optional.empty() : recon.setupTotal();
    }

    /**
     * The ISO 4217 code {@link #combinedMonthly()} is expressed in: the currency of the
     * non-zero sides, or of side A when both sides are zero.
     *
     * @return the currency code, or empty whenever {@link #combinedMonthly()} is empty
     */
    public Optional<String> combinedCurrency() {
        CurrencyReconciler recon = reconcile();
        if (recon == null) {
            return Optional.empty();
        }
        if (recon.soleCurrency() != null) {
            return Optional.of(recon.soleCurrency());
        }
        return Optional.ofNullable(sideA.getCurrency() != null ? sideA.getCurrency() : sideZ.getCurrency())
                .map(java.util.Currency::getCurrencyCode);
    }

    /**
     * Whether both sides are priced, both carry a non-zero charge, and their known currencies
     * differ.
     *
     * @return {@code true} when a combined figure is withheld because of a currency mismatch
     */
    public boolean isMixedCurrency() {
        return isFullyPriced() && nonZeroSides().isMixed();
    }

    /**
     * The priced sides' monthly charges grouped by currency, for example
     * {@code "USD 9000.90, EUR 12000.00"}. Intended for the message that explains a withheld
     * combined figure.
     *
     * @return the per-currency subtotals, or an empty string when neither side is priced
     */
    public String monthlySubtotalsByCurrency() {
        CurrencyReconciler recon = CurrencyReconciler.create();
        if (sideA != null) {
            recon.add(sideA.getCurrency(), sideA.getMonthlyRecurring(), sideA.getNonRecurring());
        }
        if (sideZ != null) {
            recon.add(sideZ.getCurrency(), sideZ.getMonthlyRecurring(), sideZ.getNonRecurring());
        }
        return recon.describeMonthlySubtotals();
    }

    /**
     * A one-sentence description of what is unpriced, naming each empty side's provider and
     * reason.
     *
     * @return the description, or empty when both sides are priced
     */
    public Optional<String> unpricedSummary() {
        if (isFullyPriced()) {
            return Optional.empty();
        }
        List<String> parts = new ArrayList<>(2);
        if (sideA == null) {
            parts.add(describeUnpriced(providerA, sideAUnpricedReason));
        }
        if (sideZ == null) {
            parts.add(describeUnpriced(providerZ, sideZUnpricedReason));
        }
        return Optional.of(String.join(" ", parts));
    }

    /**
     * Fills this quote's unpriced sides from another quote for the same link and returns the
     * result; this instance is unchanged. A side already priced here is kept, so when quotes are
     * merged in rate-card precedence order the earliest card that prices a side wins. Sides are
     * matched by provider, so {@code other} may list the two providers in either order. Notes
     * are concatenated without duplicates. A side neither quote prices keeps both reasons.
     *
     * @param other a quote for the same provider pair; {@code null} returns this quote
     * @return the merged quote
     * @throws IllegalArgumentException if {@code other} is for a different provider pair
     */
    public MulticloudLinkQuote fillUnpricedSidesFrom(MulticloudLinkQuote other) {
        if (other == null) {
            return this;
        }
        boolean sameOrder = other.providerA == providerA && other.providerZ == providerZ;
        boolean swapped = other.providerA == providerZ && other.providerZ == providerA;
        if (!sameOrder && !swapped) {
            throw new IllegalArgumentException("cannot merge quotes for different provider pairs: "
                    + providerA + "/" + providerZ + " vs " + other.providerA + "/" + other.providerZ);
        }
        PriceQuote otherA = sameOrder ? other.sideA : other.sideZ;
        PriceQuote otherZ = sameOrder ? other.sideZ : other.sideA;
        String otherAReason = sameOrder ? other.sideAUnpricedReason : other.sideZUnpricedReason;
        String otherZReason = sameOrder ? other.sideZUnpricedReason : other.sideAUnpricedReason;

        PriceQuote mergedA = sideA != null ? sideA : otherA;
        PriceQuote mergedZ = sideZ != null ? sideZ : otherZ;

        Set<String> mergedNotes = new LinkedHashSet<>(notes);
        mergedNotes.addAll(other.notes);

        return toBuilder()
                .sideA(mergedA)
                .sideZ(mergedZ)
                .sideAUnpricedReason(mergedA != null ? null : joinReasons(sideAUnpricedReason, otherAReason))
                .sideZUnpricedReason(mergedZ != null ? null : joinReasons(sideZUnpricedReason, otherZReason))
                .clearNotes()
                .notes(mergedNotes)
                .build();
    }

    /**
     * Converts a flat hourly rate to a monthly recurring charge at {@link #HOURS_PER_MONTH}
     * hours per month, rounded half-up to two decimal places.
     *
     * @param hourly the hourly rate; must not be {@code null} or negative
     * @return {@code hourly x 730}
     * @throws IllegalArgumentException if {@code hourly} is {@code null} or negative
     */
    public static BigDecimal monthlyFromHourly(BigDecimal hourly) {
        if (hourly == null || hourly.signum() < 0) {
            throw new IllegalArgumentException("hourly rate must be a non-negative number: " + hourly);
        }
        return hourly.multiply(BigDecimal.valueOf(HOURS_PER_MONTH)).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Formats a rate for a provenance note with at least two decimal places ({@code 19.00}, not
     * {@code 19.0}); more precise rates keep their own scale.
     */
    static String formatRate(BigDecimal rate) {
        return (rate.scale() < 2 ? rate.setScale(2, RoundingMode.UNNECESSARY) : rate).toPlainString();
    }

    private CurrencyReconciler reconcile() {
        if (!isFullyPriced()) {
            return null;
        }
        CurrencyReconciler recon = nonZeroSides();
        // An unknown currency contributes nothing to the reconciler, so its total would be a
        // one-sided figure presented as the link price. Withhold it.
        return recon.sawUnknownCurrency() || recon.isMixed() ? null : recon;
    }

    /** The priced sides with a non-zero charge; an all-zero side is currency-invariant. */
    private CurrencyReconciler nonZeroSides() {
        CurrencyReconciler recon = CurrencyReconciler.create();
        for (PriceQuote side : new PriceQuote[] {sideA, sideZ}) {
            if (side != null && (side.getMonthlyRecurring().signum() != 0 || side.getNonRecurring().signum() != 0)) {
                recon.add(side.getCurrency(), side.getMonthlyRecurring(), side.getNonRecurring());
            }
        }
        return recon;
    }

    private static String describeUnpriced(CloudProviderType provider, String reason) {
        return "The " + provider + " side is unpriced"
                + (reason == null || reason.isBlank() ? "." : ": " + stripTrailingPeriod(reason) + ".");
    }

    private static String stripTrailingPeriod(String text) {
        String trimmed = text.trim();
        return trimmed.endsWith(".") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    }

    private static String joinReasons(String first, String second) {
        if (first == null || first.isBlank()) {
            return second;
        }
        if (second == null || second.isBlank() || second.equals(first)) {
            return first;
        }
        return first + "; " + second;
    }
}
