package api.equinix.javasdk.ibxsmartview.model.implementation;

import api.equinix.javasdk.ibxsmartview.enums.SensorUnit;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class Reading {

    @JsonProperty("value")
    private Double value;

    @JsonProperty("unit")
    private SensorUnit unit;
}
