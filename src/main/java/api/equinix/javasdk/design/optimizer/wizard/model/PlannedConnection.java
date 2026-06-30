package api.equinix.javasdk.design.optimizer.wizard.model;

import api.equinix.javasdk.core.model.MetroId;
import api.equinix.javasdk.fabric.enums.ConnectionType;
import api.equinix.javasdk.design.optimizer.wizard.enums.ConnectionPurpose;
import lombok.Builder;
import lombok.Value;

/**
 * A Fabric connection to be created as part of a deployment plan. Represents
 * either a provider connection (Cloud Router to service profile) or a backbone
 * link (Cloud Router to Cloud Router).
 */
@Value
@Builder
public class PlannedConnection {

    String name;

    ConnectionType connectionType;

    ConnectionPurpose purpose;

    int bandwidthMbps;

    BandwidthAllocation bandwidthAllocation;

    MetroId aSideMetro;

    String aSideRouterName;

    String zSideServiceProfileUuid;

    String zSideProviderLabel;

    String zSideSellerRegion;

    MetroId zSideMetro;

    String zSideRouterName;

    String notificationEmail;

    public boolean isProviderConnection() {
        return purpose == ConnectionPurpose.PROVIDER;
    }

    public boolean isBackboneLink() {
        return purpose == ConnectionPurpose.BACKBONE;
    }
}
