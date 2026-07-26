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

package com.eqixiac.equinix.customerportal.enums;

import com.eqixiac.equinix.core.model.APIParam;

/**
 * Preferred time window to call a smart hands contact's phone number. {@code BUSINESS_HOURS}
 * is deprecated and will be removed in a future release.
 */
public enum PhonePreferenceToCall implements APIParam {
    NEVER,
    ANYTIME,
    MY_BUSINESS_HOURS,
    IBX_BUSINESS_HOURS,
    BUSINESS_HOURS
}
