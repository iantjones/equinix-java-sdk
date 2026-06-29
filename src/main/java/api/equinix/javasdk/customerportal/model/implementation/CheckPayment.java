package api.equinix.javasdk.customerportal.model.implementation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class CheckPayment {

    @JsonProperty("bankName")
    private String bankName;

    @JsonProperty("bankAddress")
    private Address bankAddress;
}
