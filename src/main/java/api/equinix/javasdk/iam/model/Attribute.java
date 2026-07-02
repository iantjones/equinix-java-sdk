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

package api.equinix.javasdk.iam.model;

/**
 * An attribute included on a resource type or action, as returned by the IAM resource-type and
 * list-actions operations.
 *
 * <p>This is a read-only response view (an item of spec schema {@code AttributeSet}).</p>
 */
public interface Attribute {

    /**
     * @return the attribute identifier (e.g. {@code attribute:myAttr})
     */
    String getAttributeId();

    /**
     * @return {@code true} when the attribute is always present; otherwise the attribute may or
     *         may not be present (may be {@code null})
     */
    Boolean getAttributeOptional();
}
