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

import api.equinix.javasdk.core.model.APIParam;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * <p>ModelUtils class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
public class ModelUtils {

    /**
     * <p>process.</p>
     *
     * @param queryParameter a {@link java.lang.Object} object.
     * @return a {@link java.util.List} object.
     */
    public static List<String> process(Object queryParameter) {
        return singleValueList(detectNull(queryParameter));
    }

    /**
     * <p>singleValueList.</p>
     *
     * @param value a {@link java.lang.String} object.
     * @return a {@link java.util.List} object.
     */
    public static List<String> singleValueList(String value) {
        if(StringUtils.isNullOrEmpty(value)) {
            return null;
        }
        List<String> singleValueList = new ArrayList<>();
        singleValueList.add(value);
        return singleValueList;
    }

    /**
     * <p>singleValueList.</p>
     *
     * @param value a {@link java.lang.Integer} object.
     * @return a {@link java.util.List} object.
     */
    public static List<String> singleValueList(Integer value) {
        return value != null ? singleValueList(value.toString()) : null;
    }

    /**
     * <p>singleValueList.</p>
     *
     * @param value a {@link java.lang.Boolean} object.
     * @return a {@link java.util.List} object.
     */
    public static List<String> singleValueList(Boolean value) {
        return value != null ? singleValueList(value.toString()) : null;
    }

    /**
     * <p>singleValueList.</p>
     *
     * @param value a {@link api.equinix.javasdk.core.model.APIParam} object.
     * @return a {@link java.util.List} object.
     */
    public static List<String> singleValueList(APIParam value) {
        return value != null ? singleValueList(value.toString()) : null;
    }

    /**
     * <p>stringListFromEnumList.</p>
     *
     * @param enumList a {@link java.util.List} object.
     * @return a {@link java.util.List} object.
     */
    public static List<String> stringListFromEnumList(List<APIParam> enumList) {
        if(enumList == null) {
            return null;
        }
        return enumList.stream().map(APIParam::toString).collect(Collectors.toList());
    }

    /**
     * <p>stringListFromIntegerList.</p>
     *
     * @param integerList a {@link java.util.List} object.
     * @return a {@link java.util.List} object.
     */
    public static List<String> stringListFromIntegerList(List<Integer> integerList) {
        if(integerList == null) {
            return null;
        }
        return integerList.stream().map(i->Integer.toString(i)).collect(Collectors.toList());
    }

    /**
     * <p>cleanseQueryParameterList.</p>
     *
     * @param queryParameterList a {@link java.util.Map} object.
     * @return a {@link java.util.Map} object.
     */
    public static Map<String, List<String>> cleanseQueryParameterList(Map<String, List<String>> queryParameterList) {
        if(queryParameterList != null) {
            queryParameterList.values().removeIf(Objects::isNull);
        }
        return queryParameterList;
    }

    /**
     * <p>detectNull.</p>
     *
     * @param objToTest a {@link java.lang.Object} object.
     * @return a {@link java.lang.String} object.
     */
    public static String detectNull(Object objToTest) {
        return objToTest != null ? objToTest.toString() : null;
    }
}
