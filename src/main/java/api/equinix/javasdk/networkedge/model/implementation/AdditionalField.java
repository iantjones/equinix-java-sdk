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

package api.equinix.javasdk.networkedge.model.implementation;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

/**
 * <p>AdditionalField class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
@Getter
public class AdditionalField {

    @JsonProperty("name")
    private String name;

    @JsonProperty("required")
    private Boolean required;

    @JsonProperty("includeInResponse")
    private Boolean includeInResponse;

    @JsonProperty("onlyForCluster")
    private Boolean onlyForCluster;

    @JsonProperty("onlyForClusterPrimaryNode")
    private Boolean onlyForClusterPrimaryNode;

    @JsonProperty("systemGenerated")
    private Boolean systemGenerated;

    @JsonProperty("shouldHideForSecondary")
    private Boolean shouldHideForSecondary;

    @JsonProperty("dataType")
    private String dataType;

    @JsonProperty("dataTypeValues")
    private List<DataTypeValue> dataTypeValues;

    @JsonProperty("dependsOn")
    private List<FieldValue> dependsOn;

    @JsonProperty("isEncryptionNeeded")
    private Boolean isEncryptionNeeded;

    @JsonProperty("isSouthBoundEncryptionNeeded")
    private Boolean isSouthBoundEncryptionNeeded;

    @JsonProperty("southBoundEncryptonType")
    private String southBoundEncryptonType;

    @JsonProperty("isSameValueAllowedForPrimaryAndSecondary")
    private Boolean isSameValueAllowedForPrimaryAndSecondary;
}
