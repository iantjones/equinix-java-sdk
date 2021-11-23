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

import api.equinix.javasdk.core.enums.Side;

import java.util.Locale;

/**
 * <p>StringUtils class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
public class StringUtils {

    private static final Locale LOCALE_ENGLISH = Locale.ENGLISH;

    /**
     * <p>isNullOrEmpty.</p>
     *
     * @param value a {@link java.lang.String} object.
     * @return a boolean.
     */
    public static boolean isNullOrEmpty(String value) {
        return "".equals(value) || value == null;
    }

    /**
     * <p>upperCase.</p>
     *
     * @param str a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public static String upperCase(String str) {
        return isNullOrEmpty(str) ? str : str.toUpperCase(LOCALE_ENGLISH);
    }

    /**
     * <p>lowerCase.</p>
     *
     * @param str a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public static String lowerCase(String str) {
        if(isNullOrEmpty(str)) {
            return str;
        }
        return str.toLowerCase(LOCALE_ENGLISH);
    }

    /**
     * <p>sideToViewPoint.</p>
     *
     * @param side a {@link api.equinix.javasdk.core.enums.Side} object.
     * @return a {@link java.lang.String} object.
     */
    public static String sideToViewPoint(Side side) {
        return side != null ? side.toString().replace("A_Side", "aSide").replace("Z_Side", "zSide") : null;
    }
}
