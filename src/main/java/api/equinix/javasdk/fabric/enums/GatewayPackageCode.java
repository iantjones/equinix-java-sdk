package api.equinix.javasdk.fabric.enums;

import api.equinix.javasdk.core.model.APIParam;
import com.fasterxml.jackson.annotation.JsonCreator;

public enum GatewayPackageCode implements APIParam {
    LAB,
    /** Not present in the current Fabric spec (code enum = LAB/BASIC/STANDARD/ADVANCED/PREMIUM); retained for compatibility. */
    LIMITED,
    BASIC,
    STANDARD,
    ADVANCED,
    PREMIUM,
    UNKNOWN;

    @JsonCreator
    public static GatewayPackageCode fromString(String value) {
        try { return GatewayPackageCode.valueOf(value); }
        catch (Exception e) { return UNKNOWN; }
    }
}
