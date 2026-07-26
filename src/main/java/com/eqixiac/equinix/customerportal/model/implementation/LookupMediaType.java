package com.eqixiac.equinix.customerportal.model.implementation;

import com.eqixiac.equinix.customerportal.enums.CrossConnectMediaType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

/**
 * A media type supported by a connection service, with its protocol types and IFC circuit-count
 * options (Customer Portal Lookup v2 API). {@code name} is a lookup value carried as a string.
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class LookupMediaType {

    @JsonProperty("name")
    private CrossConnectMediaType name;

    @JsonProperty("protocolTypes")
    private List<LookupProtocolType> protocolTypes;

    @JsonProperty("circuitCounts")
    private List<Integer> circuitCounts;
}
