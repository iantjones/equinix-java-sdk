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

package api.equinix.javasdk.spring;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Externalized configuration for the Equinix SDK, bound from properties prefixed with
 * {@code equinix} (for example {@code equinix.client-id}, {@code equinix.client-secret},
 * and {@code equinix.sandbox}).
 *
 * <p>These properties drive {@link EquinixAutoConfiguration}, which builds the SDK
 * credentials and entry-point beans (such as {@code Fabric}) when a client id is present.</p>
 *
 * @author ianjones
 */
@ConfigurationProperties(prefix = "equinix")
public class EquinixProperties {

    /**
     * The OAuth2 Client ID obtained from the Equinix Developer Portal.
     */
    private String clientId;

    /**
     * The OAuth2 Client Secret obtained from the Equinix Developer Portal.
     */
    private String clientSecret;

    /**
     * Whether to target the Equinix sandbox environment instead of production.
     * Defaults to {@code false} (production).
     */
    private boolean sandbox = false;

    /**
     * Returns the configured OAuth2 Client ID.
     *
     * @return the client id, or {@code null} if not configured
     */
    public String getClientId() {
        return clientId;
    }

    /**
     * Sets the OAuth2 Client ID.
     *
     * @param clientId the client id
     */
    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    /**
     * Returns the configured OAuth2 Client Secret.
     *
     * @return the client secret, or {@code null} if not configured
     */
    public String getClientSecret() {
        return clientSecret;
    }

    /**
     * Sets the OAuth2 Client Secret.
     *
     * @param clientSecret the client secret
     */
    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    /**
     * Returns whether the sandbox environment is targeted.
     *
     * @return {@code true} for sandbox, {@code false} for production
     */
    public boolean isSandbox() {
        return sandbox;
    }

    /**
     * Sets whether to target the sandbox environment.
     *
     * @param sandbox {@code true} for sandbox, {@code false} for production
     */
    public void setSandbox(boolean sandbox) {
        this.sandbox = sandbox;
    }
}
