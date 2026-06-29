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
 *
 * @author ianjones
 */
public interface Request<T> {

    void setHeaders(Map<String, String> headers);
    
    Map<String, String> getHeaders();

    void addHeader(String headerName, String headerValue);

    void setQueryParameters(Map<String, List<String>> parameters);

    Map<String, List<String>> getQueryParameters();

    void addQueryParameter(String parameterName, List<String> parameterValues);

    void setPathParameters(Map<String, String> pathParameters);

    Map<String, String> getPathParameters();

    void addPathParameter(String parameterName, String parameterValue);

    void setResourcePath(String path);

    String getResourcePath();

    void setEndPoint(URI endPoint);

    URI getEndPoint();

    void setContent(InputStream content);

    InputStream getContent();

    void setHttpMethod(HttpMethod httpMethod);

    HttpMethod getHttpMethod();

    void setHttpEntity(HttpEntity httpEntity);

    HttpEntity getHttpEntity();

    void setOriginalRequest(Request<T> equinixRequest);

    Request<T> getOriginalRequest();

    void setFilters(FilterProvider filterProvider);

    FilterProvider getFilters();

    void setTypeReference(TypeReference<?> typeReference);

    TypeReference<?> getTypeReference();

    void setFunctionalArea(String functionalArea);

    String getFunctionalArea();

    void setRequestParent(String requestParent);

    String getRequestParent();

    void setServiceEndpoint(String serviceEndpoint);

    String getServiceEndpoint();
}

    
