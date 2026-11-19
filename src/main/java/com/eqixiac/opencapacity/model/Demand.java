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

package com.eqixiac.opencapacity.model;

/**
 * How much capacity is needed: cabinets and density, or power with cabinets and density, or
 * power alone.
 *
 * @param power the total power, or {@code null}
 * @param powerBasis what {@code power} measures, or {@code null} for the equipment load
 * @param powerFactor the power factor, or {@code null} for the provider's default
 * @param cabinets the cabinets, or {@code null}
 * @param density the density per cabinet, or {@code null}
 * @param spaceType the form of space, or {@code null}
 * @param contiguous whether the cabinets must be adjacent, or {@code null}
 */
public record Demand(
        Power power, PowerBasis powerBasis, Double powerFactor, CabinetDemand cabinets, DensityRange density,
        SpaceType spaceType, Boolean contiguous) {}
