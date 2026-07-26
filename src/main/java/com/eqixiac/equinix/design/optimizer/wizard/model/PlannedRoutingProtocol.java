package com.eqixiac.equinix.design.optimizer.wizard.model;

import com.eqixiac.equinix.fabric.enums.RoutingProtocolType;
import lombok.Builder;
import lombok.Value;

/**
 * A routing protocol configuration to be applied to a planned connection. Each connection
 * typically receives both a DIRECT protocol (interface IP assignment) and a BGP protocol
 * (dynamic routing); Fabric requires the DIRECT protocol to exist before BGP on the same
 * connection, an ordering both execution and the Terraform export honour.
 *
 * <p>The peering addresses are /30 pairs allocated by the wizard's plan-scoped subnet
 * allocator from the base network set via {@code DeploymentWizard.Builder.subnetBase(String)}
 * (default {@code "10.100.0.0"}).</p>
 */
@Value
@Builder
public class PlannedRoutingProtocol {

    /** The protocol kind — {@code DIRECT} (IP assignment) or {@code BGP} (dynamic routing). */
    RoutingProtocolType type;

    /** The generated protocol name. */
    String name;

    /**
     * The planned connection this protocol attaches to, by name — resolved to the connection's
     * real uuid at execution time (a protocol whose parent failed to provision is skipped).
     */
    String connectionName;

    /**
     * The customer-side BGP peering address with prefix — the {@code .2} host of the
     * connection's /30 (e.g. {@code "10.100.0.2/30"}). BGP only; {@code null} for DIRECT.
     */
    String customerPeerIpv4;

    /**
     * The Equinix-side BGP peering address with prefix — the {@code .1} host of the
     * connection's /30 (e.g. {@code "10.100.0.1/30"}). BGP only; {@code null} for DIRECT.
     */
    String equinixPeerIpv4;

    /**
     * The Equinix interface address with prefix (e.g. {@code "10.100.0.1/30"} — the same
     * {@code .1} host the BGP protocol peers from). DIRECT only; {@code null} for BGP.
     */
    String equinixIfaceIpv4;

    /** The customer ASN stamped on BGP protocols ({@code DeploymentWizard.Builder.customerAsn}). */
    Long customerAsn;

    /** Whether BFD (Bidirectional Forwarding Detection) is enabled on BGP protocols. */
    boolean bfdEnabled;

    /** The BFD interval in milliseconds; meaningful only when {@code bfdEnabled} is set. */
    int bfdInterval;
}
