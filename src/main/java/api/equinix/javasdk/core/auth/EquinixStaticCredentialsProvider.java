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

package api.equinix.javasdk.core.auth;

import api.equinix.javasdk.core.util.ValidationUtils;
import org.apache.http.auth.AuthScope;
import org.apache.http.impl.client.BasicCredentialsProvider;

/**
 * <p>EquinixStaticCredentialsProvider class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
public class EquinixStaticCredentialsProvider extends BasicCredentialsProvider {

    private EquinixCredentials credentials;

    /**
     * <p>Constructor for EquinixStaticCredentialsProvider.</p>
     *
     * @param credentials a {@link api.equinix.javasdk.core.auth.EquinixCredentials} object.
     */
    public EquinixStaticCredentialsProvider(EquinixCredentials credentials) {
        this.credentials = ValidationUtils.assertNotNull(credentials, "credentials");
    }

    /**
     * <p>Getter for the field <code>credentials</code>.</p>
     *
     * @return a {@link api.equinix.javasdk.core.auth.EquinixCredentials} object.
     */
    public EquinixCredentials getCredentials() {
        return credentials;
    }

    /**
     * <p>Setter for the field <code>credentials</code>.</p>
     *
     * @param authScope a {@link org.apache.http.auth.AuthScope} object.
     * @param credentials a {@link api.equinix.javasdk.core.auth.EquinixCredentials} object.
     */
    public void setCredentials(AuthScope authScope, EquinixCredentials credentials) {
        this.credentials = credentials;
    }

    /**
     * <p>refresh.</p>
     */
    public void refresh() {
    }
}
