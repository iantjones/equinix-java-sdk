package api.equinix.javasdk.customerportal.model.implementation;

import api.equinix.javasdk.customerportal.enums.CageCabType;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class Cage {

    @JsonProperty("id")
    private String id;

    @JsonProperty("type")
    private CageCabType type;

    @JsonProperty("accountNumber")
    private String accountNumber;

    @JsonProperty("cabinetId")
    private String cabinetId;

    @JsonProperty("cabinetType")
    private CageCabType cabinetType;
}
