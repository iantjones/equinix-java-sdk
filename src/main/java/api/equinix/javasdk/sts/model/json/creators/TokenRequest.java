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

package api.equinix.javasdk.sts.model.json.creators;

import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Getter;

/**
 * An OAuth 2.0 token-exchange request body (RFC&nbsp;8693) for {@code POST /v1/token} (operationId
 * {@code generateStsToken}, spec schema {@code TokenRequest}).
 *
 * <p>Unlike the other request bodies in the SDK, this endpoint consumes
 * {@code application/x-www-form-urlencoded}, so this type exposes its fields as a name/value map
 * via {@link #toFormFields()} (using the spec's property names {@code grantType}, {@code scope},
 * {@code subjectToken}, {@code subjectTokenType}) rather than being JSON-serialized.</p>
 */
@Getter
public class TokenRequest {

    private String grantType;

    private String scope;

    private String subjectToken;

    private String subjectTokenType;

    /**
     * Sets the OAuth 2.0 grant type (required), e.g.
     * {@code urn:ietf:params:oauth:grant-type:token-exchange}.
     *
     * @param grantType the grant type
     * @return this request for chaining
     */
    public TokenRequest grantType(String grantType) {
        this.grantType = grantType;
        return this;
    }

    /**
     * Sets the requested scope for the access token.
     *
     * @param scope the requested scope
     * @return this request for chaining
     */
    public TokenRequest scope(String scope) {
        this.scope = scope;
        return this;
    }

    /**
     * Sets the ID token to exchange.
     *
     * @param subjectToken the subject token
     * @return this request for chaining
     */
    public TokenRequest subjectToken(String subjectToken) {
        this.subjectToken = subjectToken;
        return this;
    }

    /**
     * Sets the subject token type, e.g. {@code urn:ietf:params:oauth:token-type:id_token}.
     *
     * @param subjectTokenType the subject token type
     * @return this request for chaining
     */
    public TokenRequest subjectTokenType(String subjectTokenType) {
        this.subjectTokenType = subjectTokenType;
        return this;
    }

    /**
     * Renders this request as an ordered map of form field name to value, omitting unset (null)
     * fields. Used to build the {@code application/x-www-form-urlencoded} request body.
     *
     * @return the form fields for this token request
     */
    public Map<String, String> toFormFields() {
        Map<String, String> fields = new LinkedHashMap<>();
        if (grantType != null) {
            fields.put("grantType", grantType);
        }
        if (scope != null) {
            fields.put("scope", scope);
        }
        if (subjectToken != null) {
            fields.put("subjectToken", subjectToken);
        }
        if (subjectTokenType != null) {
            fields.put("subjectTokenType", subjectTokenType);
        }
        return fields;
    }
}
