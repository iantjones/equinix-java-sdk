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

package api.equinix.javasdk.networkedge.model;

import api.equinix.javasdk.networkedge.enums.DeviceACLStatus;
import api.equinix.javasdk.networkedge.model.implementation.InboundRule;
import api.equinix.javasdk.networkedge.model.implementation.VirtualDeviceACLDetail;
import api.equinix.javasdk.networkedge.model.json.creators.ACLTemplateOperator;
import api.equinix.javasdk.networkedge.model.json.creators.ACLTemplateUpdaterJson;

import java.time.LocalDateTime;
import java.util.List;

/**
 *
 * @author ianjones
 */
public interface ACLTemplate {

    String getUuid();

    String getName();

    String getDescription();

    List<InboundRule> getInboundRules();

    List<VirtualDeviceACLDetail> getVirtualDeviceDetails();

    DeviceACLStatus getStatus();

    LocalDateTime getCreatedDate();

    ACLTemplateOperator.ACLTemplateUpdater update();

    Boolean save(ACLTemplateUpdaterJson updaterJson);

    Boolean delete();

    Boolean delete(String accountUcmId);

    Boolean refresh();

    Boolean refresh(String accountUcmId);
}
