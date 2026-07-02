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

package api.equinix.javasdk.internetaccess.model.implementation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

/**
 * Current state of the latest change applied to an Equinix Internet Access (EIA) v2 service.
 *
 * <p>The change is polymorphic by {@code status} ({@code REQUESTED}, {@code SUBMITTED_FOR_APPROVAL},
 * {@code APPROVED}, {@code REJECTED}, {@code COMPLETED} or {@code FAILED}); this read view flattens
 * all status variants, exposing the common fields plus the {@code REJECTED} rejection reason and the
 * {@code FAILED} error list when present.</p>
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class Change {

    @JsonProperty("uuid")
    private String uuid;

    @JsonProperty("type")
    private String type;

    @JsonProperty("status")
    private String status;

    @JsonProperty("createdDateTime")
    private String createdDateTime;

    @JsonProperty("updatedDateTime")
    private String updatedDateTime;

    @JsonProperty("information")
    private String information;

    @JsonProperty("data")
    private ChangeData data;

    /**
     * Payload of a {@link Change}: the service the change targets plus the serialized change request,
     * and (for rejected/failed changes) the rejection reason or error list.
     */
    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ChangeData {

        @JsonProperty("service")
        private ChangeDataService service;

        @JsonProperty("request")
        private String request;

        @JsonProperty("rejectionReason")
        private String rejectionReason;

        @JsonProperty("errors")
        private List<ChangeError> errors;
    }

    /**
     * Reference to the service a {@link Change} targets.
     */
    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ChangeDataService {

        @JsonProperty("uuid")
        private String uuid;
    }

    /**
     * An error entry attached to a {@code FAILED} {@link Change}.
     */
    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ChangeError {

        @JsonProperty("errorCode")
        private String errorCode;

        @JsonProperty("errorMessage")
        private String errorMessage;

        @JsonProperty("correlationId")
        private String correlationId;

        @JsonProperty("details")
        private String details;

        @JsonProperty("help")
        private String help;

        @JsonProperty("additionalInfo")
        private List<ErrorAdditionalInfo> additionalInfo;
    }

    /**
     * Additional per-property information ({@code ErrorAdditionalInfo}) attached to a
     * {@link ChangeError}: the reason of the error and the request property that caused it.
     */
    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ErrorAdditionalInfo {

        @JsonProperty("reason")
        private String reason;

        @JsonProperty("property")
        private String property;
    }
}
