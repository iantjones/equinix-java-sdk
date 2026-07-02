package api.equinix.javasdk.fabric.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Port service type (the deprecated Fabric v4 {@code Port.serviceType} attribute:
 * {@code EPL}, {@code MSP}). Unrecognized values deserialize to {@link #UNKNOWN}
 * rather than failing the whole response.
 */
public enum PortServiceType {
    EPL,
    MSP,
    UNKNOWN;

    /**
     * Deserializes a port service type leniently: an unrecognized value maps to
     * {@link #UNKNOWN} instead of failing the enclosing response.
     *
     * @param value the raw API value
     * @return the matching constant, or {@link #UNKNOWN}
     */
    @JsonCreator
    public static PortServiceType fromString(String value) {
        try { return PortServiceType.valueOf(value); }
        catch (Exception e) { return UNKNOWN; }
    }
}
