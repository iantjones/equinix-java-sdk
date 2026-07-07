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

import api.equinix.javasdk.core.enums.HttpMethod;
import api.equinix.javasdk.core.http.request.EquinixRequest;
import api.equinix.javasdk.core.http.request.HttpDeleteWithBody;
import api.equinix.javasdk.core.http.request.PaginatedPostRequest;
import api.equinix.javasdk.core.http.request.RequestBody;
import api.equinix.javasdk.core.http.request.RequestFactory;
import api.equinix.javasdk.core.model.FilteredPaginatedPost;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Wire behavior of the unified {@link RequestBody} abstraction: {@link RequestFactory} is the
 * single place a body becomes a transport entity, built at dispatch time — so JSON payloads
 * mutated between dispatches (POST-search paging) are re-serialized, form fields are URL-encoded,
 * raw bytes pass through verbatim, and a DELETE grows a body only when one is attached.
 */
class RequestFactoryTest {

    private final RequestFactory factory = new RequestFactory();

    private static <T> EquinixRequest<T> request(HttpMethod method) {
        EquinixRequest<T> request = new EquinixRequest<>();
        request.setEndPoint(URI.create("https://api.equinix.com"));
        request.setResourcePath("fabric/v4/widgets");
        request.setHttpMethod(method);
        return request;
    }

    private static String entityString(HttpRequestBase apacheRequest) throws Exception {
        return EntityUtils.toString(((HttpEntityEnclosingRequest) apacheRequest).getEntity());
    }

    @Test
    void jsonBodyIsSerializedAtDispatchTime_pickingUpPayloadMutations() throws Exception {
        // The POST-search paging pipeline mutates the live body between dispatches; the entity
        // must be rebuilt from the payload on every create() call.
        FilteredPaginatedPost<String> body = new FilteredPaginatedPost<>("my-filter");
        PaginatedPostRequest<Object> request = new PaginatedPostRequest<>();
        request.setEndPoint(URI.create("https://api.equinix.com"));
        request.setResourcePath("fabric/v4/widgets/search");
        request.setHttpMethod(HttpMethod.POST);
        request.setBody(RequestBody.json(body));

        String firstDispatch = entityString(factory.create(request));
        assertTrue(firstDispatch.contains("\"offset\":0"), firstDispatch);

        body.getPagination().nextPage();

        String secondDispatch = entityString(factory.create(request));
        assertTrue(secondDispatch.contains("\"offset\":" + body.getPagination().getLimit()), secondDispatch);
        assertTrue(secondDispatch.contains("\"filter\":\"my-filter\""), secondDispatch);
    }

    @Test
    void jsonBodyHonorsTheRequestContentType() throws Exception {
        EquinixRequest<Object> request = request(HttpMethod.PATCH);
        request.setContentType("application/json-patch+json");
        request.setBody(RequestBody.json(Map.of("op", "replace")));

        HttpRequestBase apacheRequest = factory.create(request);

        String contentType = ((HttpEntityEnclosingRequest) apacheRequest).getEntity().getContentType().getValue();
        assertTrue(contentType.startsWith("application/json-patch+json"), contentType);
    }

    @Test
    void formBodyIsUrlEncodedPreservingFieldOrder() throws Exception {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("grantType", "client_credentials");
        fields.put("scope", "read write");

        EquinixRequest<Object> request = request(HttpMethod.POST);
        request.setBody(RequestBody.form(fields));

        HttpRequestBase apacheRequest = factory.create(request);

        assertEquals("grantType=client_credentials&scope=read+write", entityString(apacheRequest));
        String contentType = ((HttpEntityEnclosingRequest) apacheRequest).getEntity().getContentType().getValue();
        assertTrue(contentType.startsWith("application/x-www-form-urlencoded"), contentType);
    }

    @Test
    void binaryBodyPassesBytesThroughVerbatimWithItsContentType() throws Exception {
        byte[] multipart = "--boundary\r\ncontent\r\n--boundary--\r\n".getBytes();
        EquinixRequest<Object> request = request(HttpMethod.POST);
        request.setBody(RequestBody.bytes(multipart, "multipart/form-data"));

        HttpRequestBase apacheRequest = factory.create(request);

        assertArrayEquals(multipart, EntityUtils.toByteArray(((HttpEntityEnclosingRequest) apacheRequest).getEntity()));
        String contentType = ((HttpEntityEnclosingRequest) apacheRequest).getEntity().getContentType().getValue();
        assertTrue(contentType.startsWith("multipart/form-data"), contentType);
    }

    @Test
    void deleteWithBodyEnclosesTheEntity_andPlainDeleteDoesNot() throws Exception {
        EquinixRequest<Object> withBody = request(HttpMethod.DELETE);
        withBody.setBody(RequestBody.json(Map.of("lastRev", "5")));
        HttpRequestBase bodied = factory.create(withBody);
        assertInstanceOf(HttpDeleteWithBody.class, bodied);
        assertTrue(entityString(bodied).contains("\"lastRev\":\"5\""));

        HttpRequestBase plain = factory.create(request(HttpMethod.DELETE));
        assertInstanceOf(HttpDelete.class, plain);
    }

    @Test
    void hostHeaderCarriesTheExplicitPort() {
        EquinixRequest<Object> request = request(HttpMethod.GET);
        request.setEndPoint(URI.create("https://gw.corp:8443"));

        HttpRequestBase apacheRequest = factory.create(request);

        assertEquals("gw.corp:8443", apacheRequest.getFirstHeader("Host").getValue());
    }

    @Test
    void requestBodyAccessorsExposeNoTransportTypes() {
        // The unified body carrier is plain data: payload object, form fields, raw bytes.
        RequestBody json = RequestBody.json("payload");
        assertEquals(RequestBody.Kind.JSON, json.getKind());
        assertEquals("payload", json.getPayload());
        assertNull(json.getFormFields());
        assertNull(json.getBytes());

        RequestBody bytes = RequestBody.bytes(new byte[] {1, 2}, null);
        byte[] copy = bytes.getBytes();
        copy[0] = 9;
        assertArrayEquals(new byte[] {1, 2}, bytes.getBytes(), "returned bytes are a defensive copy");
    }
}
