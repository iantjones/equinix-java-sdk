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

package api.equinix.javasdk.core.exception;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.math.BigInteger;
import java.util.ArrayList;

/**
 * <p>ExceptionDetail class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
@Getter
public class ExceptionDetail {

    @JsonProperty("errorCode")
    private String errorCode;

    @JsonProperty("statusCode")
    private String statusCode;

    @JsonProperty("errorDomain")
    private String errorDomain;

    @JsonProperty("errorMessage")
    private String errorMessage;

    @JsonProperty("message")
    private String message;

    @JsonProperty("error")
    private String error;

    @JsonProperty("fault")
    private String fault;

    @JsonProperty("details")
    private String details;

    @JsonProperty("correlationId")
    private String correlationId;

    @JsonProperty("status")
    private String status;

    @JsonProperty("path")
    private String path;

    @JsonProperty("moreInfo")
    private String moreInfo;

    @JsonProperty("timestamp")
    private BigInteger timestamp;

    @JsonProperty("additionalInfo")
    private ArrayList<ExceptionAdditionalInfo> additionalInfo;

    @JsonProperty("errorMessageKey")
    private String errorMessageKey;

    @JsonProperty("property")
    private String property;

    @JsonProperty("help")
    private String help;

    @JsonProperty("errorTitle")
    private String errorTitle;

    @JsonProperty("developerMessage")
    private String developerMessage;
}
