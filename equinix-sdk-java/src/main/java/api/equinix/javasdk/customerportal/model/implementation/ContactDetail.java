package api.equinix.javasdk.customerportal.model.implementation;

import api.equinix.javasdk.customerportal.enums.ContactType;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class ContactDetail {

    @JsonProperty("type")
    private ContactType type;

    @JsonProperty("value")
    private String value;
}
