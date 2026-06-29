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

import api.equinix.javasdk.core.enums.HttpMethod;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ser.FilterProvider;
import org.apache.http.HttpEntity;

import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * <p>Request interface.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
public interface Request<T> {

    /**
     * <p>setHeaders.</p>
     *
     * @param headers a {@link java.util.Map} object.
     */
    void setHeaders(Map<String, String> headers);
    
    /**
     * <p>getHeaders.</p>
     *
     * @return a {@link java.util.Map} object.
     */
    Map<String, String> getHeaders();

    /**
     * <p>addHeader.</p>
     *
     * @param headerName a {@link java.lang.String} object.
     * @param headerValue a {@link java.lang.String} object.
     */
    void addHeader(String headerName, String headerValue);

    /**
     * <p>setQueryParameters.</p>
     *
     * @param parameters a {@link java.util.Map} object.
     */
    void setQueryParameters(Map<String, List<String>> parameters);

    /**
     * <p>getQueryParameters.</p>
     *
     * @return a {@link java.util.Map} object.
     */
    Map<String, List<String>> getQueryParameters();

    /**
     * <p>addQueryParameter.</p>
     *
     * @param parameterName a {@link java.lang.String} object.
     * @param parameterValues a {@link java.util.List} object.
     */
    void addQueryParameter(String parameterName, List<String> parameterValues);

    /**
     * <p>setPathParameters.</p>
     *
     * @param pathParameters a {@link java.util.Map} object.
     */
    void setPathParameters(Map<String, String> pathParameters);

    /**
     * <p>getPathParameters.</p>
     *
     * @return a {@link java.util.Map} object.
     */
    Map<String, String> getPathParameters();

    /**
     * <p>addPathParameter.</p>
     *
     * @param parameterName a {@link java.lang.String} object.
     * @param parameterValue a {@link java.lang.String} object.
     */
    void addPathParameter(String parameterName, String parameterValue);

    /**
     * <p>setResourcePath.</p>
     *
     * @param path a {@link java.lang.String} object.
     */
    void setResourcePath(String path);

    /**
     * <p>getResourcePath.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    String getResourcePath();

    /**
     * <p>setEndPoint.</p>
     *
     * @param endPoint a {@link java.net.URI} object.
     */
    void setEndPoint(URI endPoint);

    /**
     * <p>getEndPoint.</p>
     *
     * @return a {@link java.net.URI} object.
     */
    URI getEndPoint();

    /**
     * <p>setContent.</p>
     *
     * @param content a {@link java.io.InputStream} object.
     */
    void setContent(InputStream content);

    /**
     * <p>getContent.</p>
     *
     * @return a {@link java.io.InputStream} object.
     */
    InputStream getContent();

    /**
     * <p>setHttpMethod.</p>
     *
     * @param httpMethod a {@link api.equinix.javasdk.core.enums.HttpMethod} object.
     */
    void setHttpMethod(HttpMethod httpMethod);

    /**
     * <p>getHttpMethod.</p>
     *
     * @return a {@link api.equinix.javasdk.core.enums.HttpMethod} object.
     */
    HttpMethod getHttpMethod();

    /**
     * <p>setHttpEntity.</p>
     *
     * @param httpEntity a {@link org.apache.http.HttpEntity} object.
     */
    void setHttpEntity(HttpEntity httpEntity);

    /**
     * <p>getHttpEntity.</p>
     *
     * @return a {@link org.apache.http.HttpEntity} object.
     */
    HttpEntity getHttpEntity();

    /**
     * <p>setOriginalRequest.</p>
     *
     * @param equinixRequest a {@link api.equinix.javasdk.core.http.request.Request} object.
     */
    void setOriginalRequest(Request<T> equinixRequest);

    /**
     * <p>getOriginalRequest.</p>
     *
     * @return a {@link api.equinix.javasdk.core.http.request.Request} object.
     */
    Request<T> getOriginalRequest();

    /**
     * <p>setFilters.</p>
     *
     * @param filterProvider a {@link com.fasterxml.jackson.databind.ser.FilterProvider} object.
     */
    void setFilters(FilterProvider filterProvider);

    /**
     * <p>getFilters.</p>
     *
     * @return a {@link com.fasterxml.jackson.databind.ser.FilterProvider} object.
     */
    FilterProvider getFilters();

    /**
     * <p>setTypeReference.</p>
     *
     * @param typeReference a {@link com.fasterxml.jackson.core.type.TypeReference} object.
     */
    void setTypeReference(TypeReference<?> typeReference);

    /**
     * <p>getTypeReference.</p>
     *
     * @return a {@link com.fasterxml.jackson.core.type.TypeReference} object.
     */
    TypeReference<?> getTypeReference();

    /**
     * <p>setFunctionalArea.</p>
     *
     * @param functionalArea a {@link java.lang.String} object.
     */
    void setFunctionalArea(String functionalArea);

    /**
     * <p>getFunctionalArea.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    String getFunctionalArea();

    /**
     * <p>setRequestParent.</p>
     *
     * @param requestParent a {@link java.lang.String} object.
     */
    void setRequestParent(String requestParent);

    /**
     * <p>getRequestParent.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    String getRequestParent();

    /**
     * <p>setServiceEndpoint.</p>
     *
     * @param serviceEndpoint a {@link java.lang.String} object.
     */
    void setServiceEndpoint(String serviceEndpoint);

    /**
     * <p>getServiceEndpoint.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    String getServiceEndpoint();
}

    
