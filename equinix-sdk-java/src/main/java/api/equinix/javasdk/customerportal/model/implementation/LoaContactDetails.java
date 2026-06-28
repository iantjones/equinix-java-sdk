package api.equinix.javasdk.customerportal.model.implementation;

import api.equinix.javasdk.customerportal.enums.ContactDetailType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * A means of contacting a Digital LOA party (diLOA v1 API).
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class LoaContactDetails {

    @JsonProperty("type")
    private ContactDetailType type;

    @JsonProperty("value")
    private String value;

    @JsonProperty("notes")
    private String notes;
}
