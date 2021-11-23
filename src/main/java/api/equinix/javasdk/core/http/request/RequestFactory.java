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

import java.net.URI;
import java.util.Map.Entry;

import api.equinix.javasdk.core.enums.HttpMethod;
import api.equinix.javasdk.core.exception.ClientException;
import api.equinix.javasdk.core.util.ApacheUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.*;

/**
 * <p>RequestFactory class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
public class RequestFactory {

    private static final String DEFAULT_ENCODING = "UTF-8";

    /**
     * <p>create.</p>
     *
     * @param request a {@link api.equinix.javasdk.core.http.request.EquinixRequest} object.
     * @param <T> a T object.
     * @return a {@link org.apache.http.client.methods.HttpRequestBase} object.
     */
    public <T> HttpRequestBase create(final EquinixRequest<T> request) {

        URI endpoint = request.getEndPoint();
        String uri;
        String encodedParams;

        uri = ApacheUtils.appendUri(endpoint.toString(), request.getResourcePath(), true);

        encodedParams = ApacheUtils.encodeParameters(request);

        boolean requestHasNoPayload = request.getContent() != null;
        boolean requestIsPost = request.getHttpMethod() == HttpMethod.POST;
        boolean putParamsInUri = !requestIsPost || requestHasNoPayload;

        if (encodedParams != null) {
            uri += "?" + encodedParams;
        }

        final HttpRequestBase base = createApacheRequest(request, uri, encodedParams);
        addHeadersToRequest(base, request);

        return base;
    }

    private <T> HttpRequestBase createApacheRequest(EquinixRequest<T> request, String uri, String encodedParams) {
        HttpRequestBase httpRequestBase;

        switch (request.getHttpMethod()) {
            case GET: httpRequestBase = new HttpGet(uri);
                break;
            case DELETE: httpRequestBase = new HttpDelete(uri);
                break;
            case POST: httpRequestBase = wrapEntity(request, new HttpPost(uri), encodedParams);
                break;
            case PUT: httpRequestBase = wrapEntity(request, new HttpPut(uri), encodedParams);
                break;
            case PATCH: httpRequestBase = wrapEntity(request, new HttpPatch(uri), encodedParams);
                break;
            default: throw new ClientException("Unknown HTTP method name: " + request.getHttpMethod());
        }

        return httpRequestBase;
    }

    private <T> HttpRequestBase wrapEntity(EquinixRequest<T> request, HttpEntityEnclosingRequestBase entityEnclosingRequest, String encodedParams) {
        if (request.getContent() != null) {
            createHttpEntity(request, entityEnclosingRequest);
        }

        return entityEnclosingRequest;
    }

    private <T> void createHttpEntity(EquinixRequest<T> request, HttpEntityEnclosingRequestBase entityEnclosingRequest) {
//        HttpEntity entity = new BasicHttpEntity();
        entityEnclosingRequest.setEntity(request.getHttpEntity());
    }

    private <T> void addHeadersToRequest(HttpRequestBase httpRequest, EquinixRequest<T> request) {

        httpRequest.addHeader(HttpHeaders.HOST, getHostHeaderValue(request.getEndPoint()));

        for (Entry<String, String> entry : request.getHeaders().entrySet()) {
            httpRequest.addHeader(entry.getKey(), entry.getValue());
        }

        if (httpRequest.getHeaders(HttpHeaders.CONTENT_TYPE) == null || httpRequest.getHeaders(HttpHeaders.CONTENT_TYPE).length == 0) {
            httpRequest.addHeader(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded; " + "charset=" + DEFAULT_ENCODING.toLowerCase());
        }
    }

    private String getHostHeaderValue(final URI endpoint) {
        return endpoint.getHost();
    }

}
