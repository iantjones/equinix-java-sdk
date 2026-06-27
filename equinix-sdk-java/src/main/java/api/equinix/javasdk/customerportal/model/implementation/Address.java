package api.equinix.javasdk.customerportal.model.implementation;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
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
}
