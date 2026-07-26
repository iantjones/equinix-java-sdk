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

package com.eqixiac.equinix.networkedge.model.wrappers;

import com.eqixiac.equinix.core.http.response.Pageable;
import com.eqixiac.equinix.core.model.ResourceImpl;
import com.eqixiac.equinix.networkedge.client.internal.implementation.VPNClientImpl;
import com.eqixiac.equinix.networkedge.model.VPN;
import com.eqixiac.equinix.networkedge.model.json.VPNJson;
import com.eqixiac.equinix.networkedge.model.json.creators.VPNOperator;
import com.eqixiac.equinix.networkedge.model.json.creators.VPNUpdaterJson;
import lombok.Getter;
import lombok.experimental.Delegate;

/**
 *
 * @author ianjones
 */
public class VPNWrapper extends ResourceImpl<VPN> implements VPN {

    @Delegate
    private VPNJson json;
    @Getter
    private final Pageable<VPN> serviceClient;

    public VPNWrapper(VPNJson sshUserJson, Pageable<VPN> serviceClient) {
        this.json = sshUserJson;
        this.serviceClient = serviceClient;
    }

    public VPNOperator.VPNUpdater update() {
        return new VPNOperator(this.serviceClient).update(this.json);
    }

    public Boolean save(VPNUpdaterJson updaterJson) {
        this.json = ((VPNClientImpl)this.serviceClient).update(this.getUuid(), updaterJson);
        return true;
    }

    public Boolean delete() {
        return ((VPNClientImpl)this.serviceClient).delete(this.getUuid());
    }

    public Boolean refresh() {
        this.json = ((VPNClientImpl)this.serviceClient).refresh(this.getUuid());
        return true;
    }
}
