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
 * Thrown when the Equinix API returns an HTTP 5xx server error (500, 502, 503, 504).
 * This indicates a server-side issue on the Equinix platform. These errors are
 * typically transient and may succeed on retry.
 *
 * @author ianjones
 * @version $Id: $Id
 */
public class EquinixServerException extends EquinixServiceException {

    public EquinixServerException(String errorMessage) {
        super(errorMessage);
    }

    public EquinixServerException(String errorMessage, Exception cause) {
        super(errorMessage, cause);
    }
}
