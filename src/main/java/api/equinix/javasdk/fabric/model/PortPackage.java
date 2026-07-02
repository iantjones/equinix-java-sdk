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

import api.equinix.javasdk.fabric.enums.PortPackageSourceType;
import api.equinix.javasdk.fabric.enums.PortPackageType;
import api.equinix.javasdk.fabric.enums.PortServiceType;

import java.util.List;

/**
 * A Fabric port package describing the capabilities available for a class of ports. Read-only.
 */
public interface PortPackage {

    String getHref();

    PortPackageType getType();

    String getCode();

    Integer getVcBandwidthMax();

    Boolean getVcRemoteSupported();

    List<PortServiceType> getSupportedServiceTypes();

    List<PortPackageSourceType> getSupportedSourceTypes();

    List<String> getSupportedMetros();
}
