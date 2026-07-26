package com.eqixiac.equinix.design.optimizer.wizard.enums;

/**
 * Classifies the purpose of a planned connection within a deployment plan. The purpose decides
 * which endpoint fields of a {@code PlannedConnection} are meaningful, how the connection body is
 * assembled, and its provisioning phase (provider connections before backbone links).
 */
public enum ConnectionPurpose {

    /**
     * A connection from a Cloud Router to a cloud/service provider's service profile
     * (Cloud Router A-side, service-profile Z-side). Requires the customer's cloud
     * authorization key and VLAN tag at provisioning.
     */
    PROVIDER,

    /**
     * An inter-metro connection between two Cloud Routers (Cloud Router on both sides),
     * generated from the plan's {@link BackboneTopology}. Needs no cloud authorization key.
     */
    BACKBONE,

    /**
     * A connection from a Cloud Router to an on-premises or Network Edge device.
     * Reserved for device-side planning; the wizard does not currently generate these.
     */
    DEVICE
}
