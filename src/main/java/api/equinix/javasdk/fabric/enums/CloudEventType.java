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

package api.equinix.javasdk.fabric.enums;

public enum CloudEventType {
    CONNECTION_CREATED,
    CONNECTION_UPDATED,
    CONNECTION_DELETED,
    CONNECTION_STATUS_CHANGED,
    PORT_CREATED,
    PORT_UPDATED,
    PORT_DELETED,
    PORT_STATUS_CHANGED,
    SERVICE_TOKEN_CREATED,
    SERVICE_TOKEN_UPDATED,
    SERVICE_TOKEN_DELETED,
    GATEWAY_CREATED,
    GATEWAY_UPDATED,
    GATEWAY_DELETED,
    GATEWAY_STATUS_CHANGED,
    STREAM_CREATED,
    STREAM_UPDATED,
    STREAM_DELETED,
    STREAM_STATUS_CHANGED,
    UNKNOWN
}
