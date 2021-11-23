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

import api.equinix.javasdk.core.enums.Protocol;
import api.equinix.javasdk.core.exception.EquinixClientException;
import api.equinix.javasdk.core.exception.EquinixServiceException;
import api.equinix.javasdk.core.exception.ExceptionDetail;
import api.equinix.javasdk.core.http.request.EquinixRequest;
import api.equinix.javasdk.core.http.request.RequestFactory;
import api.equinix.javasdk.core.http.response.EquinixResponse;
import api.equinix.javasdk.core.internal.Constants;
import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.io.EmptyInputStream;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * <p>EquinixHttpClient class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
public class EquinixHttpClient {

    private static final Logger logger = Logger.getLogger(EquinixHttpClient.class.getName());

    private final CloseableHttpClient httpClient;
    private final RequestFactory requestFactory = new RequestFactory();
    private final Protocol protocol = Protocol.HTTPS;
    public Boolean outputRequestJson = true;

    /**
     * <p>Constructor for EquinixHttpClient.</p>
     */
    public EquinixHttpClient() {
        setOutputRequestJson(true);
        httpClient = HttpClients.custom()
                .setDefaultRequestConfig(RequestConfig.custom()
                        .setCookieSpec(CookieSpecs.STANDARD).build()).build();
    }

    /**
     * <p>Setter for the field <code>outputRequestJson</code>.</p>
     *
     * @param outputRequestJson a {@link java.lang.Boolean} object.
     */
    public void setOutputRequestJson(Boolean outputRequestJson) {
        this.outputRequestJson = outputRequestJson;
    }

    /**
     * <p>executeSingleRequest.</p>
     *
     * @param equinixRequest a {@link api.equinix.javasdk.core.http.request.EquinixRequest} object.
     * @param singleRequestParams a {@link api.equinix.javasdk.core.http.EquinixHttpClient.SingleRequestParams} object.
     * @param <T> a T object.
     * @return a {@link api.equinix.javasdk.core.http.response.EquinixResponse} object.
     * @throws api.equinix.javasdk.core.exception.EquinixClientException if any.
     */
    public <T> EquinixResponse<T> executeSingleRequest(EquinixRequest<T> equinixRequest, SingleRequestParams singleRequestParams)
            throws EquinixClientException {

        singleRequestParams.newApacheRequest(requestFactory, equinixRequest);

        try {
            if(this.outputRequestJson) {
                if(equinixRequest.getContent() != null) {
                    String requestJson = new BufferedReader(
                            new InputStreamReader(equinixRequest.getContent(), StandardCharsets.UTF_8)).lines()
                            .collect(Collectors.joining("\n"));
                    logger.info(requestJson);
                }
            }

            singleRequestParams.apacheResponse = httpClient.execute(singleRequestParams.apacheRequest);

            EquinixResponse<T> equinixResponse = new EquinixResponse<>(equinixRequest, singleRequestParams.apacheRequest, singleRequestParams.apacheResponse);
            equinixResponse.setEquinixRequest(equinixRequest);

            logger.info(equinixRequest.getHttpMethod() + " " + singleRequestParams.apacheRequest.getURI() + " - Status: " + equinixResponse.getStatusCode() + " " + equinixResponse.getStatusText());

            if(singleRequestParams.apacheResponse.getEntity() != null) {
                equinixResponse.setEntity(singleRequestParams.apacheResponse.getEntity());
                equinixResponse.setContent(equinixResponse.getEntity().getContent());
            }

            if(!isRequestSuccessful(singleRequestParams.apacheResponse)) {
                EquinixServiceException ese = new EquinixServiceException("Error returned by Equinix API.");
                ese.setStatusCode(equinixResponse.getStatusCode());
                ese.setPath(singleRequestParams.apacheRequest.getURI().toString());

                TypeReference<ArrayList<ExceptionDetail>> typeReference = new TypeReference<>(){};

                ArrayList<ExceptionDetail> exceptionDetails;

                if(!(equinixResponse.getContent() instanceof EmptyInputStream)) {
                    exceptionDetails = Constants.objectMapper.readValue(equinixResponse.getContent(), typeReference);
                    ese.setExceptionDetails(exceptionDetails);
                }

                throw ese;
            }
            else {
                return equinixResponse;
            }
        }
        catch (IOException ioe) {
            throw new EquinixClientException(ioe);
        }
    }

    private static class SingleRequestParams {
        HttpRequestBase apacheRequest;
        HttpResponse apacheResponse;

        <T> void newApacheRequest(final RequestFactory httpRequestFactory, final EquinixRequest<T> equinixRequest) {
            apacheRequest = httpRequestFactory.create(equinixRequest);
        }
    }

    /**
     * <p>executeHelper.</p>
     *
     * @param equinixRequest a {@link api.equinix.javasdk.core.http.request.EquinixRequest} object.
     * @param <T> a T object.
     * @return a {@link api.equinix.javasdk.core.http.response.EquinixResponse} object.
     * @throws api.equinix.javasdk.core.exception.EquinixClientException if any.
     */
    public <T> EquinixResponse<T> executeHelper(final EquinixRequest<T> equinixRequest)
            throws EquinixClientException {
        final SingleRequestParams execOneParams = new SingleRequestParams();
        return executeSingleRequest(equinixRequest, execOneParams);
    }

    private boolean isRequestSuccessful(HttpResponse response) {
        int status = response.getStatusLine().getStatusCode();
        return (status / 100 == HttpStatus.SC_OK / 100);
    }

    /**
     * <p>Getter for the field <code>protocol</code>.</p>
     *
     * @return a {@link api.equinix.javasdk.core.enums.Protocol} object.
     */
    public Protocol getProtocol() {
        return protocol;
    }
}
