package api.equinix.javasdk.customerportal.model.implementation;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class CheckPayment {

    @JsonProperty("bankName")
    private String bankName;

    @JsonProperty("bankAddress")
    private Address bankAddress;
}
