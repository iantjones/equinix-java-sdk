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

package api.equinix.javasdk.networkedge.model.json.creators;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * <p>ClusterConfig class. Cluster configuration supplied when creating a cluster virtual
 * device (the {@code clusterDetails} field of the create request).</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ClusterConfig {

    @JsonProperty("clusterName")
    private String clusterName;

    @JsonProperty("clusterNodeDetails")
    private ClusterNodeDetails clusterNodeDetails;

    public ClusterConfig() {
    }

    public ClusterConfig(String clusterName) {
        this.clusterName = clusterName;
    }

    public ClusterConfig withClusterName(String clusterName) {
        this.clusterName = clusterName;
        return this;
    }

    public ClusterConfig withNodes(NodeDetails node0, NodeDetails node1) {
        this.clusterNodeDetails = new ClusterNodeDetails(node0, node1);
        return this;
    }

    @Getter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ClusterNodeDetails {

        @JsonProperty("node0")
        private final NodeDetails node0;

        @JsonProperty("node1")
        private final NodeDetails node1;

        public ClusterNodeDetails(NodeDetails node0, NodeDetails node1) {
            this.node0 = node0;
            this.node1 = node1;
        }
    }

    @Getter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class NodeDetails {

        @JsonProperty("licenseFileId")
        private String licenseFileId;

        @JsonProperty("licenseToken")
        private String licenseToken;

        @JsonProperty("vendorConfig")
        private Object vendorConfig;

        public NodeDetails withLicenseFileId(String licenseFileId) {
            this.licenseFileId = licenseFileId;
            return this;
        }

        public NodeDetails withLicenseToken(String licenseToken) {
            this.licenseToken = licenseToken;
            return this;
        }

        public NodeDetails withVendorConfig(Object vendorConfig) {
            this.vendorConfig = vendorConfig;
            return this;
        }
    }
}
