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

package com.eqixiac.equinix.fabric.model;

import com.eqixiac.equinix.fabric.model.implementation.CloudEventData;

public interface CloudEvent {

    /**
     * The Cloud Event identifier (the {@code id} wire property; the accessor keeps its
     * historical {@code getUuid()} name).
     *
     * @return the Cloud Event identifier
     */
    String getUuid();

    String getSpec();

    String getType();

    String getSource();

    String getSubject();

    String getTime();

    String getDataSchema();

    String getDataContentType();

    String getSeverityNumber();

    String getSeverityText();

    String getEquinixAlert();

    String getEquinixOrganization();

    String getEquinixProject();

    String getAuthType();

    String getAuthId();

    String getTraceParent();

    String getTraceState();

    CloudEventData getData();
}
