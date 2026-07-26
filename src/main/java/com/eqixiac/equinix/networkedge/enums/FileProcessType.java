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

package com.eqixiac.equinix.networkedge.enums;

import com.eqixiac.equinix.core.model.APIParam;

/**
 * <p>Indicates whether an uploaded file is a license file ({@code LICENSE}) or a
 * cloud-init / bootstrap file ({@code CLOUD_INIT}).</p>
 *
 * @author ianjones
 */
public enum FileProcessType implements APIParam {
    LICENSE,
    CLOUD_INIT
}
