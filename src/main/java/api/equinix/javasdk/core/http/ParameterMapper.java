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

package api.equinix.javasdk.core.http;

import api.equinix.javasdk.core.internal.Constants;
import api.equinix.javasdk.core.model.APIParam;
import api.equinix.javasdk.core.model.OptionalRequestBuilder;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds query-parameter maps and single-value parameter lists for request construction, and
 * formats query-string values (e.g. UTC timestamps).
 *
 * <p>Split out of the former monolithic {@code Utils} class; see {@link RequestAssembler},
 * {@link ResponseHandler} and {@link SerializationHelper} for the other request/response
 * helpers.</p>
 *
 * @author ianjones
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ParameterMapper {

    public static <R> Map<String, List<String>> newMap(OptionalRequestBuilder<R> requestBuilder) {
        return requestBuilder != null ? processRequestBuilder(requestBuilder) : new HashMap<>();
    }

    public static void addAdditionalValue(Map<String, List<String>> queryParameters, String parameterName, APIParam parameterValue) {
        addAdditionalValue(queryParameters, parameterName, parameterValue != null ? parameterValue.paramValue() : null);
    }

    /**
     * Accumulates {@code parameterValue} under {@code parameterName}, creating the value list on
     * first use and appending (deduplicated) thereafter. The stored list is always mutable — the
     * previous implementation seeded new keys with an immutable {@code List.of}, so accumulating a
     * second distinct value for the same key threw {@code UnsupportedOperationException}. A
     * {@code null} value is a no-op (optional parameters are simply omitted).
     */
    public static void addAdditionalValue(Map<String, List<String>> queryParameters, String parameterName, String parameterValue) {
        if (parameterValue == null) {
            return;
        }
        List<String> existing = queryParameters.get(parameterName);
        if (existing == null) {
            queryParameters.put(parameterName, new ArrayList<>(List.of(parameterValue)));
        }
        else if (!existing.contains(parameterValue)) {
            // Copy-on-write so a key seeded elsewhere with an immutable list can still accumulate.
            List<String> updated = new ArrayList<>(existing);
            updated.add(parameterValue);
            queryParameters.put(parameterName, updated);
        }
    }

    public static Map<String, Integer> singlePropertyBody(String propertyName, Integer propertyValue) {
        return (propertyName != null && propertyValue != null) ? Map.of(propertyName, propertyValue) : null;
    }

    public static Map<String, String> singlePropertyBody(String propertyName, String propertyValue) {
        return (propertyName != null && propertyValue != null) ? Map.of(propertyName, propertyValue) : null;
    }

    @SafeVarargs
    public static Map<String, String> concatStringMaps(Map<String, String>... maps) {

        Map<String, String> newMap = new HashMap<>();

        for(Map<String, String> map : maps) {
            newMap.putAll(map);
        }

        return newMap;
    }

    public static List<String> singleParamList(String parameterValue) {
        return parameterValue != null ? List.of(parameterValue) : null;
    }

    public static List<String> singleParamList(Boolean parameterValue) {
        return singleParamList(parameterValue != null ? parameterValue.toString() : null);
    }

    public static List<String> singleParamList(Integer parameterValue) {
        return singleParamList(parameterValue != null ? parameterValue.toString() : null);
    }

    public static List<String> singleParamList(APIParam parameterValue) {
        return singleParamList(parameterValue != null ? parameterValue.paramValue() : null);
    }

    public static Map<String, List<String>> singleParamMap(String parameterName, String parameterValue) {
        return (parameterName != null && parameterValue != null) ? Map.of(parameterName, singleParamList(parameterValue)) : null;
    }

    public static Map<String, List<String>> singleParamMap(String parameterName, APIParam parameterValue) {
        return singleParamMap(parameterName, (parameterValue != null) ? parameterValue.paramValue() : null);
    }

    public static Map<String, List<String>> singleParamMap(String parameterName, Boolean parameterValue) {
        return singleParamMap(parameterName, (parameterValue != null) ? parameterValue.toString() : null);
    }

    public static Map<String, List<String>> singleParamMap(String parameterName, Integer parameterValue) {
        return singleParamMap(parameterName, (parameterValue != null) ? parameterValue.toString() : null);
    }

    public static <R> Map<String, List<String>> processRequestBuilder(OptionalRequestBuilder<R> requestBuilder) {
        if(requestBuilder != null) {
            if (!requestBuilder.wasBuilt()) {
                requestBuilder.build();
            }

            if(requestBuilder.getQueryParameters().size() > 0) {
                return requestBuilder.getQueryParameters();
            }
        }
        return Collections.emptyMap();
    }

    /**
     * Formats a date-time for a query parameter in the API's UTC wire format
     * ({@code yyyy-MM-dd'T'HH:mm:ss'Z'}).
     *
     * <p><strong>UTC contract: {@code LocalDateTime} inputs are UTC wall clock.</strong> This
     * matches every timestamp the SDK returns — the core deserializer parses
     * {@code "...T12:00:00Z"} into a bare {@code LocalDateTime} of 12:00, so any API-sourced
     * value (e.g. {@code changeLog.getCreatedDateTime()}) can be passed straight back here and
     * round-trips unchanged. The digits are formatted <em>verbatim</em> with a literal
     * {@code 'Z'}; no zone conversion is performed. For "now", use
     * {@code LocalDateTime.now(ZoneOffset.UTC)} — never zone-local {@code LocalDateTime.now()}.</p>
     *
     * @param localDateTime the date-time, as UTC wall clock
     * @return the wire representation, the input digits with a literal {@code 'Z'} appended
     */
    public static String dateTimeForQuery(LocalDateTime localDateTime) {
        return Constants.queryParamFormatter.format(localDateTime);
    }
}
