package api.equinix.javasdk.customerportal.model.implementation;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class ElectronicPayment {

    @JsonProperty("bankName")
    private String bankName;

    @JsonProperty("bankAddress")
    private Address bankAddress;

    @JsonProperty("payeeName")
    private String payeeName;

    @JsonProperty("payeeAccountNumber")
    private String payeeAccountNumber;

    @JsonProperty("bankCode")
    private String bankCode;

    @JsonProperty("swiftCode")
    private String swiftCode;

    @JsonProperty("sortCode")
    private String sortCode;
}
