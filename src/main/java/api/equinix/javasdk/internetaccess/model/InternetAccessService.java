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

package api.equinix.javasdk.internetaccess.model;

import api.equinix.javasdk.internetaccess.enums.InternetAccessServiceState;
import api.equinix.javasdk.internetaccess.enums.InternetAccessServiceType;

public interface InternetAccessService {

    String getUuid();

    String getName();

    InternetAccessServiceType getType();

    String getStatus();

    Integer getBandwidth();

    String getIbx();

    InternetAccessServiceState getState();

    Boolean delete();

    void refresh();
}
