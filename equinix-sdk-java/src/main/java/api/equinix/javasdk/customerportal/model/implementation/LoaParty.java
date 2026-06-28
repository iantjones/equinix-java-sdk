package api.equinix.javasdk.customerportal.model.implementation;

import api.equinix.javasdk.customerportal.enums.PartyType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

/**
 * A party (requestor or provider) on a Digital LOA document (diLOA v1 API). The {@code type}
 * discriminates between a {@code CUSTOMER_ORGANIZATION} (which carries {@code orgIds},
 * {@code name} and {@code contacts}) and a {@code NEW_RELATIONSHIP} (which carries {@code name}
 * and {@code email}); this flattened view exposes the union of both shapes.
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class LoaParty {

    @JsonProperty("type")
    private PartyType type;

    @JsonProperty("name")
    private String name;

    @JsonProperty("orgIds")
    private List<String> orgIds;

    @JsonProperty("contacts")
    private List<LoaContact> contacts;

    @JsonProperty("email")
    private String email;
}
