package api.equinix.javasdk.customerportal.model.implementation;

import api.equinix.javasdk.core.enums.HttpMethod;
import api.equinix.javasdk.customerportal.enums.LoaLinkRel;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * A HATEOAS link describing a follow-up interaction available on a Digital LOA document
 * (diLOA v1 API).
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class LoaLink {

    @JsonProperty("rel")
    private LoaLinkRel rel;

    @JsonProperty("href")
    private String href;

    @JsonProperty("method")
    private HttpMethod method;

    @JsonProperty("contentType")
    private String contentType;

    @JsonProperty("authenticate")
    private Boolean authenticate;
}
