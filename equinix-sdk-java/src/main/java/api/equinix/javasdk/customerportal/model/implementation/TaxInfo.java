package api.equinix.javasdk.customerportal.model.implementation;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class TaxInfo {

    @JsonProperty("description")
    private String description;

    @JsonProperty("value")
    private BigDecimal value;
}
