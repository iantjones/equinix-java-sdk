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

package com.eqixiac.equinix.fabric.model.json.creators;

import com.eqixiac.equinix.fabric.enums.EiaRoutingProtocolType;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * Routing protocol configuration for an Equinix Internet Access (EIA) service create request.
 *
 * <p>Faithful to the spec's {@code InternetAccessRoutingProtocolRequest} (and its BGP variant):
 * a {@code type} discriminator, a list of customer routes referencing IP block UUIDs, a list of
 * connection UUIDs, plus the BGP-only fields ({@code exportPolicy}, the {@code asn} reference and
 * {@code bgpAuthKey}) which are simply omitted (NON_NULL) for the {@code DIRECT} and
 * {@code STATIC} variants.</p>
 *
 * <p>The spec-deprecated {@code customerAsn} and {@code customerAsnRange} are not sent: name the
 * customer ASN with {@link #asn(String)}, the uuid of an ASN resource ({@code /fabric/v4/asns}).
 * {@code bgpAuthKey} stays on the request, where 4.34.0 does not deprecate it; on the response it
 * is replaced by {@code bgpAuthType}.</p>
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EiaRoutingProtocolRequest {

    @JsonProperty("type")
    private final EiaRoutingProtocolType type;

    @JsonProperty("customerRoutes")
    private final List<CustomerRoute> customerRoutes = new ArrayList<>();

    @JsonProperty("connections")
    private final List<Connection> connections = new ArrayList<>();

    @JsonProperty("exportPolicy")
    private String exportPolicy;

    @JsonProperty("asn")
    private AsnRef asn;

    @JsonProperty("bgpAuthKey")
    private String bgpAuthKey;

    /**
     * Creates a routing protocol request of the given type.
     *
     * @param type the routing protocol type (BGP, DIRECT, or STATIC)
     */
    public EiaRoutingProtocolRequest(EiaRoutingProtocolType type) {
        this.type = type;
    }

    /**
     * Adds a customer route referencing an existing IP block by UUID.
     *
     * @param ipBlockUuid the UUID of the IP block to advertise
     * @return this request
     */
    public EiaRoutingProtocolRequest addCustomerRoute(String ipBlockUuid) {
        this.customerRoutes.add(new CustomerRoute(new IpBlockRef(ipBlockUuid)));
        return this;
    }

    /**
     * Adds a connection reference by UUID.
     *
     * @param connectionUuid the UUID of the connection
     * @return this request
     */
    public EiaRoutingProtocolRequest addConnection(String connectionUuid) {
        this.connections.add(new Connection(connectionUuid));
        return this;
    }

    /**
     * Sets the BGP export policy (e.g. {@code FULL}, {@code DEFAULT}, {@code FULL_DEFAULT}, {@code PARTIAL}).
     *
     * @param exportPolicy the export policy
     * @return this request
     */
    public EiaRoutingProtocolRequest exportPolicy(String exportPolicy) {
        this.exportPolicy = exportPolicy;
        return this;
    }

    /**
     * Sets the ASN resource the BGP session uses ({@code "asn": {"uuid": ...}}).
     *
     * @param asnUuid the uuid of an ASN resource
     * @return this request
     */
    public EiaRoutingProtocolRequest asn(String asnUuid) {
        if (asnUuid == null || asnUuid.isBlank()) {
            throw new IllegalArgumentException("asnUuid is required");
        }
        this.asn = new AsnRef(asnUuid);
        return this;
    }

    /**
     * Sets the BGP authentication key.
     *
     * @param bgpAuthKey the BGP authentication key
     * @return this request
     */
    public EiaRoutingProtocolRequest bgpAuthKey(String bgpAuthKey) {
        this.bgpAuthKey = bgpAuthKey;
        return this;
    }


    @Getter
    public static class CustomerRoute {
        @JsonProperty("ipBlock")
        private final IpBlockRef ipBlock;

        public CustomerRoute(IpBlockRef ipBlock) {
            this.ipBlock = ipBlock;
        }
    }

    @Getter
    public static class IpBlockRef {
        @JsonProperty("uuid")
        private final String uuid;

        public IpBlockRef(String uuid) {
            this.uuid = uuid;
        }
    }

    /** ASN resource reference: {@code {"uuid": ...}}. */
    @Getter
    public static class AsnRef {
        @JsonProperty("uuid")
        private final String uuid;

        /**
         * Creates the reference.
         *
         * @param uuid the ASN resource uuid
         */
        public AsnRef(String uuid) {
            this.uuid = uuid;
        }
    }

    @Getter
    public static class Connection {
        @JsonProperty("uuid")
        private final String uuid;

        public Connection(String uuid) {
            this.uuid = uuid;
        }
    }
}
