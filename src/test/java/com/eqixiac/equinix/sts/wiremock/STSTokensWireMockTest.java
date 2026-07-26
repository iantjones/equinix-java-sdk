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

package com.eqixiac.equinix.sts.wiremock;

import com.eqixiac.equinix.STS;
import com.eqixiac.equinix.core.WireMockTestBase;
import com.eqixiac.equinix.core.exception.EquinixNotFoundException;
import com.eqixiac.equinix.core.exception.EquinixServerException;
import com.eqixiac.equinix.core.exception.EquinixServiceException;
import com.eqixiac.equinix.sts.enums.TokenType;
import com.eqixiac.equinix.sts.model.StsToken;
import com.eqixiac.equinix.sts.model.json.GrantedAccessPolicyPage;
import com.eqixiac.equinix.sts.model.json.creators.ListPoliciesGrantedRequest;
import com.eqixiac.equinix.sts.model.json.creators.TokenRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.eqixiac.equinix.core.ResponseStubs.stubErrorInline;
import static com.eqixiac.equinix.core.ResponseStubs.stubPaginatedPost;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * WireMock-based contract tests for the STS pre-auth token operations exposed by
 * {@code sts.tokens()} ({@link com.eqixiac.equinix.sts.client.STSTokens}).
 *
 * <p>The keystone is the RFC&nbsp;8693 token exchange — {@code POST /v1/token}, operationId
 * {@code generateStsToken} — which (per stsv1) consumes
 * {@code application/x-www-form-urlencoded}. These tests assert the form body carries the
 * {@code grantType}/{@code subjectToken}/{@code subjectTokenType}/{@code scope} fields and that the
 * {@code TokenResponse} parses into an {@link StsToken}. The companion JSON operation —
 * {@code POST /v1/accessPoliciesGranted} ({@code listAccessPoliciesGranted}) — is covered for a
 * populated page, an empty page and pagination.</p>
 */
class STSTokensWireMockTest extends WireMockTestBase {

    private static final String TOKEN_PATH = "/v1/token";
    private static final String POLICIES_PATH = "/v1/accessPoliciesGranted";

    private static final String GRANT_TYPE = "urn:ietf:params:oauth:grant-type:token-exchange";
    private static final String SUBJECT_TOKEN_TYPE = "urn:ietf:params:oauth:token-type:id_token";
    // A subject token with no characters that URL-encoding would rewrite, so the form body
    // can be matched with a plain substring.
    private static final String SUBJECT_TOKEN = "header.payload.signature";

    static STS sts;

    @BeforeAll
    static void setUp() {
        sts = new STS(testCredentials());
        redirectToWireMock(sts);
        sts.authenticate();
    }

    @AfterAll
    static void tearDown() throws Exception {
        if (sts != null) sts.close();
    }

    @BeforeEach
    void resetBeforeEach() {
        resetStubs();
    }

    @Nested
    @DisplayName("generate() — RFC 8693 token exchange")
    class Generate {

        @Test
        @DisplayName("POSTs a form-encoded exchange request and parses the TokenResponse")
        void exchangesIdTokenForAccessToken() {
            wireMock.stubFor(post(urlPathEqualTo(TOKEN_PATH))
                    .willReturn(okJson(loadFixture("/json/sts/token_response.json"))));

            StsToken token = sts.tokens().generate(new TokenRequest()
                    .grantType(GRANT_TYPE)
                    .subjectToken(SUBJECT_TOKEN)
                    .subjectTokenType(SUBJECT_TOKEN_TYPE)
                    .scope("accesspolicy:abc-123:idp:example-policy"));

            assertNotNull(token);
            assertEquals("eyJhbGciOiJFUzI1NiJ9.eyJzdWIiOiJzdHMtdGVzdCJ9.signature", token.getAccessToken());
            assertEquals("urn:ietf:params:oauth:token-type:access_token", token.getIssuedTokenType());
            assertEquals(TokenType.BEARER, token.getTokenType());
            assertEquals(3600, token.getExpiresIn());

            // Body is application/x-www-form-urlencoded — assert each RFC 8693 field is present
            // (values are URL-encoded, so match the unambiguous field=value substrings).
            wireMock.verify(postRequestedFor(urlPathEqualTo(TOKEN_PATH))
                    .withHeader("Content-Type", containing("application/x-www-form-urlencoded"))
                    .withRequestBody(containing("grantType=urn%3Aietf%3Aparams%3Aoauth%3Agrant-type%3Atoken-exchange"))
                    .withRequestBody(containing("subjectToken=header.payload.signature"))
                    .withRequestBody(containing("subjectTokenType=urn%3Aietf%3Aparams%3Aoauth%3Atoken-type%3Aid_token"))
                    .withRequestBody(containing("scope=accesspolicy")));
        }

        @Test
        @DisplayName("omits unset fields from the form body")
        void omitsUnsetFields() {
            wireMock.stubFor(post(urlPathEqualTo(TOKEN_PATH))
                    .willReturn(okJson(loadFixture("/json/sts/token_response.json"))));

            // No scope set — it must not appear in the encoded body.
            sts.tokens().generate(new TokenRequest()
                    .grantType(GRANT_TYPE)
                    .subjectToken(SUBJECT_TOKEN)
                    .subjectTokenType(SUBJECT_TOKEN_TYPE));

            // The three set fields are present...
            wireMock.verify(postRequestedFor(urlPathEqualTo(TOKEN_PATH))
                    .withRequestBody(containing("grantType="))
                    .withRequestBody(containing("subjectToken="))
                    .withRequestBody(containing("subjectTokenType=")));

            // ...and the unset scope field is not emitted at all.
            wireMock.verify(0, postRequestedFor(urlPathEqualTo(TOKEN_PATH))
                    .withRequestBody(containing("scope=")));
        }

        @Test
        @DisplayName("400 (invalid_request) throws EquinixServiceException")
        void invalidRequestMapsToServiceException() {
            stubErrorInline(wireMock, TOKEN_PATH, 400,
                    "{\"error\":\"invalid_request\",\"errorDescription\":\"subjectToken is required\"}");

            EquinixServiceException ex = assertThrows(EquinixServiceException.class,
                    () -> sts.tokens().generate(new TokenRequest().grantType(GRANT_TYPE)));

            assertEquals(400, ex.getStatusCode());
            wireMock.verify(postRequestedFor(urlPathEqualTo(TOKEN_PATH)));
        }

        @Test
        @DisplayName("500 throws EquinixServerException")
        void serverErrorMapsToServerException() {
            stubErrorInline(wireMock, TOKEN_PATH, 500,
                    "{\"error\":{\"errorCode\":\"internal-error\"}}");

            assertThrows(EquinixServerException.class,
                    () -> sts.tokens().generate(new TokenRequest()
                            .grantType(GRANT_TYPE)
                            .subjectToken(SUBJECT_TOKEN)
                            .subjectTokenType(SUBJECT_TOKEN_TYPE)));

            wireMock.verify(postRequestedFor(urlPathEqualTo(TOKEN_PATH)));
        }
    }

    @Nested
    @DisplayName("listAccessPoliciesGranted()")
    class ListAccessPoliciesGranted {

        @Test
        @DisplayName("POSTs the JSON request and parses the granted-policy page")
        void listsGrantedPolicies() {
            stubPaginatedPost(wireMock, POLICIES_PATH, "/json/sts/access_policies_granted.json");

            GrantedAccessPolicyPage page = sts.tokens().listAccessPoliciesGranted(
                    new ListPoliciesGrantedRequest()
                            .projectId("proj-1")
                            .subjectToken(SUBJECT_TOKEN)
                            .subjectTokenType(SUBJECT_TOKEN_TYPE)
                            .pageSize(50));

            assertNotNull(page);
            assertEquals(2, page.getList().size());
            assertEquals("abc-123", page.getList().get(0).getAccessPolicyId());
            assertEquals("def-456", page.getList().get(1).getAccessPolicyId());
            assertEquals("eyJwYWdlIjoyfQ==", page.getNextPageToken());

            wireMock.verify(postRequestedFor(urlPathEqualTo(POLICIES_PATH))
                    .withRequestBody(matchingJsonPath("$.projectId", equalTo("proj-1")))
                    .withRequestBody(matchingJsonPath("$.subjectToken", equalTo(SUBJECT_TOKEN)))
                    .withRequestBody(matchingJsonPath("$.subjectTokenType", equalTo(SUBJECT_TOKEN_TYPE)))
                    .withRequestBody(matchingJsonPath("$.pageSize", equalTo("50"))));
        }

        @Test
        @DisplayName("empty page parses to an empty list with no next-page token")
        void emptyPage() {
            stubPaginatedPost(wireMock, POLICIES_PATH, "/json/sts/access_policies_granted_empty.json");

            GrantedAccessPolicyPage page = sts.tokens().listAccessPoliciesGranted(
                    new ListPoliciesGrantedRequest()
                            .projectId("proj-1")
                            .subjectToken(SUBJECT_TOKEN)
                            .subjectTokenType(SUBJECT_TOKEN_TYPE));

            assertNotNull(page);
            assertTrue(page.getList().isEmpty());

            wireMock.verify(postRequestedFor(urlPathEqualTo(POLICIES_PATH)));
        }

        @Test
        @DisplayName("forwards the opaque pageToken for the next page")
        void forwardsPageToken() {
            stubPaginatedPost(wireMock, POLICIES_PATH, "/json/sts/access_policies_granted.json");

            sts.tokens().listAccessPoliciesGranted(new ListPoliciesGrantedRequest()
                    .projectId("proj-1")
                    .subjectToken(SUBJECT_TOKEN)
                    .subjectTokenType(SUBJECT_TOKEN_TYPE)
                    .pageToken("eyJwYWdlIjoyfQ=="));

            wireMock.verify(postRequestedFor(urlPathEqualTo(POLICIES_PATH))
                    .withRequestBody(matchingJsonPath("$.pageToken", equalTo("eyJwYWdlIjoyfQ=="))));
        }

        @Test
        @DisplayName("404 throws EquinixNotFoundException")
        void notFoundMapsToNotFoundException() {
            stubErrorInline(wireMock, POLICIES_PATH, 404,
                    "{\"error\":{\"errorCode\":\"not-found\"}}");

            assertThrows(EquinixNotFoundException.class,
                    () -> sts.tokens().listAccessPoliciesGranted(new ListPoliciesGrantedRequest()
                            .projectId("missing")
                            .subjectToken(SUBJECT_TOKEN)
                            .subjectTokenType(SUBJECT_TOKEN_TYPE)));

            wireMock.verify(postRequestedFor(urlPathEqualTo(POLICIES_PATH)));
        }
    }
}
