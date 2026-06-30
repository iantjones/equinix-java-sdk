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
 * Tests {@link EquinixIXMapping} — the live, IBX-name-driven PeeringDB&rarr;metro bridge (no hardcoded
 * city table). Each Equinix facility name carries its IBX code (e.g. {@code LA4}); the bridge reads
 * that code, looks it up in the live IBX&rarr;metro map, and seeds a city&rarr;metro map through which
 * coordinate-less IXes resolve — so city aliases work without any static lookup.
 */
@DisplayName("Equinix IX Mapping (live, IBX-name driven)")
class EquinixIXMappingTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // Live IBX -> metro map (what fabric.metros() + getIbxs() would supply).
    private static Map<String, MetroCode> ibxToMetro() {
        Map<String, MetroCode> m = new LinkedHashMap<>();
        m.put("LA3", MetroCode.LA);
        m.put("LA4", MetroCode.LA);
        m.put("SV5", MetroCode.SV);
        m.put("DC11", MetroCode.DC);
        return m;
    }

    private static PeeringDbFacility fac(int id, String name, String city) throws Exception {
        return MAPPER.readValue(
                "{\"id\":" + id + ",\"name\":\"" + name + "\",\"city\":\"" + city + "\"}",
                PeeringDbFacility.class);
    }

    private static PeeringDbIx ix(int id, String city) throws Exception {
        return MAPPER.readValue("{\"id\":" + id + ",\"city\":\"" + city + "\"}", PeeringDbIx.class);
    }

    private static EquinixIXMapping built() throws Exception {
        Map<Integer, PeeringDbFacility> facs = new LinkedHashMap<>();
        facs.put(1, fac(1, "Equinix LA4 - Los Angeles", "Los Angeles"));     // exact LA4 -> LA
        facs.put(2, fac(2, "Equinix SV5 - San Jose", "San Jose"));           // exact SV5 -> SV; seeds "san jose" -> SV
        facs.put(3, fac(3, "Equinix DC11 - Ashburn", "Ashburn"));            // exact DC11 -> DC
        facs.put(4, fac(4, "Equinix SV99 - Santa Clara", "Santa Clara"));    // SV99 not listed -> prefix SV -> SV
        facs.put(5, fac(5, "Some Carrier Hotel", "Nowhere"));                // no IBX code -> unmapped

        Map<Integer, PeeringDbIx> ixes = new LinkedHashMap<>();
        ixes.put(10, ix(10, "San Jose"));     // via facility-derived city bridge -> SV (alias of "Silicon Valley")
        ixes.put(11, ix(11, "Los Angeles"));  // -> LA
        ixes.put(12, ix(12, "Atlantis"));     // no facility city match -> unmapped

        EquinixIXMapping mapping = new EquinixIXMapping(ibxToMetro());
        mapping.mapFacilities(facs);   // facilities first (resolve by IBX name, seed the city bridge)
        mapping.mapIxes(ixes);
        return mapping;
    }

    @Test
    @DisplayName("facilities resolve to their metro by the IBX code in the name")
    void facilitiesByIbxName() throws Exception {
        EquinixIXMapping m = built();
        assertEquals(MetroCode.LA, m.metroForFacility(1));
        assertEquals(MetroCode.SV, m.metroForFacility(2));
        assertEquals(MetroCode.DC, m.metroForFacility(3));
        assertEquals(MetroCode.SV, m.metroForFacility(4), "unlisted SV99 falls back to its SV prefix");
        assertNull(m.metroForFacility(5), "a name with no IBX code is unmapped");
    }

    @Test
    @DisplayName("IXes resolve via the facility-derived city bridge — aliases included")
    void ixesViaCityBridge() throws Exception {
        EquinixIXMapping m = built();
        // "San Jose" never appears as a metro name, yet resolves to SV because the SV5 facility there
        // seeded the city bridge — no hardcoded alias needed.
        assertEquals(MetroCode.SV, m.metroForIx(10));
        assertEquals(MetroCode.LA, m.metroForIx(11));
        assertNull(m.metroForIx(12), "an IX whose city has no Equinix facility is unmapped");
    }

    @Test
    @DisplayName("resolveFromName reads the IBX code, preferring an exact map hit over the prefix")
    void resolveFromName() {
        EquinixIXMapping m = new EquinixIXMapping(ibxToMetro());
        assertEquals(MetroCode.LA, m.resolveFromName("Equinix LA4 - Los Angeles"));
        assertEquals(MetroCode.DC, m.resolveFromName("Equinix DC11"));
        assertEquals(MetroCode.SV, m.resolveFromName("Equinix SV99 - Santa Clara")); // prefix fallback
        assertNull(m.resolveFromName("Some Carrier Hotel"));
        assertNull(m.resolveFromName(null));
    }

    @Test
    @DisplayName("reverse lookups and presence sets reflect the resolved bridge")
    void reverseLookups() throws Exception {
        EquinixIXMapping m = built();
        assertTrue(m.facIdsForMetro(MetroCode.SV).contains(2));
        assertTrue(m.facIdsForMetro(MetroCode.SV).contains(4));
        assertTrue(m.ixIdsForMetro(MetroCode.SV).contains(10));
        assertTrue(m.metrosWithFacilities().contains(MetroCode.LA));
        assertTrue(m.metrosWithFacilities().contains(MetroCode.SV));
        assertTrue(m.metrosWithFacilities().contains(MetroCode.DC));
        assertTrue(m.metrosWithIx().contains(MetroCode.LA));
        assertTrue(m.metrosWithIx().contains(MetroCode.SV));
    }
}
