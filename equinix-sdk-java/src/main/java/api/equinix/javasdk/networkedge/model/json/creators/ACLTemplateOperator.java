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

import api.equinix.javasdk.core.http.response.Pageable;
import api.equinix.javasdk.core.internal.Constants;
import api.equinix.javasdk.core.model.ResourceImpl;
import api.equinix.javasdk.networkedge.client.internal.implementation.ACLTemplateClientImpl;
import api.equinix.javasdk.networkedge.enums.Protocol;
import api.equinix.javasdk.networkedge.model.ACLTemplate;
import api.equinix.javasdk.networkedge.model.json.ACLTemplateJson;
import api.equinix.javasdk.networkedge.model.wrappers.ACLTemplateWrapper;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * <p>ACLTemplateOperator class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
public class ACLTemplateOperator extends ResourceImpl<ACLTemplate> {

    @Getter
    private final Pageable<ACLTemplate> serviceClient;

    /**
     * <p>Constructor for ACLTemplateOperator.</p>
     *
     * @param serviceClient a {@link api.equinix.javasdk.core.http.response.Pageable} object.
     */
    public ACLTemplateOperator(Pageable<ACLTemplate> serviceClient) {
        this.serviceClient = serviceClient;
    }

    /**
     * <p>create.</p>
     *
     * @param aclTemplateName a {@link java.lang.String} object.
     * @return a {@link api.equinix.javasdk.networkedge.model.json.creators.ACLTemplateOperator.ACLTemplateBuilder} object.
     */
    public ACLTemplateBuilder create(String aclTemplateName) {
        return new ACLTemplateBuilder(aclTemplateName);
    }

    /**
     * <p>update.</p>
     *
     * @param json a {@link api.equinix.javasdk.networkedge.model.json.ACLTemplateJson} object.
     * @return a {@link api.equinix.javasdk.networkedge.model.json.creators.ACLTemplateOperator.ACLTemplateUpdater} object.
     */
    public ACLTemplateUpdater update(ACLTemplateJson json) {
        return new ACLTemplateUpdater(json);
    }

    @Getter
    public class ACLTemplateBuilder {
        private String accountUcmId;
        private String name;
        private String description;
        private List<ACLTemplateCreatorJson.InboundRule> inboundRules;

        protected ACLTemplateBuilder(String name) {
            this.name = name;
        }

        public ACLTemplateBuilder forAccount(String accountUcmId) {
            this.accountUcmId = accountUcmId;
            return this;
        }

        public ACLTemplateBuilder withDescription(String description) {
            this.description = description;
            return this;
        }

        public ACLTemplateBuilder withRule(Protocol protocol, String srcPort, String dstPort, String subnet, Integer seqNo) {
            if(this.inboundRules == null) {
                this.inboundRules = new ArrayList<>();
            }
            this.inboundRules.add(new ACLTemplateCreatorJson.InboundRule(protocol, srcPort, dstPort, subnet, seqNo));
            return this;
        }

        public ACLTemplate save() {
            ACLTemplateCreatorJson deviceLinkCreatorJson = new ACLTemplateCreatorJson(this);
            ACLTemplateJson deviceLinkJson = ((ACLTemplateClientImpl) ACLTemplateOperator.this.getServiceClient()).create(deviceLinkCreatorJson);
            return new ACLTemplateWrapper(deviceLinkJson, ACLTemplateOperator.this.getServiceClient());
        }
    }

    public class ACLTemplateUpdater {

        private ACLTemplateJson json;
        private ACLTemplateUpdaterJson updaterJson;

        protected ACLTemplateUpdater(ACLTemplateJson json) {
            this.json = json;
            this.updaterJson = Constants.JSON_CONVERTOR.convertValue(this.json, ACLTemplateUpdaterJson.class);
        }

        public ACLTemplateUpdater forCustomer(String accountUcmId) {
            this.updaterJson.setAccountUcmId(accountUcmId);
            return this;
        }

        public ACLTemplateUpdater withName(String name) {
            this.updaterJson.setName(name);
            return this;
        }

        public ACLTemplateUpdater withDescription(String description) {
            this.updaterJson.setDescription(description);
            return this;
        }

        public ACLTemplateUpdater addRule(Protocol protocol, String srcPort, String dstPort, String subnet, Integer seqNo) {
            List<ACLTemplateUpdaterJson.InboundRule> inboundRules = updaterJson.getInboundRules();
            if(inboundRules == null) {
                inboundRules = new ArrayList<>();
            }
            inboundRules.add(new ACLTemplateUpdaterJson.InboundRule(protocol, srcPort, dstPort, subnet, seqNo));
            return this;
        }

        public ACLTemplateUpdater removeRule(Protocol protocol, String srcPort, String dstPort, String subnet, Integer seqNo) {
            List<ACLTemplateUpdaterJson.InboundRule> inboundRules = updaterJson.getInboundRules();

            if(inboundRules == null) {
                return this;
            }

            updaterJson.setInboundRules(
                    inboundRules.stream().filter(Predicate.not(inboundRule -> inboundRule.getProtocol() == protocol && inboundRule.getSrcPort().equals(srcPort)
                    && inboundRule.getDstPort().equals(dstPort) && inboundRule.getSubnet().equals(subnet) && inboundRule.getSeqNo().equals(seqNo)))
                    .collect(Collectors.toList()));

            return this;
        }

        public ACLTemplate save() {
            json = ((ACLTemplateClientImpl) ACLTemplateOperator.this.getServiceClient()).update(this.json.getUuid(), this.updaterJson);
            return new ACLTemplateWrapper(json, ACLTemplateOperator.this.getServiceClient());
        }
    }
}
