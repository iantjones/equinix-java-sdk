package api.equinix.javasdk.customerportal.model.implementation;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class Account {

    @JsonProperty("accountNumber")
    private String accountNumber;

    @JsonProperty("accountName")
    private String accountName;

    @JsonProperty("allowOrder")
    private Boolean allowOrder;
}
