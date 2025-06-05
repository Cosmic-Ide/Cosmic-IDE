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

import static com.google.common.base.Preconditions.checkNotNull;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.ImmutableList;
import com.google.genai.types.Blob;
import com.google.genai.types.Content;
import com.google.genai.types.File;
import com.google.genai.types.FunctionDeclaration;
import com.google.genai.types.GeneratedVideo;
import com.google.genai.types.Part;
import com.google.genai.types.PrebuiltVoiceConfig;
import com.google.genai.types.Schema;
import com.google.genai.types.SpeechConfig;
import com.google.genai.types.Tool;
import com.google.genai.types.Video;
import com.google.genai.types.VoiceConfig;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;

/**
 * Transformers for GenAI SDK.
 */
final class Transformers {
    private static final Logger logger = Logger.getLogger(Transformers.class.getName());

    private Transformers() {
    }

    /**
     * Transforms a model name to the correct format for the API.
     *
     * @param apiClient the API client to use for transformation
     * @param origin    the model name to transform, can only be a string
     * @return the transformed model name
     */
    public static String tModel(ApiClient apiClient, Object origin) {
        String model;
        if (origin == null) {
            return null;
        } else if (origin instanceof String) {
            model = (String) origin;
        } else if (origin instanceof JsonNode) {
            model = JsonSerializable.toJsonString((JsonNode) origin);
            model = model.replace("\"", "");
        } else {
            throw new IllegalArgumentException("Unsupported model type: " + origin.getClass());
        }
        if (model.startsWith("models/") || model.startsWith("tunedModels/")) {
            return model;
        } else {
            return "models/" + model;
        }
    }

    /**
     * Determines the appropriate models URL based on the API client type and whether base models are
     * requested.
     *
     * @param apiClient  the API client to use for transformation
     * @param baseModels true or null if base models are requested, false otherwise.
     * @return The transformed model name
     */
    @SuppressWarnings("PatternMatchingInstanceof")
    public static String tModelsUrl(ApiClient apiClient, @Nullable Object baseModels) {
        if (baseModels == null) {
            return "models";
        }
        if (!(baseModels instanceof JsonNode)) {
            throw new IllegalArgumentException("Unsupported response type: " + baseModels.getClass());
        }

        if (((JsonNode) baseModels).asBoolean()) {
            return "models";
        } else {
            return "tunedModels";
        }
    }

    /**
     * Extracts the models from the models.list response.
     *
     * <p>This is used in the converters.
     */
    @SuppressWarnings("PatternMatchingInstanceof")
    public static @Nullable JsonNode tExtractModels(ApiClient apiClient, Object origin) {
        if (origin == null) {
            return null;
        }
        if (!(origin instanceof JsonNode)) {
            throw new IllegalArgumentException("Unsupported response type: " + origin.getClass());
        }

        JsonNode response = (JsonNode) origin;
        JsonNode models = response.get("models");
        if (models != null) {
            return models;
        }
        models = response.get("tunedModels");
        if (models != null) {
            return models;
        }
        models = response.get("publisherModels");
        if (models != null) {
            return models;
        }
        if ((response.get("httpHeaders") == null) || (response.get("jsonPayload") != null)) {
            logger.warning("Cannot determine the models type for response: " + response.toPrettyString());
        }
        return JsonSerializable.objectMapper.createArrayNode();
    }

    /**
     * In other languages, this transformer is used for supporting union types. Java doesn't have
     * union types, so we define a dummy transformer here.
     *
     * <p>This is used in the converters.
     */
    public static Object tContents(ApiClient apiClient, Object origin) {
        return origin;
    }

    /**
     * Transforms a text string to a list of Content for the API.
     *
     * <p>Not intended to be used in converters.
     *
     * @param text the text string to transform
     * @return the transformed list of Content
     */
    public static ImmutableList<Content> tContents(String text) {
        checkNotNull(text, "text cannot be null");
        return ImmutableList.of(Content.fromParts(Part.fromText(text)));
    }

    /**
     * Transforms a Content to a list of Content for the API.
     *
     * <p>Not intended to be used in converters.
     *
     * @param content a {@link Content} to transform
     * @return the transformed list of Content
     */
    public static ImmutableList<Content> tContents(Content content) {
        checkNotNull(content, "text cannot be null");
        return ImmutableList.of(content);
    }

    /**
     * Transforms an object to a Content for the API.
     *
     * @param apiClient the API client to use for transformation
     * @param content   the object to transform, can be a string or Content
     * @return the transformed Content
     * @throws IllegalArgumentException if the object is not a supported type
     */
    public static Content tContent(ApiClient apiClient, Object content) {
        if (content == null) {
            return null;
        } else if (content instanceof String) {
            return Content.fromParts(Part.fromText((String) content));
        } else if (content instanceof Content) {
            return (Content) content;
        } else if (content instanceof JsonNode) {
            return JsonSerializable.fromJsonNode((JsonNode) content, Content.class);
        }

        throw new IllegalArgumentException("Unsupported content type: " + content.getClass());
    }

    /**
     * Transforms an object to a Schema for the API.
     */
    public static Schema tSchema(ApiClient apiClient, Object origin) {
        if (origin == null) {
            return null;
        } else if (origin instanceof Schema) {
            return (Schema) origin;
        } else if (origin instanceof JsonNode) {
            return JsonSerializable.fromJsonNode((JsonNode) origin, Schema.class);
        }
        throw new IllegalArgumentException("Unsupported schema type: " + origin.getClass());
    }

    public static SpeechConfig tSpeechConfig(ApiClient apiClient, Object speechConfig) {
        if (speechConfig == null) {
            return null;
        } else if (speechConfig instanceof String) {
            return SpeechConfig.builder()
                    .voiceConfig(
                            VoiceConfig.builder()
                                    .prebuiltVoiceConfig(
                                            PrebuiltVoiceConfig.builder().voiceName((String) speechConfig).build())
                                    .build())
                    .build();
        } else if (speechConfig instanceof SpeechConfig) {
            return (SpeechConfig) speechConfig;
        } else if (speechConfig instanceof JsonNode) {
            return JsonSerializable.fromJsonNode((JsonNode) speechConfig, SpeechConfig.class);
        }

        throw new IllegalArgumentException("Unsupported speechConfig type:" + speechConfig.getClass());
    }

    /**
     * Transforms a SpeechConfig object for the live API, validating it.
     *
     * @param apiClient the API client to use for transformation
     * @param origin    the object to transform, can be a SpeechConfig or a JsonNode
     * @return the transformed SpeechConfig
     * @throws IllegalArgumentException if the object is not a supported type or if
     *                                  multiSpeakerVoiceConfig is present.
     */
    public static @Nullable SpeechConfig tLiveSpeechConfig(ApiClient apiClient, Object origin) {
        SpeechConfig speechConfig;
        if (origin == null) {
            return null;
        } else if (origin instanceof SpeechConfig) {
            speechConfig = (SpeechConfig) origin;
        } else if (origin instanceof JsonNode) {
            speechConfig = JsonSerializable.fromJsonNode((JsonNode) origin, SpeechConfig.class);
        } else {
            throw new IllegalArgumentException("Unsupported speechConfig type:" + origin.getClass());
        }

        if (speechConfig.multiSpeakerVoiceConfig().isPresent()) {
            throw new IllegalArgumentException(
                    "multiSpeakerVoiceConfig is not supported in the live API.");
        }

        return speechConfig;
    }

    /**
     * In other languages, this transformer is used for supporting union types. Java doesn't have
     * union types, so we define a dummy transformer here. *
     *
     * <p>This is used in the converters.
     */
    public static Object tTools(ApiClient apiClient, Object origin) {
        return origin;
    }

    /**
     * Transforms an object to a Tool for the API.
     */
    public static Tool tTool(ApiClient apiClient, Object origin) {
        if (origin == null) {
            return null;
        } else if (origin instanceof Tool) {
            Tool tool = (Tool) origin;
            if (!tool.functions().isPresent()) {
                return tool;
            }
            List<FunctionDeclaration> combinedFunctionDeclarations = new ArrayList<>();
            for (Method method : tool.functions().get()) {
                combinedFunctionDeclarations.add(FunctionDeclaration.fromMethod(method));
            }
            if (tool.functionDeclarations().isPresent()) {
                combinedFunctionDeclarations.addAll(tool.functionDeclarations().get());
            }
            ObjectNode toolNode = JsonSerializable.objectMapper.valueToTree(tool);
            toolNode.remove("functions");
            toolNode.set(
                    "functionDeclarations",
                    JsonSerializable.objectMapper.valueToTree(combinedFunctionDeclarations));
            return JsonSerializable.fromJsonNode(toolNode, Tool.class);
        } else if (origin instanceof JsonNode) {
            // in case reflectMethods is present in the json node, call tTool to parse it and remove it
            // from the json node.
            return tTool(
                    apiClient,
                    JsonSerializable.objectMapper.convertValue(
                            (JsonNode) origin, new TypeReference<Tool>() {
                            }));
        }

        throw new IllegalArgumentException("Unsupported tool type: " + origin.getClass());
    }

    /**
     * Dummy Blobs transformer.
     */
    public static ArrayNode tBlobs(ApiClient apiClient, Object origin) {
        // 1. Check if the origin is a JsonNode
        if (!(origin instanceof JsonNode)) {
            // If origin is not a JsonNode, we create one from the object.
            origin = JsonSerializable.toJsonNode(origin);
        }

        JsonNode inputNode = (JsonNode) origin;
        // 2. Check if the input is already an array
        if (inputNode.isArray()) {
            // If it's already an array, return it as is.
            return (ArrayNode) inputNode;
        }

        // 2. If it's not an array, create a new array and add the input to it.
        ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode();
        arrayNode.add(JsonSerializable.toJsonNode(tBlob(apiClient, origin)));
        return arrayNode;
    }

    public static Blob tBlob(ApiClient apiClient, Object blob) {
        if (blob instanceof JsonNode) {
            blob =
                    JsonSerializable.objectMapper.convertValue((JsonNode) blob, new TypeReference<Blob>() {
                    });
        }

        if (blob instanceof Blob) {
            return (Blob) blob;
        } else {
            throw new IllegalArgumentException("Unsupported blob type: " + blob.getClass());
        }
    }

    /**
     * Transforms a blob to an image blob, validating its mime type.
     *
     * @param apiClient the API client to use for transformation
     * @param blob      the object to transform, can be a Blob or a dictionary.
     * @return the transformed Blob if it is an image.
     * @throws IllegalArgumentException if the blob is not an image.
     */
    public static Blob tImageBlob(ApiClient apiClient, Object blob) {
        Blob transformedBlob = tBlob(apiClient, blob);
        if (transformedBlob.mimeType().isPresent()
                && transformedBlob.mimeType().get().startsWith("image/")) {
            return transformedBlob;
        }
        throw new IllegalArgumentException(
                "Unsupported mime type for image blob: " + transformedBlob.mimeType().orElse("null"));
    }

    /**
     * Transforms a blob to an audio blob, validating its mime type.
     *
     * @param apiClient the API client to use for transformation
     * @param blob      the object to transform, can be a Blob or a dictionary.
     * @return the transformed Blob if it is an audio.
     * @throws IllegalArgumentException if the blob is not an audio.
     */
    public static Blob tAudioBlob(ApiClient apiClient, Object blob) {
        Blob transformedBlob = tBlob(apiClient, blob);
        if (transformedBlob.mimeType().isPresent()
                && transformedBlob.mimeType().get().startsWith("audio/")) {
            return transformedBlob;
        }
        throw new IllegalArgumentException(
                "Unsupported mime type for audio blob: " + transformedBlob.mimeType().orElse("null"));
    }

    /**
     * Dummy bytes transformer.
     */
    public static Object tBytes(Object origin) {
        // TODO(b/389133914): Remove dummy bytes converter.
        return origin;
    }

    /**
     * Transforms an object to a cached content name for the API.
     */
    public static String tCachedContentName(Object origin) {
        if (origin == null) {
            return null;
        } else if (origin instanceof String) {
            return getResourceName((String) origin, "cachedContents");
        } else if (origin instanceof JsonNode) {
            String cachedContentName = JsonSerializable.toJsonString((JsonNode) origin);
            cachedContentName = cachedContentName.replace("\"", "");
            return getResourceName(cachedContentName, "cachedContents");
        }

        throw new IllegalArgumentException(
                "Unsupported cached content name type: " + origin.getClass());
    }

    /**
     * Transforms an object to a list of Content for the embedding API.
     */
    @SuppressWarnings("unchecked")
    public static @Nullable List<Object> tContentsForEmbed(Object origin) {
        if (origin == null) {
            return null;
        }

        List<Content> contents;
        if (origin instanceof List) {
            contents = (List<Content>) origin;
        } else if (origin instanceof JsonNode) {
            contents =
                    JsonSerializable.objectMapper.convertValue(
                            (JsonNode) origin, new TypeReference<List<Content>>() {
                            });
        } else {
            throw new IllegalArgumentException("Unsupported contents type: " + origin.getClass());
        }

        return new ArrayList<>(contents);
    }

    /**
     * Transforms a model name to the correct format for the Caches API.
     *
     * @param apiClient the API client to use for transformation
     * @param origin    the model name to transform, can be a string or JsonNode
     * @return the transformed model name, or null if the input is null
     * @throws IllegalArgumentException if the object is not a supported type
     */
    public static @Nullable String tCachesModel(ApiClient apiClient, Object origin) {
        String model = tModel(apiClient, origin);
        if (model == null) {
            return null;
        }

        return model;
    }

    public static @Nullable String tFileName(ApiClient apiClient, Object origin) {
        String name = null;

        if (origin instanceof String) {
            name = (String) origin;
        } else if (origin instanceof File) {
            name =
                    ((File) origin)
                            .name()
                            .orElseThrow(() -> new IllegalArgumentException("File name is required."));
        } else if (origin instanceof Video) {
            name = ((Video) origin).uri().orElse(null);
            if (name == null) {
                return null;
            }
        } else if (origin instanceof GeneratedVideo) {
            Video video =
                    ((GeneratedVideo) origin)
                            .video()
                            .orElseThrow(() -> new IllegalArgumentException("Video is required."));
            name = video.uri().orElse(null);
            if (name == null) {
                return null;
            }
        } else if (origin instanceof TextNode) {
            name = ((TextNode) origin).textValue();
        } else {
            throw new IllegalArgumentException("Unsupported file name type: " + origin.getClass());
        }

        if (name.startsWith("https://")) {
            String suffix = name.split("files/")[1];
            Pattern pattern = Pattern.compile("[a-z0-9]+");
            Matcher matcher = pattern.matcher(suffix);
            if (matcher.find()) {
                name = matcher.group();
            } else {
                throw new IllegalArgumentException("Could not extract file name from " + name);
            }
        } else if (name.startsWith("files/")) {
            name = name.split("files/")[1];
        }

        return name;
    }

    /**
     * Formats a resource name given the resource name and resource prefix.
     */
    private static String getResourceName(
            String resourceName, String resourcePrefix) {
        if (resourceName.startsWith(resourcePrefix + "/")) {
            return resourceName;
        } else {
            return String.format("%s/%s", resourcePrefix, resourceName);
        }
    }
}
