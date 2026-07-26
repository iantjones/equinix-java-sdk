package com.eqixiac.equinix.core;

import com.github.tomakehurst.wiremock.WireMockServer;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

/**
 * Pre-built WireMock stubs for common Equinix API response patterns.
 */
public final class ResponseStubs {

    private ResponseStubs() {}

    /**
     * Stubs a GET endpoint to return a single-object JSON response.
     */
    public static void stubSingleton(WireMockServer wm, String urlPattern, String fixtureFile) {
        wm.stubFor(get(urlPathMatching(urlPattern))
                .willReturn(okJson(TestFixtures.load(fixtureFile))));
    }

    /**
     * Stubs a GET endpoint to return a paginated list JSON response.
     */
    public static void stubPaginatedGet(WireMockServer wm, String urlPattern, String fixtureFile) {
        wm.stubFor(get(urlPathMatching(urlPattern))
                .willReturn(okJson(TestFixtures.load(fixtureFile))));
    }

    /**
     * Stubs a POST search endpoint to return a paginated list JSON response.
     */
    public static void stubPaginatedPost(WireMockServer wm, String urlPattern, String fixtureFile) {
        wm.stubFor(post(urlPathMatching(urlPattern))
                .willReturn(okJson(TestFixtures.load(fixtureFile))));
    }

    /**
     * Stubs a POST endpoint to return a created object (201).
     */
    public static void stubCreate(WireMockServer wm, String urlPattern, String fixtureFile) {
        wm.stubFor(post(urlPathMatching(urlPattern))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody(TestFixtures.load(fixtureFile))));
    }

    /**
     * Stubs a DELETE endpoint to return 200 with a response body.
     */
    public static void stubDelete(WireMockServer wm, String urlPattern, String fixtureFile) {
        wm.stubFor(delete(urlPathMatching(urlPattern))
                .willReturn(okJson(TestFixtures.load(fixtureFile))));
    }

    /**
     * Stubs a DELETE endpoint to return 204 No Content.
     */
    public static void stubDeleteNoContent(WireMockServer wm, String urlPattern) {
        wm.stubFor(delete(urlPathMatching(urlPattern))
                .willReturn(noContent()));
    }

    /**
     * Stubs any method on a URL to return an error with the given status code and fixture body.
     */
    public static void stubError(WireMockServer wm, String urlPattern, int statusCode, String fixtureFile) {
        wm.stubFor(any(urlPathMatching(urlPattern))
                .willReturn(aResponse()
                        .withStatus(statusCode)
                        .withHeader("Content-Type", "application/json")
                        .withBody(TestFixtures.load(fixtureFile))));
    }

    /**
     * Stubs any method on a URL to return an error with the given status code and inline body.
     */
    public static void stubErrorInline(WireMockServer wm, String urlPattern, int statusCode, String body) {
        wm.stubFor(any(urlPathMatching(urlPattern))
                .willReturn(aResponse()
                        .withStatus(statusCode)
                        .withHeader("Content-Type", "application/json")
                        .withBody(body)));
    }

    /**
     * Stubs a GET endpoint to return an empty paginated response.
     */
    public static void stubEmptyList(WireMockServer wm, String urlPattern) {
        wm.stubFor(get(urlPathMatching(urlPattern))
                .willReturn(okJson(TestFixtures.load("/json/core/empty_paginated_response.json"))));
    }
}
