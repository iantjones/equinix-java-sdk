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

package api.equinix.javasdk.customerportal.client.implementation;

import api.equinix.javasdk.CustomerPortal;
import api.equinix.javasdk.core.http.Utils;
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.customerportal.client.DigitalLOAs;
import api.equinix.javasdk.customerportal.client.internal.DigitalLOAClient;
import api.equinix.javasdk.customerportal.model.DigitalLOA;
import api.equinix.javasdk.customerportal.model.json.DigitalLOAJson;
import api.equinix.javasdk.customerportal.model.json.creators.DigitalLOAOperator;
import api.equinix.javasdk.customerportal.model.wrappers.DigitalLOAWrapper;

public class DigitalLOAsImpl implements DigitalLOAs {

    private final CustomerPortal serviceManager;

    private final DigitalLOAClient<DigitalLOA> serviceClient;

    public DigitalLOAsImpl(DigitalLOAClient<DigitalLOA> serviceClient, CustomerPortal serviceManager) {
        this.serviceManager = serviceManager;
        this.serviceClient = serviceClient;
    }

    public PaginatedList<DigitalLOA> list() {
        Page<DigitalLOA, DigitalLOAJson> responsePage = this.serviceClient.list();
        PaginatedList<DigitalLOA> digitalLOAList = Utils.mapPaginatedList(responsePage.getItems(), this.serviceClient, DigitalLOAWrapper::new);
        return new PaginatedList<>(digitalLOAList, this.serviceClient, responsePage.getAssociatedRequest(), responsePage.getAssociatedResponse(), responsePage.getPagination());
    }

    public DigitalLOA getByUuid(String uuid) {
        DigitalLOAJson digitalLOAJson = this.serviceClient.getByUuid(uuid);
        return new DigitalLOAWrapper(digitalLOAJson, this.serviceClient);
    }

    public DigitalLOAOperator.DigitalLOABuilder define() {
        return new DigitalLOAOperator(this.serviceClient).create();
    }
}
