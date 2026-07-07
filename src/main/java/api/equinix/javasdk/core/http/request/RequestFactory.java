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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import api.equinix.javasdk.core.exception.EquinixClientException;
import api.equinix.javasdk.core.internal.Constants;
import api.equinix.javasdk.core.util.ApacheUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ser.FilterProvider;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.*;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicNameValuePair;

/**
 * Converts a fully-populated {@link EquinixRequest} into the Apache {@link HttpRequestBase} that is
 * actually sent on the wire: resolves the URI (endpoint + resource path + encoded query parameters),
 * picks the method-specific Apache request class (including the body-carrying
 * {@link HttpDeleteWithBody} when a DELETE has a body), builds the wire entity from the request's
 * {@link RequestBody} (serializing JSON payloads, encoding form fields, or attaching raw bytes),
 * and copies the request headers.
 *
 * <p>This is the single place where a {@link RequestBody} becomes a transport entity, and it runs
 * once per dispatch attempt — so a JSON payload mutated between attempts (e.g. a POST-search body
 * whose pagination offset was advanced) is re-serialized automatically.</p>
 *
 * @author ianjones
 */
public class RequestFactory {

    public <T> HttpRequestBase create(final EquinixRequest<T> request) {

        URI endpoint = request.getEndPoint();
        String uri;
        String encodedParams;

        uri = ApacheUtils.appendUri(endpoint.toString(), request.getResourcePath(), true);

        encodedParams = ApacheUtils.encodeParameters(request);

        if (encodedParams != null) {
            uri += "?" + encodedParams;
        }

        final HttpRequestBase base = createApacheRequest(request, uri);
        addHeadersToRequest(base, request);

        return base;
    }

    private <T> HttpRequestBase createApacheRequest(EquinixRequest<T> request, String uri) {
        HttpRequestBase httpRequestBase;

        switch (request.getHttpMethod()) {
            case GET: httpRequestBase = new HttpGet(uri);
                break;
            case DELETE: httpRequestBase = request.getBody() != null
                    ? wrapEntity(request, new HttpDeleteWithBody(uri))
                    : new HttpDelete(uri);
                break;
            case POST: httpRequestBase = wrapEntity(request, new HttpPost(uri));
                break;
            case PUT: httpRequestBase = wrapEntity(request, new HttpPut(uri));
                break;
            case PATCH: httpRequestBase = wrapEntity(request, new HttpPatch(uri));
                break;
            default: throw new EquinixClientException("Unknown HTTP method name: " + request.getHttpMethod());
        }

        return httpRequestBase;
    }

    /**
     * Builds the wire entity from the request's {@link RequestBody} (when present) and attaches it
     * to an entity-enclosing Apache request. Keyed directly off {@code getBody()} — the request's
     * only payload representation — so a set body can never be silently dropped.
     */
    private <T> HttpRequestBase wrapEntity(EquinixRequest<T> request, HttpEntityEnclosingRequestBase entityEnclosingRequest) {
        RequestBody body = request.getBody();
        if (body != null) {
            entityEnclosingRequest.setEntity(createEntity(request, body));
        }

        return entityEnclosingRequest;
    }

    /**
     * Materializes a {@link RequestBody} as the Apache entity that goes on the wire.
     */
    private HttpEntity createEntity(EquinixRequest<?> request, RequestBody body) {
        switch (body.getKind()) {
            case JSON:
                return jsonEntity(body.getPayload(), request.getFilters(), request.getContentType());
            case FORM:
                return formEntity(body.getFormFields());
            case BINARY:
                return new ByteArrayEntity(body.getBytes(),
                        body.getBinaryContentType() != null ? ContentType.create(body.getBinaryContentType()) : null);
            default:
                throw new EquinixClientException("Unknown request body kind: " + body.getKind());
        }
    }

    /**
     * Serializes a JSON payload, honoring the request's content type (e.g.
     * {@code application/json-patch+json} for RFC&nbsp;6902 updates) and any Jackson
     * {@link FilterProvider} set on the request.
     */
    private StringEntity jsonEntity(Object payload, FilterProvider filterProvider, String contentType) {
        ContentType entityContentType = (contentType != null)
                ? ContentType.create(contentType, StandardCharsets.UTF_8)
                : ContentType.APPLICATION_JSON;
        try {
            String json = (filterProvider != null)
                    ? Constants.mapper().writer(filterProvider).writeValueAsString(payload)
                    : Constants.mapper().writeValueAsString(payload);
            return new StringEntity(json, entityContentType);
        } catch (JsonProcessingException jpe) {
            throw new EquinixClientException(Constants.JSON_SERIALIZE_EXCEPTION, jpe);
        }
    }

    /**
     * Builds an {@code application/x-www-form-urlencoded} entity from the body's form fields
     * (UTF-8, field order preserved).
     */
    private UrlEncodedFormEntity formEntity(Map<String, String> formFields) {
        List<NameValuePair> pairs = new ArrayList<>();
        for (Entry<String, String> field : formFields.entrySet()) {
            pairs.add(new BasicNameValuePair(field.getKey(), field.getValue()));
        }
        return new UrlEncodedFormEntity(pairs, StandardCharsets.UTF_8);
    }

    private <T> void addHeadersToRequest(HttpRequestBase httpRequest, EquinixRequest<T> request) {

        httpRequest.addHeader(HttpHeaders.HOST, getHostHeaderValue(request.getEndPoint()));

        for (Entry<String, String> entry : request.getHeaders().entrySet()) {
            httpRequest.addHeader(entry.getKey(), entry.getValue());
        }
    }

    /**
     * The RFC 7230 §5.4 Host header value: {@code host} for a default-port endpoint,
     * {@code host:port} when the endpoint carries an explicit port (e.g. a gateway or test server).
     */
    private String getHostHeaderValue(final URI endpoint) {
        return endpoint.getPort() == -1
                ? endpoint.getHost()
                : endpoint.getHost() + ":" + endpoint.getPort();
    }

}
