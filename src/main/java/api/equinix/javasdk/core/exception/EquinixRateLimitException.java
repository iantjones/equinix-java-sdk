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

package api.equinix.javasdk.core.exception;

/**
 * Thrown when the Equinix API returns HTTP 429 Too Many Requests.
 * This indicates the client has exceeded the API rate limit. Implement
 * exponential backoff and retry logic when catching this exception.
 *
 * @author ianjones
 * @version $Id: $Id
 */
public class EquinixRateLimitException extends EquinixServiceException {

    public EquinixRateLimitException(String errorMessage) {
        super(errorMessage);
    }

    public EquinixRateLimitException(String errorMessage, Exception cause) {
        super(errorMessage, cause);
    }
}
