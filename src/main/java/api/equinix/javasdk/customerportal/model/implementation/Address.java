package api.equinix.javasdk.customerportal.model.implementation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * A postal address on a Customer Portal resource.
 *
 * <p>Prefer {@code builder()} over the positional constructors — all six parameters are
 * {@code String}s, so builder construction is self-documenting and transposition-proof.</p>
 */
@NoArgsConstructor
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class Address {

    @JsonProperty("addressLine1")
    private String addressLine1;

    @JsonProperty("addressLine2")
    private String addressLine2;

    @JsonProperty("city")
    private String city;

    @JsonProperty("state")
    private String state;

    @JsonProperty("countryCode")
    private String countryCode;

    @JsonProperty("zipCode")
    private String zipCode;

    public Address(String addressLine1, String city, String state, String countryCode, String zipCode) {
        this.addressLine1 = addressLine1;
        this.city = city;
        this.state = state;
        this.countryCode = countryCode;
        this.zipCode = zipCode;
    }

    /**
     * Explicit constructor replacing the Lombok-generated {@code @AllArgsConstructor}: the
     * argument order is pinned here in code (six same-typed {@code String} parameters)
     * rather than by field declaration order.
     *
     * @param addressLine1 the first address line
     * @param addressLine2 the second address line
     * @param city         the city
     * @param state        the state or province
     * @param countryCode  the country code
     * @param zipCode      the postal code
     */
    @Builder
    public Address(String addressLine1, String addressLine2, String city, String state,
                   String countryCode, String zipCode) {
        this.addressLine1 = addressLine1;
        this.addressLine2 = addressLine2;
        this.city = city;
        this.state = state;
        this.countryCode = countryCode;
        this.zipCode = zipCode;
    }
}
