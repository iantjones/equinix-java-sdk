package api.equinix.javasdk.design.peering;

import api.equinix.javasdk.core.enums.MetroCode;
import api.equinix.javasdk.design.peering.client.PeeringDbFacility;
import api.equinix.javasdk.design.peering.client.PeeringDbIx;
import api.equinix.javasdk.design.peering.model.EquinixIXMapping;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests {@link EquinixIXMapping} — the fully live, coordinate-driven PeeringDB&rarr;metro bridge
 * (no hardcoded city table). Facilities resolve to the nearest live metro by lat/lng and seed a
 * city&rarr;metro map; IXes (city-only) resolve through that map, so city aliases work without any
 * static lookup.
 */
@DisplayName("Equinix IX Mapping (live, coordinate-driven)")
class EquinixIXMappingTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // Live Fabric metro coordinates (what fabric.metros() would supply).
    private static Map<MetroCode, double[]> metroCoords() {
        Map<MetroCode, double[]> coords = new LinkedHashMap<>();
        coords.put(MetroCode.DC, new double[]{39.0438, -77.4874});  // Ashburn
        coords.put(MetroCode.SV, new double[]{37.3382, -121.8863}); // Silicon Valley
        coords.put(MetroCode.LD, new double[]{51.5074, -0.1278});   // London
        return coords;
    }

    private static PeeringDbFacility fac(int id, String city, Double lat, Double lng) throws Exception {
        StringBuilder json = new StringBuilder("{\"id\":").append(id).append(",\"city\":\"").append(city).append("\"");
        if (lat != null) {
            json.append(",\"latitude\":").append(lat);
        }
        if (lng != null) {
            json.append(",\"longitude\":").append(lng);
        }
        return MAPPER.readValue(json.append("}").toString(), PeeringDbFacility.class);
    }

    private static PeeringDbIx ix(int id, String city) throws Exception {
        return MAPPER.readValue("{\"id\":" + id + ",\"city\":\"" + city + "\"}", PeeringDbIx.class);
    }

    private static EquinixIXMapping built() throws Exception {
        Map<Integer, PeeringDbFacility> facs = new LinkedHashMap<>();
        facs.put(1, fac(1, "Ashburn", 39.04, -77.49));     // -> DC by coordinates
        facs.put(2, fac(2, "San Jose", 37.33, -121.89));   // -> SV by coordinates (alias of metro "Silicon Valley")
        facs.put(3, fac(3, "London", 51.50, -0.13));       // -> LD by coordinates
        facs.put(4, fac(4, "Nowhere", 0.0, 0.0));          // no metro within range -> unmapped

        Map<Integer, PeeringDbIx> ixes = new LinkedHashMap<>();
        ixes.put(10, ix(10, "San Jose"));   // resolves via the facility-derived city bridge -> SV
        ixes.put(11, ix(11, "Ashburn"));    // -> DC
        ixes.put(12, ix(12, "Atlantis"));   // no facility city match -> unmapped

        EquinixIXMapping mapping = new EquinixIXMapping(metroCoords());
        mapping.mapFacilities(facs);   // facilities first (seed the city bridge)
        mapping.mapIxes(ixes);
        return mapping;
    }

    @Test
    @DisplayName("facilities bind to the nearest live metro by coordinates")
    void facilitiesByCoordinates() throws Exception {
        EquinixIXMapping m = built();
        assertEquals(MetroCode.DC, m.metroForFacility(1));
        assertEquals(MetroCode.SV, m.metroForFacility(2));
        assertEquals(MetroCode.LD, m.metroForFacility(3));
        assertNull(m.metroForFacility(4), "a facility with no metro in range is unmapped");
    }

    @Test
    @DisplayName("IXes resolve via the facility-derived city bridge — aliases included")
    void ixesViaCityBridge() throws Exception {
        EquinixIXMapping m = built();
        // "San Jose" never appears as a metro name, yet resolves to SV because a San Jose facility
        // resolved there by coordinates — no hardcoded alias needed.
        assertEquals(MetroCode.SV, m.metroForIx(10));
        assertEquals(MetroCode.DC, m.metroForIx(11));
        assertNull(m.metroForIx(12), "an IX whose city has no Equinix facility is unmapped");
    }

    @Test
    @DisplayName("nearestMetro respects the co-location distance threshold")
    void nearestMetroThreshold() throws Exception {
        EquinixIXMapping m = new EquinixIXMapping(metroCoords());
        assertEquals(MetroCode.SV, m.nearestMetro(37.33, -121.89));
        assertNull(m.nearestMetro(0.0, 0.0), "the Gulf of Guinea is not near any metro");
    }

    @Test
    @DisplayName("reverse lookups and presence sets reflect the resolved bridge")
    void reverseLookups() throws Exception {
        EquinixIXMapping m = built();
        assertTrue(m.facIdsForMetro(MetroCode.SV).contains(2));
        assertTrue(m.ixIdsForMetro(MetroCode.SV).contains(10));
        assertTrue(m.metrosWithFacilities().contains(MetroCode.DC));
        assertTrue(m.metrosWithFacilities().contains(MetroCode.SV));
        assertTrue(m.metrosWithIx().contains(MetroCode.DC));
        assertTrue(m.metrosWithIx().contains(MetroCode.SV));
    }
}
