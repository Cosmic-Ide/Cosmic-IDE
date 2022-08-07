/*
 * reserved comment block
 * DO NOT REMOVE OR ALTER!
 */
/*
 * Copyright 2004,2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.openjdk.com.sun.org.apache.xerces.internal.util;

/**
 * A structure that represents an error code, characterized by a domain and a message key.
 *
 * @author Naela Nissar, IBM
 */
final class XMLErrorCode {

    //
    // Data
    //

    /** error domain * */
    private String fDomain;

    /** message key * */
    private String fKey;

    /**
     * Constructs an XMLErrorCode with the given domain and key.
     *
     * @param domain The error domain.
     * @param key The key of the error message.
     */
    public XMLErrorCode(String domain, String key) {
        fDomain = domain;
        fKey = key;
    }

    /**
     * Convenience method to set the values of an XMLErrorCode.
     *
     * @param domain The error domain.
     * @param key The key of the error message.
     */
    public void setValues(String domain, String key) {
        fDomain = domain;
        fKey = key;
    }

    /**
     * Indicates whether some other object is equal to this XMLErrorCode.
     *
     * @param obj the object with which to compare.
     */
    public boolean equals(Object obj) {
        if (!(obj instanceof XMLErrorCode)) return false;
        XMLErrorCode err = (XMLErrorCode) obj;
        return (fDomain.equals(err.fDomain) && fKey.equals(err.fKey));
    }

    /**
     * Returns a hash code value for this XMLErrorCode.
     *
     * @return a hash code value for this XMLErrorCode.
     */
    public int hashCode() {
        return fDomain.hashCode() + fKey.hashCode();
    }
}
