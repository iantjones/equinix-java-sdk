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

package com.eqixiac.equinix.ibxsmartview.client.internal.implementation;

import com.eqixiac.equinix.core.client.ResourceClientBase;
import com.eqixiac.equinix.core.enums.RequestType;
import com.eqixiac.equinix.core.http.ResponseHandler;
import com.eqixiac.equinix.core.http.SerializationHelper;
import com.eqixiac.equinix.core.http.request.EquinixRequest;
import com.eqixiac.equinix.core.http.response.EquinixResponse;
import com.eqixiac.equinix.ibxsmartview.client.implementation.IBXSmartViewConfigImpl;
import com.eqixiac.equinix.ibxsmartview.client.internal.StreamingSubscriptionClient;
import com.eqixiac.equinix.ibxsmartview.model.StreamingSubscription;
import com.eqixiac.equinix.ibxsmartview.model.json.StreamingSubscriptionJson;
import com.eqixiac.equinix.ibxsmartview.model.json.SubscriptionCertificateJson;
import com.eqixiac.equinix.ibxsmartview.model.json.SubscriptionDataJson;
import com.eqixiac.equinix.ibxsmartview.model.json.creators.StreamingSubscriptionCreatorJson;
import com.eqixiac.equinix.ibxsmartview.model.wrappers.StreamingSubscriptionWrapper;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class StreamingSubscriptionClientImpl extends ResourceClientBase<StreamingSubscription, StreamingSubscriptionJson> implements StreamingSubscriptionClient<StreamingSubscription> {

    // Subscription ids are Mongo ObjectIds (e.g. 607460b4e4a78360425bca56), not dashed UUIDs, so
    // Constants.UUID_PATTERN does not match the create-response Location header. Capture the
    // trailing path segment instead (e.g. /smartview/v2/streaming/subscriptions/{id}).
    private static final Pattern LOCATION_ID_PATTERN = Pattern.compile(".*/([^/]+)/?$");

    public StreamingSubscriptionClientImpl(IBXSmartViewConfigImpl configClient) {
        super(configClient, "IBXSmartView", "StreamingSubscriptions", StreamingSubscriptionJson.class);
    }

    @Override
    protected StreamingSubscription wrap(StreamingSubscriptionJson json) {
        return new StreamingSubscriptionWrapper(json, this);
    }

    public List<StreamingSubscriptionJson> list() {
        EquinixRequest<StreamingSubscription> equinixRequest = this.buildRequest("ListSubscriptions", RequestType.LIST, StreamingSubscriptionJson.class);
        EquinixResponse<StreamingSubscription> equinixResponse = this.invoke(equinixRequest);
        return ResponseHandler.handleListResponse(equinixResponse, equinixRequest);
    }

    public StreamingSubscriptionJson getByUuid(String uuid) {
        return getOne("GetSubscription", uuid);
    }

    // POST /smartview/v2/streaming/subscriptions returns 201 with only a Location header (no body),
    // so we parse the new subscription id from Location and re-fetch the created resource.
    public StreamingSubscriptionJson create(StreamingSubscriptionCreatorJson creatorJson) {
        EquinixRequest<Object> request = buildRequest("CreateSubscription", RequestType.SINGLE, Object.class);
        SerializationHelper.serializeJson(request, creatorJson);
        String id = ResponseHandler.extractFromHeader(invoke(request), "Location", LOCATION_ID_PATTERN);
        return getByUuid(id);
    }

    // PUT /smartview/v2/streaming/subscriptions/{id} returns 204 No Content, so we drain/validate the
    // response and re-fetch the updated resource rather than deserialize an empty body.
    public StreamingSubscriptionJson update(String uuid, StreamingSubscriptionCreatorJson creatorJson) {
        voidOp("UpdateSubscription", RequestType.SINGLE, Map.of("uuid", uuid), null, creatorJson);
        return getByUuid(uuid);
    }

    // DELETE /smartview/v2/streaming/subscriptions/{id} returns 204 No Content; drain/validate the
    // response and return null (no resource to deserialize).
    public StreamingSubscriptionJson delete(String uuid) {
        voidOp("DeleteSubscription", RequestType.SINGLE, Map.of("uuid", uuid), null, null);
        return null;
    }

    public StreamingSubscriptionJson refresh(String uuid) {
        return this.getByUuid(uuid);
    }

    public SubscriptionDataJson getSubscriptionData(String subscriptionId, List<String> ibxs, List<String> messageTypes,
                                                    List<String> streamIds, Integer offset, Integer limit) {
        Map<String, List<String>> qParams = new java.util.HashMap<>();
        if (ibxs != null && !ibxs.isEmpty()) {
            qParams.put("ibxs", ibxs);
        }
        if (messageTypes != null && !messageTypes.isEmpty()) {
            qParams.put("messageTypes", messageTypes);
        }
        if (streamIds != null && !streamIds.isEmpty()) {
            qParams.put("streamIds", streamIds);
        }
        if (offset != null) {
            qParams.put("offset", List.of(String.valueOf(offset)));
        }
        if (limit != null) {
            qParams.put("limit", List.of(String.valueOf(limit)));
        }
        return getAs("GetSubscriptionData", Map.of("subscriptionId", subscriptionId), qParams, SubscriptionDataJson.class);
    }

    public SubscriptionCertificateJson getCertificate(String channelType) {
        return getAs("GetCertificate", Map.of(), Map.of("channelType", List.of(channelType)), SubscriptionCertificateJson.class);
    }
}
