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

package api.equinix.javasdk.fabric.internal;

import api.equinix.javasdk.core.internal.JacksonModuleProvider;
import api.equinix.javasdk.fabric.enums.Side;
import api.equinix.javasdk.fabric.model.deserializers.SideDeserializer;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * Registers Fabric-specific Jackson (de)serializers on the shared SDK {@code ObjectMapper}
 * via the {@link JacksonModuleProvider} SPI, so core does not depend on Fabric types.
 *
 * @author ianjones
 */
public class FabricJacksonModuleProvider implements JacksonModuleProvider {

    @Override
    public Module getModule() {
        SimpleModule module = new SimpleModule("FabricModule");
        module.addDeserializer(Side.class, new SideDeserializer());
        return module;
    }
}
