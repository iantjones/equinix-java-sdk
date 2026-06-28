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
 * The OpenID Connect discovery document for the Equinix Security Token Service, as returned by the
 * STS OpenID configuration operation.
 *
 * <p>This is a read-only response view (spec schema {@code OpenIdConfiguration}).</p>
 */
public interface OpenIdConfiguration {

    /**
     * @return the issuer identifier
     */
    String getIssuer();

    /**
     * @return the URI of the JSON Web Key Set
     */
    String getJwksUri();

    /**
     * @return the token endpoint URI
     */
    String getTokenEndpoint();

    /**
     * @return the claims supported by the provider
     */
    List<String> getClaimsSupported();

    /**
     * @return the response types supported by the provider
     */
    List<String> getResponseTypesSupported();

    /**
     * @return the subject types supported by the provider
     */
    List<String> getSubjectTypesSupported();

    /**
     * @return the ID token signing algorithms supported by the provider
     */
    List<String> getIdTokenSigningAlgValuesSupported();
}
