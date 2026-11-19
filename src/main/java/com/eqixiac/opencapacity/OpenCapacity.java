/*
 * Copyright 2026 Ian Jones. All Rights Reserved.
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

package com.eqixiac.opencapacity;

import com.eqixiac.opencapacity.model.Capabilities;
import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;

/**
 * Entry point of the OpenCapacity client for one provider.
 *
 * <p><strong>Proposal.</strong> No provider implements this specification. It is not an Equinix
 * API, and it can change before version 1.0 is final.
 *
 * <p>There is no default endpoint. {@link Builder#build()} fails until you set one.
 */
public final class OpenCapacity {

    private final Transport transport;
    private final Locations locations;
    private final Queries queries;
    private final Signals signals;
    private final Subscriptions subscriptions;

    private OpenCapacity(Transport transport) {
        this.transport = transport;
        this.locations = new Locations(transport);
        this.queries = new Queries(transport);
        this.signals = new Signals(transport);
        this.subscriptions = new Subscriptions(transport);
    }

    /**
     * Starts a client.
     *
     * @return a builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Reads what the provider supports. The request needs no token.
     *
     * @return the provider's capabilities
     * @throws OpenCapacityException if the request fails
     */
    public Capabilities capabilities() {
        Transport.Reply reply = transport.call("GetCapabilities", Map.of(), null, null, Map.of());
        return transport.read(reply, Transport.type(Capabilities.class));
    }

    /**
     * Returns the client for the provider's location list.
     *
     * @return the locations client
     */
    public Locations locations() {
        return locations;
    }

    /**
     * Returns the client for one-time capacity questions.
     *
     * @return the queries client
     */
    public Queries queries() {
        return queries;
    }

    /**
     * Returns the client for standing signals of need.
     *
     * @return the signals client
     */
    public Signals signals() {
        return signals;
    }

    /**
     * Returns the client for subscriptions and their events.
     *
     * @return the subscriptions client
     */
    public Subscriptions subscriptions() {
        return subscriptions;
    }

    /** Builds an {@link OpenCapacity} client. */
    public static final class Builder {
        private URI endpoint;
        private URI tokenUrl;
        private String clientId;
        private String clientSecret;
        private TokenSource tokenSource;
        private Duration timeout = Duration.ofSeconds(30);

        private Builder() {
        }

        /**
         * Sets the provider's host, for example {@code https://provider.example}. The paths come
         * from the endpoint catalog {@code json/apiParams_OpenCapacity.json}, as they do for the
         * Equinix clients. A URL that already ends in the catalog's base path
         * ({@code /capacity/v1}) is accepted as well.
         *
         * @param url the provider's host URL
         * @return this builder
         */
        public Builder endpoint(String url) {
            String trimmed = Objects.requireNonNull(url, "url").replaceAll("/+$", "");
            if (trimmed.endsWith(Endpoints.basePath())) {
                trimmed = trimmed.substring(0, trimmed.length() - Endpoints.basePath().length());
            }
            this.endpoint = URI.create(trimmed);
            return this;
        }

        /**
         * Sets the client ID and secret that this provider issued. The client exchanges them for a
         * token at the provider's token endpoint.
         *
         * @param id the client ID
         * @param secret the client secret
         * @return this builder
         */
        public Builder credentials(String id, String secret) {
            this.clientId = Objects.requireNonNull(id, "id");
            this.clientSecret = Objects.requireNonNull(secret, "secret");
            return this;
        }

        /**
         * Sets the token endpoint. Without it the client reads the endpoint from the provider's
         * capabilities document.
         *
         * @param url the OAuth 2.0 token endpoint
         * @return this builder
         */
        public Builder tokenUrl(String url) {
            this.tokenUrl = URI.create(Objects.requireNonNull(url, "url"));
            return this;
        }

        /**
         * Sets a source of tokens that you manage yourself, in place of {@link #credentials}.
         *
         * @param source the token source
         * @return this builder
         */
        public Builder tokenSource(TokenSource source) {
            this.tokenSource = Objects.requireNonNull(source, "source");
            return this;
        }

        /**
         * Sets the time limit for connecting and for each request. The default is 30 seconds.
         *
         * @param limit the time limit
         * @return this builder
         */
        public Builder timeout(Duration limit) {
            this.timeout = Objects.requireNonNull(limit, "limit");
            return this;
        }

        /**
         * Creates the client. No request is sent.
         *
         * @return the client
         * @throws IllegalStateException if no endpoint is set, or the endpoint is not HTTPS and not
         *     on the local machine
         */
        public OpenCapacity build() {
            if (endpoint == null) {
                throw new IllegalStateException(
                        "No provider endpoint is configured. Call endpoint() with the provider's "
                                + "OpenCapacity base URL. The client has no default host.");
            }
            if (!Transport.isSafe(endpoint)) {
                throw new IllegalStateException(
                        "The endpoint must be HTTPS, or HTTP on localhost for testing: " + endpoint);
            }
            Transport transport = new Transport(endpoint, timeout);
            if (tokenSource != null) {
                transport.tokens(tokenSource);
            } else if (clientId != null) {
                transport.tokens(new Transport.ClientCredentials(transport, clientId, clientSecret, tokenUrl));
            }
            return new OpenCapacity(transport);
        }
    }
}
