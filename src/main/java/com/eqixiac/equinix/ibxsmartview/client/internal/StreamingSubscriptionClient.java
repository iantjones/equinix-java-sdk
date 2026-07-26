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

package com.eqixiac.equinix.ibxsmartview.client.internal;

import com.eqixiac.equinix.core.http.response.Pageable;
import com.eqixiac.equinix.ibxsmartview.model.StreamingSubscription;
import com.eqixiac.equinix.ibxsmartview.model.json.StreamingSubscriptionJson;
import com.eqixiac.equinix.ibxsmartview.model.json.SubscriptionCertificateJson;
import com.eqixiac.equinix.ibxsmartview.model.json.SubscriptionDataJson;
import com.eqixiac.equinix.ibxsmartview.model.json.creators.StreamingSubscriptionCreatorJson;

import java.util.List;

public interface StreamingSubscriptionClient<T> extends Pageable<T> {

    List<StreamingSubscriptionJson> list();

    StreamingSubscriptionJson getByUuid(String uuid);

    StreamingSubscriptionJson create(StreamingSubscriptionCreatorJson creatorJson);

    StreamingSubscriptionJson update(String uuid, StreamingSubscriptionCreatorJson creatorJson);

    StreamingSubscriptionJson delete(String uuid);

    StreamingSubscriptionJson refresh(String uuid);

    SubscriptionDataJson getSubscriptionData(String subscriptionId, List<String> ibxs, List<String> messageTypes,
                                             List<String> streamIds, Integer offset, Integer limit);

    SubscriptionCertificateJson getCertificate(String channelType);
}
