package com.eqixiac.equinix.design.optimizer;

import com.eqixiac.equinix.FabricGateway;
import com.eqixiac.equinix.core.enums.MetroCode;
import com.eqixiac.equinix.core.enums.Region;
import com.eqixiac.equinix.core.http.response.PaginatedFilteredList;
import com.eqixiac.equinix.core.http.response.PaginatedList;
import com.eqixiac.equinix.core.model.MetroId;
import com.eqixiac.equinix.design.optimizer.model.OptimizationResult;
import com.eqixiac.equinix.design.value.ratecard.ReferenceRateCard;
import com.eqixiac.equinix.fabric.client.CloudRouters;
import com.eqixiac.equinix.fabric.client.Connections;
import com.eqixiac.equinix.fabric.client.Metros;
import com.eqixiac.equinix.fabric.client.Prices;
import com.eqixiac.equinix.fabric.client.RoutingProtocols;
import com.eqixiac.equinix.fabric.client.ServiceProfiles;
import com.eqixiac.equinix.fabric.enums.MetroPresence;
import com.eqixiac.equinix.fabric.enums.MetroType;
import com.eqixiac.equinix.fabric.enums.ServiceProfileType;
import com.eqixiac.equinix.fabric.model.Metro;
import com.eqixiac.equinix.fabric.model.ServiceProfile;
import com.eqixiac.equinix.fabric.model.ServiceProfileAction;
import com.eqixiac.equinix.fabric.model.implementation.ConnectedMetro;
import com.eqixiac.equinix.fabric.model.implementation.GeoCoordinate;
import com.eqixiac.equinix.fabric.model.implementation.ServiceMetro;
import com.eqixiac.equinix.fabric.model.implementation.filter.FilterPropertyList;
import com.eqixiac.equinix.fabric.model.implementation.sort.SortPropertyList;
import com.eqixiac.equinix.fabric.model.json.creators.ServiceProfileOperator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Proves the optimizer ranks metros that are not listed by {@link MetroCode}. The engine keys its
 * scoring graph by {@link MetroId}, so two brand-new metros (both {@link MetroCode#UNKNOWN} as an
 * enum, but distinct as ids) are scored and recommended distinctly instead of colliding on
 * {@code UNKNOWN}. This test reads {@code MetroRecommendation.getMetroId()} and therefore only
 * compiles against the {@code MetroId}-keyed optimizer — it locks the rekey against regression.
 */
class MetroOptimizerForwardCompatTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    @DisplayName("two metros absent from the enum are ranked distinctly, not collapsed to UNKNOWN")
    void newMetrosRankDistinctly() throws Exception {
        Metro dc = metro("DC", 39.0438, -77.4874, "Ashburn");   // known
        Metro zz = metro("ZZ", 1.0, 2.0, "New Metro One");      // not in the enum
        Metro yy = metro("YY", 3.0, 4.0, "New Metro Two");      // not in the enum

        FabricGateway gateway = gatewayReturning(dc, zz, yy);

        OptimizationResult result = MetroOptimizer.builder(gateway)
                .addSite("HQ").nearestMetro(MetroCode.DC).headcount(100).done()
                .rateCard(ReferenceRateCard.standard())
                .optimize();

        List<String> codes = result.getRecommendations().stream()
                .map(r -> r.getMetroId().code())
                .collect(Collectors.toList());

        assertTrue(codes.contains("ZZ"), "the first unlisted metro was ranked: " + codes);
        assertTrue(codes.contains("YY"), "the second unlisted metro was ranked: " + codes);
        // Pre-rekey both would have been MetroCode.UNKNOWN and collided; assert all keys are distinct.
        assertEquals(codes.size(), new HashSet<>(codes).size(), "no UNKNOWN collision: " + codes);
    }

    private static Metro metro(String code, double lat, double lng, String name) throws Exception {
        GeoCoordinate geo = MAPPER.readValue(
                "{\"latitude\":" + lat + ",\"longitude\":" + lng + "}", GeoCoordinate.class);
        return new Metro() {
            @Override public MetroCode getCode() { return MetroCode.fromCode(code); }
            @Override public MetroId metroId() { return MetroId.of(code); }
            @Override public MetroType getType() { return null; }
            @Override public String getName() { return name; }
            @Override public String getHref() { return null; }
            @Override public Region getRegion() { return Region.AMER; }
            @Override public List<String> getIbxs() { return List.of(); }
            @Override public GeoCoordinate geoCoordinates() { return geo; }
            @Override public List<ConnectedMetro> getConnectedMetros() { return List.of(); }
            @Override public String getCountry() { return null; }
            @Override public Long getEquinixAsn() { return null; }
            @Override public Long getLocalVCBandwidthMax() { return null; }
            @Override public List<com.eqixiac.equinix.fabric.model.implementation.MetroService> getServices() { return List.of(); }
            @Override public List<com.eqixiac.equinix.fabric.enums.GeoScopeType> getGeoScopes() { return List.of(); }
            @Override public List<com.eqixiac.equinix.fabric.model.implementation.GeoZone> getGeoZones() { return List.of(); }
            @Override public Metro refresh() { return this; }
        };
    }

    private static FabricGateway gatewayReturning(Metro... metros) {
        Metros metrosClient = new Metros() {
            @Override public PaginatedList<Metro> list() {
                return new PaginatedList<>(List.of(metros), null, null, null, null);
            }
            @Override public PaginatedList<Metro> list(MetroPresence presence) { return list(); }
            @Override public Metro getByMetroCode(MetroCode metroCode) { return null; }
            @Override public Metro getByMetroCode(String metroCode) { return null; }
            @Override public Metro getByMetroId(MetroId metroId) { return null; }
        };
        ServiceProfiles serviceProfiles = new ServiceProfiles() {
            @Override public PaginatedList<ServiceProfile> list() { return null; }
            @Override public PaginatedFilteredList<ServiceProfile> search() {
                return new PaginatedFilteredList<>(List.<ServiceProfile>of(), null, null, null, null);
            }
            @Override public PaginatedFilteredList<ServiceProfile> search(FilterPropertyList filter) { return search(); }
            @Override public PaginatedFilteredList<ServiceProfile> search(SortPropertyList sort) { return search(); }
            @Override public PaginatedFilteredList<ServiceProfile> search(FilterPropertyList filter, SortPropertyList sort) { return search(); }
            @Override public ServiceProfile getByUuid(String uuid) { return null; }
            @Override public ServiceProfileOperator.ServiceProfileBuilder define(ServiceProfileType serviceProfileType) { return null; }
            @Override public ServiceProfileOperator.ServiceProfileUpdater update(String uuid) { return null; }
            @Override public ServiceProfileAction createAction(String uuid, String type, String description) { return null; }
            @Override public List<ServiceMetro> getMetros(String uuid) { return null; }
        };
        return new FabricGateway() {
            @Override public Metros metros() { return metrosClient; }
            @Override public ServiceProfiles serviceProfiles() { return serviceProfiles; }
            @Override public CloudRouters cloudRouters() { return null; }
            @Override public Connections connections() { return null; }
            @Override public RoutingProtocols routingProtocols() { return null; }
            @Override public Prices prices() { return null; }
        };
    }
}
