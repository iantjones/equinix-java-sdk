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

import api.equinix.javasdk.customerportal.model.implementation.LookupMediaType;

import java.util.List;

/**
 * A connection service available at an IBX, with its supported media types.
 */
public interface ConnectionService {

    /**
     * Returns the connection service name.
     *
     * @return the connection service name
     */
    String getName();

    /**
     * Returns the supported media types (each with name, protocol types and circuit counts).
     *
     * @return the media types
     */
    List<LookupMediaType> getMediaTypes();
}
