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

import com.eqixiac.equinix.core.client.EquinixClient;
import com.eqixiac.equinix.core.enums.HttpMethod;
import com.eqixiac.equinix.core.enums.RequestType;
import com.eqixiac.equinix.core.exception.EquinixClientException;
import com.eqixiac.equinix.core.http.request.EquinixRequest;
import com.eqixiac.equinix.core.http.request.PaginatedPostRequest;
import com.eqixiac.equinix.core.http.request.PaginatedRequest;
import com.eqixiac.equinix.core.http.response.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.eqixiac.equinix.core.internal.Constants;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Assembles {@link EquinixRequest} instances: instantiates the right request subtype for the
 * operation's {@link RequestType}, derives the response {@link JavaType} from the resource's
 * JSON class, and resolves the endpoint's URI template ({@code {$token}} placeholders) against
 * the request's properties and path parameters.
 *
 * <p>Split out of the former monolithic {@code Utils} class; see {@link ResponseHandler},
 * {@link ParameterMapper} and {@link SerializationHelper} for the other request/response
 * helpers.</p>
 *
 * @author ianjones
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RequestAssembler {

    private static final String uriParamFormat = "(\\{\\$(\\w+)})";
    private static final Pattern uriParamPattern = Pattern.compile(uriParamFormat);

    // Per request-class property->getter map, derived once via Introspector and reused. Without this,
    // getBeanInfo + descriptor streaming ran on every API call that has a {$param} URI (only ~3 request classes).
    private static final java.util.concurrent.ConcurrentMap<Class<?>, Map<String, Method>> GETTER_CACHE =
            new java.util.concurrent.ConcurrentHashMap<>();

    public static <T> EquinixRequest<T> buildRequest(String functionalArea, String requestParent,
                                                     String serviceEndpoint, RequestType requestType,
                                                     EquinixClient equinixClient, Map<String, String> pathParams,
                                                     Map<String, List<String>> queryParams, TypeReference<?> typeReference) {

        EquinixRequest<T> equinixRequest;

        switch (requestType) {
            case PAGINATED:
                equinixRequest = new PaginatedRequest<>();
                break;
            case PAGINATED_POST:
                equinixRequest = new PaginatedPostRequest<>();
                break;
            case LIST:
            case SINGLE:
                equinixRequest = new EquinixRequest<>();
                break;
            default:
                throw new EquinixClientException("Unsupported request type '" + requestType
                        + "' for service endpoint '" + serviceEndpoint + "'.");
        }

        if(pathParams != null) {
            equinixRequest.setPathParameters(pathParams);
        }

        if(queryParams != null) {
            equinixRequest.setQueryParameters(queryParams);
        }

        if(requestType == RequestType.PAGINATED) {
            ((PaginatedRequest<T>) equinixRequest).seedPagingFromQueryParams();
        }

        equinixRequest.setTypeReference(typeReference);

        equinixRequest.setFunctionalAreaJson(equinixClient.getClientResourceFile().path("functionalAreas").get(functionalArea));
        equinixRequest.setFunctionalArea(functionalArea);
        equinixRequest.setRequestParent(requestParent);
        equinixRequest.setServiceEndpoint(serviceEndpoint);
        equinixRequest.setEndPoint(equinixClient.getEndPointFor(functionalArea));

        return equinixRequest;
    }

    /**
     * Builds a request whose response type is <em>derived</em> from the resource's JSON class
     * (via {@link #deriveResponseType}) rather than a hand-declared {@code TypeReference}.
     *
     * @param jsonClass the resource's JSON model class (e.g. {@code ServiceProfileJson.class})
     */
    public static <T> EquinixRequest<T> buildRequest(String functionalArea, String requestParent,
                                                     String serviceEndpoint, RequestType requestType,
                                                     EquinixClient equinixClient, Map<String, String> pathParams,
                                                     Map<String, List<String>> queryParams, Class<?> jsonClass) {
        EquinixRequest<T> equinixRequest = buildRequest(functionalArea, requestParent, serviceEndpoint,
                requestType, equinixClient, pathParams, queryParams, (TypeReference<?>) null);
        equinixRequest.setJavaType(deriveResponseType(requestType, jsonClass));
        return equinixRequest;
    }

    /**
     * Derives the Jackson response {@link JavaType} for an operation from its {@link RequestType}
     * and the resource's JSON class, so each JSON model no longer needs hand-declared
     * {@code paged/list/single} {@code TypeReference} fields.
     *
     * @param requestType the operation's request type
     * @param jsonClass the resource's JSON model class
     * @return the JavaType to deserialize the response into
     */
    public static JavaType deriveResponseType(RequestType requestType, Class<?> jsonClass) {
        var typeFactory = Constants.mapper().getTypeFactory();
        switch (requestType) {
            case PAGINATED:
            case PAGINATED_POST:
                return typeFactory.constructParametricType(Page.class, jsonClass);
            case LIST:
                return typeFactory.constructCollectionType(ArrayList.class, jsonClass);
            case SINGLE:
            default:
                return typeFactory.constructType(jsonClass);
        }
    }

    /**
     * Resolves the endpoint's URI template and HTTP method from the apiParams catalogue onto the
     * request. Every {@code {$token}} placeholder in the endpoint's {@code requestUri} must resolve
     * to a non-null, non-blank request property or path parameter; otherwise an
     * {@link EquinixClientException} naming the token and endpoint is thrown rather than dispatching
     * a URI that addresses a different resource. Values are percent-encoded as single path segments
     * (see {@link #encodePathSegment(String)}).
     *
     * @throws com.eqixiac.equinix.core.exception.EquinixClientException if the endpoint is unknown
     *         or a URI token cannot be resolved
     */
    public static <T> void addRequestParams(EquinixRequest<T> equinixRequest) throws EquinixClientException {
        JsonNode functionalArea = equinixRequest.getFunctionalAreaJson();
        JsonNode requestParent = functionalArea.path(equinixRequest.getRequestParent());
        JsonNode requestEndpoint = requestParent.path("serviceEndpoints").path(equinixRequest.getServiceEndpoint());

        // Fail fast on an unknown endpoint name. Without this, a missing serviceEndpoint silently
        // yields a null httpMethod/requestUri and the request is dispatched with an empty method/URI,
        // surfacing only as an obscure runtime failure far from the cause. A clear error here pins
        // the bug to the exact client/endpoint mismatch against apiParams.
        if (requestEndpoint.isMissingNode()) {
            throw new EquinixClientException("Unknown service endpoint '" + equinixRequest.getServiceEndpoint()
                    + "' for resource '" + equinixRequest.getRequestParent() + "'"
                    + (equinixRequest.getFunctionalArea() != null ? " in functional area '" + equinixRequest.getFunctionalArea() + "'" : "")
                    + ". The endpoint name used by the client does not match any serviceEndpoint declared in the corresponding apiParams JSON.");
        }

        String empty = "";

        String formattedResourcePath;
        String uriFormat = functionalArea.path("uriFormat").textValue();
        int defaultVersion = functionalArea.path("defaultVersion").intValue();

        if (requestParent.has("defaultVersion")) {
            defaultVersion = requestParent.path("defaultVersion").intValue();
        }

        if (requestParent.has("overrideUriFormat")) {
            uriFormat = requestParent.path("overrideUriFormat").textValue();
        }

        String rootUri = requestParent.path("rootUri").textValue();
        String httpMethod = requestEndpoint.path("httpMethod").textValue();
        String requestUri = requestEndpoint.path("requestUri").textValue();
        boolean overrideRootUri = requestEndpoint.path("overrideRootUri").booleanValue();

        formattedResourcePath = uriFormat
                .replace("{$version}", Integer.toString(defaultVersion));

        if(overrideRootUri || rootUri == null) {
            formattedResourcePath = formattedResourcePath.replace("{$rootUri}", empty);
        }
        else {
            formattedResourcePath = formattedResourcePath.replace("{$rootUri}", rootUri);
        }

        // Assemble the structural template first and normalise it (an empty rootUri or requestUri
        // leaves a "//" or a trailing "/" in the TEMPLATE). Only then are path-parameter values
        // substituted, percent-encoded, so a value can never be collapsed away or add a segment.
        formattedResourcePath = formattedResourcePath.replace("{$requestUri}", requestUri != null ? requestUri : empty);
        formattedResourcePath = collapseSlashes(formattedResourcePath);
        if (formattedResourcePath.endsWith("/")) {
            formattedResourcePath = formattedResourcePath.substring(0, formattedResourcePath.length() - 1);
        }

        if (requestUri != null) {
            Matcher uriParamMatcher = uriParamPattern.matcher(requestUri);

            Map<String, Method> getterMethods = GETTER_CACHE.computeIfAbsent(equinixRequest.getClass(), cls -> {
                try {
                    return Arrays.stream(Introspector.getBeanInfo(cls, Object.class).getPropertyDescriptors())
                            .filter(pd -> Objects.nonNull(pd.getReadMethod()))
                            .collect(Collectors.toMap(PropertyDescriptor::getName, PropertyDescriptor::getReadMethod));
                }
                catch (java.beans.IntrospectionException e) {
                    throw new EquinixClientException(e);
                }
            });

            String uriTemplate = requestUri;
            try {
                while (uriParamMatcher.find()) {
                    String token = uriParamMatcher.group(1);
                    String propertyName = uriParamMatcher.group(2);
                    Object propertyValue = null;
                    Method getter = getterMethods.get(propertyName);
                    if (getter != null) {
                        propertyValue = getter.invoke(equinixRequest);
                    }
                    else if (equinixRequest.getPathParameters().containsKey(propertyName)){
                        propertyValue = equinixRequest.getPathParameters().get(propertyName);
                    }

                    // Fail fast on an unresolved or blank URI token. Substituting an empty string
                    // produces a structurally different URI (e.g. /connections/ collapsing to the
                    // collection URL, or /routingProtocols//bgpActions) that dispatches and fails
                    // server-side far from the actual cause, or silently acts on the wrong resource.
                    if (propertyValue == null) {
                        throw new EquinixClientException("Unresolved URI token '" + token
                                + "' in request URI template '" + uriTemplate + "' for service endpoint '"
                                + equinixRequest.getServiceEndpoint() + "' of resource '" + equinixRequest.getRequestParent()
                                + "'. No request property or path parameter named '" + propertyName + "' was supplied.");
                    }
                    String value = propertyValue.toString();
                    if (value.isBlank()) {
                        throw new EquinixClientException("Path parameter '" + propertyName + "' is blank for service endpoint '"
                                + equinixRequest.getServiceEndpoint() + "' of resource '" + equinixRequest.getRequestParent()
                                + "' (URI template '" + uriTemplate + "'). A blank value would address a different resource.");
                    }

                    formattedResourcePath = formattedResourcePath.replace(token, encodePathSegment(value));
                }
            }
            catch (EquinixClientException ece) {
                throw ece;
            }
            catch (Exception e) {
                throw new EquinixClientException(e);
            }
        }

        equinixRequest.setResourcePath(formattedResourcePath);
        equinixRequest.setHttpMethod(HttpMethod.valueOf(httpMethod));
    }

    /** Collapses runs of {@code /} in a URI template (never applied to substituted values). */
    private static String collapseSlashes(String template) {
        return template.replaceAll("/{2,}", "/");
    }

    private static final char[] HEX = "0123456789ABCDEF".toCharArray();

    /**
     * Percent-encodes a value for use as one RFC&nbsp;3986 path segment. The unreserved characters
     * ({@code A-Z a-z 0-9 - . _ ~}) and {@code :} and {@code @} (both legal in a segment) are kept
     * literally, so IAM's {@code project:<uuid>} and ERN values pass through unchanged; every other
     * character, including {@code /}, space, {@code #}, {@code ?}, {@code %}, {@code &} and
     * {@code +}, is UTF-8 percent-encoded.
     *
     * @param value the raw path-parameter value
     * @return the encoded segment
     */
    public static String encodePathSegment(String value) {
        StringBuilder encoded = new StringBuilder(value.length());
        for (byte b : value.getBytes(java.nio.charset.StandardCharsets.UTF_8)) {
            int c = b & 0xFF;
            if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')
                    || c == '-' || c == '.' || c == '_' || c == '~' || c == ':' || c == '@') {
                encoded.append((char) c);
            } else {
                encoded.append('%').append(HEX[c >> 4]).append(HEX[c & 0x0F]);
            }
        }
        return encoded.toString();
    }
}
