/*
 * Copyright (c) 2015-2018 Angelo ZERR.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * <p>
 * SPDX-License-Identifier: EPL-2.0
 * <p>
 * Contributors:
 * Angelo Zerr <angelo.zerr@gmail.com> - initial API and implementation
 */
package io.github.rosemoe.sora.textmate.core.internal.parser.json;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

import io.github.rosemoe.sora.textmate.core.internal.parser.PList;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class JSONPListParser<T> {

    private final boolean theme;

    public JSONPListParser(boolean theme) {
        this.theme = theme;
    }

    public T parse(InputStream contents) throws Exception {
        PList<T> pList = new PList<T>(theme);
        JsonReader reader = new JsonReader(new InputStreamReader(contents, StandardCharsets.UTF_8));
        // reader.setLenient(true);
        boolean parsing = true;
        while (parsing) {
            JsonToken nextToken = reader.peek();
            switch (nextToken) {
                case BEGIN_ARRAY:
                    pList.startElement(null, "array", null, null);
                    reader.beginArray();
                    break;
                case END_ARRAY:
                    pList.endElement(null, "array", null);
                    reader.endArray();
                    break;
                case BEGIN_OBJECT:
                    pList.startElement(null, "dict", null, null);
                    reader.beginObject();
                    break;
                case END_OBJECT:
                    pList.endElement(null, "dict", null);
                    reader.endObject();
                    break;
                case NAME:
                    String lastName = reader.nextName();
                    pList.startElement(null, "key", null, null);
                    pList.characters(lastName.toCharArray(), 0, lastName.length());
                    pList.endElement(null, "key", null);
                    break;
                case NULL:
                    reader.nextNull();
                    break;
                case BOOLEAN:
                    reader.nextBoolean();
                    break;
                case NUMBER:
                    reader.nextLong();
                    break;
                case STRING:
                    String value = reader.nextString();
                    pList.startElement(null, "string", null, null);
                    pList.characters(value.toCharArray(), 0, value.length());
                    pList.endElement(null, "string", null);
                    break;
                case END_DOCUMENT:
                    parsing = false;
                    break;
                default:
                    break;
            }
        }
        reader.close();
        return pList.getResult();
    }
}
