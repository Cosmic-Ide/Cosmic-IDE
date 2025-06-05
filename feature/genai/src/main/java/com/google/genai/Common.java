/*
 * Copyright 2025 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.genai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import org.jspecify.annotations.Nullable;

import com.google.genai.errors.GenAiIOException;

/**
 * Common utility methods for the GenAI SDK.
 */
final class Common {

    private Common() {
    }

    /**
     * Sets the value of an object by a path.
     *
     * <p>setValueByPath({}, ['a', 'b'], v) -> {'a': {'b': v}}
     *
     * <p>setValueByPath({}, ['a', 'b[]', c], [v1, v2]) -> {'a': {'b': [{'c': v1}, {'c': v2}]}}
     *
     * <p>setValueByPath({'a': {'b':[{'c': v1}, {'c': v2}]}}, ['a', 'b[]', 'd'], v3) -> {'a': {'b':
     * [{'c': v1, 'd': v3}, {'c': v2,'d': v3}]}}
     */
    static void setValueByPath(ObjectNode jsonObject, String[] path, Object value) {
        if (path == null || path.length == 0) {
            throw new IllegalArgumentException("Path cannot be empty.");
        }
        if (jsonObject == null) {
            throw new IllegalArgumentException("JsonObject cannot be null.");
        }

        ObjectNode currentObject = jsonObject;
        for (int i = 0; i < path.length - 1; i++) {
            String key = path[i];

            if (key.endsWith("[]")) {
                String keyName = key.substring(0, key.length() - 2);
                if (!currentObject.has(keyName)) {
                    currentObject.putArray(keyName);
                }
                ArrayNode arrayNode = (ArrayNode) currentObject.get(keyName);
                if (value instanceof List) {
                    List<?> listValue = (List<?>) value;
                    if (arrayNode.size() != listValue.size()) {
                        arrayNode.removeAll();
                        for (int j = 0; j < listValue.size(); j++) {
                            arrayNode.addObject();
                        }
                    }
                    for (int j = 0; j < arrayNode.size(); j++) {
                        setValueByPath(
                                (ObjectNode) arrayNode.get(j),
                                Arrays.copyOfRange(path, i + 1, path.length),
                                listValue.get(j));
                    }
                } else {
                    if (arrayNode.size() == 0) {
                        arrayNode.addObject();
                    }
                    for (int j = 0; j < arrayNode.size(); j++) {
                        setValueByPath(
                                (ObjectNode) arrayNode.get(j), Arrays.copyOfRange(path, i + 1, path.length), value);
                    }
                }
                return;
            } else if (key.endsWith("[0]")) {
                String keyName = key.substring(0, key.length() - 3);
                if (!currentObject.has(keyName)) {
                    currentObject.putArray(keyName).addObject();
                }
                currentObject = (ObjectNode) ((ArrayNode) currentObject.get(keyName)).get(0);
            } else {
                if (!currentObject.has(key)) {
                    currentObject.putObject(key);
                }
                currentObject = (ObjectNode) currentObject.get(key);
            }
        }

        currentObject.put(path[path.length - 1], JsonSerializable.toJsonNode(value));
    }

    /**
     * Gets the value of an object by a path.
     *
     * <p>getValueByPath({'a': {'b': v}}, ['a', 'b']) -> v
     *
     * <p>getValueByPath({'a': {'b': [{'c': v1}, {'c': v2}]}}, ['a', 'b[]', 'c']) -> [v1, v2]
     */
    static @Nullable Object getValueByPath(JsonNode object, String[] keys) {
        if (object == null || keys == null) {
            return null;
        }
        if (keys.length == 1 && keys[0].equals("_self")) {
            return object;
        }

        JsonNode currentObject = object;
        for (int i = 0; i < keys.length; i++) {
            String key = keys[i];

            if (currentObject == null) {
                return null;
            }

            if (key.endsWith("[]")) {
                String keyName = key.substring(0, key.length() - 2);
                if (currentObject.isObject()
                        && ((ObjectNode) currentObject).has(keyName)
                        && ((ObjectNode) currentObject).get(keyName).isArray()) {
                    ArrayNode arrayNode = (ArrayNode) ((ObjectNode) currentObject).get(keyName);
                    if (keys.length - 1 == i) {
                        return arrayNode;
                    }
                    ArrayNode result = JsonSerializable.objectMapper.createArrayNode();
                    for (JsonNode element : arrayNode) {
                        JsonNode node =
                                (JsonNode) getValueByPath(element, Arrays.copyOfRange(keys, i + 1, keys.length));
                        if (node != null) {
                            result.add(node);
                        }
                    }
                    return result;
                } else {
                    return null;
                }
            } else if (key.endsWith("[0]")) {
                String keyName = key.substring(0, key.length() - 3);
                if (currentObject.isObject()
                        && ((ObjectNode) currentObject).has(keyName)
                        && ((ObjectNode) currentObject).get(keyName).isArray()
                        && ((ArrayNode) ((ObjectNode) currentObject).get(keyName)).size() > 0) {
                    currentObject = ((ArrayNode) ((ObjectNode) currentObject).get(keyName)).get(0);
                } else {
                    return null;
                }
            } else {
                if (currentObject.isObject() && ((ObjectNode) currentObject).has(key)) {
                    currentObject = ((ObjectNode) currentObject).get(key);
                } else {
                    return null;
                }
            }
        }

        return currentObject;
    }

    static String formatMap(String template, JsonNode data) {
        if (data == null) {
            return template;
        }

        Iterator<Map.Entry<String, JsonNode>> fields = data.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            String key = field.getKey();
            String placeholder = "{" + key + "}";
            if (template.contains(placeholder)) {
                template = template.replace(placeholder, data.get(key).asText());
            }
        }
        return template;
    }

    static boolean isZero(Object obj) {
        if (obj == null) {
            return true;
        }

        if (obj instanceof Number) {
            Number num = (Number) obj;
            return num.doubleValue() == 0.0;
        } else if (obj instanceof Character) {
            Character ch = (Character) obj;
            return ch == '\0';
        } else if (obj instanceof Boolean) {
            Boolean bool = (Boolean) obj;
            return !bool;
        }

        return false;
    }

    /**
     * Converts a Jackson ObjectNode into a URL-encoded query string. Assumes values are simple types
     * (text, number, boolean, or null) that can be represented as a single string.
     *
     * @param paramsNode The ObjectNode containing the parameters to encode.
     * @return A URL-encoded string (e.g., "key1=value1&key2=value2").
     */
    public static String urlEncode(ObjectNode paramsNode) {
        if (paramsNode == null || paramsNode.size() == 0) {
            return "";
        }

        StringJoiner queryBuilder = new StringJoiner("&");
        String utf8 = StandardCharsets.UTF_8.name();

        try {
            Iterator<Map.Entry<String, JsonNode>> fields = paramsNode.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                String encodedKey = URLEncoder.encode(entry.getKey(), utf8);
                JsonNode valueNode = entry.getValue();

                if (valueNode.isNull()) {
                    queryBuilder.add(encodedKey + "=");
                } else {
                    String encodedValue = URLEncoder.encode(valueNode.asText(""), utf8);
                    queryBuilder.add(encodedKey + "=" + encodedValue);
                }
            }
        } catch (UnsupportedEncodingException e) {
            throw new GenAiIOException("UTF-8 encoding not supported", e);
        }
        return queryBuilder.toString();
    }
}
