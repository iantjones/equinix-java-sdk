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
import api.equinix.javasdk.core.exception.EquinixClientException;
import api.equinix.javasdk.core.http.request.*;
import api.equinix.javasdk.core.http.response.*;
import api.equinix.javasdk.core.internal.Constants;
import api.equinix.javasdk.core.model.APIParam;
import api.equinix.javasdk.core.model.OptionalRequestBuilder;
import api.equinix.javasdk.core.enums.HttpMethod;
import api.equinix.javasdk.core.enums.RequestType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ser.FilterProvider;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * <p>Utils class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
public class Utils {

    private static final String uriParamFormat = "(\\{\\$(\\w+)})";
    private static final Pattern uriParamPattern = Pattern.compile(uriParamFormat);

    /**
     * <p>buildRequest.</p>
     *
     * @param functionalArea a {@link java.lang.String} object.
     * @param requestParent a {@link java.lang.String} object.
     * @param serviceEndpoint a {@link java.lang.String} object.
     * @param requestType a {@link api.equinix.javasdk.core.enums.RequestType} object.
     * @param equinixClient a {@link api.equinix.javasdk.core.client.EquinixClient} object.
     * @param pathParams a {@link java.util.Map} object.
     * @param queryParams a {@link java.util.Map} object.
     * @param typeReference a {@link com.fasterxml.jackson.core.type.TypeReference} object.
     * @param <T> a T object.
     * @return a {@link api.equinix.javasdk.core.http.request.EquinixRequest} object.
     */
    public static <T> EquinixRequest<T> buildRequest(String functionalArea, String requestParent,
                                                     String serviceEndpoint, RequestType requestType,
                                                     EquinixClient equinixClient, Map<String, String> pathParams,
                                                     Map<String, List<String>> queryParams, TypeReference<?> typeReference) {

        EquinixRequest<T> equinixRequest = null;

        switch (requestType) {
            case PAGINATED:
                equinixRequest = new PaginatedRequest<>();
                break;
            case PAGINATED_POST:
                equinixRequest = new PaginatedPostRequest<>();
                break;
            case LIST:
                equinixRequest = new ListRequest<>();
                break;
            case SINGLE:
                equinixRequest = new SingletonRequest<>();
                break;
        }

        if(pathParams != null) {
            equinixRequest.setPathParameters(pathParams);
        }

        if(queryParams != null) {
            equinixRequest.setQueryParameters(queryParams);
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
     * <p>newMap.</p>
     *
     * @param requestBuilder a {@link api.equinix.javasdk.core.model.OptionalRequestBuilder} object.
     * @param <R> a R object.
     * @return a {@link java.util.Map} object.
     */
    public static <R> Map<String, List<String>> newMap(OptionalRequestBuilder<R> requestBuilder) {
        return requestBuilder != null ? Utils.processRequestBuilder(requestBuilder) : new HashMap<>();
    }

    /**
     * <p>addAdditionalValue.</p>
     *
     * @param queryParameters a {@link java.util.Map} object.
     * @param parameterName a {@link java.lang.String} object.
     * @param parameterValue a {@link api.equinix.javasdk.core.model.APIParam} object.
     */
    public static void addAdditionalValue(Map<String, List<String>> queryParameters, String parameterName, APIParam parameterValue) {
        addAdditionalValue(queryParameters, parameterName, parameterValue.toString());
    }

    /**
     * <p>addAdditionalValue.</p>
     *
     * @param queryParameters a {@link java.util.Map} object.
     * @param parameterName a {@link java.lang.String} object.
     * @param parameterValue a {@link java.lang.String} object.
     */
    public static void addAdditionalValue(Map<String, List<String>> queryParameters, String parameterName, String parameterValue) {
        if(queryParameters.containsKey(parameterName)) {
            if(!queryParameters.get(parameterName).contains(parameterValue)) {
                queryParameters.get(parameterName).add(parameterValue);
            }
        }
        else {
            queryParameters.put(parameterName, singleParamList(parameterValue));
        }
    }

    /**
     * <p>singlePropertyBody.</p>
     *
     * @param propertyName a {@link java.lang.String} object.
     * @param propertyValue a {@link java.lang.Integer} object.
     * @return a {@link java.util.Map} object.
     */
    public static Map<String, Integer> singlePropertyBody(String propertyName, Integer propertyValue) {
        return (propertyName != null && propertyValue != null) ? Map.of(propertyName, propertyValue) : null;
    }

    /**
     * <p>singlePropertyBody.</p>
     *
     * @param propertyName a {@link java.lang.String} object.
     * @param propertyValue a {@link java.lang.String} object.
     * @return a {@link java.util.Map} object.
     */
    public static Map<String, String> singlePropertyBody(String propertyName, String propertyValue) {
        return (propertyName != null && propertyValue != null) ? Map.of(propertyName, propertyValue) : null;
    }

    /**
     * <p>concatStringMaps.</p>
     *
     * @param maps a {@link java.util.Map} object.
     * @return a {@link java.util.Map} object.
     */
    @SafeVarargs
    public static Map<String, String> concatStringMaps(Map<String, String>... maps) {

        Map<String, String> newMap = new HashMap<>();

        for(Map<String, String> map : maps) {
            newMap.putAll(map);
        }

        return newMap;
    }


    /**
     * <p>singleParamList.</p>
     *
     * @param parameterValue a {@link java.lang.String} object.
     * @return a {@link java.util.List} object.
     */
    public static List<String> singleParamList(String parameterValue) {
        return parameterValue != null ? List.of(parameterValue) : null;
    }

    /**
     * <p>singleParamList.</p>
     *
     * @param parameterValue a {@link java.lang.Boolean} object.
     * @return a {@link java.util.List} object.
     */
    public static List<String> singleParamList(Boolean parameterValue) {
        return singleParamList(parameterValue != null ? parameterValue.toString() : null);
    }

    /**
     * <p>singleParamList.</p>
     *
     * @param parameterValue a {@link java.lang.Integer} object.
     * @return a {@link java.util.List} object.
     */
    public static List<String> singleParamList(Integer parameterValue) {
        return singleParamList(parameterValue != null ? parameterValue.toString() : null);
    }

    /**
     * <p>singleParamList.</p>
     *
     * @param parameterValue a {@link api.equinix.javasdk.core.model.APIParam} object.
     * @return a {@link java.util.List} object.
     */
    public static List<String> singleParamList(APIParam parameterValue) {
        return singleParamList(parameterValue != null ? parameterValue.toString() : null);
    }

    /**
     * <p>singleParamMap.</p>
     *
     * @param parameterName a {@link java.lang.String} object.
     * @param parameterValue a {@link java.lang.String} object.
     * @return a {@link java.util.Map} object.
     */
    public static Map<String, List<String>> singleParamMap(String parameterName, String parameterValue) {
        return (parameterName != null && parameterValue != null) ? Map.of(parameterName, singleParamList(parameterValue)) : null;
    }

    /**
     * <p>singleParamMap.</p>
     *
     * @param parameterName a {@link java.lang.String} object.
     * @param parameterValue a {@link api.equinix.javasdk.core.model.APIParam} object.
     * @return a {@link java.util.Map} object.
     */
    public static Map<String, List<String>> singleParamMap(String parameterName, APIParam parameterValue) {
        return singleParamMap(parameterName, (parameterValue != null) ? parameterValue.toString() : null);
    }

    /**
     * <p>singleParamMap.</p>
     *
     * @param parameterName a {@link java.lang.String} object.
     * @param parameterValue a {@link java.lang.Boolean} object.
     * @return a {@link java.util.Map} object.
     */
    public static Map<String, List<String>> singleParamMap(String parameterName, Boolean parameterValue) {
        return singleParamMap(parameterName, (parameterValue != null) ? parameterValue.toString() : null);
    }

    /**
     * <p>singleParamMap.</p>
     *
     * @param parameterName a {@link java.lang.String} object.
     * @param parameterValue a {@link java.lang.Integer} object.
     * @return a {@link java.util.Map} object.
     */
    public static Map<String, List<String>> singleParamMap(String parameterName, Integer parameterValue) {
        return singleParamMap(parameterName, (parameterValue != null) ? parameterValue.toString() : null);
    }

    /**
     * <p>processRequestBuilder.</p>
     *
     * @param requestBuilder a {@link api.equinix.javasdk.core.model.OptionalRequestBuilder} object.
     * @param <R> a R object.
     * @return a {@link java.util.Map} object.
     */
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
     * <p>dateTimeForQuery.</p>
     *
     * @param localDateTime a {@link java.time.LocalDateTime} object.
     * @return a {@link java.lang.String} object.
     */
    public static String dateTimeForQuery(LocalDateTime localDateTime) {
        return Constants.queryParamFormatter.format(localDateTime);
    }

    /**
     * <p>serializeJson.</p>
     *
     * @param equinixRequest a {@link api.equinix.javasdk.core.http.request.EquinixRequest} object.
     * @param objectToSerialize a {@link java.lang.Object} object.
     * @param <T> a T object.
     */
    public static <T> void serializeJson(EquinixRequest<T> equinixRequest, Object objectToSerialize) {
        equinixRequest.setObjectToSerialize(objectToSerialize);
        equinixRequest.setHttpEntity(serializeJson(equinixRequest.getObjectToSerialize(), equinixRequest.getFilters()));
    }

    private static StringEntity serializeJson(Object content, FilterProvider filterProvider) {
        try {
            if (filterProvider != null) {
                return new StringEntity(Constants.objectMapper.writer(filterProvider).writeValueAsString(content), ContentType.APPLICATION_JSON);
            } else {
                return new StringEntity(Constants.objectMapper.writeValueAsString(content), ContentType.APPLICATION_JSON);
            }
        } catch (JsonProcessingException jpe) {
            throw new EquinixClientException(Constants.JSON_SERIALIZE_EXCEPTION, jpe);
        }
    }

    /**
     * <p>mapPaginatedList.</p>
     *
     * @param paginatedList a {@link java.util.ArrayList} object.
     * @param serviceClient a {@link api.equinix.javasdk.core.http.response.Pageable} object.
     * @param objectMapper a {@link java.util.function.BiFunction} object.
     * @param <T> a T object.
     * @param <S> a S object.
     * @return a {@link api.equinix.javasdk.core.http.response.PaginatedList} object.
     */
    public static <T, S> PaginatedList<T> mapPaginatedList(ArrayList<S> paginatedList, Pageable<T> serviceClient,
                                                           BiFunction<? super S, ? super Pageable<T>, ? extends T> objectMapper){
        return paginatedList.stream()
                .map(jsonObject -> objectMapper.apply(jsonObject, serviceClient))
                .collect(Collectors.toCollection(PaginatedList::new));
    }

    public static <T, S> PaginatedFilteredList<T> mapPaginatedFilteredList(ArrayList<S> paginatedList, PageablePost<T> serviceClient,
                                                                           BiFunction<? super S, ? super PageablePost<T>, ? extends T> objectMapper){
        return paginatedList.stream()
                .map(jsonObject -> objectMapper.apply(jsonObject, serviceClient))
                .collect(Collectors.toCollection(PaginatedFilteredList::new));
    }

    /**
     * <p>mapList.</p>
     *
     * @param itemList a {@link java.util.List} object.
     * @param serviceClient a {@link api.equinix.javasdk.core.http.response.Pageable} object.
     * @param objectMapper a {@link java.util.function.BiFunction} object.
     * @param <T> a T object.
     * @param <S> a S object.
     * @return a {@link java.util.List} object.
     */
    public static <T, S> List<T> mapList(List<S> itemList, Pageable<T> serviceClient,
                                                           BiFunction<? super S, ? super Pageable<T>, ? extends T> objectMapper){
        return itemList.stream()
                .map(jsonObject -> objectMapper.apply(jsonObject, serviceClient))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * <p>addRequestParams.</p>
     *
     * @param equinixRequest a {@link api.equinix.javasdk.core.http.request.EquinixRequest} object.
     * @param <T> a T object.
     * @throws api.equinix.javasdk.core.exception.EquinixClientException if any.
     */
    public static <T> void addRequestParams(EquinixRequest<T> equinixRequest) throws EquinixClientException {
        JsonNode functionalArea = equinixRequest.getFunctionalAreaJson();
        JsonNode requestParent = functionalArea.path(equinixRequest.getRequestParent());
        JsonNode requestEndpoint = requestParent.path("serviceEndpoints").path(equinixRequest.getServiceEndpoint());

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

            try {
                Map<String, Object> getterMethods = Arrays.stream(Introspector.getBeanInfo(equinixRequest.getClass(), Object.class).getPropertyDescriptors())
                        .filter(pd -> Objects.nonNull(pd.getReadMethod()))
                        .collect(Collectors.toMap(PropertyDescriptor::getName, PropertyDescriptor::getReadMethod));

                while (uriParamMatcher.find()) {
                    String propertyName = uriParamMatcher.group(2);
                    Object propertyValue = null;
                    if (getterMethods.containsKey(propertyName)) {
                        propertyValue = ((Method) getterMethods.get(propertyName)).invoke(equinixRequest);
                    }
                    else if (equinixRequest.getPathParameters().containsKey(propertyName)){
                        propertyValue = equinixRequest.getPathParameters().get(propertyName);
                    }

                    requestUri = requestUri.replace(uriParamMatcher.group(1), propertyValue == null ? "" : propertyValue.toString());
                }
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

    /**
     * <p>handlePaginatedListResponse.</p>
     *
     * @param equinixResponse a {@link api.equinix.javasdk.core.http.response.EquinixResponse} object.
     * @param equinixRequest a {@link api.equinix.javasdk.core.http.request.EquinixRequest} object.
     * @param <T> a T object.
     * @param <S> a S object.
     * @return a {@link api.equinix.javasdk.core.http.response.Page} object.
     * @throws api.equinix.javasdk.core.exception.EquinixClientException if any.
     */
    @SuppressWarnings("unchecked")
    public static <T, S> Page<T, S> handlePaginatedListResponse(EquinixResponse<T> equinixResponse, EquinixRequest<T> equinixRequest) throws EquinixClientException {
        try {
            Page<T, S> responsePage = (Page<T, S>) Constants.objectMapper.readValue(equinixResponse.getContent(), equinixRequest.getTypeReference());
            responsePage.setAssociatedRequest(equinixRequest);
            responsePage.setAssociatedResponse(equinixResponse);
            return responsePage;
        }
        catch (Exception ioe) {
            throw new EquinixClientException(Constants.JSON_DESERIALIZE_EXCEPTION, ioe);
        }
    }

    /**
     * <p>handleListResponse.</p>
     *
     * @param equinixResponse a {@link api.equinix.javasdk.core.http.response.EquinixResponse} object.
     * @param equinixRequest a {@link api.equinix.javasdk.core.http.request.EquinixRequest} object.
     * @param <T> a T object.
     * @param <S> a S object.
     * @return a {@link java.util.List} object.
     * @throws api.equinix.javasdk.core.exception.EquinixClientException if any.
     */
    @SuppressWarnings("unchecked")
    public static <T, S> List<S> handleListResponse(EquinixResponse<T> equinixResponse, EquinixRequest<T> equinixRequest) throws EquinixClientException  {
        try {
            return (List<S>) Constants.objectMapper.readValue(equinixResponse.getContent(), equinixRequest.getTypeReference());
        }
        catch (Exception ioe) {
            throw new EquinixClientException(Constants.JSON_DESERIALIZE_EXCEPTION, ioe);
        }
    }

    /**
     * <p>handleSingletonResponse.</p>
     *
     * @param equinixResponse a {@link api.equinix.javasdk.core.http.response.EquinixResponse} object.
     * @param equinixRequest a {@link api.equinix.javasdk.core.http.request.EquinixRequest} object.
     * @param <S> a S object.
     * @param <T> a T object.
     * @return a S object.
     * @throws api.equinix.javasdk.core.exception.EquinixClientException if any.
     */
    @SuppressWarnings("unchecked")
    public static <S, T> S handleSingletonResponse(EquinixResponse<T> equinixResponse, EquinixRequest<T> equinixRequest) throws EquinixClientException  {
        try {
            return (S) Constants.objectMapper.readValue(equinixResponse.getContent(), equinixRequest.getTypeReference());
        }
        catch (Exception ioe) {
            throw new EquinixClientException(Constants.JSON_DESERIALIZE_EXCEPTION, ioe);
        }
    }

    @SuppressWarnings("unchecked")
    public static <S, T> S unpackOriginalPost(EquinixRequest<T> equinixRequest, TypeReference<?> typeReference) throws EquinixClientException  {
        try {
            return (S) Constants.objectMapper.readValue(equinixRequest.getContent(), typeReference);
        }
        catch (Exception ioe) {
            throw new EquinixClientException(Constants.JSON_DESERIALIZE_EXCEPTION, ioe);
        }
    }

    /**
     * <p>extractFromHeader.</p>
     *
     * @param equinixResponse a {@link api.equinix.javasdk.core.http.response.EquinixResponse} object.
     * @param headerName a {@link java.lang.String} object.
     * @param extractionPattern a {@link java.util.regex.Pattern} object.
     * @param <T> a T object.
     * @return a {@link java.lang.String} object.
     */
    public static <T> String extractFromHeader(EquinixResponse<T> equinixResponse, String headerName, Pattern extractionPattern) {
        try {
            Header locationHeader = Arrays.stream(equinixResponse.getHttpResponse().getAllHeaders())
                    .filter(header -> header.getName().equals(headerName))
                    .findFirst()
                    .orElseThrow();
            Matcher headerMatcher = extractionPattern.matcher(locationHeader.getValue());
            return headerMatcher.find() ? headerMatcher.group(1) : null;
        }
        catch (NoSuchElementException | IllegalStateException e) {
            throw new EquinixClientException("Cannot find desired response header or failed to match pattern.", e);
        }
    }

    /**
     * <p>handleBooleanResponse.</p>
     *
     * @param equinixResponse a {@link api.equinix.javasdk.core.http.response.EquinixResponse} object.
     * @param equinixRequest a {@link api.equinix.javasdk.core.http.request.EquinixRequest} object.
     * @param <T> a T object.
     * @return a {@link java.lang.Boolean} object.
     */
    public static <T> Boolean handleBooleanResponse(EquinixResponse<T> equinixResponse, EquinixRequest<T> equinixRequest) {
        return isRequestSuccessful(equinixResponse.getHttpResponse());
    }

    /**
     * <p>handleStringResponse.</p>
     *
     * @param equinixResponse a {@link api.equinix.javasdk.core.http.response.EquinixResponse} object.
     * @param <T> a T object.
     * @return a {@link java.lang.String} object.
     * @throws api.equinix.javasdk.core.exception.EquinixClientException if any.
     */
    public static <T> String handleStringResponse(EquinixResponse<T> equinixResponse) throws EquinixClientException  {
        try {
            return EntityUtils.toString(equinixResponse.getEntity());
        }
        catch (Exception ioe) {
            throw new EquinixClientException(Constants.JSON_DESERIALIZE_EXCEPTION);
        }
    }

    public static <T> byte[] handleByteResponse(EquinixResponse<T> equinixResponse) throws EquinixClientException  {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            equinixResponse.getEntity().writeTo(outputStream);
            return outputStream.toByteArray();
        }
        catch (Exception ioe) {
            throw new EquinixClientException(Constants.JSON_DESERIALIZE_EXCEPTION);
        }
    }

    /**
     * <p>handleMapResponse.</p>
     *
     * @param equinixRequest a {@link api.equinix.javasdk.core.http.request.EquinixRequest} object.
     * @param equinixResponse a {@link api.equinix.javasdk.core.http.response.EquinixResponse} object.
     * @param <T> a T object.
     * @return a {@link java.util.HashMap} object.
     * @throws api.equinix.javasdk.core.exception.EquinixClientException if any.
     */
    @SuppressWarnings("unchecked")
    public static <T> HashMap<String, String> handleMapResponse(EquinixRequest<T> equinixRequest, EquinixResponse<T> equinixResponse) throws EquinixClientException  {
        try {
            return (HashMap<String, String>) Constants.objectMapper.readValue(equinixResponse.getContent(), equinixRequest.getTypeReference());
        }
        catch (Exception ioe) {
            throw new EquinixClientException(Constants.JSON_DESERIALIZE_EXCEPTION);
        }
    }

    private static boolean isRequestSuccessful(HttpResponse response) {
        int status = response.getStatusLine().getStatusCode();
        return (status / 100 == HttpStatus.SC_OK / 100);
    }


}
