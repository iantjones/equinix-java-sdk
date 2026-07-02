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

import api.equinix.javasdk.fabric.model.implementation.ChangeLog;
import api.equinix.javasdk.fabric.model.implementation.CompanyLogo;
import api.equinix.javasdk.fabric.model.implementation.CompanyMetro;
import api.equinix.javasdk.fabric.model.implementation.CompanyProfileAccount;
import api.equinix.javasdk.fabric.model.implementation.CompanyProfileChange;
import api.equinix.javasdk.fabric.model.implementation.CompanyServiceProfile;
import api.equinix.javasdk.fabric.model.implementation.Notification;

import java.util.List;

/**
 * A Fabric company profile (the public, marketplace-facing profile for an organization).
 */
public interface CompanyProfile {

    String getHref();

    String getUuid();

    String getType();

    String getName();

    String getSummary();

    String getDescription();

    String getState();

    CompanyProfileAccount getAccount();

    List<CompanyMetro> getMetros();

    CompanyLogo getLogo();

    List<? extends Tag> getTags();

    List<CompanyServiceProfile> getServiceProfiles();

    List<? extends PrivateService> getPrivateServices();

    List<Notification> getNotifications();

    String getWebUrl();

    String getContactUrl();

    CompanyProfileChange getChange();

    ChangeLog getChangeLog();

    Boolean delete();

    void refresh();
}
