package com.eqixiac.equinix.customerportal.model.implementation;

import com.eqixiac.equinix.core.enums.Region;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class CustomerDetail {

    @JsonProperty("accountNumber")
    private String accountNumber;

    @JsonProperty("accountName")
    private String accountName;

    @JsonProperty("legalEntity")
    private String legalEntity;

    @JsonProperty("accountContact")
    private String accountContact;

    @JsonProperty("countryCode")
    private String countryCode;

    @JsonProperty("address")
    private Address address;

    @JsonProperty("region")
    private Region region;
}