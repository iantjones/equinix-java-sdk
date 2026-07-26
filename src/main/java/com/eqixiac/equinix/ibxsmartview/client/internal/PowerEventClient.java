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

package com.eqixiac.equinix.ibxsmartview.client.internal;

import com.eqixiac.equinix.core.http.response.Page;
import com.eqixiac.equinix.core.http.response.Pageable;
import com.eqixiac.equinix.ibxsmartview.model.PowerAlertConfiguration;
import com.eqixiac.equinix.ibxsmartview.model.PowerEvent;
import com.eqixiac.equinix.ibxsmartview.model.json.PowerAlertConfigurationCreateResponseJson;
import com.eqixiac.equinix.ibxsmartview.model.json.PowerAlertConfigurationJson;
import com.eqixiac.equinix.ibxsmartview.model.json.PowerEventJson;
import com.eqixiac.equinix.ibxsmartview.model.json.creators.PowerAlertConfigurationCreatorJson;
import com.eqixiac.equinix.ibxsmartview.model.json.creators.PowerAlertConfigurationUpdateJson;

import java.util.List;

public interface PowerEventClient<T> extends Pageable<T> {

    Page<PowerEventJson> getPowerEvents(List<String> ibx, List<String> status, String edgeCollectedOn, int offset, int limit);

    PowerAlertConfigurationCreateResponseJson createPowerAlertConfiguration(PowerAlertConfigurationCreatorJson creatorJson);

    void updatePowerAlertConfiguration(PowerAlertConfigurationUpdateJson updateJson);

    Page<PowerAlertConfigurationJson> searchAlertConfigurations(List<String> ibx, List<String> state, int offset, int limit);

    Pageable<PowerAlertConfiguration> alertConfigurationPageable();

    void pauseAlertConfiguration(String alertConfigurationUid);

    void resumeAlertConfiguration(String alertConfigurationUid);

    void deleteAlertConfiguration(String alertConfigurationUid);
}
