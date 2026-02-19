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

package api.equinix.javasdk.networkedge.helpers;

import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.networkedge.model.Device;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>ModelHelper class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
public class ModelHelper {

    /**
     * <p>findDeviceByCreator.</p>
     *
     * @param devices a {@link api.equinix.javasdk.core.http.response.PaginatedList} object.
     * @param creator a {@link java.lang.String} object.
     * @return a {@link java.util.List} object.
     */
    public static List<Device> findDeviceByCreator(PaginatedList<Device> devices, String creator) {
        return devices.stream()
                .filter(device -> device.getCreatedBy().equals(creator))
                .collect(Collectors.toCollection(ArrayList::new));
    }

}
