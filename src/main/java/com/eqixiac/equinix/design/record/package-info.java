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


/**
 * The Design Decision Record: the one document an architect hands to a review board for a
 * {@link com.eqixiac.equinix.design.spec.DesignSpec}. It chains the design engines (Metro Optimizer, resilience
 * analysis, growth and value models, deployment plan) and records what was decided, why, on what evidence, and
 * what would change the decision.
 *
 * <pre>{@code
 * DesignDecisionRecord adr = DesignDecisionRecorder.forSpec(fabric, spec).record();
 * Files.writeString(Path.of("adr-0007.md"), adr.toMarkdown());
 * Files.writeString(Path.of("adr-0007.json"), adr.toJson());
 *
 * // next quarter
 * DesignDecisionRecord again = DesignDecisionRecorder.forSpec(fabric, spec).record();
 * System.out.println(DesignDecisionRecord.compare(DesignDecisionRecord.fromJson(saved), again).toMarkdown());
 * }</pre>
 *
 * <p>Every section is optional and says when it was skipped or failed and why; one section failing never loses
 * the record. Results already computed can be supplied ({@link DesignDecisionRecorder#optimization},
 * {@link DesignDecisionRecorder#resilience}, ...) and are rejected when they were computed for another spec.</p>
 */
package com.eqixiac.equinix.design.record;
