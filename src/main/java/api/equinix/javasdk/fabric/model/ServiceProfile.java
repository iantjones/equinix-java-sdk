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

package api.equinix.javasdk.fabric.model;

import api.equinix.javasdk.fabric.enums.ServiceProfileState;
import api.equinix.javasdk.fabric.enums.ServiceProfileType;
import api.equinix.javasdk.fabric.enums.ServiceProfileVisibility;
import api.equinix.javasdk.fabric.model.implementation.*;
import api.equinix.javasdk.fabric.model.json.MetroJson;

import java.util.List;

public interface ServiceProfile extends AccessPointable {

    String getUuid();

    ServiceProfileState getState();

    String getHref();

    Account getAccount();

    List<String> getTags();

    String getName();

    String getDescription();

    ServiceProfileVisibility getVisibility();

    ServiceProfileType getType();

    List<Notification> getNotifications();

    List<AccessPointTypeConfig> getAccessPointTypeConfigs();

    List<ServiceProfileMetro> metros();

    MarketingInfo getMarketingInfo();

    Boolean getSelfProfile();

    ChangeLog getChangeLog();

    List<CustomField> getCustomFields();

    List<String> getAllowedEmails();

    Boolean delete();

    void refresh();
}
