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

package api.equinix.javasdk.customerportal.model;

import api.equinix.javasdk.customerportal.enums.LoaState;
import api.equinix.javasdk.customerportal.model.implementation.LoaChangeLog;
import api.equinix.javasdk.customerportal.model.implementation.LoaLink;
import api.equinix.javasdk.customerportal.model.implementation.LoaParty;
import api.equinix.javasdk.customerportal.model.implementation.LoaProduct;

import java.util.List;

/**
 * A Digital Letter of Authorization (Digital LOA) document from the Equinix Customer Portal
 * diLOA v1 API.
 */
public interface DigitalLoa {

    String getUuid();

    String getToken();

    LoaState getState();

    List<LoaProduct> getProducts();

    LoaParty getRequestor();

    LoaParty getProvider();

    String getNotes();

    String getExpiryDateTime();

    LoaChangeLog getChangeLog();

    List<LoaLink> getLinks();

    String getHref();
}
