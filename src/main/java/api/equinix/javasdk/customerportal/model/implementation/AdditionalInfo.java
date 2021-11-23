package api.equinix.javasdk.customerportal.model.implementation;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class AdditionalInfo {

    @JsonProperty("key")
    private String key;

    @JsonProperty("value")
    private String value;
}
