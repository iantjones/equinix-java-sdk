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

import api.equinix.javasdk.customerportal.enums.ReportParameterType;
/**
 * An input parameter for a report (Reports v1 {@code parameter}).
 */
public interface ReportParameter {

    /**
     * Returns the parameter name.
     *
     * @return the name
     */
    String getName();

    /**
     * Returns the parameter value.
     *
     * @return the value
     */
    String getValue();

    /**
     * Returns the parameter type (e.g. {@code STRING}, {@code INT}, {@code ARRAY}).
     *
     * @return the type, or {@code null} if not provided
     */
    ReportParameterType getType();

    /**
     * Returns whether the parameter is required.
     *
     * @return the required flag, or {@code null} if not provided
     */
    Boolean getRequired();
}
