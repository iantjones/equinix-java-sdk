package com.eqixiac.equinix.customerportal.model.implementation;

import com.eqixiac.equinix.customerportal.enums.CabinetType;
import com.eqixiac.equinix.customerportal.enums.CageType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * Details of a cage (and its cabinet) at an IBX location (Customer Portal Lookup v2 API).
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class CageDetails {

    @JsonProperty("id")
    private String id;

    @JsonProperty("type")
    private CageType type;

    @JsonProperty("accountNumbers")
    private String accountNumbers;

    @JsonProperty("cabinetId")
    private String cabinetId;

    @JsonProperty("cabinetType")
    private CabinetType cabinetType;
}
