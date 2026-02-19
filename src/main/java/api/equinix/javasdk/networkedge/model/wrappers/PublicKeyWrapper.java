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

package api.equinix.javasdk.networkedge.model.wrappers;

import api.equinix.javasdk.core.http.response.Pageable;
import api.equinix.javasdk.core.model.ResourceImpl;
import api.equinix.javasdk.networkedge.client.internal.implementation.PublicKeyClientImpl;
import api.equinix.javasdk.networkedge.model.PublicKey;
import api.equinix.javasdk.networkedge.model.json.PublicKeyJson;
import lombok.Getter;
import lombok.experimental.Delegate;

/**
 * <p>PublicKeyWrapper class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
public class PublicKeyWrapper extends ResourceImpl<PublicKey> implements PublicKey {

    @Delegate
    private PublicKeyJson jsonObject;
    @Getter
    private final Pageable<PublicKey> serviceClient;

    /**
     * <p>Constructor for PublicKeyWrapper.</p>
     *
     * @param publicKeyJson a {@link api.equinix.javasdk.networkedge.model.json.PublicKeyJson} object.
     * @param serviceClient a {@link api.equinix.javasdk.core.http.response.Pageable} object.
     */
    public PublicKeyWrapper(PublicKeyJson publicKeyJson, Pageable<PublicKey> serviceClient) {
        this.jsonObject = publicKeyJson;
        this.serviceClient = serviceClient;
    }

    /**
     * <p>delete.</p>
     *
     * @return a {@link java.lang.Boolean} object.
     */
    public Boolean delete() {
        return ((PublicKeyClientImpl)this.serviceClient).delete(this.getUuid());
    }

    /**
     * <p>refresh.</p>
     *
     * @return a {@link java.lang.Boolean} object.
     */
    public Boolean refresh() {
        this.jsonObject = ((PublicKeyClientImpl)this.serviceClient).refresh(this.getUuid());
        return true;
    }
}
