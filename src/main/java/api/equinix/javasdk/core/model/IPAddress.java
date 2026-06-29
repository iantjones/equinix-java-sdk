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

package api.equinix.javasdk.core.model;

import api.equinix.javasdk.core.exception.EquinixClientException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * A typed IP address value, optionally with a CIDR subnet prefix length.
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
 * @author ianjones
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class IPAddress {

    private InetAddress ipAddress;

    private Integer subnet;

    /**
     * Parses a literal IPv4/IPv6 address, optionally in CIDR form (e.g. {@code "203.0.113.0/24"}).
     *
     * @param value the literal IP address, optionally suffixed with {@code /<prefixLength>}
     * @return a populated {@link IPAddress}
     * @throws EquinixClientException if the value is null/blank or not a valid IP address
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

        try {
            return new IPAddress(InetAddress.getByName(host), subnet);
        } catch (UnknownHostException uhe) {
            throw new EquinixClientException("Invalid IP address: " + value, uhe);
        }
    }

    /**
     *
     * @return the numeric host address, or {@code null} if unset
     */
    public String getHostAddress() {
        return this.ipAddress == null ? null : this.ipAddress.getHostAddress();
    }

    @Override
    public String toString() {
        return getHostAddress();
    }

    /**
     * Returns the address in CIDR notation ({@code host/subnet}), or just the host if no subnet is set.
     *
     * @return the CIDR (or host) string, or {@code null} if unset
     */
    public String toCidr() {
        return getHostAddress() + (this.subnet == null ? "" : "/" + this.subnet);
    }
}
