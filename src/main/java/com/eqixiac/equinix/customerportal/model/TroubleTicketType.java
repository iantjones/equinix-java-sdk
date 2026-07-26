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

package com.eqixiac.equinix.customerportal.model;

import com.eqixiac.equinix.customerportal.model.implementation.TroubleTicketTypeDetails;

/**
 * A trouble ticket problem category, as returned by the reference types endpoint. The
 * {@code code} value is used when placing a trouble ticket order.
 */
public interface TroubleTicketType {

    String getCategory();

    String getCode();

    String getDescription();

    String getSeverity();

    TroubleTicketTypeDetails getAdditionalDetails();
}
