/**
 * Copyright (c) 2022 Sebastian Thomschke and others.
 *
 * <p>This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * <p>SPDX-License-Identifier: EPL-2.0
 *
 * <p>Contributors: Sebastian Thomschke - initial implementation
 */
package io.github.rosemoe.sora.textmate.core.internal.parser.yaml;

import io.github.rosemoe.sora.textmate.core.internal.parser.PList;

import org.xml.sax.SAXException;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/** Parses TextMate Grammar file in YAML format. */
public final class YAMLPListParser<T> {

    private final boolean theme;

    public YAMLPListParser(boolean theme) {
        this.theme = theme;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void addListToPList(final PList<T> pList, final List<Object> list) throws SAXException {
        pList.startElement(null, "array", null, null);

        for (final Object item : list) {
            if (item instanceof List) {
                addListToPList(pList, (List) item);
            } else if (item instanceof Map) {
                addMapToPList(pList, (Map) item);
            } else {
                addStringToPList(pList, item.toString());
            }
        }

        pList.endElement(null, "array", null);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void addMapToPList(final PList<T> pList, final Map<String, Object> map)
            throws SAXException {
        pList.startElement(null, "dict", null, null);

        for (final Entry<String, Object> entry : map.entrySet()) {
            pList.startElement(null, "key", null, null);
            pList.characters(entry.getKey());
            pList.endElement(null, "key", null);
            if (entry.getValue() instanceof List) {
                addListToPList(pList, (List) entry.getValue());
            } else if (entry.getValue() instanceof Map) {
                addMapToPList(pList, (Map) entry.getValue());
            } else {
                addStringToPList(pList, castNonNull(entry.getValue()).toString());
            }
        }

        pList.endElement(null, "dict", null);
    }

    private void addStringToPList(final PListContentHandler<T> pList, final String value)
            throws SAXException {
        pList.startElement(null, "string", null, null);
        pList.characters(value);
        pList.endElement(null, "string", null);
    }

    @Override
    public T parse(final InputStream contents) throws SAXException, YAMLException {
        final var pList = new PList<T>(theme);
        pList.startElement(null, "plist", null, null);
        addMapToPList(pList, new Yaml().loadAs(contents, Map.class));
        pList.endElement(null, "plist", null);
        return pList.getResult();
    }
}
