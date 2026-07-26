package com.eqixiac.equinix.networkedge.model.implementation;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 *
 * @author ianjones
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
public class UUIDResult {

    @JsonAlias("id")
    @JsonProperty("uuid")
    private String uuid;

    @JsonProperty("secondaryUuid")
    private String secondaryUuid;
}
