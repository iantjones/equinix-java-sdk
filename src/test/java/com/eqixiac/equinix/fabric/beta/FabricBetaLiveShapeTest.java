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
package com.eqixiac.equinix.fabric.beta;

import com.eqixiac.equinix.core.TestFixtures;
import com.eqixiac.equinix.core.internal.Constants;
import com.eqixiac.equinix.fabric.beta.model.Asn;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Live shapes that differ from the Fabric v4 4.34.0 spec, captured with read-only searches on 2026-11-12.
 * Fixture values are synthetic; the key structure is the live one.
 */
class FabricBetaLiveShapeTest {

    @Test
    void asnSearchItemAssetsCarryState() throws Exception {
        // Live POST /fabric/v4/asns/search returns assets[].state, which the spec's AsnAsset does not declare.
        JsonNode item = Constants.mapper().readTree(TestFixtures.load("/json/fabric/beta-live/asns_search_live_shape.json"))
                .path("data").get(0);
        Asn asn = Constants.mapper().treeToValue(item, Asn.class);
        assertEquals("ATTACHED", asn.getAssets().get(0).getState());
        assertEquals("PRIVATE_ASN", asn.getType());
        BetaTestSupport.assertCovers(item, asn, "asns live shape");
    }
}
