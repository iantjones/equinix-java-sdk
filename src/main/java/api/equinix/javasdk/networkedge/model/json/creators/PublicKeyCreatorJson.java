package api.equinix.javasdk.networkedge.model.json.creators;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * <p>PublicKeyCreatorJson class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
public class PublicKeyCreatorJson {

    @JsonProperty("keyName")
    private String keyName;

    @JsonProperty("keyValue")
    private String keyValue;

    @JsonProperty("accountUcmId")
    private String accountUcmId;

    /**
     * <p>Constructor for PublicKeyCreatorJson.</p>
     *
     * @param publicKeyBuilder a {@link api.equinix.javasdk.networkedge.model.json.creators.PublicKeyOperator.PublicKeyBuilder} object.
     */
    public PublicKeyCreatorJson(PublicKeyOperator.PublicKeyBuilder publicKeyBuilder) {
        this.keyName = publicKeyBuilder.getKeyName();
        this.keyValue = publicKeyBuilder.getKeyValue();
        this.accountUcmId = publicKeyBuilder.getAccountUcmId();
    }
}
