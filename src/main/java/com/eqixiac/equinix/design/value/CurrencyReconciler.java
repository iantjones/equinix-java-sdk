package com.eqixiac.equinix.design.value;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.Currency;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The single reconciliation rule shared by every design-time cost model
 * ({@code TcoEngine}, {@code SavingsCalculatorEngine}, {@code MetroOptimizerEngine},
 * {@code DeploymentWizardEngine}, and plan value realization).
 *
 * <p><strong>Rule:</strong> monetary amounts may only be added, subtracted, or compared
 * when they share a currency. This SDK models prices with no FX rate at all &mdash; live
 * Fabric pricing for an EMEA metro can quote EUR while the bundled cloud-egress reference
 * figures are USD &mdash; so summing across currencies would fabricate a figure that is
 * simply wrong. When components disagree, this helper refuses to produce a combined total
 * and instead exposes the per-currency subtotals and a human-readable reason, so the caller
 * can mark the total unpriced/mixed (mirroring the existing {@code priced}/{@code complete}
 * guards) rather than emit a false number labelled as authoritative.</p>
 *
 * <p>Used two ways:</p>
 * <ul>
 *   <li>as an <em>accumulator</em> ({@link #add}) for a stream of priced components that are
 *       meant to sum into one deployment total &mdash; it tallies per-currency subtotals and
 *       yields a combined total only via {@link #monthlyTotal()}/{@link #setupTotal()}, which
 *       are empty when more than one currency is present;</li>
 *   <li>via the static {@link #knownDifferent(String, String)} / {@link #sameKnownCurrency(String, String)}
 *       predicates at the two-sided subtract/compare sites (egress&nbsp;&minus;&nbsp;interconnect,
 *       baseline&nbsp;&minus;&nbsp;recommended, break-even, payback).</li>
 * </ul>
 *
 * <p>The predicates deliberately treat an <em>unknown</em> (null/blank) currency as
 * "not provably different": they block a combination only when two currencies are both known
 * and genuinely differ, so a malformed null currency never silently flips an all-one-currency
 * calculation to unpriced. Absence is tracked separately via {@link #sawUnknownCurrency()}.</p>
 *
 * <p>Not thread-safe; each computation builds its own instance.</p>
 */
public final class CurrencyReconciler {

    private final LinkedHashMap<String, BigDecimal> monthly = new LinkedHashMap<>();
    private final LinkedHashMap<String, BigDecimal> setup = new LinkedHashMap<>();
    private boolean sawUnknownCurrency;

    private CurrencyReconciler() {
    }

    /** Creates an empty reconciler. */
    public static CurrencyReconciler create() {
        return new CurrencyReconciler();
    }

    /**
     * Records a priced component. A null/blank currency contributes no amount (it cannot be
     * reconciled) and is remembered via {@link #sawUnknownCurrency()}; null amounts count as zero.
     *
     * @param currency      the component's currency (may be null)
     * @param monthlyAmount the component's monthly-recurring amount
     * @param setupAmount   the component's one-time setup amount
     * @return this reconciler
     */
    public CurrencyReconciler add(Currency currency, BigDecimal monthlyAmount, BigDecimal setupAmount) {
        return add(currency == null ? null : currency.getCurrencyCode(), monthlyAmount, setupAmount);
    }

    /**
     * Records a priced component by currency code. See {@link #add(Currency, BigDecimal, BigDecimal)}.
     */
    public CurrencyReconciler add(String currencyCode, BigDecimal monthlyAmount, BigDecimal setupAmount) {
        if (currencyCode == null || currencyCode.isBlank()) {
            sawUnknownCurrency = true;
            return this;
        }
        monthly.merge(currencyCode, nz(monthlyAmount), BigDecimal::add);
        setup.merge(currencyCode, nz(setupAmount), BigDecimal::add);
        return this;
    }

    /** Records a monthly-only component (setup treated as zero). */
    public CurrencyReconciler addMonthly(Currency currency, BigDecimal monthlyAmount) {
        return add(currency, monthlyAmount, BigDecimal.ZERO);
    }

    /** {@code true} when no priceable component has been recorded. */
    public boolean isEmpty() {
        return monthly.isEmpty();
    }

    /** {@code true} when at least one component had a null/blank (unreconcilable) currency. */
    public boolean sawUnknownCurrency() {
        return sawUnknownCurrency;
    }

    /** The distinct currency codes recorded, in first-seen order. */
    public Set<String> currencies() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(monthly.keySet()));
    }

    /** How many distinct currencies were recorded. */
    public int distinctCurrencyCount() {
        return monthly.size();
    }

    /** {@code true} when components span more than one currency, so no single total is valid. */
    public boolean isMixed() {
        return monthly.size() > 1;
    }

    /** {@code true} when all recorded components share a currency (or none were recorded). */
    public boolean isReconciled() {
        return monthly.size() <= 1;
    }

    /** The sole currency when exactly one was recorded, otherwise {@code null}. */
    public String soleCurrency() {
        return monthly.size() == 1 ? monthly.keySet().iterator().next() : null;
    }

    /** The sole currency, or {@code fallback} when empty or mixed. */
    public String soleCurrencyOr(String fallback) {
        String sole = soleCurrency();
        return sole != null ? sole : fallback;
    }

    /**
     * The combined monthly total &mdash; present only when the components reconcile to a single
     * currency, empty when they are {@link #isMixed() mixed} (a cross-currency sum would be false).
     *
     * <p><strong>Empty-reconciler caveat:</strong> when <em>nothing</em> priceable was recorded
     * &mdash; including when every added component carried a null/blank currency &mdash; this
     * returns {@code Optional.of(ZERO)}, not {@code Optional.empty()}: zero is the correct sum
     * of no components, but it is <em>not</em> evidence that anything was priced. Callers must
     * therefore pair this with {@link #isEmpty()} / {@link #sawUnknownCurrency()} (as the
     * engines' {@code priced}/{@code complete} flags do) before presenting the total, or an
     * all-unpriceable accumulation reads as "costs $0" &mdash; exactly the fabricated figure
     * this class exists to prevent.</p>
     */
    public Optional<BigDecimal> monthlyTotal() {
        return isMixed() ? Optional.empty() : Optional.of(sum(monthly));
    }

    /**
     * The combined setup total, with the same single-currency guard &mdash; and the same
     * empty-reconciler caveat &mdash; as {@link #monthlyTotal()}.
     */
    public Optional<BigDecimal> setupTotal() {
        return isMixed() ? Optional.empty() : Optional.of(sum(setup));
    }

    /** Monthly subtotal per currency, in first-seen order. */
    public Map<String, BigDecimal> monthlySubtotals() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(monthly));
    }

    /** Setup subtotal per currency, in first-seen order. */
    public Map<String, BigDecimal> setupSubtotals() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(setup));
    }

    /** The currencies joined for a message, e.g. {@code "USD and EUR"}. */
    public String describeCurrencies() {
        return String.join(" and ", monthly.keySet());
    }

    /** The per-currency monthly subtotals for a message, e.g. {@code "USD 2000.00, EUR 350.00"}. */
    public String describeMonthlySubtotals() {
        return monthly.entrySet().stream()
                .map(e -> e.getKey() + " " + e.getValue().setScale(2, RoundingMode.HALF_UP).toPlainString())
                .collect(Collectors.joining(", "));
    }

    private static BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static BigDecimal sum(Map<String, BigDecimal> amounts) {
        BigDecimal total = BigDecimal.ZERO;
        for (BigDecimal value : amounts.values()) {
            total = total.add(value);
        }
        return total;
    }

    // ── Two-sided predicates for the subtract/compare sites ──

    /**
     * {@code true} only when both currencies are known and genuinely differ &mdash; the guard for
     * "may I subtract/compare these two?". A null/blank on either side yields {@code false}
     * (not provably different), so an absent currency never fabricates a mismatch.
     */
    public static boolean knownDifferent(String a, String b) {
        return a != null && b != null && !a.isBlank() && !b.isBlank() && !a.equals(b);
    }

    /** {@link #knownDifferent(String, String)} over {@link Currency} values. */
    public static boolean knownDifferent(Currency a, Currency b) {
        return a != null && b != null && !a.equals(b);
    }

    /** {@code true} when both currencies are known and equal. */
    public static boolean sameKnownCurrency(String a, String b) {
        return a != null && b != null && !a.isBlank() && !b.isBlank() && a.equals(b);
    }
}
