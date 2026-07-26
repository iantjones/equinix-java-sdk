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

package com.eqixiac.equinix.design.optimizer;

import com.eqixiac.equinix.core.enums.MetroCode;
import com.eqixiac.equinix.core.model.MetroId;
import com.eqixiac.equinix.design.optimizer.enums.ScoreCategory;
import com.eqixiac.equinix.design.optimizer.model.*;
import org.junit.jupiter.api.*;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Metro Optimizer models: OptimizationResult, MetroRecommendation,
 * MetroScore, and convenience accessors.
 */
@DisplayName("Metro Optimizer Models")
class MetroOptimizerModelTest {

    @Nested
    @DisplayName("MetroScore")
    class MetroScoreTests {

        @Test
        @DisplayName("Composite score should be accessible")
        void compositeScore() {
            MetroScore score = new MetroScore(87.5, Collections.emptyList());
            assertEquals(87.5, score.getComposite());
        }

        @Test
        @DisplayName("Score components should be accessible")
        void components() {
            List<ScoreComponent> comps = Arrays.asList(
                    ScoreComponent.builder()
                            .category(ScoreCategory.LATENCY)
                            .score(90.0)
                            .weight(0.3)
                            .explanation("Low latency to all sites")
                            .build(),
                    ScoreComponent.builder()
                            .category(ScoreCategory.PROVIDER_COVERAGE)
                            .score(85.0)
                            .weight(0.25)
                            .explanation("All providers available")
                            .build(),
                    ScoreComponent.builder()
                            .category(ScoreCategory.COST)
                            .score(80.0)
                            .weight(0.2)
                            .explanation("Competitive pricing")
                            .build(),
                    ScoreComponent.builder()
                            .category(ScoreCategory.REDUNDANCY)
                            .score(95.0)
                            .weight(0.15)
                            .explanation("Multi-metro redundancy")
                            .build(),
                    ScoreComponent.builder()
                            .category(ScoreCategory.COMPLIANCE)
                            .score(100.0)
                            .weight(0.1)
                            .explanation("No restrictions")
                            .build()
            );
            MetroScore score = new MetroScore(89.25, comps);

            assertEquals(5, score.getComponents().size());
            assertEquals(90.0, score.latencyScore());
            assertEquals(85.0, score.providerScore());
            assertEquals(80.0, score.costScore());
            assertEquals(95.0, score.redundancyScore());
            assertEquals(100.0, score.complianceScore());
        }

        @Test
        @DisplayName("scoreFor missing category should return 0")
        void missingCategory() {
            MetroScore score = new MetroScore(50.0, Collections.emptyList());
            assertEquals(0.0, score.latencyScore());
        }
    }

    @Nested
    @DisplayName("MetroRecommendation")
    class MetroRecommendationTests {

        @Test
        @DisplayName("Should carry all required fields")
        void fullBuild() {
            MetroScore score = new MetroScore(90.0, Collections.emptyList());
            MetroRecommendation rec = MetroRecommendation.builder()
                    .rank(1)
                    .metroId(MetroId.of(MetroCode.DC))
                    .metroName("Ashburn")
                    .score(score)
                    .reasons(Arrays.asList("Lowest latency", "All providers available"))
                    .build();

            assertEquals(1, rec.getRank());
            assertEquals(MetroId.of(MetroCode.DC), rec.getMetroId());
            assertEquals("Ashburn", rec.getMetroName());
            assertEquals(90.0, rec.getScore().getComposite());
            assertEquals(2, rec.getReasons().size());
        }
    }

    @Nested
    @DisplayName("OptimizationResult")
    class OptimizationResultTests {

        private OptimizationResult result;

        @BeforeEach
        void build() {
            MetroScore score1 = new MetroScore(95.0, Collections.emptyList());
            MetroScore score2 = new MetroScore(82.0, Collections.emptyList());
            MetroScore score3 = new MetroScore(75.0, Collections.emptyList());

            result = OptimizationResult.builder()
                    .recommendations(Arrays.asList(
                            MetroRecommendation.builder()
                                    .rank(1).metroId(MetroId.of(MetroCode.DC)).metroName("Ashburn")
                                    .score(score1).reasons(Collections.singletonList("Best overall"))
                                    .build(),
                            MetroRecommendation.builder()
                                    .rank(2).metroId(MetroId.of(MetroCode.DA)).metroName("Dallas")
                                    .score(score2).reasons(Collections.singletonList("Good secondary"))
                                    .build(),
                            MetroRecommendation.builder()
                                    .rank(3).metroId(MetroId.of(MetroCode.SV)).metroName("Silicon Valley")
                                    .score(score3).reasons(Collections.singletonList("West coast coverage"))
                                    .build()))
                    .computedAt(Instant.parse("2026-03-15T12:00:00Z"))
                    .computeTimeMs(350)
                    .build();
        }

        @Test
        @DisplayName("primaryMetro should return top-ranked")
        void primaryMetro() {
            MetroRecommendation primary = result.primaryMetro();
            assertNotNull(primary);
            assertEquals(MetroId.of(MetroCode.DC), primary.getMetroId());
            assertEquals(1, primary.getRank());
        }

        @Test
        @DisplayName("top(N) should return first N recommendations")
        void topN() {
            List<MetroRecommendation> top2 = result.top(2);
            assertEquals(2, top2.size());
            assertEquals(MetroId.of(MetroCode.DC), top2.get(0).getMetroId());
            assertEquals(MetroId.of(MetroCode.DA), top2.get(1).getMetroId());
        }

        @Test
        @DisplayName("top(N) where N > size should return all")
        void topNExceeds() {
            List<MetroRecommendation> topAll = result.top(10);
            assertEquals(3, topAll.size());
        }

        @Test
        @DisplayName("Empty recommendations should return null primaryMetro")
        void emptyRecommendations() {
            OptimizationResult empty = OptimizationResult.builder()
                    .recommendations(Collections.emptyList())
                    .computedAt(Instant.now())
                    .computeTimeMs(0)
                    .build();
            assertNull(empty.primaryMetro());
        }
    }
}
