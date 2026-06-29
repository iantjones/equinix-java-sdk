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

/**
 * Supplies the {@link EquinixCredentials} used to obtain an OAuth2 access token. The SDK calls
 * {@link #getCredentials()} each time it authenticates — on the first API call and again whenever
 * the current token has expired — so a provider that returns freshly-resolved credentials can
 * rotate a Client Secret (or portal password) without the client being rebuilt.
 *
 * <p>{@link EquinixStaticCredentialsProvider} is the default: it returns a fixed credentials
 * instance. Implement this interface to source credentials elsewhere — a secrets manager, an
 * environment lookup, or a rotating store — and pass the provider to the client or session
 * constructors that accept one (e.g. {@code new Equinix(provider)}, {@code new Fabric(provider)}).</p>
 *
 * @author ianjones
 * @see EquinixStaticCredentialsProvider
 * @see EquinixCredentials
 */
public interface EquinixCredentialsProvider {

    /**
     * Returns the credentials to authenticate with. Called once per authentication; implementations
     * may resolve and return a fresh instance on each call to support rotation.
     *
     * @return the current Equinix credentials (must not be {@code null})
     */
    EquinixCredentials getCredentials();
}
