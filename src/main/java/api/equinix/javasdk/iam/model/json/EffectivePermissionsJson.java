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

package api.equinix.javasdk.iam.model.json;

import api.equinix.javasdk.iam.model.EffectivePermissions;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Getter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Read-only JSON model for the IAM {@code EffectivePermissions} response. Implements
 * {@link EffectivePermissions} directly.
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class EffectivePermissionsJson implements EffectivePermissions {

    @JsonProperty("principalId")
    private String principalId;

    @JsonProperty("projectId")
    private String projectId;

    @JsonProperty("serviceId")
    private String serviceId;

    @JsonProperty("accessPolicyIds")
    private List<String> accessPolicyIds;

    @JsonProperty("permissions")
    private List<PermissionJson> permissions;

    @Override
    public List<EffectivePermissions.Permission> getPermissions() {
        if (permissions == null) {
            return null;
        }
        return java.util.Collections.unmodifiableList(new ArrayList<EffectivePermissions.Permission>(permissions));
    }

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PermissionJson implements EffectivePermissions.Permission {

        @JsonProperty("actions")
        private List<String> actions;

        @JsonProperty("resources")
        private ResourceSelectorJson resources;

        @JsonProperty("metroCodes")
        private ResourceSelectorJson metroCodes;

        @JsonProperty("ibxIds")
        private ResourceSelectorJson ibxIds;

        @JsonProperty("cageIds")
        private ResourceSelectorJson cageIds;

        @JsonProperty("condition")
        private String condition;

        @Override
        public EffectivePermissions.ResourceSelector getResources() {
            return resources;
        }

        @Override
        public EffectivePermissions.ResourceSelector getMetroCodes() {
            return metroCodes;
        }

        @Override
        public EffectivePermissions.ResourceSelector getIbxIds() {
            return ibxIds;
        }

        @Override
        public EffectivePermissions.ResourceSelector getCageIds() {
            return cageIds;
        }
    }

    /**
     * Read-only JSON model for the {@code anyOf} resource selector, which is either a bare array of
     * values (inclusion) or an object {@code {except: [...]}} (exclusion). A custom deserializer
     * normalizes both wire shapes into {@link #getInclude()} / {@link #getExcept()}.
     */
    @Getter
    @JsonDeserialize(using = ResourceSelectorJson.Deserializer.class)
    public static class ResourceSelectorJson implements EffectivePermissions.ResourceSelector {

        private List<String> include;

        private List<String> except;

        public static class Deserializer extends JsonDeserializer<ResourceSelectorJson> {

            @Override
            public ResourceSelectorJson deserialize(JsonParser parser, DeserializationContext context)
                    throws IOException {
                JsonNode node = parser.getCodec().readTree(parser);
                ResourceSelectorJson selector = new ResourceSelectorJson();
                if (node.isArray()) {
                    selector.include = readStrings(node);
                } else if (node.isObject() && node.has("except")) {
                    selector.except = readStrings(node.get("except"));
                }
                return selector;
            }

            private List<String> readStrings(JsonNode arrayNode) {
                if (arrayNode == null || !arrayNode.isArray()) {
                    return null;
                }
                List<String> values = new ArrayList<>();
                for (JsonNode element : arrayNode) {
                    values.add(element.asText());
                }
                return values;
            }
        }
    }
}
