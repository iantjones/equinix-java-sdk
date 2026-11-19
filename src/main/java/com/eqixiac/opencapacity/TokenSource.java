/*
 * Copyright 2026 Ian Jones. All Rights Reserved.
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

package com.eqixiac.opencapacity;

/**
 * Supplies the bearer token for one provider. Implement it to use a token you obtain yourself.
 * {@link OpenCapacity.Builder#credentials(String, String)} installs an implementation that runs the
 * OAuth 2.0 client credentials flow.
 */
public interface TokenSource {

    /**
     * Returns a token that is valid now.
     *
     * @return the bearer token
     */
    String token();

    /**
     * Tells the source that the provider rejected the current token, so that the next call to
     * {@link #token()} obtains a new one. The default does nothing.
     */
    default void invalidate() {
    }
}
