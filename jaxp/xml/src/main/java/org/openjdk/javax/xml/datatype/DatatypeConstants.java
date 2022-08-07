/*
 * Copyright (c) 2004, 2006, Oracle and/or its affiliates. All rights reserved.
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

package org.openjdk.javax.xml.datatype;

import org.openjdk.javax.xml.XMLConstants;
import org.openjdk.javax.xml.namespace.QName;

/**
 * Utility class to contain basic Datatype values as constants.
 *
 * @author <a href="mailto:Jeff.Suttor@Sun.com">Jeff Suttor</a>
 * @since 1.5
 */
public final class DatatypeConstants {

    /** Private constructor to prevent instantiation. */
    private DatatypeConstants() {}

    /** Value for first month of year. */
    public static final int JANUARY = 1;

    /** Value for second month of year. */
    public static final int FEBRUARY = 2;

    /** Value for third month of year. */
    public static final int MARCH = 3;

    /** Value for fourth month of year. */
    public static final int APRIL = 4;

    /** Value for fifth month of year. */
    public static final int MAY = 5;

    /** Value for sixth month of year. */
    public static final int JUNE = 6;

    /** Value for seventh month of year. */
    public static final int JULY = 7;

    /** Value for eighth month of year. */
    public static final int AUGUST = 8;

    /** Value for ninth month of year. */
    public static final int SEPTEMBER = 9;

    /** Value for tenth month of year. */
    public static final int OCTOBER = 10;

    /** Value for eleven month of year. */
    public static final int NOVEMBER = 11;

    /** Value for twelve month of year. */
    public static final int DECEMBER = 12;

    /** Comparison result. */
    public static final int LESSER = -1;

    /** Comparison result. */
    public static final int EQUAL = 0;

    /** Comparison result. */
    public static final int GREATER = 1;

    /** Comparison result. */
    public static final int INDETERMINATE = 2;

    /** Designation that an "int" field is not set. */
    public static final int FIELD_UNDEFINED = Integer.MIN_VALUE;

    /** A constant that represents the years field. */
    public static final Field YEARS = new Field("YEARS", 0);

    /** A constant that represents the months field. */
    public static final Field MONTHS = new Field("MONTHS", 1);

    /** A constant that represents the days field. */
    public static final Field DAYS = new Field("DAYS", 2);

    /** A constant that represents the hours field. */
    public static final Field HOURS = new Field("HOURS", 3);

    /** A constant that represents the minutes field. */
    public static final Field MINUTES = new Field("MINUTES", 4);

    /** A constant that represents the seconds field. */
    public static final Field SECONDS = new Field("SECONDS", 5);

    /**
     * Type-safe enum class that represents six fields of the {@link Duration} class.
     *
     * @since 1.5
     */
    public static final class Field {

        /** <code>String</code> representation of <code>Field</code>. */
        private final String str;
        /**
         * Unique id of the field.
         *
         * <p>This value allows the {@link Duration} class to use switch statements to process
         * fields.
         */
        private final int id;

        /**
         * Construct a <code>Field</code> with specified values.
         *
         * @param str <code>String</code> representation of <code>Field</code>
         * @param id <code>int</code> representation of <code>Field</code>
         */
        private Field(final String str, final int id) {
            this.str = str;
            this.id = id;
        }
        /**
         * Returns a field name in English. This method is intended to be used for
         * debugging/diagnosis and not for display to end-users.
         *
         * @return a non-null valid String constant.
         */
        public String toString() {
            return str;
        }

        /**
         * Get id of this Field.
         *
         * @return Id of field.
         */
        public int getId() {
            return id;
        }
    }

    /** Fully qualified name for W3C XML Schema 1.0 datatype <code>dateTime</code>. */
    public static final QName DATETIME = new QName(XMLConstants.W3C_XML_SCHEMA_NS_URI, "dateTime");

    /** Fully qualified name for W3C XML Schema 1.0 datatype <code>time</code>. */
    public static final QName TIME = new QName(XMLConstants.W3C_XML_SCHEMA_NS_URI, "time");

    /** Fully qualified name for W3C XML Schema 1.0 datatype <code>date</code>. */
    public static final QName DATE = new QName(XMLConstants.W3C_XML_SCHEMA_NS_URI, "date");

    /** Fully qualified name for W3C XML Schema 1.0 datatype <code>gYearMonth</code>. */
    public static final QName GYEARMONTH =
            new QName(XMLConstants.W3C_XML_SCHEMA_NS_URI, "gYearMonth");

    /** Fully qualified name for W3C XML Schema 1.0 datatype <code>gMonthDay</code>. */
    public static final QName GMONTHDAY =
            new QName(XMLConstants.W3C_XML_SCHEMA_NS_URI, "gMonthDay");

    /** Fully qualified name for W3C XML Schema 1.0 datatype <code>gYear</code>. */
    public static final QName GYEAR = new QName(XMLConstants.W3C_XML_SCHEMA_NS_URI, "gYear");

    /** Fully qualified name for W3C XML Schema 1.0 datatype <code>gMonth</code>. */
    public static final QName GMONTH = new QName(XMLConstants.W3C_XML_SCHEMA_NS_URI, "gMonth");

    /** Fully qualified name for W3C XML Schema 1.0 datatype <code>gDay</code>. */
    public static final QName GDAY = new QName(XMLConstants.W3C_XML_SCHEMA_NS_URI, "gDay");

    /** Fully qualified name for W3C XML Schema datatype <code>duration</code>. */
    public static final QName DURATION = new QName(XMLConstants.W3C_XML_SCHEMA_NS_URI, "duration");

    /** Fully qualified name for XQuery 1.0 and XPath 2.0 datatype <code>dayTimeDuration</code>. */
    public static final QName DURATION_DAYTIME =
            new QName(XMLConstants.W3C_XPATH_DATATYPE_NS_URI, "dayTimeDuration");

    /**
     * Fully qualified name for XQuery 1.0 and XPath 2.0 datatype <code>yearMonthDuration</code>.
     */
    public static final QName DURATION_YEARMONTH =
            new QName(XMLConstants.W3C_XPATH_DATATYPE_NS_URI, "yearMonthDuration");

    /** W3C XML Schema max timezone offset is -14:00. Zone offset is in minutes. */
    public static final int MAX_TIMEZONE_OFFSET = -14 * 60;

    /** W3C XML Schema min timezone offset is +14:00. Zone offset is in minutes. */
    public static final int MIN_TIMEZONE_OFFSET = 14 * 60;
}
