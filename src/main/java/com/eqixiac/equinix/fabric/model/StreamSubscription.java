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

package com.eqixiac.equinix.fabric.model;

import com.eqixiac.equinix.fabric.enums.StreamSubscriptionState;
import com.eqixiac.equinix.fabric.enums.StreamSubscriptionType;
import com.eqixiac.equinix.fabric.model.implementation.ChangeLog;
import com.eqixiac.equinix.fabric.model.implementation.StreamSink;
import com.eqixiac.equinix.fabric.model.implementation.StreamSubscriptionOperation;
import com.eqixiac.equinix.fabric.model.implementation.StreamSubscriptionSelector;
import com.eqixiac.equinix.fabric.model.json.creators.StreamSubscriptionOperator;

public interface StreamSubscription {

    String getUuid();

    String getHref();

    String getName();

    StreamSubscriptionType getType();

    StreamSubscriptionState getState();

    String getDescription();

    Boolean getEnabled();

    StreamSubscriptionSelector getMetricSelector();

    StreamSubscriptionSelector getEventSelector();

    StreamSink getSink();

    StreamSubscriptionOperation getOperation();

    ChangeLog getChangeLog();

    /**
     * Begins a fluent full-body update of this stream subscription, pre-populated with its current
     * state, e.g. {@code subscription.update(streamId).withName("New-Name").save()}.
     *
     * @param streamId the unique identifier of the parent stream
     * @return a seeded {@link com.eqixiac.equinix.fabric.model.json.creators.StreamSubscriptionOperator.StreamSubscriptionBuilder}
     */
    StreamSubscriptionOperator.StreamSubscriptionBuilder update(String streamId);

    Boolean delete(String streamId);

    void refresh(String streamId);
}
