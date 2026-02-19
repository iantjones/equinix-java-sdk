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

package api.equinix.javasdk.fabric.model.implementation.cloud;

import lombok.Getter;

/**
 * Enumeration of supported cloud providers for Equinix Fabric interconnection.
 *
 * <p>Each constant represents a cloud provider whose direct connectivity product can be
 * integrated with Equinix Fabric connections. The enum provides the display name and
 * the cloud provider's connectivity product name for reference.</p>
 *
 * @author ianjones
 * @see CloudProviderConnectionAdapter
 */
@Getter
public enum CloudProviderType {

    /**
     * Amazon Web Services &mdash; Direct Connect.
     * Uses AWS Account ID as the authentication key.
     */
    AWS("AWS Direct Connect", "Amazon Web Services"),

    /**
     * Microsoft Azure &mdash; ExpressRoute.
     * Uses ExpressRoute Service Key (GUID) as the authentication key.
     */
    AZURE("Azure ExpressRoute", "Microsoft Azure"),

    /**
     * Google Cloud Platform &mdash; Cloud Interconnect.
     * Uses GCP Pairing Key as the authentication key.
     */
    GOOGLE_CLOUD("Google Cloud Interconnect", "Google Cloud Platform"),

    /**
     * Oracle Cloud Infrastructure &mdash; FastConnect.
     * Uses Oracle OCID of the virtual circuit as the authentication key.
     */
    ORACLE_CLOUD("Oracle FastConnect", "Oracle Cloud Infrastructure"),

    /**
     * IBM Cloud &mdash; Direct Link.
     * Uses IBM Cloud Direct Link identifier as the authentication key.
     */
    IBM_CLOUD("IBM Cloud Direct Link", "IBM Cloud"),

    /**
     * Alibaba Cloud &mdash; Express Connect.
     * Uses Alibaba Cloud Account ID as the authentication key.
     */
    ALIBABA_CLOUD("Alibaba Express Connect", "Alibaba Cloud"),

    /**
     * Custom or unlisted cloud provider.
     * Use this for providers not covered by the built-in constants.
     */
    OTHER("Custom Provider", "Other");

    private final String displayName;
    private final String providerName;

    CloudProviderType(String displayName, String providerName) {
        this.displayName = displayName;
        this.providerName = providerName;
    }
}
