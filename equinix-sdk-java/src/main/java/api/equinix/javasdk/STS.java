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
import api.equinix.javasdk.core.model.Service;
import api.equinix.javasdk.sts.client.STSConfig;
import api.equinix.javasdk.sts.client.STSDiscovery;
import api.equinix.javasdk.sts.client.STSOidcProviders;
import api.equinix.javasdk.sts.client.STSTokens;
import api.equinix.javasdk.sts.client.implementation.STSConfigImpl;
import api.equinix.javasdk.sts.client.implementation.STSDiscoveryImpl;
import api.equinix.javasdk.sts.client.implementation.STSOidcProvidersImpl;
import api.equinix.javasdk.sts.client.implementation.STSTokensImpl;

/**
 * The primary entry point for accessing the Equinix Security Token Service (STS) API (STS v1).
 *
 * <p>The STS exchanges OpenID Connect (OIDC) ID tokens — issued by trusted OIDC providers — for
 * Equinix access tokens (RFC&nbsp;8693 token exchange), and manages the trust relationships and
 * discovery metadata that underpin that exchange:</p>
 *
 * <ul>
 *   <li>{@link #tokens()} — the OAuth 2.0 token exchange ({@code POST /v1/token}) and the
 *       granted-access-policy listing ({@code POST /v1/accessPoliciesGranted}).</li>
 *   <li>{@link #oidcProviders()} — registration and lifecycle (create / page / patch / delete /
 *       suspend / resume) of trusted OIDC providers within a root project.</li>
 *   <li>{@link #discovery()} — the unauthenticated discovery endpoints: the JSON Web Key Set
 *       ({@code GET /jwks}) and the OpenID Connect discovery document
 *       ({@code GET /.well-known/openid-configuration}).</li>
 * </ul>
 *
 * <p>Each accessor uses lazy initialization — the internal client is created on first access and
 * reused for subsequent calls.</p>
 *
 * <h3>Service host</h3>
 * <p>The STS v1 OpenAPI specification declares its own service host
 * ({@code https://sts.eqix.equinix.com}), with the discovery endpoints ({@code /jwks} and
 * {@code /.well-known/openid-configuration}) served at the host root and token exchange under
 * {@code /v1/token}. This SDK, however, routes every domain — STS included — through the unified
 * Equinix API gateway ({@code https://api.equinix.com}) configured on the shared
 * {@link EquinixClient}, which fronts the per-service hosts and exposes the same paths (the
 * {@code bearerAuth} security scheme is likewise defined against the gateway). The relative request
 * URIs in {@code apiParams_STS.json} are therefore resolved against that single gateway host rather
 * than the spec's per-service {@code sts.eqix.equinix.com} server. This is the intended SDK-wide
 * design; no per-domain host override is provided.</p>
 *
 * <h3>Quick Start</h3>
 * <pre>{@code
 * BasicEquinixCredentials credentials = new BasicEquinixCredentials("clientId", "clientSecret");
 * STS sts = new STS(credentials);
 *
 * StsToken token = sts.tokens().generate(new TokenRequest()
 *     .grantType("urn:ietf:params:oauth:grant-type:token-exchange")
 *     .subjectToken(idToken)
 *     .subjectTokenType("urn:ietf:params:oauth:token-type:id_token")
 *     .scope("accesspolicy:abc-123:idp:example-policy"));
 * }</pre>
 *
 * @author ianjones
 * @see api.equinix.javasdk.core.auth.BasicEquinixCredentials
 */
public final class STS extends EquinixClient implements Service {

    private STSTokens tokens;

    private STSOidcProviders oidcProviders;

    private STSDiscovery discovery;

    final private STSConfig stsConfig;

    /**
     * Creates a new STS client using the provided credentials.
     * Authentication occurs automatically on the first API call.
     *
     * @param equinixCredentials the OAuth2 credentials for authenticating with Equinix APIs
     */
    public STS(EquinixCredentials equinixCredentials) {
        this(equinixCredentials, false);
    }

    /**
     * Creates a new STS client with optional sandbox mode.
     *
     * @param equinixCredentials the OAuth2 credentials for authenticating with Equinix APIs
     * @param isSandBoxed {@code true} to use the sandbox environment for testing; {@code false} for production
     */
    public STS(EquinixCredentials equinixCredentials, boolean isSandBoxed) {
        super(equinixCredentials, isSandBoxed);

        String paramFile = "json/apiParams_STS.json";
        equinixClient.appendApiParams(paramFile);

        this.stsConfig = new STSConfigImpl(equinixClient);
    }

    /**
     * Returns the client for the STS OAuth 2.0 token exchange ({@code POST /v1/token}) and the
     * granted-access-policy listing ({@code POST /v1/accessPoliciesGranted}).
     *
     * @return the {@link STSTokens} client
     */
    public STSTokens tokens() {
        if (this.tokens == null) {
            this.tokens = new STSTokensImpl(this.stsConfig.getTokenClient(), this);
        }
        return tokens;
    }

    /**
     * Returns the client for managing trusted OIDC providers within a root project
     * ({@code /v1/projects/{projectId}/oidcProviders}).
     *
     * @return the {@link STSOidcProviders} client
     */
    public STSOidcProviders oidcProviders() {
        if (this.oidcProviders == null) {
            this.oidcProviders = new STSOidcProvidersImpl(this.stsConfig.getOidcProviderClient(), this);
        }
        return oidcProviders;
    }

    /**
     * Returns the client for the STS unauthenticated discovery endpoints — the JSON Web Key Set
     * ({@code GET /jwks}) and the OpenID Connect discovery document
     * ({@code GET /.well-known/openid-configuration}).
     *
     * @return the {@link STSDiscovery} client
     */
    public STSDiscovery discovery() {
        if (this.discovery == null) {
            this.discovery = new STSDiscoveryImpl(this.stsConfig.getDiscoveryClient(), this);
        }
        return discovery;
    }
}
