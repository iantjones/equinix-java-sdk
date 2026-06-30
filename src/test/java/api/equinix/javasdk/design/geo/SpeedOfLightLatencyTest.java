package api.equinix.javasdk.design.geo;

import api.equinix.javasdk.design.geo.SpeedOfLightLatency.Mode;
import api.equinix.javasdk.fabric.model.implementation.GeoCoordinate;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    @DisplayName("invalid configuration and missing coordinates are rejected")
    void validation() {
        assertThrows(IllegalArgumentException.class, () -> SpeedOfLightLatency.builder().refractiveIndex(0));
        assertThrows(IllegalArgumentException.class, () -> SpeedOfLightLatency.builder().routeFactor(0.5));
        assertThrows(IllegalArgumentException.class,
                () -> SpeedOfLightLatency.roundTrip().millisBetween((GeoCoordinate) null, null));
    }
}
