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

import api.equinix.javasdk.customerportal.enums.LoaChangeStatus;
import api.equinix.javasdk.customerportal.enums.LoaChangeType;
import api.equinix.javasdk.customerportal.model.implementation.LoaChangeResult;
import api.equinix.javasdk.customerportal.model.implementation.LoaLink;

import java.util.List;

/**
 * A change record applied to a Digital LOA document, from the Equinix Customer Portal diLOA v1 API.
 */
public interface DigitalLoaChange {

    String getUuid();

    LoaChangeType getChangeType();

    LoaChangeStatus getStatus();

    String getCreatedDateTime();

    String getUpdatedDateTime();

    String getData();

    String getDescription();

    String getHref();

    List<LoaLink> getLinks();

    LoaChangeResult getResult();
}
