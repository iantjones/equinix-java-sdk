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

package api.equinix.javasdk;

import api.equinix.javasdk.core.auth.EquinixCredentials;
import api.equinix.javasdk.core.auth.EquinixCredentialsProvider;
import api.equinix.javasdk.core.auth.EquinixStaticCredentialsProvider;
import api.equinix.javasdk.core.client.CoreConfigImpl;
import api.equinix.javasdk.core.client.CoreImpl;
import api.equinix.javasdk.core.client.interfaces.Core;
import api.equinix.javasdk.core.client.interfaces.CoreConfig;
import api.equinix.javasdk.core.exception.EquinixClientException;
import lombok.Getter;

import java.io.Closeable;
import java.io.IOException;

/**
 * The base class for all Equinix SDK domain entry points.
 *
 * <p>This class provides common functionality shared by all domain clients including
 * OAuth2 authentication, HTTP client management, and resource lifecycle. Domain-specific
 * entry points ({@link Fabric}, {@link NetworkEdge}, {@link CustomerPortal}, etc.) extend
 * this class and add their own resource accessors.</p>
 *
 * <p>Authentication is handled automatically on the first API call using OAuth2 client
 * credentials. The client can also be explicitly authenticated using {@link #authenticate()}.
 * Implements {@link Closeable} to properly release HTTP connections when the client is no
 * longer needed.</p>
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * // Domain clients extend EquinixClient
 * Fabric fabric = new Fabric(credentials);
 *
 * // Optional: explicitly authenticate before making calls
 * fabric.authenticate();
 *
 * // Use try-with-resources for proper cleanup
 * try (Fabric fabric = new Fabric(credentials)) {
 *     PaginatedList<Port> ports = fabric.ports().list();
 * }
 * }</pre>
 *
 * @author ianjones
 * @see Fabric
 * @see NetworkEdge
 * @see CustomerPortal
 * @see IBXSmartView
 * @see InternetAccess
 * @see Projects
 * @see IAM
 */
public class EquinixClient implements Closeable {

    private Core core;

    final private CoreConfig coreConfig;

    @Getter
    final protected api.equinix.javasdk.core.client.EquinixClient equinixClient;

    /**
     * Whether this client owns the underlying core client (and must close it on {@link #close()}),
     * or shares one owned by an {@link Equinix} session.
     */
    private final boolean ownsCore;

    /**
     * Whether an explicit {@link #authenticate()} should eagerly load the metro catalog. Set from
     * {@code EquinixConfig.isAutoLoadMetros()}; {@code false} for a shared-core domain client, whose
     * owning {@link Equinix} session drives metro auto-loading instead. Only acted on by domains with
     * a metro catalog (Fabric).
     */
    protected final boolean autoLoadMetros;

    /**
     * Creates a new Equinix client using the provided credentials.
     * Authentication occurs automatically on the first API call.
     *
     * @param equinixCredentials the OAuth2 credentials for authenticating with Equinix APIs
     */
    public EquinixClient(EquinixCredentials equinixCredentials) {
        this(equinixCredentials, EquinixConfig.defaults());
    }

    /**
     * Creates a new Equinix client with optional sandbox mode.
     *
     * @param equinixCredentials the OAuth2 credentials for authenticating with Equinix APIs
     * @param isSandBoxed {@code true} to use the sandbox environment for testing; {@code false} for production
     */
    public EquinixClient(EquinixCredentials equinixCredentials, boolean isSandBoxed) {
        this(equinixCredentials, EquinixConfig.builder().sandbox(isSandBoxed).build());
    }

    /**
     * Creates a new Equinix client with explicit {@link EquinixConfig} options.
     *
     * @param equinixCredentials the OAuth2 credentials for authenticating with Equinix APIs
     * @param config the construction-time options
     */
    public EquinixClient(EquinixCredentials equinixCredentials, EquinixConfig config) {
        this(new EquinixStaticCredentialsProvider(equinixCredentials), config);
    }

    /**
     * Creates a new Equinix client that resolves its credentials through the given provider.
     * Authentication occurs automatically on the first API call, consulting the provider each
     * time (including re-auth on expiry) so credentials can be rotated at runtime.
     *
     * @param credentialsProvider supplies the OAuth2 credentials for authenticating with Equinix APIs
     */
    public EquinixClient(EquinixCredentialsProvider credentialsProvider) {
        this(credentialsProvider, EquinixConfig.defaults());
    }

    /**
     * Creates a new Equinix client over a custom credentials provider, with optional sandbox mode.
     *
     * @param credentialsProvider supplies the OAuth2 credentials for authenticating with Equinix APIs
     * @param isSandBoxed {@code true} to use the sandbox environment for testing; {@code false} for production
     */
    public EquinixClient(EquinixCredentialsProvider credentialsProvider, boolean isSandBoxed) {
        this(credentialsProvider, EquinixConfig.builder().sandbox(isSandBoxed).build());
    }

    /**
     * Canonical constructor: creates a new Equinix client over a credentials provider with explicit
     * {@link EquinixConfig} options. All other public constructors funnel through this one.
     *
     * @param credentialsProvider supplies the OAuth2 credentials for authenticating with Equinix APIs
     * @param config the construction-time options
     */
    public EquinixClient(EquinixCredentialsProvider credentialsProvider, EquinixConfig config) {
        this.equinixClient = new api.equinix.javasdk.core.client.EquinixClient(credentialsProvider, config.isSandbox());
        this.coreConfig = new CoreConfigImpl(equinixClient);
        this.ownsCore = true;
        this.autoLoadMetros = config.isAutoLoadMetros();
        equinixClient.setAuthenticator(() -> core().authenticate());
        if (config.getRetryPolicy() != null) {
            equinixClient.setRetryPolicy(config.getRetryPolicy());
        }
    }

    /**
     * Creates a domain client over an existing, shared core client (e.g. one owned by an
     * {@link Equinix} session) so several domains share a single OAuth token and connection
     * pool. The shared core is <em>not</em> closed by this client's {@link #close()} — its
     * owner (the session) closes it. Package-private: used by {@link Equinix}.
     *
     * @param sharedCore the core client to share
     */
    EquinixClient(api.equinix.javasdk.core.client.EquinixClient sharedCore) {
        this.equinixClient = sharedCore;
        this.coreConfig = new CoreConfigImpl(sharedCore);
        this.ownsCore = false;
        this.autoLoadMetros = false;
        sharedCore.setAuthenticator(() -> core().authenticate());
    }

    /**
     * Explicitly performs OAuth2 authentication using the configured client credentials.
     * Stores the resulting access token for subsequent API calls. This is optional —
     * authentication occurs automatically on the first API call if not called explicitly.
     *
     * @throws EquinixClientException if authentication fails due to invalid credentials or network errors
     */
    public void authenticate() throws EquinixClientException {
        equinixClient.authenticate();
    }

    /**
     * Returns the internal core client for authentication and platform-level operations.
     * This is used internally by domain clients and is not typically called directly.
     *
     * @return the {@link Core} client for core platform operations
     */
    protected Core core() {
        if (this.core == null) {
            this.core = new CoreImpl(this.coreConfig.getCoreClient(), this);
        }
        return this.core;
    }

    @Override
    public void close() throws IOException {
        if (ownsCore) {
            equinixClient.close();
        }
    }
}
