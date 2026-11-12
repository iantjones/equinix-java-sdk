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

package com.eqixiac.equinix.design.optimizer.context;

import com.eqixiac.equinix.FabricGateway;
import com.eqixiac.equinix.core.http.response.PaginatedFilteredList;
import com.eqixiac.equinix.core.http.response.PaginatedList;
import com.eqixiac.equinix.core.model.MetroId;
import com.eqixiac.equinix.design.optimizer.model.ProviderAvailability;
import com.eqixiac.equinix.design.spec.CloudIntent;
import com.eqixiac.equinix.design.spec.DesignSpec;
import com.eqixiac.equinix.design.spi.CostModel;
import com.eqixiac.equinix.design.spi.MetroFacts;
import com.eqixiac.equinix.fabric.enums.GeoScopeType;
import com.eqixiac.equinix.fabric.model.Metro;
import com.eqixiac.equinix.fabric.model.ServiceProfile;
import com.eqixiac.equinix.fabric.model.implementation.ConnectedMetro;
import com.eqixiac.equinix.fabric.model.implementation.GeoCoordinate;
import com.eqixiac.equinix.fabric.model.implementation.GeoZone;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Builds a {@link CatalogDesignContext} from live Fabric data, read-only, once per run: every page of
 * {@code GET /fabric/v4/metros} (coordinates, region, country, geo scopes and zones, measured
 * {@code connectedMetros.avgLatency}) and, when the spec names clouds, every page of the service
 * profile search (which metros each cloud is reachable from).
 *
 * <p>Both traversals are bounded ({@value #MAX_PAGES} pages). A traversal that stops on the bound or
 * on a paging failure keeps what it read and says so in {@link CatalogDesignContext#dataNotes()};
 * a partial catalogue is never presented as complete.</p>
 */
public final class FabricCatalogLoader {

    /** Most pages read from one catalogue (100 items per page). */
    public static final int MAX_PAGES = 100;

    private FabricCatalogLoader() {
    }

    /**
     * @param fabric    the Fabric API (only read)
     * @param spec      the design
     * @param costModel the cost model the context hands to plug-ins; may be {@code null}
     * @return the context
     */
    public static CatalogDesignContext load(FabricGateway fabric, DesignSpec spec, CostModel costModel) {
        CatalogDesignContext.Builder builder = CatalogDesignContext.builder(spec).costModel(costModel)
                .asOf(Instant.now());

        Scan<Metro> metros = scan("metro", fabric.metros().list());
        builder.note(metros.note());
        for (Metro metro : metros.items) {
            MetroId id = metro.metroId();
            if (id == null) {
                continue;
            }
            GeoCoordinate geo = metro.geoCoordinates();
            builder.metro(new MetroFacts(id, metro.getName(), metro.getRegion(), metro.getCountry(),
                    geo == null ? null : geo.getLatitude(), geo == null ? null : geo.getLongitude(), zonesOf(metro)));
            if (metro.getConnectedMetros() != null) {
                for (ConnectedMetro connected : metro.getConnectedMetros()) {
                    if (connected != null && connected.metroId() != null && connected.getAvgLatency() != null) {
                        builder.measuredLatency(id.code(), connected.metroId().code(), connected.getAvgLatency());
                    }
                }
            }
        }

        if (!spec.clouds().isEmpty()) {
            Scan<ServiceProfile> profiles = scan("service profile", fabric.serviceProfiles().search());
            builder.note(profiles.note());
            ProviderCatalog catalog = ProviderCatalog.resolve(spec.clouds(), profiles.items);
            for (Map.Entry<String, Map<MetroId, ProviderAvailability>> cloud : catalog.byCloud().entrySet()) {
                cloud.getValue().forEach((metro, availability) ->
                        builder.providerAvailability(metro, cloud.getKey(), availability));
            }
            for (CloudIntent cloud : spec.clouds()) {
                String reason = catalog.unresolvedReason(cloud);
                if (reason != null) {
                    builder.note("Cloud '" + cloud.id() + "' is reachable from no metro: " + reason
                            + (profiles.complete ? "." : " (the profile catalogue was not read in full)."));
                }
            }
        }
        return builder.build();
    }

    /**
     * The zones a metro belongs to, upper-case: each Fabric geo scope by name and by its ISO-style
     * alias ({@code SWITZERLAND} and {@code CH}, {@code CONUS} and {@code US}...), plus each geo zone's
     * code and name, plus zones derived from the metro's country: its ISO country code (so {@code SG} or
     * {@code IE} can be named directly), {@code EU} for a member state and {@code UK} for {@code GB}.
     * The derived zones matter because 20 of the 67 live metros carried no geo scope or zone on
     * 2026-11-12. The geoZones constraint matches against these.
     *
     * @param metro a live metro
     * @return the zone names
     */
    public static Set<String> zonesOf(Metro metro) {
        Set<String> zones = new LinkedHashSet<>();
        if (metro.getGeoScopes() != null) {
            for (GeoScopeType scope : metro.getGeoScopes()) {
                if (scope == null || scope == GeoScopeType.UNKNOWN) {
                    continue;
                }
                zones.add(scope.name());
                String alias = alias(scope);
                if (alias != null) {
                    zones.add(alias);
                }
            }
        }
        if (metro.getGeoZones() != null) {
            for (GeoZone zone : metro.getGeoZones()) {
                if (zone == null) {
                    continue;
                }
                if (zone.getCode() != null && !zone.getCode().isBlank()) {
                    zones.add(zone.getCode().trim().toUpperCase(Locale.ROOT));
                }
                if (zone.getName() != null && !zone.getName().isBlank()) {
                    zones.add(zone.getName().trim().toUpperCase(Locale.ROOT));
                }
            }
        }
        String country = metro.getCountry();
        if (country != null && !country.isBlank()) {
            String code = country.trim().toUpperCase(Locale.ROOT);
            zones.add(code);
            if (EU_MEMBERS.contains(code)) {
                zones.add("EU");
            }
            if ("GB".equals(code)) {
                zones.add("UK");
            }
        }
        return zones;
    }

    /**
     * ISO 3166-1 alpha-2 codes of the 27 European Union member states. Used because the live catalogue
     * omits the {@code EU} geo scope for some member-state metros (Dublin and Milan carried no geo
     * scope or zone on 2026-11-12), which would wrongly exclude them from an {@code EU} design.
     */
    private static final Set<String> EU_MEMBERS = Set.of("AT", "BE", "BG", "HR", "CY", "CZ", "DK", "EE", "FI",
            "FR", "DE", "GR", "HU", "IE", "IT", "LV", "LT", "LU", "MT", "NL", "PL", "PT", "RO", "SK", "SI", "ES",
            "SE");

    private static String alias(GeoScopeType scope) {
        return switch (scope) {
            case CANADA -> "CA";
            case CONUS -> "US";
            case JAPAN -> "JP";
            case AUSTRALIA -> "AU";
            case BRAZIL -> "BR";
            case SWITZERLAND -> "CH";
            default -> null;
        };
    }

    private record Scan<T>(String catalogue, List<T> items, boolean complete, String failure) {
        String note() {
            if (complete) {
                return "The Fabric " + catalogue + " catalogue was read to its last page (" + items.size() + " items).";
            }
            return failure == null
                    ? "The Fabric " + catalogue + " catalogue scan stopped at " + items.size() + " items on the "
                            + MAX_PAGES + "-page bound: it was NOT read in full."
                    : "The Fabric " + catalogue + " catalogue scan stopped at " + items.size() + " items after a "
                            + "paging failure (" + failure + "): it was NOT read in full.";
        }
    }

    private static <T> Scan<T> scan(String catalogue, PaginatedList<T> page) {
        int pages = 1;
        while (page.hasNextPage() && pages < MAX_PAGES) {
            try {
                page.next();
            } catch (RuntimeException e) {
                return new Scan<>(catalogue, new ArrayList<>(page.toList()), false, describe(e));
            }
            pages++;
        }
        return new Scan<>(catalogue, new ArrayList<>(page.toList()), !page.hasNextPage(), null);
    }

    private static <T> Scan<T> scan(String catalogue, PaginatedFilteredList<T> page) {
        int pages = 1;
        while (page.hasNextPage() && pages < MAX_PAGES) {
            try {
                page.next();
            } catch (RuntimeException e) {
                return new Scan<>(catalogue, new ArrayList<>(page.toList()), false, describe(e));
            }
            pages++;
        }
        return new Scan<>(catalogue, new ArrayList<>(page.toList()), !page.hasNextPage(), null);
    }

    private static String describe(RuntimeException e) {
        String message = e.getMessage() == null ? "" : ": " + e.getMessage().replaceAll("\\s+", " ").trim();
        String text = e.getClass().getSimpleName() + message;
        return text.length() > 200 ? text.substring(0, 197) + "..." : text;
    }
}
