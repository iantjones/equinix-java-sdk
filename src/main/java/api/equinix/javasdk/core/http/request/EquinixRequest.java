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

package api.equinix.javasdk.core.http.request;

import api.equinix.javasdk.core.auth.EquinixStaticCredentialsProvider;
import api.equinix.javasdk.core.enums.HttpMethod;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ser.FilterProvider;
import lombok.*;
import org.apache.http.HttpEntity;

import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>Abstract EquinixRequest class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
@Getter
@Setter
@NoArgsConstructor
public abstract class EquinixRequest<T> implements Request<T> {

    private EquinixStaticCredentialsProvider equinixCredentialsProvider;
    private URI endPoint;
    private String resourcePath;
    private HttpMethod httpMethod;
    private HttpEntity httpEntity;
    private InputStream content;
    private Request<T> originalRequest;
    private JsonNode functionalAreaJson;
    private Object objectToSerialize;

    private String functionalArea;
    private String requestParent;
    private String serviceEndpoint;

    private TypeReference<?> typeReference;
    private FilterProvider filters;

    protected Map<String, List<String>> queryParameters = new HashMap<>();
    private Map<String, String> headers = new HashMap<>();
    private Map<String, String> pathParameters = new HashMap<>();

    /** {@inheritDoc} */
    @Override
    public void addHeader(String headerName, String headerValue) {
        headers.put(headerName, headerValue);
    }

    /** {@inheritDoc} */
    @Override
    public void addQueryParameter(String parameterName, List<String> parameterValues) {
        List<String> parameterList = queryParameters.computeIfAbsent(parameterName, k -> new ArrayList<>());
        parameterList.addAll(parameterValues);
        queryParameters.put(parameterName, parameterList);
    }

    /**
     * <p>addQueryParameters.</p>
     *
     * @param parameterValues a {@link java.util.Map} object.
     */
    public void addQueryParameters(Map<String, List<String>> parameterValues) {
        queryParameters.putAll(parameterValues);
    }

    /**
     * <p>replaceQueryParameter.</p>
     *
     * @param parameterName a {@link java.lang.String} object.
     * @param parameterValues a {@link java.util.List} object.
     */
    public void replaceQueryParameter(String parameterName, List<String> parameterValues) {
        queryParameters.remove(parameterName);

        List<String> parameterList = queryParameters.computeIfAbsent(parameterName, k -> new ArrayList<>());
        parameterList.addAll(parameterValues);
        queryParameters.put(parameterName, parameterList);
    }

    /**
     * <p>addSingleQueryParameter.</p>
     *
     * @param parameterName a {@link java.lang.String} object.
     * @param parameterValue a {@link java.lang.String} object.
     */
    public void addSingleQueryParameter(String parameterName, String parameterValue) {
        List<String> parameterList = queryParameters.computeIfAbsent(parameterName, k -> new ArrayList<>());
        parameterList.add(parameterValue);
        queryParameters.put(parameterName, parameterList);
    }

    /** {@inheritDoc} */
    @Override
    public void addPathParameter(String parameterName, String parameterValue) {
        pathParameters.put(parameterName, parameterValue);
    }
}
