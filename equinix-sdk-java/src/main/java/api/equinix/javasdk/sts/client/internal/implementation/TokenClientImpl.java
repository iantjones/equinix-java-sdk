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

package api.equinix.javasdk.sts.client.internal.implementation;

import api.equinix.javasdk.core.client.ClientBase;
import api.equinix.javasdk.core.enums.RequestType;
import api.equinix.javasdk.core.http.Utils;
import api.equinix.javasdk.core.http.request.EquinixRequest;
import api.equinix.javasdk.sts.client.implementation.STSConfigImpl;
import api.equinix.javasdk.sts.client.internal.TokenClient;
import api.equinix.javasdk.sts.model.StsToken;
import api.equinix.javasdk.sts.model.json.GrantedAccessPolicyPage;
import api.equinix.javasdk.sts.model.json.StsTokenJson;
import api.equinix.javasdk.sts.model.json.creators.ListPoliciesGrantedRequest;
import api.equinix.javasdk.sts.model.json.creators.TokenRequest;
import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicNameValuePair;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Internal client implementation for the STS pre-auth token operations.
 *
 * <p>{@code generateStsToken} is unusual in that it consumes
 * {@code application/x-www-form-urlencoded} rather than JSON; this implementation therefore builds
 * the form-encoded entity by hand (the core {@code serializeJson} helper always emits JSON) and
 * attaches it directly to the request, leaving the rest of the dispatch — auth headers, retries,
 * response handling — to the shared infrastructure. The {@code TokenResponse} and
 * {@code ListAccessPoliciesGrantedOutput} responses are read-only, so the deserialized JSON models
 * are returned directly.</p>
 */
public class TokenClientImpl extends ClientBase implements TokenClient {

    public TokenClientImpl(STSConfigImpl configClient) {
        super(configClient, "STS", "Tokens");
    }

    @Override
    public StsToken generateStsToken(TokenRequest request) {
        EquinixRequest<StsTokenJson> equinixRequest =
                buildRequest("GenerateStsToken", RequestType.SINGLE, StsTokenJson.class);
        equinixRequest.setContentType(ContentType.APPLICATION_FORM_URLENCODED.getMimeType());
        equinixRequest.setHttpEntity(formEntity(request.toFormFields()));
        return Utils.handleSingletonResponse(invoke(equinixRequest), equinixRequest);
    }

    @Override
    public GrantedAccessPolicyPage listAccessPoliciesGranted(ListPoliciesGrantedRequest request) {
        return postForType("ListAccessPoliciesGranted", null, request,
                new TypeReference<GrantedAccessPolicyPage>() {
                });
    }

    /**
     * Builds an {@code application/x-www-form-urlencoded} entity from the supplied form fields.
     *
     * @param fields the form field name/value pairs
     * @return the encoded request entity
     */
    private StringEntity formEntity(Map<String, String> fields) {
        List<NameValuePair> pairs = new ArrayList<>();
        for (Map.Entry<String, String> field : fields.entrySet()) {
            pairs.add(new BasicNameValuePair(field.getKey(), field.getValue()));
        }
        String encoded = URLEncodedUtils.format(pairs, StandardCharsets.UTF_8);
        return new StringEntity(encoded, ContentType.APPLICATION_FORM_URLENCODED.withCharset(StandardCharsets.UTF_8));
    }
}
