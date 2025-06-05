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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import com.google.genai.errors.GenAiIOException;

/**
 * A class that can be serialized to JSON and deserialized from JSON.
 */
public abstract class JsonSerializable {

    static final ObjectMapper objectMapper = new ObjectMapper();

    static {
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_ABSENT);
        objectMapper.registerModule(new Jdk8Module());
        objectMapper.registerModule(new JavaTimeModule());
        // Disable writing dates as timestamps to use ISO-8601 string format for Instant
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        // Create a module for custom serializers/deserializers
        SimpleModule customModule = new SimpleModule();
        customModule.addSerializer(java.time.Duration.class, new CustomDurationSerializer());
        customModule.addDeserializer(java.time.Duration.class, new CustomDurationDeserializer());

        // Register JavaTimeModule for other java.time types *before* the custom module
        // This ensures our custom Duration handling takes precedence over the default one
        // provided by JavaTimeModule.
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.registerModule(customModule);

    }

    /**
     * Serializes an object to a Json string.
     */
    static String toJsonString(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new GenAiIOException("Failed to serialize the object to JSON.", e);
        }
    }

    /**
     * Serializes an object to a JsonNode.
     */
    static JsonNode toJsonNode(Object object) {
        return objectMapper.valueToTree(object);
    }

    /**
     * Deserializes a Json string to an object of the given type. This is for internal use only.
     */
    protected static <T extends JsonSerializable> T fromJsonString(
            String jsonString, Class<T> clazz) {
        try {
            return objectMapper.readValue(jsonString, clazz);
        } catch (JsonProcessingException e) {
            throw new GenAiIOException("Failed to deserialize the JSON string.", e);
        }
    }

    /**
     * Deserializes a JsonNode to an object of the given type.
     */
    static <T extends JsonSerializable> T fromJsonNode(JsonNode jsonNode, Class<T> clazz) {
        try {
            return objectMapper.treeToValue(jsonNode, clazz);
        } catch (JsonProcessingException e) {
            throw new GenAiIOException("Failed to deserialize the JSON node.", e);
        }
    }

    /**
     * Converts a Json string to a JsonNode.
     */
    static JsonNode stringToJsonNode(String string) {
        try {
            return objectMapper.readTree(string);
        } catch (JsonProcessingException e) {
            throw new GenAiIOException("Failed to parse the JSON string.", e);
        }
    }

    /**
     * Serializes the instance to a Json string.
     */
    public String toJson() {
        return toJsonString(this);
    }

    /**
     * Custom Jackson serializer for {@link java.time.Duration} to output "Xs" format.
     */
    static class CustomDurationSerializer extends JsonSerializer<java.time.Duration> {
        @Override
        public void serialize(
                java.time.Duration duration,
                JsonGenerator jsonGenerator,
                SerializerProvider serializerProvider)
                throws java.io.IOException {
            if (duration == null) {
                jsonGenerator.writeNull();
            } else {
                jsonGenerator.writeString(duration.getSeconds() + "s");
            }
        }
    }

    /**
     * Custom Jackson deserializer for {@link java.time.Duration} to parse "Xs" format.
     */
    static class CustomDurationDeserializer extends JsonDeserializer<java.time.Duration> {
        @Override
        public java.time.Duration deserialize(JsonParser p, DeserializationContext ctxt) throws java.io.IOException, JsonProcessingException {
            String value = p.getValueAsString();

            if (value == null || value.isEmpty()) {
                return null;
            }
            if (value.endsWith("s")) {
                String secondsPart = value.substring(0, value.length() - 1);
                try {
                    long seconds = Long.parseLong(secondsPart);
                    return java.time.Duration.ofSeconds(seconds);
                } catch (NumberFormatException e) {
                    throw ctxt.weirdStringException(value, java.time.Duration.class, "Cannot parse duration from string: " + value + ". Expected format 'Xs'.");
                }
            } else {
                // If it doesn't end with 's', delegate to the default deserializer.
                throw ctxt.weirdStringException(value, java.time.Duration.class, "Expected duration in format 'Xs', but got: " + value);
            }
        }
    }
}
