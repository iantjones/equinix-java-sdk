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

package api.equinix.javasdk.sts.model;

import java.util.List;

/**
 * An OpenID Connect identity provider registered with the Equinix Security Token Service, as
 * returned by the STS OIDC provider operations.
 *
 * <p>This is a read-only response view (spec schema {@code OIDCProvider}).</p>
 */
public interface OidcProvider {

    /**
     * @return the identity provider identifier
     */
    String getIdpId();

    /**
     * @return the human-readable name of the identity provider
     */
    String getName();

    /**
     * @return the issuer URI of the identity provider
     */
    String getIssuerUri();

    /**
     * @return the issuer location of the identity provider
     */
    String getIssuerLocation();

    /**
     * @return the client ids trusted for tokens issued by this provider
     */
    List<String> getTrustedClientIds();

    /**
     * @return the claim used to derive group membership
     */
    String getGroupMembershipClaim();

    /**
     * @return the status of the identity provider
     */
    String getStatus();

    /**
     * @return the JSON Web Key Set registered for the provider
     */
    Jwks getJwks();

    /**
     * @return the timestamp at which the JWKS was last retrieved
     */
    String getJwksRetrievedAt();

    /**
     * @return the opaque revision of the identity provider
     */
    String getRev();

    /**
     * @return the principal that created the identity provider
     */
    String getCreatedBy();

    /**
     * @return the creation timestamp
     */
    String getCreatedAt();

    /**
     * @return the principal that last updated the identity provider
     */
    String getUpdatedBy();

    /**
     * @return the last-updated timestamp
     */
    String getUpdatedAt();
}
