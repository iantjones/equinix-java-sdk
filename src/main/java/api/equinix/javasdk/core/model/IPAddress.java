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

import lombok.Getter;
import lombok.Setter;

import java.net.InetAddress;

/**
 * <p>IPAddress class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
@Getter
@Setter
public class IPAddress {

    private InetAddress ipAddress;

    private Integer subnet;

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
