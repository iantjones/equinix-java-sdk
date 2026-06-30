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

package api.equinix.javasdk.design.peering.model;

import api.equinix.javasdk.core.model.MetroId;
import api.equinix.javasdk.design.peering.enums.FailureScope;
import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * Identifies a correlated failure — multiple connectivity paths that share a
 * single failure domain and would fail simultaneously.
 *
 * <p>For example, if a customer's IX peering with AWS AND their Fabric Direct Connect
 * to AWS both terminate in the same metro, a metro-level failure would take out
 * both paths simultaneously. This class captures that correlation and recommends
 * mitigations.</p>
 *
 * @author ianjones
 * @see BlastRadiusReport
 * @see ResiliencyAssessment
 */
@Value
@Builder
public class CorrelatedFailure {

    FailureScope scope;

    String failureDomain;

    MetroId affectedMetro;

    List<Long> affectedAsns;

    List<String> affectedLabels;

    List<String> affectedPaths;

    double impactRatio;

    String severity;

    String recommendation;
}
