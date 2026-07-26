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

package com.eqixiac.equinix.core.model;

import com.eqixiac.equinix.core.exception.EquinixClientException;
import lombok.Getter;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Objects;

/**
 * An immutable, typed IP address value, optionally with a CIDR subnet prefix length.
 *
 * <p>Provided as a type-safe alternative to passing raw IP strings into resource builders
 * (for example Network Edge BGP peering local/remote IPs, VPN peer/tunnel IPs, device
 * system IPs, and route-aggregation prefixes). Construct one from a literal address with
 * {@link #parse(String)}:</p>
 *
 * <pre>{@code
 * IPAddress peer    = IPAddress.parse("203.0.113.10");
 * IPAddress network = IPAddress.parse("203.0.113.0/24");
 * String cidr       = network.toCidr();   // "203.0.113.0/24"
 * }</pre>
 *
 * <p>{@link #parse(String)} accepts <em>literals only</em> — a strict IPv4 dotted-quad or an
 * IPv6 address — and never performs a DNS lookup. Hostnames, legacy {@code inet_addr}
 * shorthand (e.g. {@code "203.0.113"}) and malformed addresses are rejected with an
 * {@link EquinixClientException}.</p>
 *
 * @author ianjones
 */
@Getter
public final class IPAddress {

    private final InetAddress ipAddress;

    private final Integer subnet;

    private IPAddress(InetAddress ipAddress, Integer subnet) {
        this.ipAddress = ipAddress;
        this.subnet = subnet;
    }

    /**
     * Parses a literal IPv4/IPv6 address, optionally in CIDR form (e.g. {@code "203.0.113.0/24"}).
     *
     * <p>Validation is strict and purely local: IPv4 must be a full dotted-quad (four decimal
     * octets, each 0–255) and IPv6 must be a valid RFC 4291 literal (including {@code ::}
     * compression and embedded IPv4 forms). No DNS resolution is ever attempted, so a hostname
     * such as {@code "example.com"} fails immediately. When a {@code /<prefixLength>} suffix is
     * present it must be within {@code 0–32} for IPv4 or {@code 0–128} for IPv6.</p>
     *
     * @param value the literal IP address, optionally suffixed with {@code /<prefixLength>}
     * @return a populated {@link IPAddress}
     * @throws EquinixClientException if the value is null/blank, not a literal IP address, or has
     *         an out-of-range subnet prefix
     */
    public static IPAddress parse(String value) {
        if (value == null || value.isBlank()) {
            throw new EquinixClientException("IP address string must not be null or blank.");
        }

        String host = value.trim();
        Integer subnet = null;

        int slash = host.indexOf('/');
        if (slash >= 0) {
            try {
                subnet = Integer.valueOf(host.substring(slash + 1).trim());
            } catch (NumberFormatException nfe) {
                throw new EquinixClientException("Invalid CIDR subnet in IP address: " + value, nfe);
            }
            host = host.substring(0, slash).trim();
        }

        boolean ipv6 = host.indexOf(':') >= 0;
        if (ipv6 ? !isValidIPv6Literal(host) : !isValidIPv4Literal(host)) {
            throw new EquinixClientException("Not a literal IPv4/IPv6 address: " + value
                    + " (hostnames are not accepted and are never resolved)");
        }

        if (subnet != null) {
            int maxPrefix = ipv6 ? 128 : 32;
            if (subnet < 0 || subnet > maxPrefix) {
                throw new EquinixClientException("CIDR subnet prefix out of range (0-" + maxPrefix
                        + ") in IP address: " + value);
            }
        }

        try {
            // The literal form is fully validated above, so getByName parses it locally —
            // it cannot fall through to a DNS lookup.
            return new IPAddress(InetAddress.getByName(host), subnet);
        } catch (UnknownHostException uhe) {
            throw new EquinixClientException("Invalid IP address: " + value, uhe);
        }
    }

    /**
     * Strict dotted-quad check: exactly four decimal octets, each 1–3 digits and 0–255.
     * Rejects legacy {@code inet_addr} shorthand such as {@code "203.0.113"}.
     */
    private static boolean isValidIPv4Literal(String host) {
        int octets = 0;
        int start = 0;
        int length = host.length();
        for (int i = 0; i <= length; i++) {
            if (i == length || host.charAt(i) == '.') {
                int digits = i - start;
                if (digits < 1 || digits > 3) {
                    return false;
                }
                int octet = 0;
                for (int j = start; j < i; j++) {
                    char c = host.charAt(j);
                    if (c < '0' || c > '9') {
                        return false;
                    }
                    octet = octet * 10 + (c - '0');
                }
                if (octet > 255) {
                    return false;
                }
                octets++;
                start = i + 1;
            }
        }
        return octets == 4 && host.charAt(length - 1) != '.';
    }

    /**
     * RFC 4291 IPv6 literal check: up to eight 1–4 digit hex groups, at most one {@code ::}
     * compression, optionally ending in an embedded IPv4 dotted-quad. Zone indices
     * ({@code %eth0}) and bracketed forms are rejected.
     */
    private static boolean isValidIPv6Literal(String host) {
        if (host.isEmpty() || host.indexOf('%') >= 0 || host.indexOf('[') >= 0 || host.indexOf(']') >= 0) {
            return false;
        }

        int doubleColon = host.indexOf("::");
        if (doubleColon >= 0 && host.indexOf("::", doubleColon + 1) >= 0) {
            return false; // more than one "::"
        }

        // A single leading/trailing ':' is only legal as part of "::".
        if ((host.startsWith(":") && !host.startsWith("::"))
                || (host.endsWith(":") && !host.endsWith("::"))) {
            return false;
        }

        String head;
        String tail;
        if (doubleColon >= 0) {
            head = host.substring(0, doubleColon);
            tail = host.substring(doubleColon + 2);
        } else {
            head = host;
            tail = null;
        }

        int groups = 0;

        String[] headParts = head.isEmpty() ? new String[0] : head.split(":", -1);
        for (int i = 0; i < headParts.length; i++) {
            String part = headParts[i];
            boolean lastOfAddress = (tail == null) && (i == headParts.length - 1);
            if (lastOfAddress && part.indexOf('.') >= 0) {
                // Embedded IPv4 tail (e.g. "0:0:0:0:0:ffff:203.0.113.10") counts as two groups.
                if (!isValidIPv4Literal(part)) {
                    return false;
                }
                groups += 2;
            } else if (isHexGroup(part)) {
                groups++;
            } else {
                return false;
            }
        }

        if (tail != null) {
            String[] tailParts = tail.isEmpty() ? new String[0] : tail.split(":", -1);
            for (int i = 0; i < tailParts.length; i++) {
                String part = tailParts[i];
                if (i == tailParts.length - 1 && part.indexOf('.') >= 0) {
                    if (!isValidIPv4Literal(part)) {
                        return false;
                    }
                    groups += 2;
                } else if (isHexGroup(part)) {
                    groups++;
                } else {
                    return false;
                }
            }
            // "::" stands in for at least one zero group, so fewer than 8 explicit groups.
            return groups < 8;
        }
        return groups == 8;
    }

    private static boolean isHexGroup(String part) {
        if (part.isEmpty() || part.length() > 4) {
            return false;
        }
        for (int i = 0; i < part.length(); i++) {
            char c = part.charAt(i);
            boolean hex = (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
            if (!hex) {
                return false;
            }
        }
        return true;
    }

    /**
     *
     * @return the numeric host address
     */
    public String getHostAddress() {
        return this.ipAddress.getHostAddress();
    }

    @Override
    public String toString() {
        return getHostAddress();
    }

    /**
     * Returns the address in CIDR notation ({@code host/subnet}), or just the host if no subnet is set.
     *
     * @return the CIDR (or host) string
     */
    public String toCidr() {
        return getHostAddress() + (this.subnet == null ? "" : "/" + this.subnet);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof IPAddress other)) {
            return false;
        }
        return ipAddress.equals(other.ipAddress) && Objects.equals(subnet, other.subnet);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ipAddress, subnet);
    }
}
