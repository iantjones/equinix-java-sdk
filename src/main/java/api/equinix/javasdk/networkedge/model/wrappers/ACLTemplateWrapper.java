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
import api.equinix.javasdk.networkedge.client.internal.implementation.ACLTemplateClientImpl;
import api.equinix.javasdk.networkedge.model.ACLTemplate;
import api.equinix.javasdk.networkedge.model.json.ACLTemplateJson;
import api.equinix.javasdk.networkedge.model.json.creators.ACLTemplateOperator;
import api.equinix.javasdk.networkedge.model.json.creators.ACLTemplateUpdaterJson;
import lombok.Getter;
import lombok.experimental.Delegate;

/**
 * <p>ACLTemplateWrapper class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
public class ACLTemplateWrapper extends ResourceImpl<ACLTemplate> implements ACLTemplate {

    @Delegate
    private ACLTemplateJson json;
    @Getter
    private final Pageable<ACLTemplate> serviceClient;

    /**
     * <p>Constructor for ACLTemplateWrapper.</p>
     *
     * @param sshUserJson a {@link api.equinix.javasdk.networkedge.model.json.ACLTemplateJson} object.
     * @param serviceClient a {@link api.equinix.javasdk.core.http.response.Pageable} object.
     */
    public ACLTemplateWrapper(ACLTemplateJson sshUserJson, Pageable<ACLTemplate> serviceClient) {
        this.json = sshUserJson;
        this.serviceClient = serviceClient;
    }

    /**
     * <p>update.</p>
     *
     * @return a {@link api.equinix.javasdk.networkedge.model.json.creators.ACLTemplateOperator.ACLTemplateUpdater} object.
     */
    public ACLTemplateOperator.ACLTemplateUpdater update() {
        return new ACLTemplateOperator(this.serviceClient).update(this.json);
    }

    /** {@inheritDoc} */
    public Boolean save(ACLTemplateUpdaterJson updaterJson) {
        this.json = ((ACLTemplateClientImpl)this.serviceClient).update(this.getUuid(), updaterJson);
        return true;
    }

    /**
     * <p>delete.</p>
     *
     * @return a {@link java.lang.Boolean} object.
     */
    public Boolean delete() {
        return delete(null);
    }

    /** {@inheritDoc} */
    public Boolean delete(String accountUcmId) {
        return ((ACLTemplateClientImpl)this.serviceClient).delete(this.getUuid(), accountUcmId);
    }

    /**
     * <p>refresh.</p>
     *
     * @return a {@link java.lang.Boolean} object.
     */
    public Boolean refresh() {
        return refresh(null);
    }

    /** {@inheritDoc} */
    public Boolean refresh(String accountUcmId) {
        this.json = ((ACLTemplateClientImpl)this.serviceClient).refresh(this.getUuid(), accountUcmId);
        return true;
    }
}
