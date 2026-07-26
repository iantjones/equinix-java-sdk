package com.eqixiac.equinix.design.geo;

import com.eqixiac.equinix.core.model.MetroId;
import com.eqixiac.equinix.design.geo.SpeedOfLightLatency.Mode;
import com.eqixiac.equinix.fabric.model.Metro;
import com.eqixiac.equinix.fabric.model.implementation.GeoCoordinate;
import com.eqixiac.equinix.internetaccess.model.Ibx;
import com.eqixiac.equinix.internetaccess.model.json.IbxJson;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests {@link SpeedOfLightLatency} — the fibre speed-of-light latency-from-distance calculator.
 */
class SpeedOfLightLatencyTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    @DisplayName("default is round-trip")
    void defaultsToRoundTrip() {
        assertEquals(Mode.ROUND_TRIP, SpeedOfLightLatency.roundTrip().getMode());
        assertEquals(Mode.ROUND_TRIP, SpeedOfLightLatency.builder().build().getMode());
        assertEquals(Mode.ONE_WAY, SpeedOfLightLatency.oneWay().getMode());
    }

    @Test
    @DisplayName("one-way fibre latency ≈ 4.9 µs/km; round-trip is twice that")
    void perKm() {
        double oneWay = SpeedOfLightLatency.oneWay().millisForKm(1000);
        double rtt = SpeedOfLightLatency.roundTrip().millisForKm(1000);
        assertEquals(4.893, oneWay, 0.01);
        assertEquals(9.787, rtt, 0.01);
        assertEquals(2 * oneWay, rtt, 1e-9);
    }

    @Test
    @DisplayName("routeFactor inflates the distance")
    void routeFactor() {
        double realistic = SpeedOfLightLatency.builder().mode(Mode.ONE_WAY).routeFactor(1.4).build()
                .millisForKm(1000);
        assertEquals(6.851, realistic, 0.01);
    }

    @Test
    @DisplayName("negative distance clamps to zero")
    void negativeDistance() {
        assertEquals(0.0, SpeedOfLightLatency.roundTrip().millisForKm(-100), 1e-9);
    }

    @Test
    @DisplayName("haversine distance and latency between two coordinates")
    void betweenCoordinates() throws Exception {
        GeoCoordinate dc = MAPPER.readValue("{\"latitude\":39.0438,\"longitude\":-77.4874}", GeoCoordinate.class);
        GeoCoordinate sv = MAPPER.readValue("{\"latitude\":37.3382,\"longitude\":-121.8863}", GeoCoordinate.class);

        double km = SpeedOfLightLatency.distanceKm(dc, sv);
        assertTrue(km > 3700 && km < 4000, "DC<->SV great-circle ~3.86k km, was " + km);

        double rtt = SpeedOfLightLatency.roundTrip().millisBetween(dc, sv);
        assertEquals(SpeedOfLightLatency.roundTrip().millisForKm(km), rtt, 1e-9);
        assertTrue(rtt > 35 && rtt < 40, "DC<->SV RTT floor ~37 ms, was " + rtt);
    }

    @Test
    @DisplayName("IBX-to-IBX latency uses each IBX's own coordinates")
    void betweenIbxes() throws Exception {
        IbxJson la4 = MAPPER.readValue(
                "{\"ibxCode\":\"LA4\",\"geoCoordinates\":{\"latitude\":33.9292,\"longitude\":-118.3807}}",
                IbxJson.class);
        IbxJson sv5 = MAPPER.readValue(
                "{\"ibxCode\":\"SV5\",\"geoCoordinates\":{\"latitude\":37.4029,\"longitude\":-121.9846}}",
                IbxJson.class);

        double km = SpeedOfLightLatency.distanceKm(la4, sv5);
        assertTrue(km > 450 && km < 650, "LA4<->SV5 ~505 km, was " + km);

        double rtt = SpeedOfLightLatency.roundTrip().millisBetween(la4, sv5);
        assertEquals(SpeedOfLightLatency.roundTrip().millisForKm(km), rtt, 1e-9);
    }

    @Test
    @DisplayName("two IBXes in the same metro have a real, non-zero distance")
    void sameMetroIsNonZero() throws Exception {
        IbxJson la3 = MAPPER.readValue(
                "{\"ibxCode\":\"LA3\",\"geoCoordinates\":{\"latitude\":34.0490,\"longitude\":-118.2640}}",
                IbxJson.class);
        IbxJson la4 = MAPPER.readValue(
                "{\"ibxCode\":\"LA4\",\"geoCoordinates\":{\"latitude\":33.9292,\"longitude\":-118.3807}}",
                IbxJson.class);

        double km = SpeedOfLightLatency.distanceKm(la3, la4);
        assertTrue(km > 0 && km < 50, "intra-metro LA3<->LA4 should be small but non-zero, was " + km);
        assertTrue(SpeedOfLightLatency.roundTrip().millisBetween(la3, la4) > 0);
    }

    @Test
    @DisplayName("metro-to-metro latency uses the Fabric metro centroids")
    void betweenMetros() throws Exception {
        Metro dc = metro("DC", 39.0438, -77.4874);
        Metro sv = metro("SV", 37.3382, -121.8863);

        double km = SpeedOfLightLatency.distanceKm(dc, sv);
        assertTrue(km > 3700 && km < 4000, "DC<->SV great-circle ~3.86k km, was " + km);

        double rtt = SpeedOfLightLatency.roundTrip().millisBetween(dc, sv);
        assertEquals(SpeedOfLightLatency.roundTrip().millisForKm(km), rtt, 1e-9);

        // Metro-level math: the same metro against itself is 0 (use IBX overloads for finer grain).
        assertEquals(0.0, SpeedOfLightLatency.roundTrip().millisBetween(dc, metro("DC", 39.0438, -77.4874)), 1e-9);
    }

    @Test
    @DisplayName("mixed IBX-to-metro latency works in both directions and is symmetric")
    void betweenIbxAndMetro() throws Exception {
        IbxJson la4 = MAPPER.readValue(
                "{\"ibxCode\":\"LA4\",\"geoCoordinates\":{\"latitude\":33.9292,\"longitude\":-118.3807}}",
                IbxJson.class);
        Metro dc = metro("DC", 39.0438, -77.4874);

        double kmAtoB = SpeedOfLightLatency.distanceKm(la4, dc);
        double kmBtoA = SpeedOfLightLatency.distanceKm(dc, la4);
        assertEquals(kmAtoB, kmBtoA, 1e-9, "distance must be symmetric");
        assertTrue(kmAtoB > 3500 && kmAtoB < 3900, "LA4<->DC ~3.7k km, was " + kmAtoB);

        SpeedOfLightLatency rtt = SpeedOfLightLatency.roundTrip();
        assertEquals(rtt.millisBetween(la4, dc), rtt.millisBetween(dc, la4), 1e-9);
        assertEquals(rtt.millisForKm(kmAtoB), rtt.millisBetween(la4, dc), 1e-9);
    }

    @Test
    @DisplayName("a metro without coordinates is rejected with a message naming the metro")
    void metroMissingCoordinates() throws Exception {
        Metro dc = metro("DC", 39.0438, -77.4874);
        Metro bare = mock(Metro.class);
        when(bare.metroId()).thenReturn(MetroId.of("ZZ"));
        when(bare.geoCoordinates()).thenReturn(null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> SpeedOfLightLatency.distanceKm(dc, bare));
        assertTrue(ex.getMessage().contains("ZZ"), "message should name the metro: " + ex.getMessage());

        assertThrows(IllegalArgumentException.class,
                () -> SpeedOfLightLatency.roundTrip().millisBetween((Metro) null, dc));
    }

    private static Metro metro(String code, double lat, double lon) throws Exception {
        Metro m = mock(Metro.class);
        when(m.metroId()).thenReturn(MetroId.of(code));
        when(m.geoCoordinates()).thenReturn(MAPPER.readValue(
                "{\"latitude\":" + lat + ",\"longitude\":" + lon + "}", GeoCoordinate.class));
        return m;
    }

    @Test
    @DisplayName("invalid configuration and missing coordinates are rejected")
    void validation() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> SpeedOfLightLatency.builder().refractiveIndex(0));
        assertThrows(IllegalArgumentException.class, () -> SpeedOfLightLatency.builder().routeFactor(0.5));
        assertThrows(IllegalArgumentException.class,
                () -> SpeedOfLightLatency.roundTrip().millisBetween((GeoCoordinate) null, null));

        // An IBX with no coordinates is rejected with a clear message naming the IBX code.
        IbxJson withCoords = MAPPER.readValue(
                "{\"ibxCode\":\"LA4\",\"geoCoordinates\":{\"latitude\":33.9292,\"longitude\":-118.3807}}",
                IbxJson.class);
        IbxJson noCoords = MAPPER.readValue("{\"ibxCode\":\"ZZ9\"}", IbxJson.class);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> SpeedOfLightLatency.distanceKm(withCoords, noCoords));
        assertTrue(ex.getMessage().contains("ZZ9"), "message should name the IBX missing coordinates");

        // A geoCoordinates object present but with a null latitude/longitude is rejected too.
        IbxJson partialCoords = MAPPER.readValue(
                "{\"ibxCode\":\"YY1\",\"geoCoordinates\":{\"longitude\":-118.0}}", IbxJson.class);
        IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class,
                () -> SpeedOfLightLatency.distanceKm(withCoords, partialCoords));
        assertTrue(ex2.getMessage().contains("YY1"), "message should name the IBX with partial coordinates");

        assertThrows(IllegalArgumentException.class,
                () -> SpeedOfLightLatency.roundTrip().millisBetween((Ibx) null, withCoords));
    }
}
