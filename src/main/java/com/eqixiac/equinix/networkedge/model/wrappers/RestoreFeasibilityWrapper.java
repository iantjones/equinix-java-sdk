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

package com.eqixiac.equinix.networkedge.model.wrappers;

import com.eqixiac.equinix.core.http.response.Pageable;
import com.eqixiac.equinix.core.model.ResourceImpl;
import com.eqixiac.equinix.networkedge.enums.ServiceType;
import com.eqixiac.equinix.networkedge.model.Backup;
import com.eqixiac.equinix.networkedge.model.RestoreFeasibility;
import com.eqixiac.equinix.networkedge.model.implementation.BackupService;
import com.eqixiac.equinix.networkedge.model.json.RestoreFeasibilityJson;
import lombok.Getter;
import lombok.experimental.Delegate;

import java.util.List;
import java.util.Map;

/**
 *
 * @author ianjones
 */
public class RestoreFeasibilityWrapper extends ResourceImpl<Backup> implements RestoreFeasibility {

    @Delegate(excludes = RestoreFeasibilityMutability.class)
    private RestoreFeasibilityJson json;
    @Getter
    private final Pageable<Backup> serviceClient;

    public RestoreFeasibilityWrapper(RestoreFeasibilityJson json, Pageable<Backup> serviceClient) {
        this.json = json;
        this.serviceClient = serviceClient;
    }

    public Map<ServiceType, List<BackupService>> getServices() {
        return null;
    }

    private interface RestoreFeasibilityMutability {
        Map<ServiceType, List<BackupService>> getServices();
    }
}
