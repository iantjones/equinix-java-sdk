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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * The single request-body representation carried by an {@link EquinixRequest}. An immutable
 * carrier in one of three shapes:
 *
 * <ul>
 *   <li>{@link #json(Object)} — a Java payload object serialized to JSON;</li>
 *   <li>{@link #form(Map)} — {@code application/x-www-form-urlencoded} form fields;</li>
 *   <li>{@link #bytes(byte[], String)} — a pre-encoded raw body (e.g. a hand-built
 *       {@code multipart/form-data} payload).</li>
 * </ul>
 *
 * <p>The wire entity is built from this carrier by {@link RequestFactory} at dispatch time — once
 * per attempt — so a JSON payload that is mutated between dispatches (the POST-search paging
 * pipeline advances the body's pagination offset) is re-serialized automatically. No transport
 * (Apache HttpClient) type appears in this class's API.</p>
 *
 * @author ianjones
 */
public final class RequestBody {

    /** The shape of the body: a JSON payload, form fields, or pre-encoded raw bytes. */
    public enum Kind { JSON, FORM, BINARY }

    private final Kind kind;
    private final Object payload;
    private final Map<String, String> formFields;
    private final byte[] bytes;
    private final String binaryContentType;

    private RequestBody(Kind kind, Object payload, Map<String, String> formFields, byte[] bytes, String binaryContentType) {
        this.kind = kind;
        this.payload = payload;
        this.formFields = formFields;
        this.bytes = bytes;
        this.binaryContentType = binaryContentType;
    }

    /**
     * A body whose payload object is serialized to JSON at dispatch time, honoring the request's
     * content type and any Jackson {@code FilterProvider} set on the request.
     *
     * @param payload the object to serialize (never {@code null})
     * @return the JSON body carrier
     */
    public static RequestBody json(Object payload) {
        Objects.requireNonNull(payload, "payload");
        return new RequestBody(Kind.JSON, payload, null, null, null);
    }

    /**
     * An {@code application/x-www-form-urlencoded} body built from the given fields (field order
     * is preserved on the wire).
     *
     * @param fields the form field name/value pairs (never {@code null}; copied)
     * @return the form body carrier
     */
    public static RequestBody form(Map<String, String> fields) {
        Objects.requireNonNull(fields, "fields");
        return new RequestBody(Kind.FORM, null, Collections.unmodifiableMap(new LinkedHashMap<>(fields)), null, null);
    }

    /**
     * A pre-encoded raw body sent verbatim with the given entity content type (e.g. a hand-built
     * {@code multipart/form-data} payload; the request's own content-type header may carry
     * additional parameters such as the multipart boundary).
     *
     * @param bytes the raw body bytes (never {@code null}; copied)
     * @param contentType the entity content type (may be {@code null})
     * @return the binary body carrier
     */
    public static RequestBody bytes(byte[] bytes, String contentType) {
        Objects.requireNonNull(bytes, "bytes");
        return new RequestBody(Kind.BINARY, null, null, bytes.clone(), contentType);
    }

    /**
     * @return the shape of this body
     */
    public Kind getKind() {
        return kind;
    }

    /**
     * The JSON payload object, or {@code null} for non-JSON bodies. For POST-search requests this
     * is the live search body whose pagination state the paging pipeline advances between pages.
     *
     * @return the payload object of a {@link Kind#JSON} body, else {@code null}
     */
    public Object getPayload() {
        return payload;
    }

    /**
     * @return the unmodifiable form fields of a {@link Kind#FORM} body, else {@code null}
     */
    public Map<String, String> getFormFields() {
        return formFields;
    }

    /**
     * @return a copy of the raw bytes of a {@link Kind#BINARY} body, else {@code null}
     */
    public byte[] getBytes() {
        return bytes != null ? bytes.clone() : null;
    }

    /**
     * @return the entity content type of a {@link Kind#BINARY} body, else {@code null}
     */
    public String getBinaryContentType() {
        return binaryContentType;
    }
}
