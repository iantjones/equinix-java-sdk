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

package api.equinix.javasdk.fabric.model.wrappers;

import api.equinix.javasdk.core.http.response.Pageable;
import api.equinix.javasdk.core.model.ResourceImpl;
import api.equinix.javasdk.fabric.client.internal.implementation.ServiceProfileClientImpl;
import api.equinix.javasdk.fabric.enums.AccessPointType;
import api.equinix.javasdk.fabric.model.ServiceProfile;
import api.equinix.javasdk.fabric.model.implementation.ServiceProfileMetro;
import api.equinix.javasdk.fabric.model.implementation.SimpleAccessPoint;
import api.equinix.javasdk.fabric.model.json.ServiceProfileJson;
import lombok.Getter;
import lombok.experimental.Delegate;

import java.util.List;

public class ServiceProfileWrapper extends ResourceImpl<ServiceProfile> implements ServiceProfile {

    @Delegate(excludes = ServiceProfileMutability.class)
    private ServiceProfileJson jsonObject;
    @Getter
    private final Pageable<ServiceProfile> serviceClient;

    /**
     * <p>Constructor for ServiceProfileWrapper.</p>
     *
     * @param jsonObject a {@link api.equinix.javasdk.fabric.model.json.ServiceProfileJson} object.
     * @param serviceProfileClient a {@link api.equinix.javasdk.core.http.response.Pageable} object.
     */
    public ServiceProfileWrapper(ServiceProfileJson jsonObject, Pageable<ServiceProfile> serviceProfileClient) {
        this.jsonObject = jsonObject;
        this.serviceClient = serviceProfileClient;
    }

    @Override
    public List<ServiceProfileMetro> metros() {
        return this.jsonObject.getMetros();
    }

    public Boolean delete() {
        this.jsonObject = ((ServiceProfileClientImpl)this.serviceClient).delete(this.getUuid());
        return true;
    }

    public void refresh() {
        this.jsonObject = ((ServiceProfileClientImpl)serviceClient).refresh(this.getUuid());
    }

    public SimpleAccessPoint accessPoint() {
        return SimpleAccessPoint.define(AccessPointType.SP).port(this.getUuid()).create();
    }

    private interface ServiceProfileMutability {
        List<ServiceProfileMetro> getMetros();
        Boolean delete();
        void refresh();
        SimpleAccessPoint accessPoint();
    }
}
