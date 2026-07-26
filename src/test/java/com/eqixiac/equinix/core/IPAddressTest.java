package com.eqixiac.equinix.core;

import com.eqixiac.equinix.core.exception.EquinixClientException;
import com.eqixiac.equinix.core.model.IPAddress;
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
    void parsesIpv6Variants() {
        assertNotNull(IPAddress.parse("::1").getHostAddress());
        assertNotNull(IPAddress.parse("::").getHostAddress());
        assertNotNull(IPAddress.parse("fe80::1").getHostAddress());
        assertNotNull(IPAddress.parse("2001:0db8:0000:0000:0000:0000:0000:0001").getHostAddress());
        assertNotNull(IPAddress.parse("::ffff:203.0.113.10").getHostAddress());
        assertNotNull(IPAddress.parse("0:0:0:0:0:ffff:203.0.113.10").getHostAddress());
        assertEquals(64, IPAddress.parse("2001:db8::/64").getSubnet());
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

    @Test
    void rejectsHostnamesWithoutDnsLookup() {
        // Hostnames must never be resolved — parse() accepts literals only.
        assertThrows(EquinixClientException.class, () -> IPAddress.parse("example.com"));
        assertThrows(EquinixClientException.class, () -> IPAddress.parse("localhost"));
        // A typo that is not a literal must fail locally, not via a DNS round-trip.
        assertThrows(EquinixClientException.class, () -> IPAddress.parse("203.O.113.10"));
    }

    @Test
    void rejectsLegacyInetAddrShorthand() {
        // InetAddress.getByName would parse "203.0.113" as 203.0.0.113; the strict
        // dotted-quad validator must reject anything that is not four full octets.
        assertThrows(EquinixClientException.class, () -> IPAddress.parse("203.0.113"));
        assertThrows(EquinixClientException.class, () -> IPAddress.parse("203"));
        assertThrows(EquinixClientException.class, () -> IPAddress.parse("203.0.113.10.5"));
        assertThrows(EquinixClientException.class, () -> IPAddress.parse("203.0.113.256"));
        assertThrows(EquinixClientException.class, () -> IPAddress.parse("203.0.113."));
    }

    @Test
    void rejectsMalformedIpv6() {
        assertThrows(EquinixClientException.class, () -> IPAddress.parse("2001:db8::1::2"));   // two "::"
        assertThrows(EquinixClientException.class, () -> IPAddress.parse("1:2:3:4:5:6:7"));    // 7 groups, no "::"
        assertThrows(EquinixClientException.class, () -> IPAddress.parse("1:2:3:4:5:6:7:8:9")); // 9 groups
        assertThrows(EquinixClientException.class, () -> IPAddress.parse("2001:db8::zzzz"));   // non-hex group
        assertThrows(EquinixClientException.class, () -> IPAddress.parse("fe80::1%eth0"));     // zone index
        assertThrows(EquinixClientException.class, () -> IPAddress.parse(":1:2:3:4:5:6:7"));   // bare leading colon
    }

    @Test
    void rejectsOutOfRangeSubnetPrefix() {
        assertThrows(EquinixClientException.class, () -> IPAddress.parse("203.0.113.0/33"));
        assertThrows(EquinixClientException.class, () -> IPAddress.parse("203.0.113.0/-1"));
        assertThrows(EquinixClientException.class, () -> IPAddress.parse("2001:db8::/129"));
        // 33-128 is legal for IPv6 even though it is not for IPv4.
        assertEquals(128, IPAddress.parse("2001:db8::1/128").getSubnet());
    }

    @Test
    void equalsAndHashCodeAreValueBased() {
        assertEquals(IPAddress.parse("203.0.113.0/24"), IPAddress.parse("203.0.113.0/24"));
        assertEquals(IPAddress.parse("203.0.113.0/24").hashCode(),
                IPAddress.parse("203.0.113.0/24").hashCode());
        assertNotEquals(IPAddress.parse("203.0.113.0/24"), IPAddress.parse("203.0.113.0/25"));
        assertNotEquals(IPAddress.parse("203.0.113.0"), IPAddress.parse("203.0.113.1"));
        assertNotEquals(IPAddress.parse("203.0.113.0/24"), IPAddress.parse("203.0.113.0"));
    }
}
