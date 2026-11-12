/*
 * Copyright 2021 Ian Jones. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
 * OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package com.eqixiac.equinix.mcp.server;

import com.eqixiac.equinix.design.spi.CostLine;
import com.eqixiac.equinix.design.spi.PortfolioCost;
import com.eqixiac.equinix.design.value.ratecard.PriceSource;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BiConsumer;

import static com.eqixiac.equinix.mcp.server.Schemas.looseObject;

/**
 * Shared payload conventions of the design tools: the {@code detail} level, capped lists that say how
 * many entries were omitted, the {@code provenance} block every result carries, and the rendering of
 * money and cost lines.
 */
final class DesignPayloads {

    private DesignPayloads() {
    }

    /** How much a result returns. */
    enum Detail {
        /** Decision-relevant fields and short lists. */
        SUMMARY(5),
        /** Adds the supporting detail. */
        STANDARD(15),
        /** Everything, up to a hard cap per list. */
        FULL(50);

        private final int listCap;

        Detail(int listCap) {
            this.listCap = listCap;
        }

        /**
         * @return the most entries a list carries at this level
         */
        int cap() {
            return listCap;
        }

        boolean atLeast(Detail other) {
            return ordinal() >= other.ordinal();
        }

        static Detail of(JsonNode args, Detail fallback) {
            return Args.optEnum(args, "detail", Detail.class).orElse(fallback);
        }
    }

    /**
     * Writes at most {@code cap} items into {@code payload.name} and, when some are left out, a sibling
     * {@code name_omitted} count.
     *
     * @param payload the object to write into
     * @param name    the array field
     * @param items   the items
     * @param cap     the most to write
     * @param writer  renders one item into a fresh array element slot
     * @param <T>     the item type
     * @return the array
     */
    static <T> ArrayNode capped(ObjectNode payload, String name, Collection<T> items, int cap,
                                BiConsumer<ArrayNode, T> writer) {
        ArrayNode array = payload.putArray(name);
        int written = 0;
        for (T item : items) {
            if (written++ >= cap) {
                break;
            }
            writer.accept(array, item);
        }
        if (items.size() > cap) {
            payload.put(name + "_omitted", items.size() - cap);
        }
        return array;
    }

    /**
     * Adds the {@code provenance} block.
     *
     * @param payload         the result
     * @param kind            {@code live}, {@code reference}, {@code estimate}, {@code mixed} or
     *                        {@code computed} (derived by this server from inputs, no remote data)
     * @param asOf            when the underlying data was read or published, or {@code null}
     * @param specFingerprint the spec fingerprint, or {@code null} when the tool takes no spec
     * @param sources         where the figures came from
     * @return the block
     */
    static ObjectNode provenance(ObjectNode payload, String kind, String asOf, String specFingerprint,
                                 Collection<String> sources) {
        ObjectNode p = payload.putObject("provenance");
        p.put("kind", kind);
        if (asOf != null) {
            p.put("as_of", asOf);
        } else {
            p.putNull("as_of");
        }
        p.put("generated_at", Instant.now().toString());
        if (specFingerprint != null) {
            p.put("spec_fingerprint", specFingerprint);
        }
        ArrayNode s = p.putArray("sources");
        sources.forEach(s::add);
        return p;
    }

    /**
     * The provenance kind of a set of price sources: {@code live} when every figure is a live or caller
     * figure, {@code reference} or {@code estimate} when all are, else {@code mixed}; {@code computed}
     * when there are none.
     *
     * @param sources the price sources used
     * @return the kind
     */
    static String kindOf(Collection<PriceSource> sources) {
        if (sources.isEmpty()) {
            return "computed";
        }
        Set<PriceSource> set = EnumSet.copyOf(sources);
        if (set.stream().allMatch(s -> s == PriceSource.EQUINIX_LIVE || s == PriceSource.PROVIDER_API
                || s == PriceSource.CUSTOM)) {
            return "live";
        }
        if (set.equals(EnumSet.of(PriceSource.REFERENCE))) {
            return "reference";
        }
        if (set.equals(EnumSet.of(PriceSource.ESTIMATE))) {
            return "estimate";
        }
        return "mixed";
    }

    /**
     * @param cost a portfolio cost
     * @return its distinct price sources
     */
    static Set<PriceSource> sources(PortfolioCost cost) {
        Set<PriceSource> set = new TreeSet<>();
        if (cost != null) {
            cost.lines().forEach(l -> set.add(l.source()));
        }
        return set;
    }

    /**
     * Writes a portfolio cost: totals (or why there is no total), range, currency, completeness, the
     * unpriced items and, when asked, the lines.
     *
     * @param parent    the object to write into
     * @param field     the field name
     * @param cost      the cost (may be {@code null})
     * @param withLines whether to include the lines
     * @param cap       the most lines to include
     * @return the cost node
     */
    static ObjectNode cost(ObjectNode parent, String field, PortfolioCost cost, boolean withLines, int cap) {
        ObjectNode c = parent.putObject(field);
        if (cost == null) {
            c.putNull("monthly");
            c.put("note", "not priced");
            return c;
        }
        Optional<String> currency = cost.currency();
        if (currency.isPresent()) {
            c.put("currency", currency.get());
            money(c, "monthly", cost.monthlyTotal().orElse(null));
            money(c, "monthly_low", cost.monthlyLow().orElse(null));
            money(c, "monthly_high", cost.monthlyHigh().orElse(null));
            money(c, "one_time", cost.oneTimeTotal().orElse(null));
        } else {
            c.putNull("monthly");
            c.put("note", cost.lines().isEmpty() ? "no priced lines"
                    : "lines are in several currencies " + cost.currencies() + " and are never added; supply "
                    + "commercial.exchangeRates or let the converter price them");
        }
        c.put("complete", cost.isComplete());
        c.put("price_basis", kindOf(sources(cost)));
        if (!cost.unpriced().isEmpty()) {
            ArrayNode u = c.putArray("unpriced");
            cost.unpriced().stream().limit(cap).forEach(u::add);
            if (cost.unpriced().size() > cap) {
                c.put("unpriced_omitted", cost.unpriced().size() - cap);
            }
        }
        if (withLines) {
            capped(c, "lines", cost.lines(), cap, (array, line) -> line(array.addObject(), line));
        }
        return c;
    }

    /**
     * Renders one cost line.
     *
     * @param n    the node to fill
     * @param line the line
     */
    static void line(ObjectNode n, CostLine line) {
        n.put("label", line.label());
        if (line.metro() != null) {
            n.put("metro", line.metro().code());
        }
        n.put("currency", line.currency());
        money(n, "monthly", line.monthly());
        money(n, "monthly_low", line.monthlyLow());
        money(n, "monthly_high", line.monthlyHigh());
        if (line.oneTime() != null && line.oneTime().signum() != 0) {
            money(n, "one_time", line.oneTime());
        }
        n.put("source", line.source().name().toLowerCase(Locale.ROOT));
        if (line.note() != null) {
            UntrustedText.put(n, "note", line.note());
        }
        if (line.conversion() != null) {
            CostLine.Conversion cv = line.conversion();
            ObjectNode c = n.putObject("converted_from");
            c.put("currency", cv.originalCurrency());
            money(c, "monthly", cv.originalMonthly());
            c.put("rate", cv.rate());
            c.put("rate_kind", cv.kind() == null ? null : cv.kind().name().toLowerCase(Locale.ROOT));
            c.put("rate_source", cv.source());
            c.put("rate_as_of", cv.asOf());
            c.put("band_percent", cv.bandPercent());
        }
    }

    /**
     * The distinct currency conversions applied to the given costs.
     *
     * @param parent the object to write into
     * @param costs  the costs
     */
    static void conversions(ObjectNode parent, Collection<PortfolioCost> costs) {
        Map<String, CostLine.Conversion> seen = new java.util.TreeMap<>();
        for (PortfolioCost cost : costs) {
            if (cost == null) {
                continue;
            }
            for (CostLine line : cost.lines()) {
                if (line.conversion() != null) {
                    seen.putIfAbsent(line.conversion().originalCurrency() + "->" + line.currency(), line.conversion());
                }
            }
        }
        ArrayNode array = parent.putArray("currency_conversions");
        seen.forEach((key, cv) -> {
            ObjectNode c = array.addObject();
            c.put("pair", key);
            c.put("rate", cv.rate());
            c.put("kind", cv.kind() == null ? null : cv.kind().name().toLowerCase(Locale.ROOT));
            c.put("source", cv.source());
            c.put("as_of", cv.asOf());
            c.put("band_percent", cv.bandPercent());
        });
    }

    /**
     * Puts an amount rounded to cents, or {@code null} (never 0) when unknown.
     *
     * @param n     the object
     * @param field the field
     * @param value the amount, or {@code null}
     */
    static void money(ObjectNode n, String field, BigDecimal value) {
        if (value == null) {
            n.putNull(field);
        } else {
            n.put(field, value.setScale(2, RoundingMode.HALF_UP));
        }
    }

    /**
     * @param value a number
     * @return the number rounded to one decimal place
     */
    static double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    /**
     * @param value a number
     * @return the number rounded to four decimal places
     */
    static double round4(double value) {
        return Math.round(value * 10_000.0) / 10_000.0;
    }

    /**
     * Strings for a list field, capped.
     *
     * @param payload   the object
     * @param name      the field
     * @param values    the strings
     * @param cap       the most to write
     * @param untrusted whether the strings may contain remote text
     */
    static void strings(ObjectNode payload, String name, List<String> values, int cap, boolean untrusted) {
        capped(payload, name, values == null ? List.of() : values, cap, (array, v) -> {
            if (untrusted) {
                UntrustedText.add(array, v);
            } else {
                array.add(v);
            }
        });
    }

    /**
     * The output schema the design tools share: an open object that must carry the provenance block.
     *
     * @param description what the result holds
     * @param required    further top-level fields the result always has
     * @return the schema
     */
    static Map<String, Object> outputSchema(String description, String... required) {
        Map<String, Object> schema = looseObject(description);
        java.util.List<String> req = new java.util.ArrayList<>(List.of("provenance"));
        req.addAll(List.of(required));
        schema.put("required", req);
        Map<String, Object> provenance = new java.util.LinkedHashMap<>();
        provenance.put("type", "object");
        provenance.put("description", "Where the figures came from: kind (live, reference, estimate, mixed, "
                + "computed), as_of, generated_at, spec_fingerprint and sources.");
        provenance.put("required", new java.util.ArrayList<>(List.of("kind", "sources")));
        Map<String, Object> props = new java.util.LinkedHashMap<>();
        props.put("provenance", provenance);
        schema.put("properties", props);
        return schema;
    }
}
