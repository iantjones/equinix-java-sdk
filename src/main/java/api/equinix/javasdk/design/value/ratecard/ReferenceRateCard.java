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
        String code = root.path("currency").asText("USD");
        this.currency = safeCurrency(code);

        this.egressRates = new HashMap<>();
        for (JsonNode e : root.path("egress")) {
            String key = e.path("provider").asText() + "|" + e.path("path").asText();
            egressRates.put(key, EgressRate.of(e.path("perGb").decimalValue(), currency, PriceSource.REFERENCE)
                    .withNote(e.path("note").asText(null)));
        }

        this.connectionByBandwidth = new TreeMap<>();
        for (JsonNode c : root.path("equinixConnection")) {
            connectionByBandwidth.put(c.path("bandwidthMbps").asInt(), c.path("monthly").decimalValue());
        }

        this.routerByPackage = new HashMap<>();
        for (JsonNode r : root.path("equinixCloudRouter")) {
            routerByPackage.put(r.path("packageCode").asText(), r.path("monthly").decimalValue());
        }

        this.equinixCrossConnectMonthly = root.has("equinixCrossConnectMonthly")
                ? root.path("equinixCrossConnectMonthly").decimalValue() : null;

        this.cspPortByBandwidth = new EnumMap<>(CloudProviderType.class);
        for (JsonNode p : root.path("cspInterconnectPort")) {
            CloudProviderType provider = parseProvider(p.path("provider").asText());
            if (provider == null) {
                continue;
            }
            cspPortByBandwidth
                    .computeIfAbsent(provider, k -> new TreeMap<>())
                    .put(p.path("bandwidthMbps").asInt(), p.path("monthly").decimalValue());
        }

        this.onPrem = new HashMap<>();
        JsonNode onPremNode = root.path("onPrem");
        onPremNode.fields().forEachRemaining(f -> onPrem.put(f.getKey(), f.getValue().decimalValue()));
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
        Map.Entry<Integer, BigDecimal> tier = connectionByBandwidth.ceilingEntry(bandwidthMbps);
        if (tier == null) {
            tier = connectionByBandwidth.floorEntry(bandwidthMbps);
        }
        if (tier == null) {
            return Optional.empty();
        }
        return Optional.of(PriceQuote.monthly(tier.getValue(), currency, PriceSource.REFERENCE)
                .withNote("reference VC ~" + tier.getKey() + " Mbps"));
    }

    @Override
    public Optional<PriceQuote> cloudRouter(String packageCode, MetroCode metro, Term term) {
        BigDecimal monthly = packageCode == null ? null : routerByPackage.get(packageCode);
        if (monthly == null) {
            monthly = routerByPackage.get("STANDARD");
        }
        if (monthly == null) {
            return Optional.empty();
        }
        return Optional.of(PriceQuote.monthly(monthly, currency, PriceSource.REFERENCE)
                .withNote("reference Cloud Router"));
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

    /** The data the card was compiled as of (e.g. {@code "2026-06"}). */
    public String asOf() {
        return asOf;
    }

    /** The reference-data disclaimer. */
    public String disclaimer() {
        return disclaimer;
    }

    /** A named on-prem reference figure (e.g. {@code "transitPerMbpsMonth"}, {@code "powerPerKwMonth"}). */
    public Optional<BigDecimal> onPrem(String key) {
        return Optional.ofNullable(onPrem.get(key));
    }

    /** The indicative Equinix cross-connect monthly fee. */
    public Optional<BigDecimal> equinixCrossConnectMonthly() {
        return Optional.ofNullable(equinixCrossConnectMonthly);
    }

    /**
     * The indicative monthly fee for a cloud provider's interconnect port/circuit
     * (AWS Direct Connect / Azure ExpressRoute / GCP Interconnect) at or above the
     * requested bandwidth.
     *
     * @param provider      the cloud provider
     * @param bandwidthMbps the desired bandwidth
     * @return the reference monthly port fee, or empty if none is tabulated
     */
    public Optional<BigDecimal> cspInterconnectPortMonthly(CloudProviderType provider, int bandwidthMbps) {
        NavigableMap<Integer, BigDecimal> tiers = cspPortByBandwidth.get(provider);
        if (tiers == null || tiers.isEmpty()) {
            return Optional.empty();
        }
        Map.Entry<Integer, BigDecimal> tier = tiers.ceilingEntry(bandwidthMbps);
        if (tier == null) {
            tier = tiers.floorEntry(bandwidthMbps);
        }
        return tier == null ? Optional.empty() : Optional.of(tier.getValue());
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
