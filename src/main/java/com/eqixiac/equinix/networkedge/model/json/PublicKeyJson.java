/*
 * Copyright 2021 Ian Jones. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
 * OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package com.eqixiac.equinix.networkedge.model.json;

import com.eqixiac.equinix.networkedge.enums.KeyType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.Getter;

import java.util.List;

/**
 *
 * @author ianjones
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
public class PublicKeyJson {

    @Getter static TypeReference<PublicKeyJson.NestedList> listTypeRef = new TypeReference<>() {};

    /**
     * The getPublicKeys endpoint returns a {@code GetPublicKeysResponse} object
     * ({@code {pagination, data:[...]}}), not a flat array, so the list is read out of the
     * {@code data} property here.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @Getter
    public static class NestedList {
        @JsonProperty("data")
        private List<PublicKeyJson> data;

        /** For the spec's {@code {"data": [...]}} envelope. */
        public NestedList() {
        }

        /**
         * Accepts the bare array the live {@code GET /ne/v1/publicKeys} returns (checked
         * 2026-11-11); the spec's {@code GetPublicKeysResponse} documents an envelope.
         */
        @com.fasterxml.jackson.annotation.JsonCreator(mode = com.fasterxml.jackson.annotation.JsonCreator.Mode.DELEGATING)
        public NestedList(List<PublicKeyJson> keys) {
            this.data = keys;
        }
    }

    @JsonProperty("uuid")
    private String uuid;

    @JsonProperty("keyName")
    private String keyName;

    @JsonProperty("keyValue")
    private String keyValue;

    @JsonProperty("keyType")
    private KeyType keyType;

    /** Project of the key; returned live (2026-11-11), not in the spec. */
    @JsonProperty("projectId")
    private String projectId;

    /**
     * Builds a key from known values, for a create whose response carried no body.
     *
     * @param uuid     the key id
     * @param keyName  the key name
     * @param keyValue the key value
     * @param keyType  the key type, or {@code null}
     * @return the key
     */
    public static PublicKeyJson of(String uuid, String keyName, String keyValue, KeyType keyType) {
        PublicKeyJson json = new PublicKeyJson();
        json.uuid = uuid;
        json.keyName = keyName;
        json.keyValue = keyValue;
        json.keyType = keyType;
        return json;
    }
}
