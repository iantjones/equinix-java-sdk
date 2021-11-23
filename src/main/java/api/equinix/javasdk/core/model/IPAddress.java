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

import java.net.InetAddress;

/**
 * <p>IPAddress class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
public class IPAddress {

    InetAddress ipAddress;

    Integer subnet;

    /**
     * <p>Getter for the field <code>ipAddress</code>.</p>
     *
     * @return a {@link java.net.InetAddress} object.
     */
    public InetAddress getIpAddress() {
        return ipAddress;
    }

    /**
     * <p>Setter for the field <code>ipAddress</code>.</p>
     *
     * @param ipAddress a {@link java.net.InetAddress} object.
     */
    public void setIpAddress(InetAddress ipAddress) {
        this.ipAddress = ipAddress;
    }

    /**
     * <p>Getter for the field <code>subnet</code>.</p>
     *
     * @return a {@link java.lang.Integer} object.
     */
    public Integer getSubnet() {
        return subnet;
    }

    /**
     * <p>Setter for the field <code>subnet</code>.</p>
     *
     * @param subnet a {@link java.lang.Integer} object.
     */
    public void setSubnet(Integer subnet) {
        this.subnet = subnet;
    }

    /**
     * <p>getHostAddress.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getHostAddress() {
        return this.ipAddress.getHostAddress();
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return (this.ipAddress == null ? null : this.ipAddress.getHostAddress());
    }

    /**
     * <p>toCidr.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String toCidr() {
        return (this.ipAddress == null ? null : this.ipAddress.getHostAddress()) + (this.subnet == null ? "" : "/" + this.subnet);
    }
}
