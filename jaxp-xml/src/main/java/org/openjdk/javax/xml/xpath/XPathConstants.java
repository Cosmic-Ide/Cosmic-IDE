/*
 * Copyright (c) 2003, 2005, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package org.openjdk.javax.xml.xpath;

import org.openjdk.javax.xml.namespace.QName;

/**
 * XPath constants.
 *
 * @author <a href="mailto:Norman.Walsh@Sun.COM">Norman Walsh</a>
 * @author <a href="mailto:Jeff.Suttor@Sun.COM">Jeff Suttor</a>
 * @see <a href="http://www.w3.org/TR/xpath">XML Path Language (XPath) Version 1.0</a>
 * @since 1.5
 */
public class XPathConstants {

    /** Private constructor to prevent instantiation. */
    private XPathConstants() {}

    /**
     * The XPath 1.0 number data type.
     *
     * <p>Maps to Java {@link Double}.
     */
    public static final QName NUMBER = new QName("http://www.w3.org/1999/XSL/Transform", "NUMBER");

    /**
     * The XPath 1.0 string data type.
     *
     * <p>Maps to Java {@link String}.
     */
    public static final QName STRING = new QName("http://www.w3.org/1999/XSL/Transform", "STRING");

    /**
     * The XPath 1.0 boolean data type.
     *
     * <p>Maps to Java {@link Boolean}.
     */
    public static final QName BOOLEAN =
            new QName("http://www.w3.org/1999/XSL/Transform", "BOOLEAN");

    /**
     * The XPath 1.0 NodeSet data type.
     *
     * <p>Maps to Java {@link org.w3c.dom.NodeList}.
     */
    public static final QName NODESET =
            new QName("http://www.w3.org/1999/XSL/Transform", "NODESET");

    /**
     * The XPath 1.0 NodeSet data type.
     *
     * <p>Maps to Java {@link org.w3c.dom.Node}.
     */
    public static final QName NODE = new QName("http://www.w3.org/1999/XSL/Transform", "NODE");

    /** The URI for the DOM object model, "http://java.sun.com/jaxp/xpath/dom". */
    public static final String DOM_OBJECT_MODEL = "http://java.sun.com/jaxp/xpath/dom";
}
