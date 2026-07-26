package com.eqixiac.equinix.customerportal.model.implementation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class PaymentInstructions {

    @JsonProperty("electronic")
    private ElectronicPayment electronic;

    @JsonProperty("check")
    private CheckPayment check;

    @JsonProperty("emailRemittance")
    private String emailRemittance;
}
