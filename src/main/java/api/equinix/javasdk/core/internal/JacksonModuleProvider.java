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

package api.equinix.javasdk.core.internal;

import com.fasterxml.jackson.databind.Module;

/**
 * Service-provider interface that lets each domain contribute its own Jackson
 * (de)serializers to the SDK's shared {@link Constants#objectMapper} without core
 * having to depend on any domain package.
 *
 * <p>Implementations are discovered at mapper-construction time via {@link java.util.ServiceLoader}
 * and must be registered in
 * {@code META-INF/services/api.equinix.javasdk.core.internal.JacksonModuleProvider}.</p>
 *
 * @author ianjones
 */
public interface JacksonModuleProvider {

    /**
     * Returns a Jackson {@link Module} bundling this domain's custom (de)serializers.
     *
     * @return the module to register on the shared {@code ObjectMapper}
     */
    Module getModule();
}
