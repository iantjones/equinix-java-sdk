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

package api.equinix.javasdk.ibxsmartview.model.implementation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * A streaming reading for a single monitoring tag point within an IBX, including the
 * measured value with its unit.
 */
@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TagPoint {

    @JsonProperty("streamId")
    private String streamId;

    @JsonProperty("ibx")
    private String ibx;

    @JsonProperty("tag")
    private TagDetails tag;

    @JsonProperty("reading")
    private TagPointValueWithUnit reading;

    @JsonProperty("readingTime")
    private String readingTime;

    @JsonProperty("dataQuality")
    private String dataQuality;
}
