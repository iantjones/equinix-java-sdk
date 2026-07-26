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
import com.eqixiac.equinix.fabric.client.internal.implementation.PrecisionTimeClientImpl;
import com.eqixiac.equinix.fabric.model.PrecisionTime;
import com.eqixiac.equinix.fabric.model.json.PrecisionTimeJson;
import com.eqixiac.equinix.fabric.model.json.creators.PrecisionTimeOperator;
import lombok.Getter;
import lombok.experimental.Delegate;

public class PrecisionTimeWrapper extends ResourceImpl<PrecisionTime> implements PrecisionTime {

    @Delegate(excludes = PrecisionTimeMutability.class)
    private PrecisionTimeJson jsonObject;
    @Getter
    private final Pageable<PrecisionTime> serviceClient;

    public PrecisionTimeWrapper(PrecisionTimeJson precisionTimeJson, Pageable<PrecisionTime> serviceClient) {
        this.jsonObject = precisionTimeJson;
        this.serviceClient = serviceClient;
    }

    public PrecisionTimeOperator.PrecisionTimeUpdater update() {
        return new PrecisionTimeOperator((PrecisionTimeClientImpl) this.serviceClient).update(this.getUuid());
    }

    public Boolean delete() {
        this.jsonObject = ((PrecisionTimeClientImpl)this.serviceClient).delete(this.getUuid());
        return true;
    }

    public void refresh() {
        this.jsonObject = ((PrecisionTimeClientImpl)this.serviceClient).refresh(this.getUuid());
    }

    private interface PrecisionTimeMutability {
        PrecisionTimeOperator.PrecisionTimeUpdater update();
        Boolean delete();
        void refresh();
    }
}
