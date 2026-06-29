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

import api.equinix.javasdk.core.WireMockTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static api.equinix.javasdk.core.ResponseStubs.*;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * WireMock test for the behavioural guarantees of an {@link Equinix} session: a single OAuth
 * token is shared (fetched once) across calls over the shared core, and a session-obtained
 * domain's {@code close()} does not tear down the shared connection pool.
 */
class EquinixSessionWireMockTest extends WireMockTestBase {

    @BeforeEach
    void reset() {
        resetStubs();
    }

    @Test
    @DisplayName("one token across the session; a domain close() leaves the shared pool open")
    void sharedTokenAndCloseSafety() throws Exception {
        stubPaginatedPost(wireMock, "/fabric/v4/prices/search", "/json/fabric/paginated_prices.json");

        Equinix eq = new Equinix(testCredentials());
        // Point the SHARED core at WireMock (also disables retries). fabric()/networkEdge()/...
        // all share this same core, so this redirects the whole session.
        redirectToWireMock(eq.fabric());

        // Authenticate the session once — fetches a single OAuth token onto the shared core.
        eq.authenticate();

        // A call over the shared core succeeds.
        assertNotNull(eq.fabric().prices().list(null));

        // Closing a session-obtained domain must NOT close the shared core (ownsCore == false)...
        eq.fabric().close();
        // ...so a subsequent call still succeeds over the same pool + cached token.
        assertNotNull(eq.fabric().prices().list(null));

        // Token endpoint hit exactly once for the whole session — a single shared token.
        wireMock.verify(1, postRequestedFor(urlPathEqualTo("/oauth2/v1/token")));
        // Both post-auth calls executed over the still-open shared pool (close-safety).
        wireMock.verify(2, postRequestedFor(urlPathEqualTo("/fabric/v4/prices/search")));

        eq.close();
    }
}
