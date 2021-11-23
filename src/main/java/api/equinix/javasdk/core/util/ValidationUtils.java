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

package api.equinix.javasdk.core.util;

/**
 * <p>ValidationUtils class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
public class ValidationUtils {

    /**
     * <p>assertNotNull.</p>
     *
     * @param object a T object.
     * @param fieldName a {@link java.lang.String} object.
     * @param <T> a T object.
     * @return a T object.
     * @throws java.lang.IllegalArgumentException if any.
     */
    public static <T> T assertNotNull(T object, String fieldName) throws IllegalArgumentException {
        if (object == null) {
            throw new IllegalArgumentException(String.format("%s cannot be null.", fieldName));
        }
        return object;
    }
}
