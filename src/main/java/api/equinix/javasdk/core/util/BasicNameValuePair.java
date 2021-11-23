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

package api.equinix.javasdk.core.util;

import java.io.Serializable;
import java.util.Objects;

class BasicNameValuePair implements NameValuePair, Cloneable, Serializable {
    private static final long serialVersionUID = 1L;
    /** Constant <code>HASH_SEED=17</code> */
    public static final int HASH_SEED = 17;
    /** Constant <code>HASH_OFFSET=37</code> */
    public static final int HASH_OFFSET = 37;

    private final String name;
    private final String value;

    BasicNameValuePair(final String name, final String value) {
        if (name == null) {
            throw new IllegalArgumentException("Name must not be null");
        }
        this.name = name;
        this.value = value;
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return this.name;
    }

    /** {@inheritDoc} */
    @Override
    public String getValue() {
        return this.value;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        if (this.value == null) {
            return name;
        }
        final int len = this.name.length() + 1 + this.value.length();
        String buffer = this.name +
                "=" +
                this.value;
        return buffer;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(final Object object) {
        if (this == object) {
            return true;
        }
        if (object instanceof NameValuePair) {
            final BasicNameValuePair that = (BasicNameValuePair) object;
            return this.name.equals(that.name)
                    && equals(this.value, that.value);
        }
        return false;
    }

    private static boolean equals(final Object obj1, final Object obj2) {
        return Objects.equals(obj1, obj2);
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        int hash = HASH_SEED;
        hash = hashCode(hash, this.name);
        hash = hashCode(hash, this.value);
        return hash;
    }

    /** {@inheritDoc} */
    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    private static int hashCode(final int seed, final Object obj) {
        return hashCode(seed, obj != null ? obj.hashCode() : 0);
    }

    private static int hashCode(final int seed, final int hashcode) {
        return seed * HASH_OFFSET + hashcode;
    }
}
