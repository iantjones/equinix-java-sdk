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

package com.eqixiac.equinix.core.http;

import com.eqixiac.equinix.core.enums.HttpMethod;
import com.eqixiac.equinix.core.exception.EquinixClientException;
import com.eqixiac.equinix.core.http.request.EquinixRequest;
import com.eqixiac.equinix.core.internal.Constants;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * URI-template resolution in {@link RequestAssembler#addRequestParams}: every {@code {$token}}
 * placeholder must resolve, and an unresolved token must fail fast naming the token and endpoint
 * (previously it was silently substituted with an empty string, producing a structurally
 * different URI that failed server-side far from the cause - the {$bgpPeeringUuid} lesson).
 */
class RequestAssemblerTest {

    private static final String FUNCTIONAL_AREA_JSON = """
            {
              "uriFormat": "widgets/v{$version}/{$rootUri}/{$requestUri}",
              "defaultVersion": 4,
              "Widgets": {
                "rootUri": "widgets",
                "serviceEndpoints": {
                  "GetWidget": { "httpMethod": "GET", "requestUri": "{$uuid}" },
                  "GetWidgetPart": { "httpMethod": "GET", "requestUri": "{$uuid}/parts/{$partId}" },
                  "ListWidgets": { "httpMethod": "GET" }
                }
              },
              "Rootless": {
                "rootUri": "",
                "serviceEndpoints": {
                  "GetProjectRoles": { "httpMethod": "GET", "requestUri": "projects/{$projectId}/roles" }
                }
              }
            }
            """;

    private static EquinixRequest<Object> request(String serviceEndpoint, Map<String, String> pathParams) throws Exception {
        return request("Widgets", serviceEndpoint, pathParams);
    }

    private static EquinixRequest<Object> request(String parent, String serviceEndpoint, Map<String, String> pathParams) throws Exception {
        JsonNode functionalArea = Constants.mapper().readTree(FUNCTIONAL_AREA_JSON);
        EquinixRequest<Object> request = new EquinixRequest<>();
        request.setFunctionalAreaJson(functionalArea);
        request.setFunctionalArea("Widgets");
        request.setRequestParent(parent);
        request.setServiceEndpoint(serviceEndpoint);
        if (pathParams != null) {
            request.setPathParameters(pathParams);
        }
        return request;
    }

    @Test
    void resolvesAllTokensFromPathParameters() throws Exception {
        EquinixRequest<Object> request = request("GetWidgetPart", Map.of("uuid", "abc-123", "partId", "p-9"));

        RequestAssembler.addRequestParams(request);

        assertEquals("widgets/v4/widgets/abc-123/parts/p-9", request.getResourcePath());
        assertEquals(HttpMethod.GET, request.getHttpMethod());
    }

    @Test
    void endpointWithoutRequestUriResolvesToRoot() throws Exception {
        EquinixRequest<Object> request = request("ListWidgets", null);

        RequestAssembler.addRequestParams(request);

        assertEquals("widgets/v4/widgets", request.getResourcePath());
    }

    @Test
    void unresolvedTokenFailsFastNamingTokenAndEndpoint() throws Exception {
        // "uuid" supplied, "partId" not - previously this dispatched .../abc-123/parts/ silently.
        EquinixRequest<Object> request = request("GetWidgetPart", Map.of("uuid", "abc-123"));

        EquinixClientException e = assertThrows(EquinixClientException.class,
                () -> RequestAssembler.addRequestParams(request));

        assertTrue(e.getMessage().contains("{$partId}"), "message must name the unresolved token: " + e.getMessage());
        assertTrue(e.getMessage().contains("GetWidgetPart"), "message must name the endpoint: " + e.getMessage());
    }

    @Test
    void unknownEndpointFailsFastNamingEndpoint() throws Exception {
        EquinixRequest<Object> request = request("NoSuchEndpoint", null);

        EquinixClientException e = assertThrows(EquinixClientException.class,
                () -> RequestAssembler.addRequestParams(request));

        assertTrue(e.getMessage().contains("NoSuchEndpoint"));
    }

    @Test
    void emptyPathParameterIsRejectedInsteadOfCollapsingToTheCollectionUrl() throws Exception {
        // Previously "" yielded widgets/v4/widgets/ -> "widgets/v4/widgets", the LIST url.
        EquinixRequest<Object> request = request("GetWidget", Map.of("uuid", ""));

        EquinixClientException e = assertThrows(EquinixClientException.class,
                () -> RequestAssembler.addRequestParams(request));
        assertTrue(e.getMessage().contains("'uuid' is blank"), e.getMessage());
    }

    @Test
    void blankPathParameterIsRejected() throws Exception {
        EquinixRequest<Object> request = request("GetWidgetPart", Map.of("uuid", "abc", "partId", "  "));

        EquinixClientException e = assertThrows(EquinixClientException.class,
                () -> RequestAssembler.addRequestParams(request));
        assertTrue(e.getMessage().contains("partId"), e.getMessage());
    }

    @Test
    void pathParameterValuesArePercentEncodedAsOneSegment() throws Exception {
        EquinixRequest<Object> request = request("GetWidgetPart",
                Map.of("uuid", "a/b c#d?e%f", "partId", "x&y+z"));

        RequestAssembler.addRequestParams(request);

        assertEquals("widgets/v4/widgets/a%2Fb%20c%23d%3Fe%25f/parts/x%26y%2Bz", request.getResourcePath());
    }

    @Test
    void colonAndAtAreKeptSoIamProjectIdsAndErnsStillWork() throws Exception {
        EquinixRequest<Object> request = request("Rootless", "GetProjectRoles",
                Map.of("projectId", "project:2f6c6a6e-0d3c-4f7f-9c1a-1b2c3d4e5f60"));

        RequestAssembler.addRequestParams(request);

        // An empty rootUri still collapses structurally; the value's ':' is kept.
        assertEquals("widgets/v4/projects/project:2f6c6a6e-0d3c-4f7f-9c1a-1b2c3d4e5f60/roles", request.getResourcePath());
        assertEquals("ern:equinix:user@example.com", RequestAssembler.encodePathSegment("ern:equinix:user@example.com"));
    }

    @Test
    void encodesNonAsciiAsUtf8() {
        assertEquals("caf%C3%A9", RequestAssembler.encodePathSegment("caf\u00e9"));
        assertEquals("a-b.c_d~e", RequestAssembler.encodePathSegment("a-b.c_d~e"));
    }

    @Test
    void encodedValueCannotIntroduceADoubleSlash() throws Exception {
        EquinixRequest<Object> request = request("GetWidgetPart", Map.of("uuid", "/", "partId", "p"));

        RequestAssembler.addRequestParams(request);

        assertEquals("widgets/v4/widgets/%2F/parts/p", request.getResourcePath());
    }
}
