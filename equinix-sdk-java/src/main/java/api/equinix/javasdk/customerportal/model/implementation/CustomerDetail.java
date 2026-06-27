package api.equinix.javasdk.customerportal.model.implementation;

import api.equinix.javasdk.core.enums.Region;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class CustomerDetail {

    @JsonProperty("accountNumber")
    private String accountNumber;

    @JsonProperty("accountName")
    private String accountName;

    @JsonProperty("legalEntity")
    private String legalEntity;

    @JsonProperty("countryCode")
    private String countryCode;

    @JsonProperty("address")
    private Address address;

    @JsonProperty("region")
    private Region region;
}