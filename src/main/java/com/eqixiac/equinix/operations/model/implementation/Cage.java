package com.eqixiac.equinix.operations.model.implementation;

import com.eqixiac.equinix.operations.enums.CageCabType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
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
