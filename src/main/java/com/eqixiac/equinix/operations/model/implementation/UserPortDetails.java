package com.eqixiac.equinix.operations.model.implementation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * Details of a port currently in use on a patch panel (Lookup v2 API).
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserPortDetails {

    @JsonProperty("portNumber")
    private Integer portNumber;

    @JsonProperty("serialNumber")
    private String serialNumber;

    @JsonProperty("connectionServicesName")
    private String connectionServicesName;

    @JsonProperty("zSideProviderName")
    private String zSideProviderName;

    @JsonProperty("circuitId")
    private String circuitId;
}
