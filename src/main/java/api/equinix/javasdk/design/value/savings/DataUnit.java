package api.equinix.javasdk.design.value.savings;

import java.math.BigDecimal;

/**
 * Unit of data volume for egress inputs. Conversions use decimal (SI) factors —
 * 1 TB = 1000 GB — consistent with how cloud providers meter data transfer.
 */
public enum DataUnit {

    GIGABYTE(BigDecimal.ONE),

    TERABYTE(BigDecimal.valueOf(1_000)),

    PETABYTE(BigDecimal.valueOf(1_000_000));

    private final BigDecimal gigabytesPerUnit;

    DataUnit(BigDecimal gigabytesPerUnit) {
        this.gigabytesPerUnit = gigabytesPerUnit;
    }

    /**
     * Converts an amount in this unit to gigabytes.
     *
     * @param amount the amount in this unit
     * @return the equivalent number of gigabytes
     */
    public BigDecimal toGigabytes(BigDecimal amount) {
        return (amount == null ? BigDecimal.ZERO : amount).multiply(gigabytesPerUnit);
    }
}
