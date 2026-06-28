package api.equinix.javasdk.networkedge.model.json.creators;

import api.equinix.javasdk.networkedge.enums.KeyType;
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

    @JsonProperty("keyType")
    private KeyType keyType;

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
        this.keyType = publicKeyBuilder.getKeyType();
        this.accountUcmId = publicKeyBuilder.getAccountUcmId();
    }
}
