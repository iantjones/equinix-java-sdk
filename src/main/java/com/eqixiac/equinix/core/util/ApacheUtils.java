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

package com.eqixiac.equinix.core.util;

import com.eqixiac.equinix.core.http.request.EquinixRequest;
import com.eqixiac.equinix.core.internal.Constants;
import com.eqixiac.equinix.core.enums.Protocol;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *
 * @author ianjones
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ApacheUtils {

    public static URI toUri(String endPoint, Protocol protocol) {

        if (!endPoint.contains("://")) {
            endPoint = protocol.toString() + "://" + endPoint;
        }

        try {
            return new URI(endPoint);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     *
     * @param escapeDoubleSlash a boolean.
     */
    public static String appendUri(final String baseUri, String path, final boolean escapeDoubleSlash) {
        String resultUri = baseUri;
        if (path != null && path.length() > 0) {
            if (path.startsWith("/")) {
                if (resultUri.endsWith("/")) {
                    resultUri = resultUri.substring(0, resultUri.length() - 1);
                }
            } else if (!resultUri.endsWith("/")) {
                resultUri += "/";
            }
            if (escapeDoubleSlash) {
                resultUri += path.replace("//", "/%2F");
            } else {
                resultUri += path;
            }
        } else if (!resultUri.endsWith("/")) {
            resultUri += "/";
        }

        return resultUri;
    }

    public static <T> String encodeParameters(EquinixRequest<T> equinixRequest) {

        final Map<String, List<String>> requestParams = equinixRequest.getQueryParameters();

        if (requestParams.isEmpty()) {
            return null;
        }

        List<NameValuePair> nameValuePairs = new ArrayList<>();

        for (Map.Entry<String, List<String>> entry : requestParams.entrySet()) {
            String parameterName = entry.getKey();
            for (String value : entry.getValue()) {
                nameValuePairs.add(new BasicNameValuePair(parameterName, value));
            }
        }

        return URLEncodedUtils.format(nameValuePairs, Constants.DEFAULT_ENCODING);
    }
}
