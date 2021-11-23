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

package api.equinix.javasdk.core.http.response;

import api.equinix.javasdk.core.http.request.EquinixRequest;
import lombok.Getter;
import lombok.Setter;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpRequestBase;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>EquinixResponse class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
@Getter
@Setter
public class EquinixResponse<T> {

    private EquinixRequest<T> equinixRequest;
    private HttpRequestBase httpRequest;
    private HttpResponse httpResponse;

    private String statusText;
    private int statusCode;
    private InputStream content;
    private HttpEntity entity;

    private Map<String, String> headers = new HashMap<>();

    /**
     * <p>Constructor for EquinixResponse.</p>
     *
     * @param equinixRequest a {@link api.equinix.javasdk.core.http.request.EquinixRequest} object.
     * @param httpRequest a {@link org.apache.http.client.methods.HttpRequestBase} object.
     * @param httpResponse a {@link org.apache.http.HttpResponse} object.
     */
    public EquinixResponse(EquinixRequest<T> equinixRequest, HttpRequestBase httpRequest, HttpResponse httpResponse) {
        this.equinixRequest = equinixRequest;
        this.httpRequest = httpRequest;
        this.httpResponse = httpResponse;

        this.setStatusCode(httpResponse.getStatusLine().getStatusCode());
        this.setStatusText(httpResponse.getStatusLine().getReasonPhrase());

        for (Header header : httpResponse.getAllHeaders()) {
            httpResponse.addHeader(header.getName(), header.getValue());
        }
    }
}
