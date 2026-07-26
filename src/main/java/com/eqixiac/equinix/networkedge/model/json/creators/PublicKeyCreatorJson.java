package com.eqixiac.equinix.networkedge.model.json.creators;

import com.eqixiac.equinix.networkedge.enums.KeyType;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 *
 * @author ianjones
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

    public PublicKeyCreatorJson(PublicKeyOperator.PublicKeyBuilder publicKeyBuilder) {
        this.keyName = publicKeyBuilder.getKeyName();
        this.keyValue = publicKeyBuilder.getKeyValue();
        this.keyType = publicKeyBuilder.getKeyType();
        this.accountUcmId = publicKeyBuilder.getAccountUcmId();
    }
}
