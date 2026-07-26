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

package com.eqixiac.equinix.fabric.model.wrappers;

import com.eqixiac.equinix.core.http.response.Pageable;
import com.eqixiac.equinix.core.model.ResourceImpl;
import com.eqixiac.equinix.fabric.client.internal.implementation.CompanyProfileClientImpl;
import com.eqixiac.equinix.fabric.model.CompanyProfile;
import com.eqixiac.equinix.fabric.model.json.CompanyProfileJson;
import lombok.Getter;
import lombok.experimental.Delegate;

public class CompanyProfileWrapper extends ResourceImpl<CompanyProfile> implements CompanyProfile {

    @Delegate(excludes = CompanyProfileMutability.class)
    private CompanyProfileJson jsonObject;
    @Getter
    private final Pageable<CompanyProfile> serviceClient;

    public CompanyProfileWrapper(CompanyProfileJson jsonObject, Pageable<CompanyProfile> serviceClient) {
        this.jsonObject = jsonObject;
        this.serviceClient = serviceClient;
    }

    public Boolean delete() {
        ((CompanyProfileClientImpl) this.serviceClient).delete(this.getUuid());
        return true;
    }

    public void refresh() {
        this.jsonObject = ((CompanyProfileClientImpl) this.serviceClient).refresh(this.getUuid());
    }

    private interface CompanyProfileMutability {
        Boolean delete();
        void refresh();
    }
}
