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

package api.equinix.javasdk.ibxsmartview.model;

import api.equinix.javasdk.ibxsmartview.enums.SubscriptionStatus;
import api.equinix.javasdk.ibxsmartview.model.implementation.Channel;
import api.equinix.javasdk.ibxsmartview.model.implementation.MessageType;

/**
 * A streaming subscription ({@code SubscriptionResponse} in the spec). Describes the message
 * types delivered, the single delivery channel, and lifecycle/audit metadata.
 */
public interface StreamingSubscription {

    String getId();

    SubscriptionStatus getStatus();

    MessageType getMessageType();

    Channel getChannel();

    String getOrgId();

    String getCreatedBy();

    String getCreatedDateTime();

    String getUpdatedBy();

    String getUpdatedDateTime();

    Boolean delete();

    void refresh();
}
