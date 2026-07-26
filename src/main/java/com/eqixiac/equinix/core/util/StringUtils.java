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

package com.eqixiac.equinix.core.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Locale;

/**
 *
 * @author ianjones
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class StringUtils {

    private static final Locale LOCALE_ENGLISH = Locale.ENGLISH;

    /**
     *
     * @return a boolean.
     */
    public static boolean isNullOrEmpty(String value) {
        return "".equals(value) || value == null;
    }

    public static String upperCase(String str) {
        return isNullOrEmpty(str) ? str : str.toUpperCase(LOCALE_ENGLISH);
    }

    public static String lowerCase(String str) {
        if(isNullOrEmpty(str)) {
            return str;
        }
        return str.toLowerCase(LOCALE_ENGLISH);
    }
}
