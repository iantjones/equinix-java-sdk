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

package api.equinix.javasdk.networkedge.model.json;

import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.networkedge.model.PublicKey;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.Getter;

import java.util.List;

/**
 * <p>PublicKeyJson class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
@Getter
public class PublicKeyJson {

    @Getter static TypeReference<Page<PublicKey, PublicKeyJson>> pagedTypeRef = new TypeReference<>() {};
    @Getter static TypeReference<List<PublicKeyJson>> listTypeRef = new TypeReference<>() {};
    @Getter static TypeReference<PublicKeyJson> singleTypeRef = new TypeReference<>() {};

    @JsonProperty("uuid")
    private String uuid;

    @JsonProperty("keyName")
    private String keyName;

    @JsonProperty("keyValue")
    private String keyValue;

    @JsonProperty("custOrgId")
    private String custOrgId;

    @JsonProperty("accountUcmId")
    private String accountUcmId;
}
