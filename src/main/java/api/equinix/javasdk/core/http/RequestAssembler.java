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

import api.equinix.javasdk.core.client.EquinixClient;
import api.equinix.javasdk.core.enums.HttpMethod;
import api.equinix.javasdk.core.enums.RequestType;
import api.equinix.javasdk.core.exception.EquinixClientException;
import api.equinix.javasdk.core.http.request.EquinixRequest;
import api.equinix.javasdk.core.http.request.PaginatedPostRequest;
import api.equinix.javasdk.core.http.request.PaginatedRequest;
import api.equinix.javasdk.core.http.response.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import api.equinix.javasdk.core.internal.Constants;
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
        equinixRequest.setEndPoint(equinixClient.getEndPoint());
        equinixRequest.setEquinixCredentialsProvider(equinixClient.getEquinixCredentialsProvider());

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
     * to a non-null request property or path parameter; an unresolved token fails fast with an
     * {@link EquinixClientException} naming the token and endpoint rather than silently dispatching
     * a malformed URI.
     *
     * @throws api.equinix.javasdk.core.exception.EquinixClientException if the endpoint is unknown
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

        if(requestUri != null) {
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
                    String propertyName = uriParamMatcher.group(2);
                    Object propertyValue = null;
                    Method getter = getterMethods.get(propertyName);
                    if (getter != null) {
                        propertyValue = getter.invoke(equinixRequest);
                    }
                    else if (equinixRequest.getPathParameters().containsKey(propertyName)){
                        propertyValue = equinixRequest.getPathParameters().get(propertyName);
                    }

                    // Fail fast on an unresolved URI token. Silently substituting an empty string
                    // produces a structurally different URI (e.g. /routingProtocols//bgpActions)
                    // that dispatches and fails server-side far from the actual cause.
                    if (propertyValue == null) {
                        throw new EquinixClientException("Unresolved URI token '" + uriParamMatcher.group(1)
                                + "' in request URI template '" + uriTemplate + "' for service endpoint '"
                                + equinixRequest.getServiceEndpoint() + "' of resource '" + equinixRequest.getRequestParent()
                                + "'. No request property or path parameter named '" + propertyName + "' was supplied.");
                    }

                    requestUri = requestUri.replace(uriParamMatcher.group(1), propertyValue.toString());
                }
            }
            catch (EquinixClientException ece) {
                throw ece;
            }
            catch (Exception e) {
                throw new EquinixClientException(e);
            }

            formattedResourcePath = formattedResourcePath.replace("{$requestUri}", requestUri);
        }
        else {
            formattedResourcePath = formattedResourcePath.replace("{$requestUri}", empty);
        }

        formattedResourcePath = formattedResourcePath.replace("//", "/");

        if(formattedResourcePath.endsWith("/")){
            formattedResourcePath = formattedResourcePath.substring(0, formattedResourcePath.length()-1);
        }
        equinixRequest.setResourcePath(formattedResourcePath);
        equinixRequest.setHttpMethod(HttpMethod.valueOf(httpMethod));
    }
}
