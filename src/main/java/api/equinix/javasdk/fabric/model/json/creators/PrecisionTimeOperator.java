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

import api.equinix.javasdk.core.http.response.Pageable;
import api.equinix.javasdk.core.model.ResourceImpl;
import api.equinix.javasdk.fabric.client.internal.implementation.PrecisionTimeClientImpl;
import api.equinix.javasdk.fabric.enums.PrecisionTimePackageCode;
import api.equinix.javasdk.fabric.enums.PrecisionTimeType;
import api.equinix.javasdk.fabric.model.PrecisionTime;
import api.equinix.javasdk.fabric.model.Project;
import api.equinix.javasdk.fabric.model.json.PrecisionTimeJson;
import api.equinix.javasdk.fabric.model.wrappers.PrecisionTimeWrapper;
import lombok.Getter;

/**
 * <p>PrecisionTimeOperator class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
public class PrecisionTimeOperator extends ResourceImpl<PrecisionTime> {

    @Getter
    private final Pageable<PrecisionTime> serviceClient;

    /**
     * <p>Constructor for PrecisionTimeOperator.</p>
     *
     * @param serviceClient a {@link api.equinix.javasdk.core.http.response.Pageable} object.
     */
    public PrecisionTimeOperator(Pageable<PrecisionTime> serviceClient) {
        this.serviceClient = serviceClient;
    }

    /**
     * <p>create.</p>
     *
     * @return a {@link api.equinix.javasdk.fabric.model.json.creators.PrecisionTimeOperator.PrecisionTimeBuilder} object.
     */
    public PrecisionTimeBuilder create() {
        return new PrecisionTimeBuilder();
    }

    @Getter
    public class PrecisionTimeBuilder {

        private PrecisionTimeType type;
        private String name;
        private String description;
        private PrecisionTimePackageCode packageCode;
        private Project project;

        protected PrecisionTimeBuilder() {
        }

        public PrecisionTimeOperator.PrecisionTimeBuilder withType(PrecisionTimeType type) {
            this.type = type;
            return this;
        }

        public PrecisionTimeOperator.PrecisionTimeBuilder withName(String name) {
            this.name = name;
            return this;
        }

        public PrecisionTimeOperator.PrecisionTimeBuilder withDescription(String description) {
            this.description = description;
            return this;
        }

        public PrecisionTimeOperator.PrecisionTimeBuilder withPackageCode(PrecisionTimePackageCode packageCode) {
            this.packageCode = packageCode;
            return this;
        }

        public PrecisionTimeOperator.PrecisionTimeBuilder withProject(Project project) {
            this.project = project;
            return this;
        }

        public PrecisionTime create() {
            PrecisionTimeCreatorJson precisionTimeCreatorJson = new PrecisionTimeCreatorJson(this);
            PrecisionTimeJson precisionTimeJson = ((PrecisionTimeClientImpl) PrecisionTimeOperator.this.getServiceClient()).create(precisionTimeCreatorJson);
            return new PrecisionTimeWrapper(precisionTimeJson, PrecisionTimeOperator.this.getServiceClient());
        }
    }
}
