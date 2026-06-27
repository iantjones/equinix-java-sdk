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
 * Thrown for client-side errors such as request building failures, serialization errors,
 * or network connectivity issues. This is the base class for all Equinix SDK exceptions
 * that are not directly caused by an API response.
 *
 * <p>For errors returned by the Equinix API, see {@link EquinixServiceException} and its subclasses.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 * @see EquinixServiceException
 */
public class EquinixClientException extends BaseException {
    private static final long serialVersionUID = 1L;

    /**
     * <p>Constructor for EquinixClientException.</p>
     *
     * @param message a {@link java.lang.String} object.
     * @param t a {@link java.lang.Throwable} object.
     */
    public EquinixClientException(String message, Throwable t) {
        super(message, t);
    }

    /**
     * <p>Constructor for EquinixClientException.</p>
     *
     * @param message a {@link java.lang.String} object.
     */
    public EquinixClientException(String message) {
        super(message);
    }

    /**
     * <p>Constructor for EquinixClientException.</p>
     *
     * @param t a {@link java.lang.Throwable} object.
     */
    public EquinixClientException(Throwable t) {
        super(t);
    }
}
