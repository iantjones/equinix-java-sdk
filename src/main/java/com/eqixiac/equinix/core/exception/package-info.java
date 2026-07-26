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

/**
 * Exception hierarchy for Equinix API errors. The base
 * {@link com.eqixiac.equinix.core.exception.EquinixServiceException} represents
 * server-side failures, while {@link com.eqixiac.equinix.core.exception.EquinixClientException}
 * covers client-side issues. HTTP status codes are mapped to typed exceptions:
 * 401 to {@link com.eqixiac.equinix.core.exception.EquinixAuthenticationException},
 * 403 to {@link com.eqixiac.equinix.core.exception.EquinixAuthorizationException},
 * 404 to {@link com.eqixiac.equinix.core.exception.EquinixNotFoundException},
 * 409 to {@link com.eqixiac.equinix.core.exception.EquinixConflictException},
 * 429 to {@link com.eqixiac.equinix.core.exception.EquinixRateLimitException}, and
 * 5xx to {@link com.eqixiac.equinix.core.exception.EquinixServerException}.
 *
 * @see com.eqixiac.equinix.core.exception.BaseException
 * @see com.eqixiac.equinix.core.exception.EquinixServiceException
 */
package com.eqixiac.equinix.core.exception;
