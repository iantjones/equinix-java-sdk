package api.equinix.javasdk.design.value.savings;

import java.math.BigDecimal;

/**
 * Unit of data volume for egress inputs. Conversions use decimal (SI) factors —
 * 1 TB = 1000 GB — consistent with how cloud providers meter data transfer.
 */
public enum DataUnit {

    /** One decimal gigabyte (the base unit). */
    GIGABYTE(BigDecimal.ONE),

    /** One decimal terabyte = 1000 GB. */
    TERABYTE(BigDecimal.valueOf(1_000)),

    /** One decimal petabyte = 1,000,000 GB. */
    PETABYTE(BigDecimal.valueOf(1_000_000));

    private final BigDecimal gigabytesPerUnit;

    DataUnit(BigDecimal gigabytesPerUnit) {
        this.gigabytesPerUnit = gigabytesPerUnit;
    }

    /**
     * Converts an amount in this unit to gigabytes.
     *
     * @param amount the amount in this unit; {@code null} is treated as zero
     * @return the equivalent number of decimal gigabytes ({@code ZERO} for a null amount)
     */
    public BigDecimal toGigabytes(BigDecimal amount) {
        return (amount == null ? BigDecimal.ZERO : amount).multiply(gigabytesPerUnit);
    }
}
