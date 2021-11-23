package api.equinix.javasdk.customerportal.model.implementation;

import api.equinix.javasdk.customerportal.enums.BillingContactType;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

@Getter
public class BillingContact {

    @JsonProperty("firstName")
    private String firstName;

    @JsonProperty("lastName")
    private String lastName;

    @JsonProperty("type")
    private BillingContactType type;

    @JsonProperty("details")
    private List<ContactDetail> details;
}
