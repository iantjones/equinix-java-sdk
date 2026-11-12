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
 * The facts of a Metro Optimizer run: {@link com.eqixiac.equinix.design.optimizer.context.CatalogDesignContext}
 * (an in-memory {@link com.eqixiac.equinix.design.spi.DesignContext} with a builder),
 * {@link com.eqixiac.equinix.design.optimizer.context.FabricCatalogLoader} (builds one from live Fabric
 * data, read-only) and {@link com.eqixiac.equinix.design.optimizer.context.ProviderCatalog} (which metros
 * each cloud is reachable from). Latencies carry their basis as a
 * {@link com.eqixiac.equinix.design.optimizer.context.LatencyFigure}.
 */
package com.eqixiac.equinix.design.optimizer.context;
