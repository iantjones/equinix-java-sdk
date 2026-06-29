package api.equinix.javasdk.design.optimizer.wizard.model;

import api.equinix.javasdk.fabric.enums.RoutingProtocolType;
import lombok.Builder;
import lombok.Value;

/**
 * A routing protocol configuration to be applied to a planned connection.
 * Each connection typically receives both a DIRECT protocol (IP assignment)
 * and a BGP protocol (dynamic routing).
 */
@Value
@Builder
public class PlannedRoutingProtocol {

    /** The protocol type (BGP, DIRECT). */
    RoutingProtocolType type;

    /** Display name for this protocol instance. */
    String name;

    /** The planned connection this protocol will be attached to. */
    String connectionName;

    /** Customer-side peer IPv4 address (for BGP). */
    String customerPeerIpv4;

    /** Equinix-side peer IPv4 address (for BGP). */
    String equinixPeerIpv4;

    /** Equinix interface IPv4 address (for DIRECT). */
    String equinixIfaceIpv4;

    /** Customer ASN for BGP peering. */
    Long customerAsn;

    /** Whether BFD (Bidirectional Forwarding Detection) is enabled. */
    boolean bfdEnabled;

    /** BFD interval in milliseconds. */
    int bfdInterval;
}
