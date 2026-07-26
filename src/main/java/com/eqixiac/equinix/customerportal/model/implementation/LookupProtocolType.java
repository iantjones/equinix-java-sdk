package com.eqixiac.equinix.customerportal.model.implementation;

import com.eqixiac.equinix.customerportal.enums.ConnectorType;
import com.eqixiac.equinix.customerportal.enums.ProtocolType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

/**
 * A protocol type supported by a connection-service media type, with its connector types
 * (Customer Portal Lookup v2 API). {@code name} and {@code connectorTypes} are lookup values
 * carried as strings.
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class LookupProtocolType {

    @JsonProperty("name")
    private ProtocolType name;

    @JsonProperty("connectorTypes")
    private List<ConnectorType> connectorTypes;
}
