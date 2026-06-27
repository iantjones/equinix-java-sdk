package api.equinix.javasdk.customerportal.model.implementation;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class PaymentInstructions {

    @JsonProperty("electronic")
    private ElectronicPayment electronic;

    @JsonProperty("check")
    private CheckPayment check;

    @JsonProperty("emailRemittance")
    private String emailRemittance;
}
