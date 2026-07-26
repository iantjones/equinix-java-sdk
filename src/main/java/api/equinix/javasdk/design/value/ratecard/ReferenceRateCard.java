package api.equinix.javasdk.design.value.ratecard;

import api.equinix.javasdk.core.enums.MetroCode;
import api.equinix.javasdk.fabric.enums.ConnectionType;
import api.equinix.javasdk.fabric.model.implementation.cloud.CloudProviderType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;

/**
 * A {@link RateCard} backed by a bundled, dated table of <em>indicative reference</em>
 * figures — published cloud-provider egress rates, interconnect port fees, and
 * on-prem/colocation midpoints — compiled so the value-realization models work out
 * of the box without a live connection or caller-supplied rates.
 *
 * <p>Every value it returns is tagged {@link PriceSource#REFERENCE} and the card
 * carries a {@link #disclaimer()} making clear these are estimates, not quotes:
 * cloud egress rates are standard-tier US/North-America headline prices, and
 * Equinix-side figures are indicative fallbacks that should be superseded by the
 * live Fabric Pricing API (use a {@code LayeredRateCard} with
 * {@code EquinixRateCard} ahead of this one).</p>
 *
 * <p>Because the bundled table is deliberately coarse, the lookups are coarse too:
 * {@code connection(...)} matches on <em>bandwidth alone</em> — the smallest
 * tabulated tier at or above the request, linearly extrapolated (with an
 * {@code EXTRAPOLATED} note) above the top tier — and ignores connection type,
 * metro, and term; {@code cloudRouter(...)} ignores metro and term, and
 * substitutes the STANDARD figure for an unlisted package with a note naming the
 * substitution; {@code egress(...)} matches provider + path, ignoring region and
 * term. The emitted notes identify the tier or substitution; the data vintage is
 * available via {@link #asOf()} (the engines surface it in their reports'
 * "as of" line).</p>
 *
 * <p>The bundled data set is loaded once from
 * {@code /json/ratecard_reference_2026_06.json} and cached.</p>
 */
public final class ReferenceRateCard implements RateCard {

    private static final Currency USD = Currency.getInstance("USD");
    private static final String RESOURCE = "/json/ratecard_reference_2026_06.json";

    private static volatile ReferenceRateCard standard;

    private final String asOf;
    private final String disclaimer;
    private final Currency currency;
    private final Map<String, EgressRate> egressRates;                 // "PROVIDER|PATH" -> rate
    private final NavigableMap<Integer, BigDecimal> connectionByBandwidth;
    private final Map<String, BigDecimal> routerByPackage;
    private final BigDecimal equinixCrossConnectMonthly;
    private final Map<CloudProviderType, NavigableMap<Integer, BigDecimal>> cspPortByBandwidth;
    private final Map<String, BigDecimal> onPrem;

    private ReferenceRateCard(JsonNode root) {
        this.asOf = root.path("asOf").asText(null);
        this.disclaimer = root.path("disclaimer").asText(null);
        this.currency = safeCurrency(root.path("currency").asText("USD"));

        // Missing or non-numeric monetary fields are treated as UNAVAILABLE (the entry is
        // skipped), never as a fabricated $0 — Jackson's MissingNode/TextNode.decimalValue()
        // both return ZERO, which would silently violate the "empty != free" contract.
        this.egressRates = new HashMap<>();
        for (JsonNode e : root.path("egress")) {
            BigDecimal perGb = num(e, "perGb");
            if (perGb == null) {
                continue;
            }
            String key = e.path("provider").asText() + "|" + e.path("path").asText();
            egressRates.put(key, EgressRate.of(perGb, currency, PriceSource.REFERENCE)
                    .withNote(e.path("note").asText(null)));
        }

        this.connectionByBandwidth = new TreeMap<>();
        for (JsonNode c : root.path("equinixConnection")) {
            BigDecimal monthly = num(c, "monthly");
            if (monthly != null && c.path("bandwidthMbps").isNumber()) {
                connectionByBandwidth.put(c.path("bandwidthMbps").asInt(), monthly);
            }
        }

        this.routerByPackage = new HashMap<>();
        for (JsonNode r : root.path("equinixCloudRouter")) {
            BigDecimal monthly = num(r, "monthly");
            if (monthly != null) {
                routerByPackage.put(r.path("packageCode").asText(), monthly);
            }
        }

        this.equinixCrossConnectMonthly = num(root, "equinixCrossConnectMonthly");

        this.cspPortByBandwidth = new EnumMap<>(CloudProviderType.class);
        for (JsonNode p : root.path("cspInterconnectPort")) {
            CloudProviderType provider = parseProvider(p.path("provider").asText());
            BigDecimal monthly = num(p, "monthly");
            if (provider == null || monthly == null || !p.path("bandwidthMbps").isNumber()) {
                continue;
            }
            cspPortByBandwidth
                    .computeIfAbsent(provider, k -> new TreeMap<>())
                    .put(p.path("bandwidthMbps").asInt(), monthly);
        }

        this.onPrem = new HashMap<>();
        root.path("onPrem").fields().forEachRemaining(f -> {
            if (f.getValue().isNumber()) {
                onPrem.put(f.getKey(), f.getValue().decimalValue());
            }
        });
    }

    private static BigDecimal num(JsonNode parent, String field) {
        JsonNode n = parent.path(field);
        return n.isNumber() ? n.decimalValue() : null;
    }

    /**
     * Returns the bundled standard reference rate card (2026-06 figures), loading
     * and caching it on first use.
     *
     * @return the shared reference rate card
     */
    public static ReferenceRateCard standard() {
        ReferenceRateCard local = standard;
        if (local == null) {
            synchronized (ReferenceRateCard.class) {
                local = standard;
                if (local == null) {
                    local = load(RESOURCE);
                    standard = local;
                }
            }
        }
        return local;
    }

    private static ReferenceRateCard load(String resource) {
        try (InputStream in = ReferenceRateCard.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException("Bundled reference rate card not found on classpath: " + resource);
            }
            JsonNode root = new ObjectMapper().readTree(in);
            return new ReferenceRateCard(root);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load reference rate card: " + resource, e);
        }
    }

    // ── RateCard ──

    @Override
    public Optional<PriceQuote> connection(ConnectionType type, int bandwidthMbps, MetroCode metro, Term term) {
        if (connectionByBandwidth.isEmpty()) {
            return Optional.empty();
        }
        // The smallest tabulated tier at or above the requested bandwidth is a direct
        // reference figure (a request below the smallest tier ceilings up to it).
        Map.Entry<Integer, BigDecimal> tier = connectionByBandwidth.ceilingEntry(bandwidthMbps);
        if (tier != null) {
            return Optional.of(PriceQuote.monthly(tier.getValue(), currency, PriceSource.REFERENCE)
                    .withNote("reference VC ~" + tier.getKey() + " Mbps"));
        }
        // The request exceeds every tabulated tier. Do NOT fall back to the top tier's flat
        // price — that would price, e.g., a 100G link at the 10G rate, a silent under-price.
        // Extrapolate linearly from the top tier's per-Mbps rate and TAG the result as an
        // extrapolation so callers never mistake it for a tabulated figure. Linear
        // extrapolation of a flat schedule over-prices at higher bandwidth (the safe
        // direction), and REFERENCE already carries the "indicative, not a quote" disclaimer.
        Map.Entry<Integer, BigDecimal> top = connectionByBandwidth.lastEntry();
        BigDecimal extrapolated = top.getValue()
                .multiply(BigDecimal.valueOf(bandwidthMbps))
                .divide(BigDecimal.valueOf(top.getKey()), 2, RoundingMode.HALF_UP);
        return Optional.of(PriceQuote.monthly(extrapolated, currency, PriceSource.REFERENCE)
                .withNote("reference VC EXTRAPOLATED above top tabulated tier (" + top.getKey() + " Mbps = "
                        + top.getValue() + "): linear per-Mbps estimate for " + bandwidthMbps
                        + " Mbps, not a tabulated rate"));
    }

    @Override
    public Optional<PriceQuote> cloudRouter(String packageCode, MetroCode metro, Term term) {
        BigDecimal monthly = packageCode == null ? null : routerByPackage.get(packageCode);
        String note = "reference Cloud Router";
        if (monthly == null) {
            monthly = routerByPackage.get("STANDARD");
            // The STANDARD figure standing in for a package the table does not list is a
            // substitution, and the note must say so — a PREMIUM estimate silently priced at the
            // STANDARD rate would read as a genuine PREMIUM reference figure.
            if (monthly != null && packageCode != null && !"STANDARD".equals(packageCode)) {
                note = "reference Cloud Router (STANDARD substituted for " + packageCode + ")";
            }
        }
        if (monthly == null) {
            return Optional.empty();
        }
        return Optional.of(PriceQuote.monthly(monthly, currency, PriceSource.REFERENCE)
                .withNote(note));
    }

    @Override
    public Optional<EgressRate> egress(CloudProviderType provider, String region, EgressPath path, Term term) {
        if (provider == null || path == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(egressRates.get(provider.name() + "|" + path.name()));
    }

    @Override
    public PriceSource source() {
        return PriceSource.REFERENCE;
    }

    // ── Reference accessors used by the TCO archetype models ──

    /**
     * The vintage of the bundled figures, e.g. {@code "2026-06"}. Reports should surface
     * this next to any reference-derived number so its age is visible.
     *
     * @return the data set's as-of stamp, or {@code null} if the bundle omits it
     */
    public String asOf() {
        return asOf;
    }

    /**
     * The bundled data set's own disclaimer text — what the figures are, what they
     * exclude, and why they are estimates rather than quotes. Engines append it to
     * their report output.
     *
     * @return the disclaimer, or {@code null} if the bundle omits it
     */
    public String disclaimer() {
        return disclaimer;
    }

    /**
     * The ISO&nbsp;4217 code of the currency every bundled figure is expressed in
     * ({@code "USD"} for the standard bundle). The engines compare it against their
     * comparison currency before folding reference figures into a total — reference
     * numbers are never mixed into a differently-denominated sum.
     *
     * @return the reference data's currency code
     */
    public String currencyCode() {
        return currency.getCurrencyCode();
    }

    /**
     * An indicative on-premises cost midpoint by key. The bundled keys are:
     * <ul>
     *   <li>{@code "transitPerMbpsMonth"} — carrier IP transit, per Mbps per month;</li>
     *   <li>{@code "crossConnectMonthly"} — a carrier-hotel cross-connect, per month;</li>
     *   <li>{@code "hardwareMonthly"} — amortized network hardware, per month;</li>
     *   <li>{@code "powerPerKwMonth"} — power/space, per kW per month.</li>
     * </ul>
     * These feed the TCO model's on-prem archetype and are individually overridable via
     * the {@code TcoCalculator.Builder.onPrem*} methods.
     *
     * @param key one of the keys above
     * @return the midpoint in the reference currency, or empty for an unknown key or an
     *         entry missing from the bundle
     */
    public Optional<BigDecimal> onPrem(String key) {
        return Optional.ofNullable(onPrem.get(key));
    }

    /**
     * The indicative monthly fee for one Equinix cross-connect (per physical
     * cross-connect, in the reference currency) — the fallback the TCO model uses when
     * the caller supplies no {@code ColocationItem.CROSS_CONNECT} rate.
     *
     * @return the per-unit monthly fee, or empty if the bundle omits it
     */
    public Optional<BigDecimal> equinixCrossConnectMonthly() {
        return Optional.ofNullable(equinixCrossConnectMonthly);
    }

    /**
     * The indicative monthly fee for a cloud provider's interconnect port/circuit
     * (AWS Direct Connect / Azure ExpressRoute / GCP Interconnect) at or above the
     * requested bandwidth. A request above the largest tabulated tier is never
     * silently floored back to that tier's flat price (which would price a 100G
     * requirement as a single 10G circuit) — it is extrapolated linearly from the
     * top tier's per-Mbps rate, exactly as {@code connection(...)} does; use
     * {@link #cspInterconnectPortMonthlyQuote} to see the extrapolation tag.
     *
     * @param provider      the cloud provider
     * @param bandwidthMbps the desired bandwidth
     * @return the reference monthly port fee (extrapolated above the top tabulated
     *         tier), or empty if none is tabulated
     */
    public Optional<BigDecimal> cspInterconnectPortMonthly(CloudProviderType provider, int bandwidthMbps) {
        return cspInterconnectPortMonthlyQuote(provider, bandwidthMbps)
                .map(PriceQuote::getMonthlyRecurring);
    }

    /**
     * As {@link #cspInterconnectPortMonthly}, returning the fee as a tagged
     * {@link PriceQuote}: an at-or-above tabulated tier carries a plain reference
     * note, while a request above the top tier carries a note flagging the figure
     * as {@code EXTRAPOLATED above top tabulated tier} so it is never mistaken for
     * a tabulated rate.
     *
     * @param provider      the cloud provider
     * @param bandwidthMbps the desired bandwidth
     * @return the reference monthly port fee as a noted quote, or empty if none is tabulated
     */
    public Optional<PriceQuote> cspInterconnectPortMonthlyQuote(CloudProviderType provider, int bandwidthMbps) {
        NavigableMap<Integer, BigDecimal> tiers = cspPortByBandwidth.get(provider);
        if (tiers == null || tiers.isEmpty()) {
            return Optional.empty();
        }
        Map.Entry<Integer, BigDecimal> tier = tiers.ceilingEntry(bandwidthMbps);
        if (tier != null) {
            return Optional.of(PriceQuote.monthly(tier.getValue(), currency, PriceSource.REFERENCE)
                    .withNote("reference " + provider + " interconnect port ~" + tier.getKey() + " Mbps"));
        }
        // The request exceeds every tabulated tier. Do NOT floor back to the top tier's flat
        // price — that would price, e.g., a 100G requirement as one 10G circuit, a silent
        // under-price. Mirror connection(): extrapolate linearly from the top tier's per-Mbps
        // rate and TAG the result as an extrapolation. Linear extrapolation of a flat schedule
        // over-prices at higher bandwidth (the safe direction).
        Map.Entry<Integer, BigDecimal> top = tiers.lastEntry();
        BigDecimal extrapolated = top.getValue()
                .multiply(BigDecimal.valueOf(bandwidthMbps))
                .divide(BigDecimal.valueOf(top.getKey()), 2, RoundingMode.HALF_UP);
        return Optional.of(PriceQuote.monthly(extrapolated, currency, PriceSource.REFERENCE)
                .withNote("reference " + provider + " interconnect port EXTRAPOLATED above top tabulated tier ("
                        + top.getKey() + " Mbps = " + top.getValue() + "): linear per-Mbps estimate for "
                        + bandwidthMbps + " Mbps, not a tabulated rate"));
    }

    private static CloudProviderType parseProvider(String name) {
        try {
            return CloudProviderType.valueOf(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static Currency safeCurrency(String code) {
        try {
            return Currency.getInstance(code);
        } catch (RuntimeException e) {
            return USD;
        }
    }
}
