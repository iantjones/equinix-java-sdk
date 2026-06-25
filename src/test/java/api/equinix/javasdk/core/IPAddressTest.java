package api.equinix.javasdk.core;

import api.equinix.javasdk.core.exception.EquinixClientException;
import api.equinix.javasdk.core.model.IPAddress;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link IPAddress} value type and its {@link IPAddress#parse(String)} factory.
 */
class IPAddressTest {

    @Test
    void parsesPlainIpv4() {
        IPAddress ip = IPAddress.parse("203.0.113.10");
        assertEquals("203.0.113.10", ip.getHostAddress());
        assertNull(ip.getSubnet());
        assertEquals("203.0.113.10", ip.toCidr());
        assertEquals("203.0.113.10", ip.toString());
    }

    @Test
    void parsesCidr() {
        IPAddress ip = IPAddress.parse("203.0.113.0/24");
        assertEquals("203.0.113.0", ip.getHostAddress());
        assertEquals(24, ip.getSubnet());
        assertEquals("203.0.113.0/24", ip.toCidr());
    }

    @Test
    void parsesIpv6() {
        IPAddress ip = IPAddress.parse("2001:db8::1");
        assertNotNull(ip.getHostAddress());
    }

    @Test
    void rejectsNullOrBlank() {
        assertThrows(EquinixClientException.class, () -> IPAddress.parse(null));
        assertThrows(EquinixClientException.class, () -> IPAddress.parse("   "));
    }

    @Test
    void rejectsNonNumericSubnet() {
        // Subnet is parsed before any address resolution, so this fails fast without DNS.
        assertThrows(EquinixClientException.class, () -> IPAddress.parse("203.0.113.0/xx"));
    }
}
