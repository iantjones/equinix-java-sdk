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

package api.equinix.javasdk.networkedge.model;

import api.equinix.javasdk.networkedge.model.implementation.InboundRule;
import api.equinix.javasdk.networkedge.model.json.creators.ACLTemplateOperator;
import api.equinix.javasdk.networkedge.model.json.creators.ACLTemplateUpdaterJson;

import java.util.List;

/**
 * <p>ACLTemplate interface.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
public interface ACLTemplate {

    /**
     * <p>getUuid.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    String getUuid();

    /**
     * <p>getName.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    String getName();

    /**
     * <p>getDescription.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    String getDescription();

    /**
     * <p>getInboundRules.</p>
     *
     * @return a {@link java.util.List} object.
     */
    List<InboundRule> getInboundRules();

    /**
     * <p>update.</p>
     *
     * @return a {@link api.equinix.javasdk.networkedge.model.json.creators.ACLTemplateOperator.ACLTemplateUpdater} object.
     */
    ACLTemplateOperator.ACLTemplateUpdater update();

    /**
     * <p>save.</p>
     *
     * @param updaterJson a {@link api.equinix.javasdk.networkedge.model.json.creators.ACLTemplateUpdaterJson} object.
     * @return a {@link java.lang.Boolean} object.
     */
    Boolean save(ACLTemplateUpdaterJson updaterJson);

    /**
     * <p>delete.</p>
     *
     * @return a {@link java.lang.Boolean} object.
     */
    Boolean delete();

    /**
     * <p>delete.</p>
     *
     * @param accountUcmId a {@link java.lang.String} object.
     * @return a {@link java.lang.Boolean} object.
     */
    Boolean delete(String accountUcmId);

    /**
     * <p>refresh.</p>
     *
     * @return a {@link java.lang.Boolean} object.
     */
    Boolean refresh();

    /**
     * <p>refresh.</p>
     *
     * @param accountUcmId a {@link java.lang.String} object.
     * @return a {@link java.lang.Boolean} object.
     */
    Boolean refresh(String accountUcmId);
}
