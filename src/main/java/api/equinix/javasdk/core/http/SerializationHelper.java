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

import api.equinix.javasdk.core.http.request.EquinixRequest;
import api.equinix.javasdk.core.http.request.RequestBody;

/**
 * Attaches JSON request bodies: the payload is carried on the request as a
 * {@link RequestBody} and serialized to the wire by
 * {@link api.equinix.javasdk.core.http.request.RequestFactory} at dispatch time (once per
 * attempt), honoring the request's content type and any Jackson
 * {@link com.fasterxml.jackson.databind.ser.FilterProvider}. Because serialization is deferred,
 * a payload mutated after attachment (e.g. a POST-search body whose pagination offset the paging
 * pipeline advances) is re-serialized automatically on the next dispatch.
 *
 * <p>Split out of the former monolithic {@code Utils} class; see {@link RequestAssembler},
 * {@link ResponseHandler} and {@link ParameterMapper} for the other request/response helpers.</p>
 *
 * @author ianjones
 */
public final class SerializationHelper {

    private SerializationHelper() {
    }

    /**
     * Attaches {@code objectToSerialize} as the request's JSON body.
     *
     * @param equinixRequest the request to carry the body
     * @param objectToSerialize the payload to serialize at dispatch time (never {@code null})
     */
    public static <T> void serializeJson(EquinixRequest<T> equinixRequest, Object objectToSerialize) {
        equinixRequest.setBody(RequestBody.json(objectToSerialize));
    }
}
