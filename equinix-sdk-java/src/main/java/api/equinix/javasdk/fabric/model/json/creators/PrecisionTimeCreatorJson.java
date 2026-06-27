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

package api.equinix.javasdk.fabric.model.json.creators;

import api.equinix.javasdk.fabric.enums.PrecisionTimePackageCode;
import api.equinix.javasdk.fabric.enums.PrecisionTimeType;
import api.equinix.javasdk.fabric.model.Project;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Setter;

@Setter(AccessLevel.PRIVATE)
public class PrecisionTimeCreatorJson {

    @JsonProperty("type")
    private PrecisionTimeType type;

    @JsonProperty("name")
    private String name;

    @JsonProperty("description")
    private String description;

    @JsonProperty("packageCode")
    private PrecisionTimePackageCode packageCode;

    @JsonProperty("project")
    private Project project;

    /**
     * <p>Constructor for PrecisionTimeCreatorJson.</p>
     *
     * @param precisionTimeBuilder a {@link api.equinix.javasdk.fabric.model.json.creators.PrecisionTimeOperator.PrecisionTimeBuilder} object.
     */
    public PrecisionTimeCreatorJson(PrecisionTimeOperator.PrecisionTimeBuilder precisionTimeBuilder) {
        this.type = precisionTimeBuilder.getType();
        this.name = precisionTimeBuilder.getName();
        this.description = precisionTimeBuilder.getDescription();
        this.packageCode = precisionTimeBuilder.getPackageCode();
        this.project = precisionTimeBuilder.getProject();
    }
}
