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

package api.equinix.javasdk.fabric.model.implementation;

import api.equinix.javasdk.fabric.enums.LinkProtocolType;
import api.equinix.javasdk.fabric.model.implementation.filter.Filter;
import api.equinix.javasdk.fabric.model.implementation.filter.FilterPropertyList;
import api.equinix.javasdk.fabric.model.implementation.filter.FilterType;
import api.equinix.javasdk.fabric.model.json.creators.ConnectionOperator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

/**
 * <p>LinkProtocol class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
@Getter
@Setter(AccessLevel.PRIVATE)
@NoArgsConstructor
public class LinkProtocol {

    @JsonProperty("type")
    private LinkProtocolType type;

    @JsonProperty("vlanTag")
    private Integer vlanTag;

    @JsonProperty("vlanCTag")
    private Integer vlanCTag;

    @JsonProperty("vlanSTag")
    private Integer vlanSTag;

    public LinkProtocol(Dot1QBuilder dot1QBuilder) {
        this.type = dot1QBuilder.linkProtocolType;
        this.vlanTag = dot1QBuilder.vlanTag;
    }

    public LinkProtocol(QinQBuilder qinQBuilder) {
        this.type = qinQBuilder.linkProtocolType;
        this.vlanCTag = qinQBuilder.vlanCTag;
        this.vlanSTag = qinQBuilder.vlanSTag;
    }

    public LinkProtocol(UntaggedBuilder untaggedBuilder) {
        this.type = untaggedBuilder.linkProtocolType;
    }

    public static Dot1QBuilder dot1q() {
        return new Dot1QBuilder(LinkProtocolType.DOT1Q);
    }

    public static QinQBuilder qinq() {
        return new QinQBuilder(LinkProtocolType.QINQ);
    }

    public static UntaggedBuilder untagged() {
        return new UntaggedBuilder(LinkProtocolType.UNTAGGED);
    }

    public static class Dot1QBuilder {
        private final LinkProtocolType linkProtocolType;
        private Integer vlanTag;

        protected Dot1QBuilder(LinkProtocolType linkProtocolType) {
            this.linkProtocolType = linkProtocolType;
        }

        public Dot1QBuilder vlanTag(Integer vlanTag) {
            this.vlanTag = vlanTag;
            return this;
        }

        public LinkProtocol create() {
            return new LinkProtocol(this);
        }
    }

    public static class QinQBuilder {
        private final LinkProtocolType linkProtocolType;
        private Integer vlanCTag;
        private Integer vlanSTag;

        protected QinQBuilder(LinkProtocolType linkProtocolType) {
            this.linkProtocolType = linkProtocolType;
        }

        public QinQBuilder vlanCTag(Integer vlanCTag) {
            this.vlanCTag = vlanCTag;
            return this;
        }
        
        public QinQBuilder vlanSTag(Integer vlanSTag) {
            this.vlanSTag = vlanSTag;
            return this;
        }

        public LinkProtocol create() {
            return new LinkProtocol(this);
        }
    }

    public static class UntaggedBuilder {
        private final LinkProtocolType linkProtocolType;

        protected UntaggedBuilder(LinkProtocolType linkProtocolType) {
            this.linkProtocolType = linkProtocolType;
        }

        public LinkProtocol create() {
            return new LinkProtocol(this);
        }
    }
}
