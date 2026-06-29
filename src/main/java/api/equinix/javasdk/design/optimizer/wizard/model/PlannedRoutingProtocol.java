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

    RoutingProtocolType type;

    String name;

    String connectionName;

    String customerPeerIpv4;

    String equinixPeerIpv4;

    String equinixIfaceIpv4;

    Long customerAsn;

    boolean bfdEnabled;

    int bfdInterval;
}
