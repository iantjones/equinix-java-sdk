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
import api.equinix.javasdk.core.exception.EquinixClientException;

import java.io.Closeable;
import java.io.IOException;

/**
 * A single Equinix session that owns <strong>one</strong> authenticated core client — a single
 * OAuth token and a single HTTP connection pool — and vends every domain client over it. Use this
 * when an application talks to more than one domain so they share the token and pool instead of
 * each {@code new Fabric(creds)} / {@code new NetworkEdge(creds)} standing up its own (which would
 * authenticate and pool independently).
 *
 * <pre>{@code
 * try (Equinix eq = new Equinix(credentials)) {
 *     Fabric fabric       = eq.fabric();        // shares the session's token + pool
 *     NetworkEdge edge    = eq.networkEdge();    // ditto
 *     Design design       = eq.design();         // value-add engines over the shared Fabric
 *     // ... one token fetched, one pool, closed once when the session closes
 * }
 * }</pre>
 *
 * <p>Domain clients are created lazily and cached, so repeated accessor calls return the same
 * instance. The session owns the shared core client; closing the session closes it (and the
 * {@link Mcp} client if one was created). The individual domain clients obtained from a session
 * do <em>not</em> close the shared core when their own {@code close()} is called.</p>
 *
 * <p>The standalone {@code new Fabric(credentials)} (etc.) constructors remain for the common
 * single-domain case; this session is purely additive.</p>
 *
 * <p><strong>MCP note:</strong> {@link #mcp()} talks to the separate Equinix MCP servers over
 * their own endpoints, so it uses its own transport (constructed from the session's credentials)
 * rather than the shared Fabric/REST core — that separation is inherent to MCP being a distinct
 * service.</p>
 */
public final class Equinix implements Closeable {

    private final EquinixCredentialsProvider credentialsProvider;
    private final EquinixConfig config;
    private final api.equinix.javasdk.core.client.EquinixClient core;

    private Fabric fabric;
    private NetworkEdge networkEdge;
    private CustomerPortal customerPortal;
    private IBXSmartView ibxSmartView;
    private InternetAccess internetAccess;
    private Projects projects;
    private IAM iam;
    private STS sts;
    private Mcp mcp;

    /**
     * Opens a session against the production environment.
     *
     * @param credentials the OAuth2 credentials shared by every client in this session
     */
    public Equinix(EquinixCredentials credentials) {
        this(credentials, false);
    }

    /**
     * Opens a session, optionally against the sandbox environment.
     *
     * @param credentials the OAuth2 credentials shared by every client in this session
     * @param isSandBoxed {@code true} to use the sandbox environment; {@code false} for production
     */
    public Equinix(EquinixCredentials credentials, boolean isSandBoxed) {
        this(new EquinixStaticCredentialsProvider(credentials), EquinixConfig.builder().sandbox(isSandBoxed).build());
    }

    /**
     * Opens a session with explicit {@link EquinixConfig} options (sandbox, retry, metro auto-loading).
     *
     * @param credentials the OAuth2 credentials shared by every client in this session
     * @param config the construction-time options
     */
    public Equinix(EquinixCredentials credentials, EquinixConfig config) {
        this(new EquinixStaticCredentialsProvider(credentials), config);
    }

    /**
     * Opens a session whose credentials are resolved through the given provider — consulted on each
     * authentication, so the whole session (every domain client over the shared core) can rotate
     * credentials at runtime.
     *
     * @param credentialsProvider supplies the OAuth2 credentials shared by every client in this session
     */
    public Equinix(EquinixCredentialsProvider credentialsProvider) {
        this(credentialsProvider, EquinixConfig.defaults());
    }

    /**
     * Opens a session over a custom credentials provider, optionally against the sandbox environment.
     *
     * @param credentialsProvider supplies the OAuth2 credentials shared by every client in this session
     * @param isSandBoxed {@code true} to use the sandbox environment; {@code false} for production
     */
    public Equinix(EquinixCredentialsProvider credentialsProvider, boolean isSandBoxed) {
        this(credentialsProvider, EquinixConfig.builder().sandbox(isSandBoxed).build());
    }

    /**
     * Opens a session over a custom credentials provider with explicit {@link EquinixConfig} options.
     * When {@link EquinixConfig#isAutoLoadMetros()} is set (the default), {@link #authenticate()}
     * eagerly loads the shared metro catalogue.
     *
     * @param credentialsProvider supplies the OAuth2 credentials shared by every client in this session
     * @param config the construction-time options
     */
    public Equinix(EquinixCredentialsProvider credentialsProvider, EquinixConfig config) {
        this.credentialsProvider = credentialsProvider;
        this.config = config;
        this.core = new api.equinix.javasdk.core.client.EquinixClient(credentialsProvider, config.isSandbox());
        if (config.getRetryPolicy() != null) {
            this.core.setRetryPolicy(config.getRetryPolicy());
        }
    }

    public Fabric fabric() {
        if (fabric == null) {
            fabric = new Fabric(core);
        }
        return fabric;
    }

    public NetworkEdge networkEdge() {
        if (networkEdge == null) {
            networkEdge = new NetworkEdge(core);
        }
        return networkEdge;
    }

    public CustomerPortal customerPortal() {
        if (customerPortal == null) {
            customerPortal = new CustomerPortal(core);
        }
        return customerPortal;
    }

    public IBXSmartView ibxSmartView() {
        if (ibxSmartView == null) {
            ibxSmartView = new IBXSmartView(core);
        }
        return ibxSmartView;
    }

    public InternetAccess internetAccess() {
        if (internetAccess == null) {
            internetAccess = new InternetAccess(core);
        }
        return internetAccess;
    }

    public Projects projects() {
        if (projects == null) {
            projects = new Projects(core);
        }
        return projects;
    }

    public IAM iam() {
        if (iam == null) {
            iam = new IAM(core);
        }
        return iam;
    }

    public STS sts() {
        if (sts == null) {
            sts = new STS(core);
        }
        return sts;
    }

    /**
     * @return the value-add design facade (Metro Optimizer, Deployment Wizard, Peering
     *         Intelligence, Savings, TCO) over this session's shared Fabric client
     */
    public Design design() {
        return Design.over(fabric());
    }

    /**
     * @return the MCP client for this session's credentials. Uses its own transport (the MCP
     *         servers are a separate service); created lazily and closed with the session.
     */
    public Mcp mcp() {
        if (mcp == null) {
            mcp = new Mcp(credentialsProvider.getCredentials());
        }
        return mcp;
    }

    /**
     * Explicitly performs OAuth2 authentication, warming the session's shared token. Optional —
     * authentication otherwise happens automatically on the first API call. When
     * {@link EquinixConfig#isAutoLoadMetros()} is enabled (the default), this also eagerly loads the
     * shared metro catalogue ({@code fabric().metroRegistry()}); the load is best-effort and does
     * not fail authentication.
     *
     * @return this session, for chaining
     * @throws EquinixClientException if authentication fails
     */
    public Equinix authenticate() throws EquinixClientException {
        fabric().authenticate();
        if (config.isAutoLoadMetros()) {
            try {
                fabric().metroRegistry();
            }
            catch (RuntimeException ignored) {
                // best-effort eager load; metroRegistry() remains available lazily
            }
        }
        return this;
    }

    @Override
    public void close() throws IOException {
        if (mcp != null) {
            mcp.close();
        }
        core.close();
    }
}
