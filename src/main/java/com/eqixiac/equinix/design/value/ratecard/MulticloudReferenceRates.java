package com.eqixiac.equinix.design.value.ratecard;

import com.eqixiac.equinix.core.model.multicloud.BandwidthTier;
import com.eqixiac.equinix.fabric.model.implementation.cloud.CloudProviderType;
import com.fasterxml.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Currency;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The bundled native-multicloud-link price data behind {@code ReferenceRateCard.multicloudLink}
 * and its {@code EgressPath.MULTICLOUD_INTERCONNECT} egress rates, parsed from
 * {@code /json/ratecard_multicloud_reference_2026_09.json}.
 *
 * <p><b>Beta.</b> The data set holds only figures copied from the providers' public pricing
 * pages; every row carries its source URL and retrieval date, and both are repeated in the note
 * of each quote built from the row.</p>
 *
 * <p>Lookup rule, per side: an exact match on provider, bandwidth, path tier (providers that use
 * one) and transport location (providers that use one). No rounding up to a larger size, no
 * interpolation and no extrapolation: the providers sell sizes whose prices are not published, so
 * a larger published size is not evidence of what a smaller link costs. A row with a missing or
 * non-numeric {@code hourly} is skipped, never read as zero.</p>
 */
final class MulticloudReferenceRates {

    private static final Currency USD = Currency.getInstance("USD");

    private final String asOf;
    private final String retrieved;
    private final String disclaimer;
    private final Currency currency;
    private final Map<CloudProviderType, ProviderMeta> providers = new EnumMap<>(CloudProviderType.class);
    private final Map<CloudProviderType, List<RateRow>> rates = new EnumMap<>(CloudProviderType.class);
    private final Map<CloudProviderType, EgressRate> dataTransfer = new EnumMap<>(CloudProviderType.class);
    private final Map<CloudProviderType, FreeTier> freeTiers = new EnumMap<>(CloudProviderType.class);
    private final Map<CloudProviderType, String> unpricedProviders = new EnumMap<>(CloudProviderType.class);

    MulticloudReferenceRates(JsonNode root) {
        this.asOf = root.path("asOf").asText(null);
        this.retrieved = root.path("retrieved").asText(null);
        this.disclaimer = root.path("disclaimer").asText(null);
        this.currency = safeCurrency(root.path("currency").asText("USD"));
        // The bundle documents the conversion its figures were checked against. Monthly amounts
        // are computed with the code constant, so a bundle that states another value is rejected
        // instead of being converted at a rate its notes would misreport.
        int bundleHours = root.path("hoursPerMonth").asInt(MulticloudLinkQuote.HOURS_PER_MONTH);
        if (bundleHours != MulticloudLinkQuote.HOURS_PER_MONTH) {
            throw new IllegalStateException("multicloud reference bundle states hoursPerMonth=" + bundleHours
                    + " but MulticloudLinkQuote.HOURS_PER_MONTH is " + MulticloudLinkQuote.HOURS_PER_MONTH);
        }

        for (JsonNode p : root.path("providers")) {
            CloudProviderType provider = parseProvider(p.path("provider").asText());
            if (provider != null) {
                providers.put(provider, new ProviderMeta(p));
            }
        }
        for (JsonNode r : root.path("rates")) {
            CloudProviderType provider = parseProvider(r.path("provider").asText());
            JsonNode hourly = r.path("hourly");
            // A missing or non-numeric hourly is UNAVAILABLE, not zero (MissingNode.decimalValue()
            // returns ZERO, which would bundle a fabricated free link).
            if (provider == null || !hourly.isNumber() || !r.path("bandwidthMbps").isNumber()) {
                continue;
            }
            rates.computeIfAbsent(provider, k -> new ArrayList<>()).add(new RateRow(r, hourly.decimalValue()));
        }
        for (JsonNode d : root.path("dataTransfer")) {
            CloudProviderType provider = parseProvider(d.path("provider").asText());
            JsonNode perGb = d.path("perGb");
            if (provider == null || !perGb.isNumber()) {
                continue;
            }
            dataTransfer.put(provider, EgressRate.of(perGb.decimalValue(), currency, PriceSource.REFERENCE)
                    .withNote("native multicloud link, " + provider + " side: \"" + d.path("statement").asText("")
                            + "\" (source " + d.path("source").asText("n/a")
                            + ", retrieved " + d.path("retrieved").asText("n/a") + ")"));
        }
        for (JsonNode f : root.path("freeTier")) {
            CloudProviderType provider = parseProvider(f.path("provider").asText());
            if (provider != null && f.path("bandwidthMbps").isNumber() && f.path("pathTier").isNumber()) {
                freeTiers.put(provider, new FreeTier(f));
            }
        }
        for (JsonNode u : root.path("unpricedProviders")) {
            CloudProviderType provider = parseProvider(u.path("provider").asText());
            if (provider != null) {
                unpricedProviders.put(provider, u.path("reason").asText("no published price bundled"));
            }
        }
    }

    String asOf() {
        return asOf;
    }

    String retrieved() {
        return retrieved;
    }

    String disclaimer() {
        return disclaimer;
    }

    /** The published per-GB rate on the native link for one provider; empty when none is bundled. */
    Optional<EgressRate> dataTransfer(CloudProviderType provider) {
        return provider == null ? Optional.empty() : Optional.ofNullable(dataTransfer.get(provider));
    }

    /** The published link sizes for a provider, across every tier and location. */
    BandwidthTier publishedSizes(CloudProviderType provider) {
        List<RateRow> rows = rates.get(provider);
        if (rows == null) {
            return BandwidthTier.none();
        }
        return BandwidthTier.of(rows.stream().map(r -> r.bandwidthMbps).collect(Collectors.toList()));
    }

    Optional<MulticloudLinkQuote> quote(MulticloudLinkRequest request) {
        if (request == null) {
            return Optional.empty();
        }
        boolean knowsA = knows(request.getProviderA());
        boolean knowsZ = knows(request.getProviderZ());
        if (!knowsA && !knowsZ) {
            return Optional.empty();
        }
        List<String> notes = new ArrayList<>();
        Side a = priceSide(request.getProviderA(), request.getRegionA(), request.getProviderZ(), request, notes);
        Side z = priceSide(request.getProviderZ(), request.getRegionZ(), request.getProviderA(), request, notes);

        MulticloudLinkQuote.MulticloudLinkQuoteBuilder quote = MulticloudLinkQuote.builder()
                .providerA(request.getProviderA()).regionA(request.getRegionA())
                .providerZ(request.getProviderZ()).regionZ(request.getRegionZ())
                .bandwidthMbps(request.getBandwidthMbps())
                .pathTier(request.getPathTier())
                .sideA(a.quote).sideAUnpricedReason(a.unpricedReason)
                .sideZ(z.quote).sideZUnpricedReason(z.unpricedReason);
        quote.note("Hourly list rates are converted to monthly at " + MulticloudLinkQuote.HOURS_PER_MONTH
                + " h/month (MulticloudLinkQuote.HOURS_PER_MONTH); a 31-day month has 744 h.");
        quote.note("Path tier " + request.getPathTier() + " is a caller input (default "
                + MulticloudLinkQuote.DEFAULT_PATH_TIER + " = local scope); AWS assigns the real tier from the "
                + "Region paths the interconnect serves and publishes no path-to-tier table.");
        quote.note("Both products bill hourly from creation to deletion with no commitment term, so the "
                + "requested term does not change the rate.");
        if (request.isUseAwsFreeTier() && !request.involves(CloudProviderType.AWS)) {
            quote.note("The AWS free tier was requested but neither end of the link is AWS, so it has no effect.");
        }
        notes.forEach(quote::note);
        return Optional.of(quote.build());
    }

    private boolean knows(CloudProviderType provider) {
        return providers.containsKey(provider) || unpricedProviders.containsKey(provider);
    }

    private Side priceSide(CloudProviderType provider, String region, CloudProviderType peer,
                           MulticloudLinkRequest request, List<String> notes) {
        ProviderMeta meta = providers.get(provider);
        if (meta == null) {
            String reason = unpricedProviders.get(provider);
            return Side.unpriced(reason != null ? reason
                    : "the bundled reference data (" + asOf + ") holds no native multicloud link price for "
                    + provider);
        }

        FreeTier freeTier = freeTiers.get(provider);
        if (request.isUseAwsFreeTier() && freeTier != null) {
            String declined = freeTier.ineligibility(request.getBandwidthMbps(), request.getPathTier(), peer);
            if (declined == null) {
                return Side.priced(PriceQuote.zero(currency, PriceSource.REFERENCE)
                        .withNote(meta.product + ", free tier applied on request: \"" + freeTier.statement
                                + "\" (source " + freeTier.source + ", retrieved " + freeTier.retrieved
                                + "). The requested " + request.getBandwidthMbps() + " Mbps is covered by the "
                                + freeTier.bandwidthMbps + " Mbps free interconnect. A second interconnect to "
                                + "the same provider in the same Region is billed at the listed rate."));
            }
            notes.add("The AWS free tier was requested but not applied: " + declined + " (source "
                    + freeTier.source + ", retrieved " + freeTier.retrieved + ").");
        }

        String location = null;
        boolean locationAssumed = false;
        if (!meta.locations.isEmpty()) {
            if (region == null) {
                location = meta.defaultLocation;
                locationAssumed = location != null;
            } else {
                location = meta.locationOf(region);
            }
            if (location == null) {
                return Side.unpriced("the " + provider + " region '" + region + "' maps to no published "
                        + "transport location (" + String.join(", ", meta.locationNames()) + "). "
                        + meta.unpublishedGuidance + " (source " + meta.source + ", retrieved "
                        + meta.retrieved + ")");
            }
        }

        RateRow match = null;
        for (RateRow row : rates.getOrDefault(provider, List.of())) {
            if (row.matches(request.getBandwidthMbps(), meta.usesPathTier ? request.getPathTier() : null, location)) {
                match = row;
                break;
            }
        }
        if (match == null) {
            return Side.unpriced("no published " + meta.product + " rate for " + request.getBandwidthMbps()
                    + " Mbps" + (meta.usesPathTier ? " at path tier " + request.getPathTier() : "")
                    + (location != null ? " in " + location : "")
                    + "; published sizes: " + publishedSizes(provider).toList() + " Mbps. "
                    + meta.unpublishedGuidance + " (source " + meta.source + ", retrieved " + meta.retrieved + ")");
        }

        BigDecimal monthly = MulticloudLinkQuote.monthlyFromHourly(match.hourly);
        StringBuilder note = new StringBuilder(meta.product).append(' ')
                .append(match.bandwidthMbps).append(" Mbps");
        if (match.pathTier != null) {
            note.append(", path tier ").append(match.pathTier);
        }
        if (location != null) {
            note.append(", ").append(location);
            if (locationAssumed) {
                note.append(" (no ").append(provider).append(" region supplied; ").append(location)
                        .append(" transport location assumed)");
            }
        }
        note.append(": ").append(MulticloudLinkQuote.formatRate(match.hourly)).append(' ').append(currency.getCurrencyCode())
                .append("/h x ").append(MulticloudLinkQuote.HOURS_PER_MONTH).append(" h/month = ")
                .append(monthly.toPlainString()).append(' ').append(currency.getCurrencyCode())
                .append("/month; source ").append(match.source).append(", retrieved ").append(match.retrieved);
        if (match.note != null) {
            note.append("; ").append(match.note);
        }
        return Side.priced(PriceQuote.monthly(monthly, currency, PriceSource.REFERENCE).withNote(note.toString()));
    }

    // ── Parsed rows ──

    private static final class Side {
        final PriceQuote quote;
        final String unpricedReason;

        private Side(PriceQuote quote, String unpricedReason) {
            this.quote = quote;
            this.unpricedReason = unpricedReason;
        }

        static Side priced(PriceQuote quote) {
            return new Side(quote, null);
        }

        static Side unpriced(String reason) {
            return new Side(null, reason);
        }
    }

    private static final class ProviderMeta {
        final String product;
        final String source;
        final String retrieved;
        final boolean usesPathTier;
        final String defaultLocation;
        final String unpublishedGuidance;
        final List<Location> locations = new ArrayList<>();

        ProviderMeta(JsonNode p) {
            this.product = p.path("product").asText("native multicloud link");
            this.source = p.path("source").asText("n/a");
            this.retrieved = p.path("retrieved").asText("n/a");
            this.usesPathTier = p.path("usesPathTier").asBoolean(false);
            this.defaultLocation = p.path("defaultLocation").asText(null);
            this.unpublishedGuidance = p.path("unpublishedGuidance").asText("");
            for (JsonNode l : p.path("locations")) {
                List<String> prefixes = new ArrayList<>();
                l.path("regionPrefixes").forEach(x -> prefixes.add(x.asText().toLowerCase(Locale.ROOT)));
                locations.add(new Location(l.path("location").asText(), prefixes));
            }
        }

        String locationOf(String region) {
            String normalized = region.trim().toLowerCase(Locale.ROOT);
            for (Location l : locations) {
                for (String prefix : l.regionPrefixes) {
                    if (normalized.startsWith(prefix)) {
                        return l.name;
                    }
                }
            }
            return null;
        }

        List<String> locationNames() {
            return locations.stream().map(l -> l.name).collect(Collectors.toList());
        }
    }

    private static final class Location {
        final String name;
        final List<String> regionPrefixes;

        Location(String name, List<String> regionPrefixes) {
            this.name = name;
            this.regionPrefixes = regionPrefixes;
        }
    }

    private static final class RateRow {
        final int bandwidthMbps;
        final Integer pathTier;
        final String location;
        final BigDecimal hourly;
        final String source;
        final String retrieved;
        final String note;

        RateRow(JsonNode r, BigDecimal hourly) {
            this.bandwidthMbps = r.path("bandwidthMbps").asInt();
            this.pathTier = r.path("pathTier").isNumber() ? r.path("pathTier").asInt() : null;
            this.location = r.path("location").asText(null);
            this.hourly = hourly;
            this.source = r.path("source").asText("n/a");
            this.retrieved = r.path("retrieved").asText("n/a");
            this.note = r.path("note").asText(null);
        }

        boolean matches(int requestedMbps, Integer requestedTier, String requestedLocation) {
            if (bandwidthMbps != requestedMbps) {
                return false;
            }
            if (requestedTier != null && (pathTier == null || !pathTier.equals(requestedTier))) {
                return false;
            }
            return requestedLocation == null ? location == null : requestedLocation.equals(location);
        }
    }

    private static final class FreeTier {
        final int bandwidthMbps;
        final int pathTier;
        final String statement;
        final String source;
        final String retrieved;
        final Set<CloudProviderType> eligiblePeers = EnumSet.noneOf(CloudProviderType.class);
        final String eligiblePeersNote;

        FreeTier(JsonNode f) {
            this.bandwidthMbps = f.path("bandwidthMbps").asInt();
            this.pathTier = f.path("pathTier").asInt();
            this.statement = f.path("statement").asText("");
            this.source = f.path("source").asText("n/a");
            this.retrieved = f.path("retrieved").asText("n/a");
            this.eligiblePeersNote = f.path("eligiblePeersNote").asText("");
            for (JsonNode peer : f.path("eligiblePeers")) {
                CloudProviderType parsed = parseProvider(peer.asText());
                if (parsed != null) {
                    eligiblePeers.add(parsed);
                }
            }
        }

        /** Why the free tier does not cover the request, or {@code null} when it does. */
        String ineligibility(int requestedMbps, int requestedTier, CloudProviderType peer) {
            if (requestedMbps > bandwidthMbps) {
                return "it covers one " + bandwidthMbps + " Mbps interconnect and " + requestedMbps
                        + " Mbps was requested";
            }
            if (requestedTier != pathTier) {
                return "it covers path tier " + pathTier + " only and tier " + requestedTier + " was requested";
            }
            if (!eligiblePeers.contains(peer)) {
                return "it covers generally-available providers only and " + peer + " is not listed as one. "
                        + eligiblePeersNote;
            }
            return null;
        }
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
