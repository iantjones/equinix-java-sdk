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

package api.equinix.javasdk.core.model;

import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.List;
import java.util.Map;

/**
 * <p>Abstract RequestBuilderBase class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
public abstract class RequestBuilderBase<R> implements OptionalRequestBuilder<R> {

    @Accessors(fluent = true)
    @Getter
    protected Boolean wasBuilt = false;

    @Getter
    protected Map<String, List<String>> queryParameters;

}
