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
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.genai.errors.GenAiIOException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;
import java.util.NoSuchElementException;

import org.apache.hc.core5.http.HttpEntity;

/**
 * An iterable of datatype objects.
 */
public class ResponseStream<T extends JsonSerializable> implements Iterable<T>, AutoCloseable {

    private static final Logger logger = Logger.getLogger(ChatBase.class.getName());
    private final ResponseStreamIterator iterator;
    private final ApiResponse response;
    private final BufferedReader reader;
    boolean recordingHistory = false;
    List<T> history = new ArrayList<>();
    Chat chatSession = null;
    AsyncChat asyncChatSession = null;
    public ResponseStream(Class<T> clazz, ApiResponse response, Object obj, String converterName) {
        HttpEntity entity = response.getEntity();
        InputStream responseStream;
        try {
            responseStream = entity.getContent();
        } catch (IOException e) {
            throw new GenAiIOException("Failed to read HTTP response.", e);
        }
        this.reader = new BufferedReader(new InputStreamReader(responseStream, StandardCharsets.UTF_8));
        this.iterator = new ResponseStreamIterator(clazz, this.reader, obj, converterName);
        this.response = response;
    }

    @Override
    public Iterator<T> iterator() {
        return iterator;
    }

    @Override
    public void close() {
        try {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    throw new GenAiIOException("Failed to close the response stream.", e);
                }
            }
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }

    boolean isConsumed() {
        return iterator.consumed;
    }

    /**
     * Iterator for the ResponseStream.
     */
    class ResponseStreamIterator implements Iterator<T> {
        private final BufferedReader reader;
        private final Class<T> clazz;
        private final Object obj;
        private final Method converter;
        private String nextJson;
        private boolean consumed = false;

        ResponseStreamIterator(
                Class<T> clazz, BufferedReader reader, Object obj, String converterName) {
            this.reader = reader;
            this.clazz = clazz;
            this.nextJson = readNextJson();
            this.obj = obj;
            try {
                this.converter =
                        obj.getClass()
                                .getDeclaredMethod(
                                        converterName, ApiClient.class, JsonNode.class, ObjectNode.class);
            } catch (NoSuchMethodException e) {
                throw new IllegalStateException("Failed to find converter method " + converterName, e);
            }
        }

        @Override
        public boolean hasNext() {
            if (nextJson == null) {
                consumed = true;
                if (recordingHistory) {
                    try {
                        if (chatSession != null) {
                            chatSession.checkStreamResponseAndUpdateHistory();
                            recordingHistory = false;
                        }
                        if (asyncChatSession != null) {
                            asyncChatSession.checkStreamResponseAndUpdateHistory();
                            recordingHistory = false;
                        }
                    } catch (IllegalStateException e) {
                        logger.info(
                                "Error while updating history: " + e.getMessage() + ". Continuing execution...");
                    }
                }
            }
            return nextJson != null;
        }

        @Override
        public T next() {
            if (!hasNext()) {
                throw new NoSuchElementException("No more JSON objects in the stream");
            }
            String currentJson = nextJson;
            nextJson = readNextJson();
            try {
                JsonNode currentJsonNode = JsonSerializable.stringToJsonNode(currentJson);
                currentJsonNode = (JsonNode) converter.invoke(obj, null, currentJsonNode, null);
                if (recordingHistory) {
                    T response = JsonSerializable.fromJsonNode(currentJsonNode, clazz);
                    history.add(response);
                    return response;
                }
                return JsonSerializable.fromJsonNode(currentJsonNode, clazz);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new IllegalStateException("Failed to convert JSON object " + currentJson, e);
            }
        }

        private String readNextJson() {
            // Streaming API returns in the following format:
            // data: {contents: ...}
            // \n
            // data: {contents: ...}
            // \n
            // ...
            try {
                String line = reader.readLine();
                if (line == null) {
                    return null;
                } else if (line.length() == 0) {
                    return readNextJson();
                } else {
                    return line.substring("data: ".length());
                }
            } catch (IOException e) {
                throw new GenAiIOException("Failed to read next JSON object from the stream", e);
            }
        }
    }
}
