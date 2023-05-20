/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Foobar. If not, see <https://www.gnu.org/licenses/>.
 */

package com.intellij.openapi.util;

import static javax.xml.stream.XMLStreamConstants.CHARACTERS;
import static javax.xml.stream.XMLStreamConstants.COMMENT;
import static javax.xml.stream.XMLStreamConstants.DTD;
import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;
import static javax.xml.stream.XMLStreamConstants.PROCESSING_INSTRUCTION;
import static javax.xml.stream.XMLStreamConstants.SPACE;
import static javax.xml.stream.XMLStreamConstants.START_DOCUMENT;
import static javax.xml.stream.XMLStreamConstants.START_ELEMENT;

import org.jdom.AttributeType;
import org.jdom.Element;
import org.jdom.Namespace;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

public class SafeStAXStreamBuilderWrapper {

    public static final SafeJdomFactory FACTORY = new SafeJdomFactory.BaseSafeJdomFactory();

    public static Element build(
            XMLStreamReader stream,
            boolean isIgnoreBoundaryWhitespace,
            boolean isNsSupported,
            SafeJdomFactory factory)
            throws XMLStreamException {
        int state = stream.getEventType();
        if (state != START_DOCUMENT) {
            throw new XMLStreamException("beginning");
        }

        Element rootElement = null;
        while (state != END_ELEMENT) {
            switch (state) {
                case START_DOCUMENT:
                case SPACE:
                case CHARACTERS:
                case COMMENT:
                case PROCESSING_INSTRUCTION:
                case DTD:
                    break;

                case START_ELEMENT:
                    rootElement = processElement(stream, isNsSupported, factory);
                    break;

                default:
                    throw new XMLStreamException("Unexpected");
            }

            if (stream.hasNext()) {
                state = stream.next();
            } else {
                throw new XMLStreamException("Unexpected");
            }
        }

        if (rootElement == null) {
            return new Element("empty");
        }
        return rootElement;
    }

    public static Element processElement(
            XMLStreamReader reader, boolean isNsSupported, SafeJdomFactory factory) {
        Element element =
                factory.element(
                        reader.getLocalName(),
                        isNsSupported
                                ? Namespace.getNamespace(
                                reader.getPrefix(), reader.getNamespaceURI())
                                : Namespace.NO_NAMESPACE);
        // handle attributes
        for (int i = 0, len = reader.getAttributeCount(); i < len; i++) {
            element.setAttribute(
                    factory.attribute(
                            reader.getAttributeLocalName(i),
                            reader.getAttributeValue(i),
                            AttributeType.valueOf(reader.getAttributeType(i)),
                            isNsSupported
                                    ? Namespace.getNamespace(
                                    reader.getAttributePrefix(i), reader.getNamespaceURI())
                                    : Namespace.NO_NAMESPACE));
        }

        if (isNsSupported) {
            for (int i = 0, len = reader.getNamespaceCount(); i < len; i++) {
                element.addNamespaceDeclaration(
                        Namespace.getNamespace(
                                reader.getAttributePrefix(i), reader.getNamespaceURI(i)));
            }
        }

        return element;
    }
}
