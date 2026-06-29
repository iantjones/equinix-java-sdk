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

import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;

import java.net.URI;

/**
 * A {@code DELETE} request that can carry a request body.
 *
 * <p>Apache HttpClient's {@link org.apache.http.client.methods.HttpDelete} extends
 * {@code HttpRequestBase} and therefore cannot enclose an entity. A few Equinix APIs require a body
 * on {@code DELETE} — notably IAM optimistic-concurrency deletes that take a required
 * {@code lastRev} body. This variant extends {@link HttpEntityEnclosingRequestBase} so the body
 * reaches the wire while still using the {@code DELETE} method.</p>
 */
public class HttpDeleteWithBody extends HttpEntityEnclosingRequestBase {

    /** The HTTP method name ({@code DELETE}). */
    public static final String METHOD_NAME = "DELETE";

    /**
     * Creates a body-carrying {@code DELETE} request for the given URI.
     *
     * @param uri the request URI
     */
    public HttpDeleteWithBody(final String uri) {
        super();
        setURI(URI.create(uri));
    }

    /** {@inheritDoc} */
    @Override
    public String getMethod() {
        return METHOD_NAME;
    }
}
