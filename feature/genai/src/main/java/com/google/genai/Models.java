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

// Auto-generated code. Do not edit.

package com.google.genai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.genai.errors.GenAiIOException;
import com.google.genai.types.ComputeTokensConfig;
import com.google.genai.types.ComputeTokensParameters;
import com.google.genai.types.ComputeTokensResponse;
import com.google.genai.types.Content;
import com.google.genai.types.CountTokensConfig;
import com.google.genai.types.CountTokensParameters;
import com.google.genai.types.CountTokensResponse;
import com.google.genai.types.DeleteModelConfig;
import com.google.genai.types.DeleteModelParameters;
import com.google.genai.types.DeleteModelResponse;
import com.google.genai.types.EditImageConfig;
import com.google.genai.types.EditImageParameters;
import com.google.genai.types.EditImageResponse;
import com.google.genai.types.EmbedContentConfig;
import com.google.genai.types.EmbedContentParameters;
import com.google.genai.types.EmbedContentResponse;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentParameters;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.GenerateImagesConfig;
import com.google.genai.types.GenerateImagesParameters;
import com.google.genai.types.GenerateImagesResponse;
import com.google.genai.types.GenerateVideosConfig;
import com.google.genai.types.GenerateVideosOperation;
import com.google.genai.types.GenerateVideosParameters;
import com.google.genai.types.GeneratedImage;
import com.google.genai.types.GetModelConfig;
import com.google.genai.types.GetModelParameters;
import com.google.genai.types.HttpOptions;
import com.google.genai.types.Image;
import com.google.genai.types.ListModelsConfig;
import com.google.genai.types.ListModelsParameters;
import com.google.genai.types.ListModelsResponse;
import com.google.genai.types.Model;
import com.google.genai.types.Part;
import com.google.genai.types.ReferenceImage;
import com.google.genai.types.ReferenceImageAPI;
import com.google.genai.types.SafetyAttributes;
import com.google.genai.types.UpdateModelConfig;
import com.google.genai.types.UpdateModelParameters;
import com.google.genai.types.UpscaleImageAPIConfig;
import com.google.genai.types.UpscaleImageAPIParameters;
import com.google.genai.types.UpscaleImageConfig;
import com.google.genai.types.UpscaleImageResponse;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.logging.Logger;

import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;

/**
 * Provides methods for interacting with the available GenAI models. Instantiating this class is not
 * required. After instantiating a {@link Client}, access methods through
 * `client.models.methodName(...)` directly.
 */
public final class Models {
    private static final Logger logger = Logger.getLogger(Models.class.getName());
    final ApiClient apiClient;

    public Models(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    @ExcludeFromGeneratedCoverageReport
    ObjectNode videoMetadataToMldev(
            ApiClient apiClient, JsonNode fromObject, ObjectNode parentObject) {
        ObjectNode toObject = JsonSerializable.objectMapper.createObjectNode();
        if (Common.getValueByPath(fromObject, new String[]{"fps"}) != null) {
            Common.setValueByPath(
                    toObject, new String[]{"fps"}, Common.getValueByPath(fromObject, new String[]{"fps"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"endOffset"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"endOffset"},
                    Common.getValueByPath(fromObject, new String[]{"endOffset"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"startOffset"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"startOffset"},
                    Common.getValueByPath(fromObject, new String[]{"startOffset"}));
        }

        return toObject;
    }

    @ExcludeFromGeneratedCoverageReport
    ObjectNode blobToMldev(ApiClient apiClient, JsonNode fromObject, ObjectNode parentObject) {
        ObjectNode toObject = JsonSerializable.objectMapper.createObjectNode();
        if (!Common.isZero(Common.getValueByPath(fromObject, new String[]{"displayName"}))) {
            throw new IllegalArgumentException("displayName parameter is not supported in Gemini API.");
        }

        if (Common.getValueByPath(fromObject, new String[]{"data"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"data"},
                    Common.getValueByPath(fromObject, new String[]{"data"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"mimeType"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"mimeType"},
                    Common.getValueByPath(fromObject, new String[]{"mimeType"}));
        }

        return toObject;
    }

    @ExcludeFromGeneratedCoverageReport
    ObjectNode fileDataToMldev(ApiClient apiClient, JsonNode fromObject, ObjectNode parentObject) {
        ObjectNode toObject = JsonSerializable.objectMapper.createObjectNode();
        if (!Common.isZero(Common.getValueByPath(fromObject, new String[]{"displayName"}))) {
            throw new IllegalArgumentException("displayName parameter is not supported in Gemini API.");
        }

        if (Common.getValueByPath(fromObject, new String[]{"fileUri"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"fileUri"},
                    Common.getValueByPath(fromObject, new String[]{"fileUri"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"mimeType"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"mimeType"},
                    Common.getValueByPath(fromObject, new String[]{"mimeType"}));
        }

        return toObject;
    }

    @ExcludeFromGeneratedCoverageReport
    ObjectNode partToMldev(ApiClient apiClient, JsonNode fromObject, ObjectNode parentObject) {
        ObjectNode toObject = JsonSerializable.objectMapper.createObjectNode();
        if (Common.getValueByPath(fromObject, new String[]{"videoMetadata"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"videoMetadata"},
                    videoMetadataToMldev(
                            apiClient,
                            JsonSerializable.toJsonNode(
                                    Common.getValueByPath(fromObject, new String[]{"videoMetadata"})),
                            toObject));
        }

        if (Common.getValueByPath(fromObject, new String[]{"thought"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"thought"},
                    Common.getValueByPath(fromObject, new String[]{"thought"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"inlineData"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"inlineData"},
                    blobToMldev(
                            apiClient,
                            JsonSerializable.toJsonNode(
                                    Common.getValueByPath(fromObject, new String[]{"inlineData"})),
                            toObject));
        }

        if (Common.getValueByPath(fromObject, new String[]{"fileData"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"fileData"},
                    fileDataToMldev(
                            apiClient,
                            JsonSerializable.toJsonNode(
                                    Common.getValueByPath(fromObject, new String[]{"fileData"})),
                            toObject));
        }

        if (Common.getValueByPath(fromObject, new String[]{"thoughtSignature"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"thoughtSignature"},
                    Common.getValueByPath(fromObject, new String[]{"thoughtSignature"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"codeExecutionResult"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"codeExecutionResult"},
                    Common.getValueByPath(fromObject, new String[]{"codeExecutionResult"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"executableCode"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"executableCode"},
                    Common.getValueByPath(fromObject, new String[]{"executableCode"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"functionCall"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"functionCall"},
                    Common.getValueByPath(fromObject, new String[]{"functionCall"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"functionResponse"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"functionResponse"},
                    Common.getValueByPath(fromObject, new String[]{"functionResponse"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"text"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"text"},
                    Common.getValueByPath(fromObject, new String[]{"text"}));
        }

        return toObject;
    }

    @ExcludeFromGeneratedCoverageReport
    ObjectNode contentToMldev(ApiClient apiClient, JsonNode fromObject, ObjectNode parentObject) {
        ObjectNode toObject = JsonSerializable.objectMapper.createObjectNode();
        if (Common.getValueByPath(fromObject, new String[]{"parts"}) != null) {
            ArrayNode keyArray = (ArrayNode) Common.getValueByPath(fromObject, new String[]{"parts"});
            ObjectMapper objectMapper = new ObjectMapper();
            ArrayNode result = objectMapper.createArrayNode();

            for (JsonNode item : keyArray) {
                result.add(partToMldev(apiClient, JsonSerializable.toJsonNode(item), toObject));
            }
            Common.setValueByPath(toObject, new String[]{"parts"}, result);
        }

        if (Common.getValueByPath(fromObject, new String[]{"role"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"role"},
                    Common.getValueByPath(fromObject, new String[]{"role"}));
        }

        return toObject;
    }

    @ExcludeFromGeneratedCoverageReport
    ObjectNode schemaToMldev(ApiClient apiClient, JsonNode fromObject, ObjectNode parentObject) {
        ObjectNode toObject = JsonSerializable.objectMapper.createObjectNode();

        if (Common.getValueByPath(fromObject, new String[]{"anyOf"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"anyOf"},
                    Common.getValueByPath(fromObject, new String[]{"anyOf"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"default"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"default"},
                    Common.getValueByPath(fromObject, new String[]{"default"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"description"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"description"},
                    Common.getValueByPath(fromObject, new String[]{"description"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"enum"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"enum"},
                    Common.getValueByPath(fromObject, new String[]{"enum"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"example"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"example"},
                    Common.getValueByPath(fromObject, new String[]{"example"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"format"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"format"},
                    Common.getValueByPath(fromObject, new String[]{"format"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"items"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"items"},
                    Common.getValueByPath(fromObject, new String[]{"items"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"maxItems"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"maxItems"},
                    Common.getValueByPath(fromObject, new String[]{"maxItems"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"maxLength"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"maxLength"},
                    Common.getValueByPath(fromObject, new String[]{"maxLength"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"maxProperties"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"maxProperties"},
                    Common.getValueByPath(fromObject, new String[]{"maxProperties"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"maximum"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"maximum"},
                    Common.getValueByPath(fromObject, new String[]{"maximum"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"minItems"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"minItems"},
                    Common.getValueByPath(fromObject, new String[]{"minItems"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"minLength"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"minLength"},
                    Common.getValueByPath(fromObject, new String[]{"minLength"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"minProperties"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"minProperties"},
                    Common.getValueByPath(fromObject, new String[]{"minProperties"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"minimum"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"minimum"},
                    Common.getValueByPath(fromObject, new String[]{"minimum"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"nullable"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"nullable"},
                    Common.getValueByPath(fromObject, new String[]{"nullable"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"pattern"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"pattern"},
                    Common.getValueByPath(fromObject, new String[]{"pattern"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"properties"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"properties"},
                    Common.getValueByPath(fromObject, new String[]{"properties"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"propertyOrdering"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"propertyOrdering"},
                    Common.getValueByPath(fromObject, new String[]{"propertyOrdering"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"required"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"required"},
                    Common.getValueByPath(fromObject, new String[]{"required"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"title"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"title"},
                    Common.getValueByPath(fromObject, new String[]{"title"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"type"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"type"},
                    Common.getValueByPath(fromObject, new String[]{"type"}));
        }

        return toObject;
    }

    @ExcludeFromGeneratedCoverageReport
    ObjectNode modelSelectionConfigToMldev(
            ApiClient apiClient, JsonNode fromObject, ObjectNode parentObject) {
        ObjectNode toObject = JsonSerializable.objectMapper.createObjectNode();
        if (!Common.isZero(
                Common.getValueByPath(fromObject, new String[]{"featureSelectionPreference"}))) {
            throw new IllegalArgumentException(
                    "featureSelectionPreference parameter is not supported in Gemini API.");
        }

        return toObject;
    }

    @ExcludeFromGeneratedCoverageReport
    ObjectNode safetySettingToMldev(
            ApiClient apiClient, JsonNode fromObject, ObjectNode parentObject) {
        ObjectNode toObject = JsonSerializable.objectMapper.createObjectNode();
        if (!Common.isZero(Common.getValueByPath(fromObject, new String[]{"method"}))) {
            throw new IllegalArgumentException("method parameter is not supported in Gemini API.");
        }

        if (Common.getValueByPath(fromObject, new String[]{"category"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"category"},
                    Common.getValueByPath(fromObject, new String[]{"category"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"threshold"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"threshold"},
                    Common.getValueByPath(fromObject, new String[]{"threshold"}));
        }

        return toObject;
    }

    @ExcludeFromGeneratedCoverageReport
    ObjectNode functionDeclarationToMldev(
            ApiClient apiClient, JsonNode fromObject, ObjectNode parentObject) {
        ObjectNode toObject = JsonSerializable.objectMapper.createObjectNode();
        if (Common.getValueByPath(fromObject, new String[]{"behavior"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"behavior"},
                    Common.getValueByPath(fromObject, new String[]{"behavior"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"description"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"description"},
                    Common.getValueByPath(fromObject, new String[]{"description"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"name"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"name"},
                    Common.getValueByPath(fromObject, new String[]{"name"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"parameters"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"parameters"},
                    Common.getValueByPath(fromObject, new String[]{"parameters"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"response"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"response"},
                    Common.getValueByPath(fromObject, new String[]{"response"}));
        }

        return toObject;
    }

    @ExcludeFromGeneratedCoverageReport
    ObjectNode intervalToMldev(ApiClient apiClient, JsonNode fromObject, ObjectNode parentObject) {
        ObjectNode toObject = JsonSerializable.objectMapper.createObjectNode();
        if (Common.getValueByPath(fromObject, new String[]{"startTime"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"startTime"},
                    Common.getValueByPath(fromObject, new String[]{"startTime"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"endTime"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"endTime"},
                    Common.getValueByPath(fromObject, new String[]{"endTime"}));
        }

        return toObject;
    }

    @ExcludeFromGeneratedCoverageReport
    ObjectNode googleSearchToMldev(
            ApiClient apiClient, JsonNode fromObject, ObjectNode parentObject) {
        ObjectNode toObject = JsonSerializable.objectMapper.createObjectNode();
        if (Common.getValueByPath(fromObject, new String[]{"timeRangeFilter"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"timeRangeFilter"},
                    intervalToMldev(
                            apiClient,
                            JsonSerializable.toJsonNode(
                                    Common.getValueByPath(fromObject, new String[]{"timeRangeFilter"})),
                            toObject));
        }

        return toObject;
    }

    @ExcludeFromGeneratedCoverageReport
    ObjectNode dynamicRetrievalConfigToMldev(
            ApiClient apiClient, JsonNode fromObject, ObjectNode parentObject) {
        ObjectNode toObject = JsonSerializable.objectMapper.createObjectNode();
        if (Common.getValueByPath(fromObject, new String[]{"mode"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"mode"},
                    Common.getValueByPath(fromObject, new String[]{"mode"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"dynamicThreshold"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"dynamicThreshold"},
                    Common.getValueByPath(fromObject, new String[]{"dynamicThreshold"}));
        }

        return toObject;
    }

    @ExcludeFromGeneratedCoverageReport
    ObjectNode googleSearchRetrievalToMldev(
            ApiClient apiClient, JsonNode fromObject, ObjectNode parentObject) {
        ObjectNode toObject = JsonSerializable.objectMapper.createObjectNode();
        if (Common.getValueByPath(fromObject, new String[]{"dynamicRetrievalConfig"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"dynamicRetrievalConfig"},
                    dynamicRetrievalConfigToMldev(
                            apiClient,
                            JsonSerializable.toJsonNode(
                                    Common.getValueByPath(fromObject, new String[]{"dynamicRetrievalConfig"})),
                            toObject));
        }

        return toObject;
    }

    @ExcludeFromGeneratedCoverageReport
    ObjectNode enterpriseWebSearchToMldev(
            ApiClient apiClient, JsonNode fromObject, ObjectNode parentObject) {
        ObjectNode toObject = JsonSerializable.objectMapper.createObjectNode();

        return toObject;
    }

    @ExcludeFromGeneratedCoverageReport
    ObjectNode apiKeyConfigToMldev(
            ApiClient apiClient, JsonNode fromObject, ObjectNode parentObject) {
        ObjectNode toObject = JsonSerializable.objectMapper.createObjectNode();
        if (!Common.isZero(Common.getValueByPath(fromObject, new String[]{"apiKeyString"}))) {
            throw new IllegalArgumentException("apiKeyString parameter is not supported in Gemini API.");
        }

        return toObject;
    }

    @ExcludeFromGeneratedCoverageReport
    ObjectNode authConfigToMldev(ApiClient apiClient, JsonNode fromObject, ObjectNode parentObject) {
        ObjectNode toObject = JsonSerializable.objectMapper.createObjectNode();
        if (!Common.isZero(Common.getValueByPath(fromObject, new String[]{"apiKeyConfig"}))) {
            throw new IllegalArgumentException("apiKeyConfig parameter is not supported in Gemini API.");
        }

        if (Common.getValueByPath(fromObject, new String[]{"authType"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"authType"},
                    Common.getValueByPath(fromObject, new String[]{"authType"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"googleServiceAccountConfig"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"googleServiceAccountConfig"},
                    Common.getValueByPath(fromObject, new String[]{"googleServiceAccountConfig"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"httpBasicAuthConfig"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"httpBasicAuthConfig"},
                    Common.getValueByPath(fromObject, new String[]{"httpBasicAuthConfig"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"oauthConfig"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"oauthConfig"},
                    Common.getValueByPath(fromObject, new String[]{"oauthConfig"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"oidcConfig"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"oidcConfig"},
                    Common.getValueByPath(fromObject, new String[]{"oidcConfig"}));
        }

        return toObject;
    }

    @ExcludeFromGeneratedCoverageReport
    ObjectNode googleMapsToMldev(ApiClient apiClient, JsonNode fromObject, ObjectNode parentObject) {
        ObjectNode toObject = JsonSerializable.objectMapper.createObjectNode();
        if (!Common.isZero(Common.getValueByPath(fromObject, new String[]{"authConfig"}))) {
            throw new IllegalArgumentException("authConfig parameter is not supported in Gemini API.");
        }

        return toObject;
    }

    @ExcludeFromGeneratedCoverageReport
    ObjectNode urlContextToMldev(ApiClient apiClient, JsonNode fromObject, ObjectNode parentObject) {
        ObjectNode toObject = JsonSerializable.objectMapper.createObjectNode();

        return toObject;
    }

    @ExcludeFromGeneratedCoverageReport
    ObjectNode toolToMldev(ApiClient apiClient, JsonNode fromObject, ObjectNode parentObject) {
        ObjectNode toObject = JsonSerializable.objectMapper.createObjectNode();
        if (Common.getValueByPath(fromObject, new String[]{"functionDeclarations"}) != null) {
            ArrayNode keyArray =
                    (ArrayNode) Common.getValueByPath(fromObject, new String[]{"functionDeclarations"});
            ObjectMapper objectMapper = new ObjectMapper();
            ArrayNode result = objectMapper.createArrayNode();

            for (JsonNode item : keyArray) {
                result.add(
                        functionDeclarationToMldev(apiClient, JsonSerializable.toJsonNode(item), toObject));
            }
            Common.setValueByPath(toObject, new String[]{"functionDeclarations"}, result);
        }

        if (!Common.isZero(Common.getValueByPath(fromObject, new String[]{"retrieval"}))) {
            throw new IllegalArgumentException("retrieval parameter is not supported in Gemini API.");
        }

        if (Common.getValueByPath(fromObject, new String[]{"googleSearch"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"googleSearch"},
                    googleSearchToMldev(
                            apiClient,
                            JsonSerializable.toJsonNode(
                                    Common.getValueByPath(fromObject, new String[]{"googleSearch"})),
                            toObject));
        }

        if (Common.getValueByPath(fromObject, new String[]{"googleSearchRetrieval"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"googleSearchRetrieval"},
                    googleSearchRetrievalToMldev(
                            apiClient,
                            JsonSerializable.toJsonNode(
                                    Common.getValueByPath(fromObject, new String[]{"googleSearchRetrieval"})),
                            toObject));
        }

        if (!Common.isZero(Common.getValueByPath(fromObject, new String[]{"enterpriseWebSearch"}))) {
            throw new IllegalArgumentException(
                    "enterpriseWebSearch parameter is not supported in Gemini API.");
        }

        if (!Common.isZero(Common.getValueByPath(fromObject, new String[]{"googleMaps"}))) {
            throw new IllegalArgumentException("googleMaps parameter is not supported in Gemini API.");
        }

        if (Common.getValueByPath(fromObject, new String[]{"urlContext"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"urlContext"},
                    urlContextToMldev(
                            apiClient,
                            JsonSerializable.toJsonNode(
                                    Common.getValueByPath(fromObject, new String[]{"urlContext"})),
                            toObject));
        }

        if (Common.getValueByPath(fromObject, new String[]{"codeExecution"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"codeExecution"},
                    Common.getValueByPath(fromObject, new String[]{"codeExecution"}));
        }

        return toObject;
    }

    @ExcludeFromGeneratedCoverageReport
    ObjectNode functionCallingConfigToMldev(
            ApiClient apiClient, JsonNode fromObject, ObjectNode parentObject) {
        ObjectNode toObject = JsonSerializable.objectMapper.createObjectNode();
        if (Common.getValueByPath(fromObject, new String[]{"mode"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"mode"},
                    Common.getValueByPath(fromObject, new String[]{"mode"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"allowedFunctionNames"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"allowedFunctionNames"},
                    Common.getValueByPath(fromObject, new String[]{"allowedFunctionNames"}));
        }

        return toObject;
    }

    @ExcludeFromGeneratedCoverageReport
    ObjectNode latLngToMldev(ApiClient apiClient, JsonNode fromObject, ObjectNode parentObject) {
        ObjectNode toObject = JsonSerializable.objectMapper.createObjectNode();
        if (Common.getValueByPath(fromObject, new String[]{"latitude"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"latitude"},
                    Common.getValueByPath(fromObject, new String[]{"latitude"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"longitude"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"longitude"},
                    Common.getValueByPath(fromObject, new String[]{"longitude"}));
        }

        return toObject;
    }

    @ExcludeFromGeneratedCoverageReport
    ObjectNode retrievalConfigToMldev(
            ApiClient apiClient, JsonNode fromObject, ObjectNode parentObject) {
        ObjectNode toObject = JsonSerializable.objectMapper.createObjectNode();
        if (Common.getValueByPath(fromObject, new String[]{"latLng"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"latLng"},
                    latLngToMldev(
                            apiClient,
                            JsonSerializable.toJsonNode(
                                    Common.getValueByPath(fromObject, new String[]{"latLng"})),
                            toObject));
        }

        if (Common.getValueByPath(fromObject, new String[]{"languageCode"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"languageCode"},
                    Common.getValueByPath(fromObject, new String[]{"languageCode"}));
        }

        return toObject;
    }

    @ExcludeFromGeneratedCoverageReport
    ObjectNode toolConfigToMldev(ApiClient apiClient, JsonNode fromObject, ObjectNode parentObject) {
        ObjectNode toObject = JsonSerializable.objectMapper.createObjectNode();
        if (Common.getValueByPath(fromObject, new String[]{"functionCallingConfig"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"functionCallingConfig"},
                    functionCallingConfigToMldev(
                            apiClient,
                            JsonSerializable.toJsonNode(
                                    Common.getValueByPath(fromObject, new String[]{"functionCallingConfig"})),
                            toObject));
        }

        if (Common.getValueByPath(fromObject, new String[]{"retrievalConfig"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"retrievalConfig"},
                    retrievalConfigToMldev(
                            apiClient,
                            JsonSerializable.toJsonNode(
                                    Common.getValueByPath(fromObject, new String[]{"retrievalConfig"})),
                            toObject));
        }

        return toObject;
    }

    @ExcludeFromGeneratedCoverageReport
    ObjectNode prebuiltVoiceConfigToMldev(
            ApiClient apiClient, JsonNode fromObject, ObjectNode parentObject) {
        ObjectNode toObject = JsonSerializable.objectMapper.createObjectNode();
        if (Common.getValueByPath(fromObject, new String[]{"voiceName"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"voiceName"},
                    Common.getValueByPath(fromObject, new String[]{"voiceName"}));
        }

        return toObject;
    }

    @ExcludeFromGeneratedCoverageReport
    ObjectNode voiceConfigToMldev(ApiClient apiClient, JsonNode fromObject, ObjectNode parentObject) {
        ObjectNode toObject = JsonSerializable.objectMapper.createObjectNode();
        if (Common.getValueByPath(fromObject, new String[]{"prebuiltVoiceConfig"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"prebuiltVoiceConfig"},
                    prebuiltVoiceConfigToMldev(
                            apiClient,
                            JsonSerializable.toJsonNode(
                                    Common.getValueByPath(fromObject, new String[]{"prebuiltVoiceConfig"})),
                            toObject));
        }

        return toObject;
    }

    @ExcludeFromGeneratedCoverageReport
    ObjectNode speakerVoiceConfigToMldev(
            ApiClient apiClient, JsonNode fromObject, ObjectNode parentObject) {
        ObjectNode toObject = JsonSerializable.objectMapper.createObjectNode();
        if (Common.getValueByPath(fromObject, new String[]{"speaker"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"speaker"},
                    Common.getValueByPath(fromObject, new String[]{"speaker"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"voiceConfig"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"voiceConfig"},
                    voiceConfigToMldev(
                            apiClient,
                            JsonSerializable.toJsonNode(
                                    Common.getValueByPath(fromObject, new String[]{"voiceConfig"})),
                            toObject));
        }

        return toObject;
    }

    @ExcludeFromGeneratedCoverageReport
    ObjectNode multiSpeakerVoiceConfigToMldev(
            ApiClient apiClient, JsonNode fromObject, ObjectNode parentObject) {
        ObjectNode toObject = JsonSerializable.objectMapper.createObjectNode();
        if (Common.getValueByPath(fromObject, new String[]{"speakerVoiceConfigs"}) != null) {
            ArrayNode keyArray =
                    (ArrayNode) Common.getValueByPath(fromObject, new String[]{"speakerVoiceConfigs"});
            ObjectMapper objectMapper = new ObjectMapper();
            ArrayNode result = objectMapper.createArrayNode();

            for (JsonNode item : keyArray) {
                result.add(
                        speakerVoiceConfigToMldev(apiClient, JsonSerializable.toJsonNode(item), toObject));
            }
            Common.setValueByPath(toObject, new String[]{"speakerVoiceConfigs"}, result);
        }

        return toObject;
    }

    @ExcludeFromGeneratedCoverageReport
    ObjectNode speechConfigToMldev(
            ApiClient apiClient, JsonNode fromObject, ObjectNode parentObject) {
        ObjectNode toObject = JsonSerializable.objectMapper.createObjectNode();
        if (Common.getValueByPath(fromObject, new String[]{"voiceConfig"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"voiceConfig"},
                    voiceConfigToMldev(
                            apiClient,
                            JsonSerializable.toJsonNode(
                                    Common.getValueByPath(fromObject, new String[]{"voiceConfig"})),
                            toObject));
        }

        if (Common.getValueByPath(fromObject, new String[]{"multiSpeakerVoiceConfig"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"multiSpeakerVoiceConfig"},
                    multiSpeakerVoiceConfigToMldev(
                            apiClient,
                            JsonSerializable.toJsonNode(
                                    Common.getValueByPath(fromObject, new String[]{"multiSpeakerVoiceConfig"})),
                            toObject));
        }

        if (Common.getValueByPath(fromObject, new String[]{"languageCode"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"languageCode"},
                    Common.getValueByPath(fromObject, new String[]{"languageCode"}));
        }

        return toObject;
    }

    @ExcludeFromGeneratedCoverageReport
    ObjectNode thinkingConfigToMldev(
            ApiClient apiClient, JsonNode fromObject, ObjectNode parentObject) {
        ObjectNode toObject = JsonSerializable.objectMapper.createObjectNode();
        if (Common.getValueByPath(fromObject, new String[]{"includeThoughts"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"includeThoughts"},
                    Common.getValueByPath(fromObject, new String[]{"includeThoughts"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"thinkingBudget"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"thinkingBudget"},
                    Common.getValueByPath(fromObject, new String[]{"thinkingBudget"}));
        }

        return toObject;
    }

    @ExcludeFromGeneratedCoverageReport
    ObjectNode generateContentConfigToMldev(
            ApiClient apiClient, JsonNode fromObject, ObjectNode parentObject) {
        ObjectNode toObject = JsonSerializable.objectMapper.createObjectNode();

        if (Common.getValueByPath(fromObject, new String[]{"systemInstruction"}) != null) {
            Common.setValueByPath(
                    parentObject,
                    new String[]{"systemInstruction"},
                    contentToMldev(
                            apiClient,
                            JsonSerializable.toJsonNode(
                                    Transformers.tContent(
                                            this.apiClient,
                                            Common.getValueByPath(fromObject, new String[]{"systemInstruction"}))),
                            toObject));
        }

        if (Common.getValueByPath(fromObject, new String[]{"temperature"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"temperature"},
                    Common.getValueByPath(fromObject, new String[]{"temperature"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"topP"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"topP"},
                    Common.getValueByPath(fromObject, new String[]{"topP"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"topK"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"topK"},
                    Common.getValueByPath(fromObject, new String[]{"topK"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"candidateCount"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"candidateCount"},
                    Common.getValueByPath(fromObject, new String[]{"candidateCount"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"maxOutputTokens"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"maxOutputTokens"},
                    Common.getValueByPath(fromObject, new String[]{"maxOutputTokens"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"stopSequences"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"stopSequences"},
                    Common.getValueByPath(fromObject, new String[]{"stopSequences"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"responseLogprobs"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"responseLogprobs"},
                    Common.getValueByPath(fromObject, new String[]{"responseLogprobs"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"logprobs"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"logprobs"},
                    Common.getValueByPath(fromObject, new String[]{"logprobs"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"presencePenalty"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"presencePenalty"},
                    Common.getValueByPath(fromObject, new String[]{"presencePenalty"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"frequencyPenalty"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"frequencyPenalty"},
                    Common.getValueByPath(fromObject, new String[]{"frequencyPenalty"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"seed"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"seed"},
                    Common.getValueByPath(fromObject, new String[]{"seed"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"responseMimeType"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"responseMimeType"},
                    Common.getValueByPath(fromObject, new String[]{"responseMimeType"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"responseSchema"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"responseSchema"},
                    schemaToMldev(
                            apiClient,
                            JsonSerializable.toJsonNode(
                                    Transformers.tSchema(
                                            this.apiClient,
                                            Common.getValueByPath(fromObject, new String[]{"responseSchema"}))),
                            toObject));
        }

        if (!Common.isZero(Common.getValueByPath(fromObject, new String[]{"routingConfig"}))) {
            throw new IllegalArgumentException("routingConfig parameter is not supported in Gemini API.");
        }

        if (!Common.isZero(Common.getValueByPath(fromObject, new String[]{"modelSelectionConfig"}))) {
            throw new IllegalArgumentException(
                    "modelSelectionConfig parameter is not supported in Gemini API.");
        }

        if (Common.getValueByPath(fromObject, new String[]{"safetySettings"}) != null) {
            ArrayNode keyArray =
                    (ArrayNode) Common.getValueByPath(fromObject, new String[]{"safetySettings"});
            ObjectMapper objectMapper = new ObjectMapper();
            ArrayNode result = objectMapper.createArrayNode();

            for (JsonNode item : keyArray) {
                result.add(safetySettingToMldev(apiClient, JsonSerializable.toJsonNode(item), toObject));
            }
            Common.setValueByPath(parentObject, new String[]{"safetySettings"}, result);
        }

        if (Common.getValueByPath(fromObject, new String[]{"tools"}) != null) {
            ArrayNode keyArray =
                    (ArrayNode)
                            Transformers.tTools(
                                    this.apiClient, Common.getValueByPath(fromObject, new String[]{"tools"}));
            ObjectMapper objectMapper = new ObjectMapper();
            ArrayNode result = objectMapper.createArrayNode();

            for (JsonNode item : keyArray) {
                result.add(
                        toolToMldev(
                                apiClient,
                                JsonSerializable.toJsonNode(Transformers.tTool(this.apiClient, item)),
                                toObject));
            }
            Common.setValueByPath(parentObject, new String[]{"tools"}, result);
        }

        if (Common.getValueByPath(fromObject, new String[]{"toolConfig"}) != null) {
            Common.setValueByPath(
                    parentObject,
                    new String[]{"toolConfig"},
                    toolConfigToMldev(
                            apiClient,
                            JsonSerializable.toJsonNode(
                                    Common.getValueByPath(fromObject, new String[]{"toolConfig"})),
                            toObject));
        }

        if (!Common.isZero(Common.getValueByPath(fromObject, new String[]{"labels"}))) {
            throw new IllegalArgumentException("labels parameter is not supported in Gemini API.");
        }

        if (Common.getValueByPath(fromObject, new String[]{"cachedContent"}) != null) {
            Common.setValueByPath(
                    parentObject,
                    new String[]{"cachedContent"},
                    Transformers.tCachedContentName(
                            Common.getValueByPath(fromObject, new String[]{"cachedContent"})));
        }

        if (Common.getValueByPath(fromObject, new String[]{"responseModalities"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"responseModalities"},
                    Common.getValueByPath(fromObject, new String[]{"responseModalities"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"mediaResolution"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"mediaResolution"},
                    Common.getValueByPath(fromObject, new String[]{"mediaResolution"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"speechConfig"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"speechConfig"},
                    speechConfigToMldev(
                            apiClient,
                            JsonSerializable.toJsonNode(
                                    Transformers.tSpeechConfig(
                                            this.apiClient,
                                            Common.getValueByPath(fromObject, new String[]{"speechConfig"}))),
                            toObject));
        }

        if (!Common.isZero(Common.getValueByPath(fromObject, new String[]{"audioTimestamp"}))) {
            throw new IllegalArgumentException(
                    "audioTimestamp parameter is not supported in Gemini API.");
        }

        if (Common.getValueByPath(fromObject, new String[]{"thinkingConfig"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"thinkingConfig"},
                    thinkingConfigToMldev(
                            apiClient,
                            JsonSerializable.toJsonNode(
                                    Common.getValueByPath(fromObject, new String[]{"thinkingConfig"})),
                            toObject));
        }

        return toObject;
    }

    @ExcludeFromGeneratedCoverageReport
    ObjectNode generateContentParametersToMldev(
            ApiClient apiClient, JsonNode fromObject, ObjectNode parentObject) {
        ObjectNode toObject = JsonSerializable.objectMapper.createObjectNode();
        if (Common.getValueByPath(fromObject, new String[]{"model"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"_url", "model"},
                    Transformers.tModel(
                            this.apiClient, Common.getValueByPath(fromObject, new String[]{"model"})));
        }

        if (Common.getValueByPath(fromObject, new String[]{"contents"}) != null) {
            ArrayNode keyArray =
                    (ArrayNode)
                            Transformers.tContents(
                                    this.apiClient, Common.getValueByPath(fromObject, new String[]{"contents"}));
            ObjectMapper objectMapper = new ObjectMapper();
            ArrayNode result = objectMapper.createArrayNode();

            for (JsonNode item : keyArray) {
                result.add(contentToMldev(apiClient, JsonSerializable.toJsonNode(item), toObject));
            }
            Common.setValueByPath(toObject, new String[]{"contents"}, result);
        }

        if (Common.getValueByPath(fromObject, new String[]{"config"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"generationConfig"},
                    generateContentConfigToMldev(
                            apiClient,
                            JsonSerializable.toJsonNode(
                                    Common.getValueByPath(fromObject, new String[]{"config"})),
                            toObject));
        }

        return toObject;
    }

    @ExcludeFromGeneratedCoverageReport
    ObjectNode embedContentConfigToMldev(
            ApiClient apiClient, JsonNode fromObject, ObjectNode parentObject) {
        ObjectNode toObject = JsonSerializable.objectMapper.createObjectNode();

        if (Common.getValueByPath(fromObject, new String[]{"taskType"}) != null) {
            Common.setValueByPath(
                    parentObject,
                    new String[]{"requests[]", "taskType"},
                    Common.getValueByPath(fromObject, new String[]{"taskType"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"title"}) != null) {
            Common.setValueByPath(
                    parentObject,
                    new String[]{"requests[]", "title"},
                    Common.getValueByPath(fromObject, new String[]{"title"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"outputDimensionality"}) != null) {
            Common.setValueByPath(
                    parentObject,
                    new String[]{"requests[]", "outputDimensionality"},
                    Common.getValueByPath(fromObject, new String[]{"outputDimensionality"}));
        }

        if (!Common.isZero(Common.getValueByPath(fromObject, new String[]{"mimeType"}))) {
            throw new IllegalArgumentException("mimeType parameter is not supported in Gemini API.");
        }

        if (!Common.isZero(Common.getValueByPath(fromObject, new String[]{"autoTruncate"}))) {
            throw new IllegalArgumentException("autoTruncate parameter is not supported in Gemini API.");
        }

        return toObject;
    }

    @ExcludeFromGeneratedCoverageReport
    ObjectNode embedContentParametersToMldev(
            ApiClient apiClient, JsonNode fromObject, ObjectNode parentObject) {
        ObjectNode toObject = JsonSerializable.objectMapper.createObjectNode();
        if (Common.getValueByPath(fromObject, new String[]{"model"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"_url", "model"},
                    Transformers.tModel(
                            this.apiClient, Common.getValueByPath(fromObject, new String[]{"model"})));
        }

        if (Common.getValueByPath(fromObject, new String[]{"contents"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"requests[]", "content"},
                    Transformers.tContentsForEmbed(
                            Common.getValueByPath(fromObject, new String[]{"contents"})));
        }

        if (Common.getValueByPath(fromObject, new String[]{"config"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"config"},
                    embedContentConfigToMldev(
                            apiClient,
                            JsonSerializable.toJsonNode(
                                    Common.getValueByPath(fromObject, new String[]{"config"})),
                            toObject));
        }

        Common.setValueByPath(
                toObject,
                new String[]{"requests[]", "model"},
                Transformers.tModel(
                        this.apiClient, Common.getValueByPath(fromObject, new String[]{"model"})));
        return toObject;
    }

    @ExcludeFromGeneratedCoverageReport
    ObjectNode generateImagesConfigToMldev(
            ApiClient apiClient, JsonNode fromObject, ObjectNode parentObject) {
        ObjectNode toObject = JsonSerializable.objectMapper.createObjectNode();

        if (!Common.isZero(Common.getValueByPath(fromObject, new String[]{"outputGcsUri"}))) {
            throw new IllegalArgumentException("outputGcsUri parameter is not supported in Gemini API.");
        }

        if (!Common.isZero(Common.getValueByPath(fromObject, new String[]{"negativePrompt"}))) {
            throw new IllegalArgumentException(
                    "negativePrompt parameter is not supported in Gemini API.");
        }

        if (Common.getValueByPath(fromObject, new String[]{"numberOfImages"}) != null) {
            Common.setValueByPath(
                    parentObject,
                    new String[]{"parameters", "sampleCount"},
                    Common.getValueByPath(fromObject, new String[]{"numberOfImages"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"aspectRatio"}) != null) {
            Common.setValueByPath(
                    parentObject,
                    new String[]{"parameters", "aspectRatio"},
                    Common.getValueByPath(fromObject, new String[]{"aspectRatio"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"guidanceScale"}) != null) {
            Common.setValueByPath(
                    parentObject,
                    new String[]{"parameters", "guidanceScale"},
                    Common.getValueByPath(fromObject, new String[]{"guidanceScale"}));
        }

        if (!Common.isZero(Common.getValueByPath(fromObject, new String[]{"seed"}))) {
            throw new IllegalArgumentException("seed parameter is not supported in Gemini API.");
        }

        if (Common.getValueByPath(fromObject, new String[]{"safetyFilterLevel"}) != null) {
            Common.setValueByPath(
                    parentObject,
                    new String[]{"parameters", "safetySetting"},
                    Common.getValueByPath(fromObject, new String[]{"safetyFilterLevel"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"personGeneration"}) != null) {
            Common.setValueByPath(
                    parentObject,
                    new String[]{"parameters", "personGeneration"},
                    Common.getValueByPath(fromObject, new String[]{"personGeneration"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"includeSafetyAttributes"}) != null) {
            Common.setValueByPath(
                    parentObject,
                    new String[]{"parameters", "includeSafetyAttributes"},
                    Common.getValueByPath(fromObject, new String[]{"includeSafetyAttributes"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"includeRaiReason"}) != null) {
            Common.setValueByPath(
                    parentObject,
                    new String[]{"parameters", "includeRaiReason"},
                    Common.getValueByPath(fromObject, new String[]{"includeRaiReason"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"language"}) != null) {
            Common.setValueByPath(
                    parentObject,
                    new String[]{"parameters", "language"},
                    Common.getValueByPath(fromObject, new String[]{"language"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"outputMimeType"}) != null) {
            Common.setValueByPath(
                    parentObject,
                    new String[]{"parameters", "outputOptions", "mimeType"},
                    Common.getValueByPath(fromObject, new String[]{"outputMimeType"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"outputCompressionQuality"}) != null) {
            Common.setValueByPath(
                    parentObject,
                    new String[]{"parameters", "outputOptions", "compressionQuality"},
                    Common.getValueByPath(fromObject, new String[]{"outputCompressionQuality"}));
        }

        if (!Common.isZero(Common.getValueByPath(fromObject, new String[]{"addWatermark"}))) {
            throw new IllegalArgumentException("addWatermark parameter is not supported in Gemini API.");
        }

        if (!Common.isZero(Common.getValueByPath(fromObject, new String[]{"enhancePrompt"}))) {
            throw new IllegalArgumentException("enhancePrompt parameter is not supported in Gemini API.");
        }

        return toObject;
    }

    @ExcludeFromGeneratedCoverageReport
    ObjectNode generateImagesParametersToMldev(
            ApiClient apiClient, JsonNode fromObject, ObjectNode parentObject) {
        ObjectNode toObject = JsonSerializable.objectMapper.createObjectNode();
        if (Common.getValueByPath(fromObject, new String[]{"model"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"_url", "model"},
                    Transformers.tModel(
                            this.apiClient, Common.getValueByPath(fromObject, new String[]{"model"})));
        }

        if (Common.getValueByPath(fromObject, new String[]{"prompt"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"instances[0]", "prompt"},
                    Common.getValueByPath(fromObject, new String[]{"prompt"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"config"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"config"},
                    generateImagesConfigToMldev(
                            apiClient,
                            JsonSerializable.toJsonNode(
                                    Common.getValueByPath(fromObject, new String[]{"config"})),
                            toObject));
        }

        return toObject;
    }

    @ExcludeFromGeneratedCoverageReport
    ObjectNode getModelParametersToMldev(
            ApiClient apiClient, JsonNode fromObject, ObjectNode parentObject) {
        ObjectNode toObject = JsonSerializable.objectMapper.createObjectNode();
        if (Common.getValueByPath(fromObject, new String[]{"model"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"_url", "name"},
                    Transformers.tModel(
                            this.apiClient, Common.getValueByPath(fromObject, new String[]{"model"})));
        }

        if (Common.getValueByPath(fromObject, new String[]{"config"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"config"},
                    Common.getValueByPath(fromObject, new String[]{"config"}));
        }

        return toObject;
    }

    @ExcludeFromGeneratedCoverageReport
    ObjectNode listModelsConfigToMldev(
            ApiClient apiClient, JsonNode fromObject, ObjectNode parentObject) {
        ObjectNode toObject = JsonSerializable.objectMapper.createObjectNode();

        if (Common.getValueByPath(fromObject, new String[]{"pageSize"}) != null) {
            Common.setValueByPath(
                    parentObject,
                    new String[]{"_query", "pageSize"},
                    Common.getValueByPath(fromObject, new String[]{"pageSize"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"pageToken"}) != null) {
            Common.setValueByPath(
                    parentObject,
                    new String[]{"_query", "pageToken"},
                    Common.getValueByPath(fromObject, new String[]{"pageToken"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"filter"}) != null) {
            Common.setValueByPath(
                    parentObject,
                    new String[]{"_query", "filter"},
                    Common.getValueByPath(fromObject, new String[]{"filter"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"queryBase"}) != null) {
            Common.setValueByPath(
                    parentObject,
                    new String[]{"_url", "models_url"},
                    Transformers.tModelsUrl(
                            this.apiClient, Common.getValueByPath(fromObject, new String[]{"queryBase"})));
        }

        return toObject;
    }

    @ExcludeFromGeneratedCoverageReport
    ObjectNode listModelsParametersToMldev(
            ApiClient apiClient, JsonNode fromObject, ObjectNode parentObject) {
        ObjectNode toObject = JsonSerializable.objectMapper.createObjectNode();
        if (Common.getValueByPath(fromObject, new String[]{"config"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"config"},
                    listModelsConfigToMldev(
                            apiClient,
                            JsonSerializable.toJsonNode(
                                    Common.getValueByPath(fromObject, new String[]{"config"})),
                            toObject));
        }

        return toObject;
    }

    @ExcludeFromGeneratedCoverageReport
    ObjectNode updateModelConfigToMldev(
            ApiClient apiClient, JsonNode fromObject, ObjectNode parentObject) {
        ObjectNode toObject = JsonSerializable.objectMapper.createObjectNode();

        if (Common.getValueByPath(fromObject, new String[]{"displayName"}) != null) {
            Common.setValueByPath(
                    parentObject,
                    new String[]{"displayName"},
                    Common.getValueByPath(fromObject, new String[]{"displayName"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"description"}) != null) {
            Common.setValueByPath(
                    parentObject,
                    new String[]{"description"},
                    Common.getValueByPath(fromObject, new String[]{"description"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"defaultCheckpointId"}) != null) {
            Common.setValueByPath(
                    parentObject,
                    new String[]{"defaultCheckpointId"},
                    Common.getValueByPath(fromObject, new String[]{"defaultCheckpointId"}));
        }

        return toObject;
    }

    @ExcludeFromGeneratedCoverageReport
    ObjectNode updateModelParametersToMldev(
            ApiClient apiClient, JsonNode fromObject, ObjectNode parentObject) {
        ObjectNode toObject = JsonSerializable.objectMapper.createObjectNode();
        if (Common.getValueByPath(fromObject, new String[]{"model"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"_url", "name"},
                    Transformers.tModel(
                            this.apiClient, Common.getValueByPath(fromObject, new String[]{"model"})));
        }

        if (Common.getValueByPath(fromObject, new String[]{"config"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"config"},
                    updateModelConfigToMldev(
                            apiClient,
                            JsonSerializable.toJsonNode(
                                    Common.getValueByPath(fromObject, new String[]{"config"})),
                            toObject));
        }

        return toObject;
    }

    @ExcludeFromGeneratedCoverageReport
    ObjectNode deleteModelParametersToMldev(
            ApiClient apiClient, JsonNode fromObject, ObjectNode parentObject) {
        ObjectNode toObject = JsonSerializable.objectMapper.createObjectNode();
        if (Common.getValueByPath(fromObject, new String[]{"model"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"_url", "name"},
                    Transformers.tModel(
                            this.apiClient, Common.getValueByPath(fromObject, new String[]{"model"})));
        }

        if (Common.getValueByPath(fromObject, new String[]{"config"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"config"},
                    Common.getValueByPath(fromObject, new String[]{"config"}));
        }

        return toObject;
    }

    @ExcludeFromGeneratedCoverageReport
    ObjectNode countTokensConfigToMldev(
            ApiClient apiClient, JsonNode fromObject, ObjectNode parentObject) {
        ObjectNode toObject = JsonSerializable.objectMapper.createObjectNode();

        if (!Common.isZero(Common.getValueByPath(fromObject, new String[]{"systemInstruction"}))) {
            throw new IllegalArgumentException(
                    "systemInstruction parameter is not supported in Gemini API.");
        }

        if (!Common.isZero(Common.getValueByPath(fromObject, new String[]{"tools"}))) {
            throw new IllegalArgumentException("tools parameter is not supported in Gemini API.");
        }

        if (!Common.isZero(Common.getValueByPath(fromObject, new String[]{"generationConfig"}))) {
            throw new IllegalArgumentException(
                    "generationConfig parameter is not supported in Gemini API.");
        }

        return toObject;
    }

    @ExcludeFromGeneratedCoverageReport
    ObjectNode countTokensParametersToMldev(
            ApiClient apiClient, JsonNode fromObject, ObjectNode parentObject) {
        ObjectNode toObject = JsonSerializable.objectMapper.createObjectNode();
        if (Common.getValueByPath(fromObject, new String[]{"model"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"_url", "model"},
                    Transformers.tModel(
                            this.apiClient, Common.getValueByPath(fromObject, new String[]{"model"})));
        }

        if (Common.getValueByPath(fromObject, new String[]{"contents"}) != null) {
            ArrayNode keyArray =
                    (ArrayNode)
                            Transformers.tContents(
                                    this.apiClient, Common.getValueByPath(fromObject, new String[]{"contents"}));
            ObjectMapper objectMapper = new ObjectMapper();
            ArrayNode result = objectMapper.createArrayNode();

            for (JsonNode item : keyArray) {
                result.add(contentToMldev(apiClient, JsonSerializable.toJsonNode(item), toObject));
            }
            Common.setValueByPath(toObject, new String[]{"contents"}, result);
        }

        if (Common.getValueByPath(fromObject, new String[]{"config"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"config"},
                    countTokensConfigToMldev(
                            apiClient,
                            JsonSerializable.toJsonNode(
                                    Common.getValueByPath(fromObject, new String[]{"config"})),
                            toObject));
        }

        return toObject;
    }

    @ExcludeFromGeneratedCoverageReport
    ObjectNode imageToMldev(ApiClient apiClient, JsonNode fromObject, ObjectNode parentObject) {
        ObjectNode toObject = JsonSerializable.objectMapper.createObjectNode();
        if (!Common.isZero(Common.getValueByPath(fromObject, new String[]{"gcsUri"}))) {
            throw new IllegalArgumentException("gcsUri parameter is not supported in Gemini API.");
        }

        if (Common.getValueByPath(fromObject, new String[]{"imageBytes"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"bytesBase64Encoded"},
                    Transformers.tBytes(
                            Common.getValueByPath(fromObject, new String[]{"imageBytes"})));
        }

        if (Common.getValueByPath(fromObject, new String[]{"mimeType"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"mimeType"},
                    Common.getValueByPath(fromObject, new String[]{"mimeType"}));
        }

        return toObject;
    }

    @ExcludeFromGeneratedCoverageReport
    ObjectNode generateVideosConfigToMldev(
            ApiClient apiClient, JsonNode fromObject, ObjectNode parentObject) {
        ObjectNode toObject = JsonSerializable.objectMapper.createObjectNode();

        if (Common.getValueByPath(fromObject, new String[]{"numberOfVideos"}) != null) {
            Common.setValueByPath(
                    parentObject,
                    new String[]{"parameters", "sampleCount"},
                    Common.getValueByPath(fromObject, new String[]{"numberOfVideos"}));
        }

        if (!Common.isZero(Common.getValueByPath(fromObject, new String[]{"outputGcsUri"}))) {
            throw new IllegalArgumentException("outputGcsUri parameter is not supported in Gemini API.");
        }

        if (!Common.isZero(Common.getValueByPath(fromObject, new String[]{"fps"}))) {
            throw new IllegalArgumentException("fps parameter is not supported in Gemini API.");
        }

        if (Common.getValueByPath(fromObject, new String[]{"durationSeconds"}) != null) {
            Common.setValueByPath(
                    parentObject,
                    new String[]{"parameters", "durationSeconds"},
                    Common.getValueByPath(fromObject, new String[]{"durationSeconds"}));
        }

        if (!Common.isZero(Common.getValueByPath(fromObject, new String[]{"seed"}))) {
            throw new IllegalArgumentException("seed parameter is not supported in Gemini API.");
        }

        if (Common.getValueByPath(fromObject, new String[]{"aspectRatio"}) != null) {
            Common.setValueByPath(
                    parentObject,
                    new String[]{"parameters", "aspectRatio"},
                    Common.getValueByPath(fromObject, new String[]{"aspectRatio"}));
        }

        if (!Common.isZero(Common.getValueByPath(fromObject, new String[]{"resolution"}))) {
            throw new IllegalArgumentException("resolution parameter is not supported in Gemini API.");
        }

        if (Common.getValueByPath(fromObject, new String[]{"personGeneration"}) != null) {
            Common.setValueByPath(
                    parentObject,
                    new String[]{"parameters", "personGeneration"},
                    Common.getValueByPath(fromObject, new String[]{"personGeneration"}));
        }

        if (!Common.isZero(Common.getValueByPath(fromObject, new String[]{"pubsubTopic"}))) {
            throw new IllegalArgumentException("pubsubTopic parameter is not supported in Gemini API.");
        }

        if (Common.getValueByPath(fromObject, new String[]{"negativePrompt"}) != null) {
            Common.setValueByPath(
                    parentObject,
                    new String[]{"parameters", "negativePrompt"},
                    Common.getValueByPath(fromObject, new String[]{"negativePrompt"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"enhancePrompt"}) != null) {
            Common.setValueByPath(
                    parentObject,
                    new String[]{"parameters", "enhancePrompt"},
                    Common.getValueByPath(fromObject, new String[]{"enhancePrompt"}));
        }

        if (!Common.isZero(Common.getValueByPath(fromObject, new String[]{"generateAudio"}))) {
            throw new IllegalArgumentException("generateAudio parameter is not supported in Gemini API.");
        }

        return toObject;
    }

    @ExcludeFromGeneratedCoverageReport
    ObjectNode generateVideosParametersToMldev(
            ApiClient apiClient, JsonNode fromObject, ObjectNode parentObject) {
        ObjectNode toObject = JsonSerializable.objectMapper.createObjectNode();
        if (Common.getValueByPath(fromObject, new String[]{"model"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"_url", "model"},
                    Transformers.tModel(
                            this.apiClient, Common.getValueByPath(fromObject, new String[]{"model"})));
        }

        if (Common.getValueByPath(fromObject, new String[]{"prompt"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"instances[0]", "prompt"},
                    Common.getValueByPath(fromObject, new String[]{"prompt"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"image"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"instances[0]", "image"},
                    imageToMldev(
                            apiClient,
                            JsonSerializable.toJsonNode(
                                    Common.getValueByPath(fromObject, new String[]{"image"})),
                            toObject));
        }

        if (Common.getValueByPath(fromObject, new String[]{"config"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"config"},
                    generateVideosConfigToMldev(
                            apiClient,
                            JsonSerializable.toJsonNode(
                                    Common.getValueByPath(fromObject, new String[]{"config"})),
                            toObject));
        }

        return toObject;
    }

    @ExcludeFromGeneratedCoverageReport
    ObjectNode videoMetadataFromMldev(
            ApiClient apiClient, JsonNode fromObject, ObjectNode parentObject) {
        ObjectNode toObject = JsonSerializable.objectMapper.createObjectNode();
        if (Common.getValueByPath(fromObject, new String[]{"fps"}) != null) {
            Common.setValueByPath(
                    toObject, new String[]{"fps"}, Common.getValueByPath(fromObject, new String[]{"fps"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"endOffset"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"endOffset"},
                    Common.getValueByPath(fromObject, new String[]{"endOffset"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"startOffset"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"startOffset"},
                    Common.getValueByPath(fromObject, new String[]{"startOffset"}));
        }

        return toObject;
    }

    @ExcludeFromGeneratedCoverageReport
    ObjectNode blobFromMldev(ApiClient apiClient, JsonNode fromObject, ObjectNode parentObject) {
        ObjectNode toObject = JsonSerializable.objectMapper.createObjectNode();

        if (Common.getValueByPath(fromObject, new String[]{"data"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"data"},
                    Common.getValueByPath(fromObject, new String[]{"data"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"mimeType"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"mimeType"},
                    Common.getValueByPath(fromObject, new String[]{"mimeType"}));
        }

        return toObject;
    }

    @ExcludeFromGeneratedCoverageReport
    ObjectNode fileDataFromMldev(ApiClient apiClient, JsonNode fromObject, ObjectNode parentObject) {
        ObjectNode toObject = JsonSerializable.objectMapper.createObjectNode();

        if (Common.getValueByPath(fromObject, new String[]{"fileUri"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"fileUri"},
                    Common.getValueByPath(fromObject, new String[]{"fileUri"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"mimeType"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"mimeType"},
                    Common.getValueByPath(fromObject, new String[]{"mimeType"}));
        }

        return toObject;
    }

    @ExcludeFromGeneratedCoverageReport
    ObjectNode partFromMldev(ApiClient apiClient, JsonNode fromObject, ObjectNode parentObject) {
        ObjectNode toObject = JsonSerializable.objectMapper.createObjectNode();
        if (Common.getValueByPath(fromObject, new String[]{"videoMetadata"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"videoMetadata"},
                    videoMetadataFromMldev(
                            apiClient,
                            JsonSerializable.toJsonNode(
                                    Common.getValueByPath(fromObject, new String[]{"videoMetadata"})),
                            toObject));
        }

        if (Common.getValueByPath(fromObject, new String[]{"thought"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"thought"},
                    Common.getValueByPath(fromObject, new String[]{"thought"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"inlineData"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"inlineData"},
                    blobFromMldev(
                            apiClient,
                            JsonSerializable.toJsonNode(
                                    Common.getValueByPath(fromObject, new String[]{"inlineData"})),
                            toObject));
        }

        if (Common.getValueByPath(fromObject, new String[]{"fileData"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"fileData"},
                    fileDataFromMldev(
                            apiClient,
                            JsonSerializable.toJsonNode(
                                    Common.getValueByPath(fromObject, new String[]{"fileData"})),
                            toObject));
        }

        if (Common.getValueByPath(fromObject, new String[]{"thoughtSignature"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"thoughtSignature"},
                    Common.getValueByPath(fromObject, new String[]{"thoughtSignature"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"codeExecutionResult"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"codeExecutionResult"},
                    Common.getValueByPath(fromObject, new String[]{"codeExecutionResult"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"executableCode"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"executableCode"},
                    Common.getValueByPath(fromObject, new String[]{"executableCode"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"functionCall"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"functionCall"},
                    Common.getValueByPath(fromObject, new String[]{"functionCall"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"functionResponse"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"functionResponse"},
                    Common.getValueByPath(fromObject, new String[]{"functionResponse"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"text"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"text"},
                    Common.getValueByPath(fromObject, new String[]{"text"}));
        }

        return toObject;
    }

    @ExcludeFromGeneratedCoverageReport
    ObjectNode contentFromMldev(ApiClient apiClient, JsonNode fromObject, ObjectNode parentObject) {
        ObjectNode toObject = JsonSerializable.objectMapper.createObjectNode();
        if (Common.getValueByPath(fromObject, new String[]{"parts"}) != null) {
            ArrayNode keyArray = (ArrayNode) Common.getValueByPath(fromObject, new String[]{"parts"});
            ObjectMapper objectMapper = new ObjectMapper();
            ArrayNode result = objectMapper.createArrayNode();

            for (JsonNode item : keyArray) {
                result.add(partFromMldev(apiClient, JsonSerializable.toJsonNode(item), toObject));
            }
            Common.setValueByPath(toObject, new String[]{"parts"}, result);
        }

        if (Common.getValueByPath(fromObject, new String[]{"role"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"role"},
                    Common.getValueByPath(fromObject, new String[]{"role"}));
        }

        return toObject;
    }

    @ExcludeFromGeneratedCoverageReport
    ObjectNode citationMetadataFromMldev(
            ApiClient apiClient, JsonNode fromObject, ObjectNode parentObject) {
        ObjectNode toObject = JsonSerializable.objectMapper.createObjectNode();
        if (Common.getValueByPath(fromObject, new String[]{"citationSources"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"citations"},
                    Common.getValueByPath(fromObject, new String[]{"citationSources"}));
        }

        return toObject;
    }

    @ExcludeFromGeneratedCoverageReport
    ObjectNode urlMetadataFromMldev(
            ApiClient apiClient, JsonNode fromObject, ObjectNode parentObject) {
        ObjectNode toObject = JsonSerializable.objectMapper.createObjectNode();
        if (Common.getValueByPath(fromObject, new String[]{"retrievedUrl"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"retrievedUrl"},
                    Common.getValueByPath(fromObject, new String[]{"retrievedUrl"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"urlRetrievalStatus"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"urlRetrievalStatus"},
                    Common.getValueByPath(fromObject, new String[]{"urlRetrievalStatus"}));
        }

        return toObject;
    }

    @ExcludeFromGeneratedCoverageReport
    ObjectNode urlContextMetadataFromMldev(
            ApiClient apiClient, JsonNode fromObject, ObjectNode parentObject) {
        ObjectNode toObject = JsonSerializable.objectMapper.createObjectNode();
        if (Common.getValueByPath(fromObject, new String[]{"urlMetadata"}) != null) {
            ArrayNode keyArray =
                    (ArrayNode) Common.getValueByPath(fromObject, new String[]{"urlMetadata"});
            ObjectMapper objectMapper = new ObjectMapper();
            ArrayNode result = objectMapper.createArrayNode();

            for (JsonNode item : keyArray) {
                result.add(urlMetadataFromMldev(apiClient, JsonSerializable.toJsonNode(item), toObject));
            }
            Common.setValueByPath(toObject, new String[]{"urlMetadata"}, result);
        }

        return toObject;
    }

    @ExcludeFromGeneratedCoverageReport
    ObjectNode candidateFromMldev(ApiClient apiClient, JsonNode fromObject, ObjectNode parentObject) {
        ObjectNode toObject = JsonSerializable.objectMapper.createObjectNode();
        if (Common.getValueByPath(fromObject, new String[]{"content"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"content"},
                    contentFromMldev(
                            apiClient,
                            JsonSerializable.toJsonNode(
                                    Common.getValueByPath(fromObject, new String[]{"content"})),
                            toObject));
        }

        if (Common.getValueByPath(fromObject, new String[]{"citationMetadata"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"citationMetadata"},
                    citationMetadataFromMldev(
                            apiClient,
                            JsonSerializable.toJsonNode(
                                    Common.getValueByPath(fromObject, new String[]{"citationMetadata"})),
                            toObject));
        }

        if (Common.getValueByPath(fromObject, new String[]{"tokenCount"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"tokenCount"},
                    Common.getValueByPath(fromObject, new String[]{"tokenCount"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"finishReason"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"finishReason"},
                    Common.getValueByPath(fromObject, new String[]{"finishReason"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"urlContextMetadata"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"urlContextMetadata"},
                    urlContextMetadataFromMldev(
                            apiClient,
                            JsonSerializable.toJsonNode(
                                    Common.getValueByPath(fromObject, new String[]{"urlContextMetadata"})),
                            toObject));
        }

        if (Common.getValueByPath(fromObject, new String[]{"avgLogprobs"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"avgLogprobs"},
                    Common.getValueByPath(fromObject, new String[]{"avgLogprobs"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"groundingMetadata"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"groundingMetadata"},
                    Common.getValueByPath(fromObject, new String[]{"groundingMetadata"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"index"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"index"},
                    Common.getValueByPath(fromObject, new String[]{"index"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"logprobsResult"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"logprobsResult"},
                    Common.getValueByPath(fromObject, new String[]{"logprobsResult"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"safetyRatings"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"safetyRatings"},
                    Common.getValueByPath(fromObject, new String[]{"safetyRatings"}));
        }

        return toObject;
    }

    @ExcludeFromGeneratedCoverageReport
    ObjectNode generateContentResponseFromMldev(
            ApiClient apiClient, JsonNode fromObject, ObjectNode parentObject) {
        ObjectNode toObject = JsonSerializable.objectMapper.createObjectNode();
        if (Common.getValueByPath(fromObject, new String[]{"candidates"}) != null) {
            ArrayNode keyArray =
                    (ArrayNode) Common.getValueByPath(fromObject, new String[]{"candidates"});
            ObjectMapper objectMapper = new ObjectMapper();
            ArrayNode result = objectMapper.createArrayNode();

            for (JsonNode item : keyArray) {
                result.add(candidateFromMldev(apiClient, JsonSerializable.toJsonNode(item), toObject));
            }
            Common.setValueByPath(toObject, new String[]{"candidates"}, result);
        }

        if (Common.getValueByPath(fromObject, new String[]{"modelVersion"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"modelVersion"},
                    Common.getValueByPath(fromObject, new String[]{"modelVersion"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"promptFeedback"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"promptFeedback"},
                    Common.getValueByPath(fromObject, new String[]{"promptFeedback"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"usageMetadata"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"usageMetadata"},
                    Common.getValueByPath(fromObject, new String[]{"usageMetadata"}));
        }

        return toObject;
    }

    @ExcludeFromGeneratedCoverageReport
    ObjectNode contentEmbeddingStatisticsFromMldev(
            ApiClient apiClient, JsonNode fromObject, ObjectNode parentObject) {
        ObjectNode toObject = JsonSerializable.objectMapper.createObjectNode();

        return toObject;
    }

    @ExcludeFromGeneratedCoverageReport
    ObjectNode contentEmbeddingFromMldev(
            ApiClient apiClient, JsonNode fromObject, ObjectNode parentObject) {
        ObjectNode toObject = JsonSerializable.objectMapper.createObjectNode();
        if (Common.getValueByPath(fromObject, new String[]{"values"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"values"},
                    Common.getValueByPath(fromObject, new String[]{"values"}));
        }

        return toObject;
    }

    @ExcludeFromGeneratedCoverageReport
    ObjectNode embedContentMetadataFromMldev(
            ApiClient apiClient, JsonNode fromObject, ObjectNode parentObject) {
        ObjectNode toObject = JsonSerializable.objectMapper.createObjectNode();

        return toObject;
    }

    @ExcludeFromGeneratedCoverageReport
    ObjectNode embedContentResponseFromMldev(
            ApiClient apiClient, JsonNode fromObject, ObjectNode parentObject) {
        ObjectNode toObject = JsonSerializable.objectMapper.createObjectNode();
        if (Common.getValueByPath(fromObject, new String[]{"embeddings"}) != null) {
            ArrayNode keyArray =
                    (ArrayNode) Common.getValueByPath(fromObject, new String[]{"embeddings"});
            ObjectMapper objectMapper = new ObjectMapper();
            ArrayNode result = objectMapper.createArrayNode();

            for (JsonNode item : keyArray) {
                result.add(
                        contentEmbeddingFromMldev(apiClient, JsonSerializable.toJsonNode(item), toObject));
            }
            Common.setValueByPath(toObject, new String[]{"embeddings"}, result);
        }

        if (Common.getValueByPath(fromObject, new String[]{"metadata"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"metadata"},
                    embedContentMetadataFromMldev(
                            apiClient,
                            JsonSerializable.toJsonNode(
                                    Common.getValueByPath(fromObject, new String[]{"metadata"})),
                            toObject));
        }

        return toObject;
    }

    @ExcludeFromGeneratedCoverageReport
    ObjectNode imageFromMldev(ApiClient apiClient, JsonNode fromObject, ObjectNode parentObject) {
        ObjectNode toObject = JsonSerializable.objectMapper.createObjectNode();

        if (Common.getValueByPath(fromObject, new String[]{"bytesBase64Encoded"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"imageBytes"},
                    Transformers.tBytes(
                            Common.getValueByPath(fromObject, new String[]{"bytesBase64Encoded"})));
        }

        if (Common.getValueByPath(fromObject, new String[]{"mimeType"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"mimeType"},
                    Common.getValueByPath(fromObject, new String[]{"mimeType"}));
        }

        return toObject;
    }

    @ExcludeFromGeneratedCoverageReport
    ObjectNode safetyAttributesFromMldev(
            ApiClient apiClient, JsonNode fromObject, ObjectNode parentObject) {
        ObjectNode toObject = JsonSerializable.objectMapper.createObjectNode();
        if (Common.getValueByPath(fromObject, new String[]{"safetyAttributes", "categories"})
                != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"categories"},
                    Common.getValueByPath(fromObject, new String[]{"safetyAttributes", "categories"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"safetyAttributes", "scores"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"scores"},
                    Common.getValueByPath(fromObject, new String[]{"safetyAttributes", "scores"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"contentType"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"contentType"},
                    Common.getValueByPath(fromObject, new String[]{"contentType"}));
        }

        return toObject;
    }

    @ExcludeFromGeneratedCoverageReport
    ObjectNode generatedImageFromMldev(
            ApiClient apiClient, JsonNode fromObject, ObjectNode parentObject) {
        ObjectNode toObject = JsonSerializable.objectMapper.createObjectNode();
        if (Common.getValueByPath(fromObject, new String[]{"_self"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"image"},
                    imageFromMldev(
                            apiClient,
                            JsonSerializable.toJsonNode(
                                    Common.getValueByPath(fromObject, new String[]{"_self"})),
                            toObject));
        }

        if (Common.getValueByPath(fromObject, new String[]{"raiFilteredReason"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"raiFilteredReason"},
                    Common.getValueByPath(fromObject, new String[]{"raiFilteredReason"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"_self"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"safetyAttributes"},
                    safetyAttributesFromMldev(
                            apiClient,
                            JsonSerializable.toJsonNode(
                                    Common.getValueByPath(fromObject, new String[]{"_self"})),
                            toObject));
        }

        return toObject;
    }

    @ExcludeFromGeneratedCoverageReport
    ObjectNode generateImagesResponseFromMldev(
            ApiClient apiClient, JsonNode fromObject, ObjectNode parentObject) {
        ObjectNode toObject = JsonSerializable.objectMapper.createObjectNode();
        if (Common.getValueByPath(fromObject, new String[]{"predictions"}) != null) {
            ArrayNode keyArray =
                    (ArrayNode) Common.getValueByPath(fromObject, new String[]{"predictions"});
            ObjectMapper objectMapper = new ObjectMapper();
            ArrayNode result = objectMapper.createArrayNode();

            for (JsonNode item : keyArray) {
                result.add(generatedImageFromMldev(apiClient, JsonSerializable.toJsonNode(item), toObject));
            }
            Common.setValueByPath(toObject, new String[]{"generatedImages"}, result);
        }

        if (Common.getValueByPath(fromObject, new String[]{"positivePromptSafetyAttributes"})
                != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"positivePromptSafetyAttributes"},
                    safetyAttributesFromMldev(
                            apiClient,
                            JsonSerializable.toJsonNode(
                                    Common.getValueByPath(
                                            fromObject, new String[]{"positivePromptSafetyAttributes"})),
                            toObject));
        }

        return toObject;
    }

    @ExcludeFromGeneratedCoverageReport
    ObjectNode endpointFromMldev(ApiClient apiClient, JsonNode fromObject, ObjectNode parentObject) {
        ObjectNode toObject = JsonSerializable.objectMapper.createObjectNode();

        return toObject;
    }

    @ExcludeFromGeneratedCoverageReport
    ObjectNode tunedModelInfoFromMldev(
            ApiClient apiClient, JsonNode fromObject, ObjectNode parentObject) {
        ObjectNode toObject = JsonSerializable.objectMapper.createObjectNode();
        if (Common.getValueByPath(fromObject, new String[]{"baseModel"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"baseModel"},
                    Common.getValueByPath(fromObject, new String[]{"baseModel"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"createTime"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"createTime"},
                    Common.getValueByPath(fromObject, new String[]{"createTime"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"updateTime"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"updateTime"},
                    Common.getValueByPath(fromObject, new String[]{"updateTime"}));
        }

        return toObject;
    }

    @ExcludeFromGeneratedCoverageReport
    ObjectNode checkpointFromMldev(
            ApiClient apiClient, JsonNode fromObject, ObjectNode parentObject) {
        ObjectNode toObject = JsonSerializable.objectMapper.createObjectNode();

        return toObject;
    }

    @ExcludeFromGeneratedCoverageReport
    ObjectNode modelFromMldev(ApiClient apiClient, JsonNode fromObject, ObjectNode parentObject) {
        ObjectNode toObject = JsonSerializable.objectMapper.createObjectNode();
        if (Common.getValueByPath(fromObject, new String[]{"name"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"name"},
                    Common.getValueByPath(fromObject, new String[]{"name"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"displayName"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"displayName"},
                    Common.getValueByPath(fromObject, new String[]{"displayName"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"description"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"description"},
                    Common.getValueByPath(fromObject, new String[]{"description"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"version"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"version"},
                    Common.getValueByPath(fromObject, new String[]{"version"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"_self"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"tunedModelInfo"},
                    tunedModelInfoFromMldev(
                            apiClient,
                            JsonSerializable.toJsonNode(
                                    Common.getValueByPath(fromObject, new String[]{"_self"})),
                            toObject));
        }

        if (Common.getValueByPath(fromObject, new String[]{"inputTokenLimit"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"inputTokenLimit"},
                    Common.getValueByPath(fromObject, new String[]{"inputTokenLimit"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"outputTokenLimit"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"outputTokenLimit"},
                    Common.getValueByPath(fromObject, new String[]{"outputTokenLimit"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"supportedGenerationMethods"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"supportedActions"},
                    Common.getValueByPath(fromObject, new String[]{"supportedGenerationMethods"}));
        }

        return toObject;
    }

    @ExcludeFromGeneratedCoverageReport
    ObjectNode listModelsResponseFromMldev(
            ApiClient apiClient, JsonNode fromObject, ObjectNode parentObject) {
        ObjectNode toObject = JsonSerializable.objectMapper.createObjectNode();
        if (Common.getValueByPath(fromObject, new String[]{"nextPageToken"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"nextPageToken"},
                    Common.getValueByPath(fromObject, new String[]{"nextPageToken"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"_self"}) != null) {
            ArrayNode keyArray =
                    (ArrayNode)
                            Transformers.tExtractModels(
                                    this.apiClient, Common.getValueByPath(fromObject, new String[]{"_self"}));
            ObjectMapper objectMapper = new ObjectMapper();
            ArrayNode result = objectMapper.createArrayNode();

            for (JsonNode item : keyArray) {
                result.add(modelFromMldev(apiClient, JsonSerializable.toJsonNode(item), toObject));
            }
            Common.setValueByPath(toObject, new String[]{"models"}, result);
        }

        return toObject;
    }

    @ExcludeFromGeneratedCoverageReport
    ObjectNode deleteModelResponseFromMldev(
            ApiClient apiClient, JsonNode fromObject, ObjectNode parentObject) {
        ObjectNode toObject = JsonSerializable.objectMapper.createObjectNode();

        return toObject;
    }

    @ExcludeFromGeneratedCoverageReport
    ObjectNode countTokensResponseFromMldev(
            ApiClient apiClient, JsonNode fromObject, ObjectNode parentObject) {
        ObjectNode toObject = JsonSerializable.objectMapper.createObjectNode();
        if (Common.getValueByPath(fromObject, new String[]{"totalTokens"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"totalTokens"},
                    Common.getValueByPath(fromObject, new String[]{"totalTokens"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"cachedContentTokenCount"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"cachedContentTokenCount"},
                    Common.getValueByPath(fromObject, new String[]{"cachedContentTokenCount"}));
        }

        return toObject;
    }

    @ExcludeFromGeneratedCoverageReport
    ObjectNode videoFromMldev(ApiClient apiClient, JsonNode fromObject, ObjectNode parentObject) {
        ObjectNode toObject = JsonSerializable.objectMapper.createObjectNode();
        if (Common.getValueByPath(fromObject, new String[]{"video", "uri"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"uri"},
                    Common.getValueByPath(fromObject, new String[]{"video", "uri"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"video", "encodedVideo"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"videoBytes"},
                    Transformers.tBytes(
                            Common.getValueByPath(fromObject, new String[]{"video", "encodedVideo"})));
        }

        if (Common.getValueByPath(fromObject, new String[]{"encoding"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"mimeType"},
                    Common.getValueByPath(fromObject, new String[]{"encoding"}));
        }

        return toObject;
    }

    @ExcludeFromGeneratedCoverageReport
    ObjectNode generatedVideoFromMldev(
            ApiClient apiClient, JsonNode fromObject, ObjectNode parentObject) {
        ObjectNode toObject = JsonSerializable.objectMapper.createObjectNode();
        if (Common.getValueByPath(fromObject, new String[]{"_self"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"video"},
                    videoFromMldev(
                            apiClient,
                            JsonSerializable.toJsonNode(
                                    Common.getValueByPath(fromObject, new String[]{"_self"})),
                            toObject));
        }

        return toObject;
    }

    @ExcludeFromGeneratedCoverageReport
    ObjectNode generateVideosResponseFromMldev(
            ApiClient apiClient, JsonNode fromObject, ObjectNode parentObject) {
        ObjectNode toObject = JsonSerializable.objectMapper.createObjectNode();
        if (Common.getValueByPath(fromObject, new String[]{"generatedSamples"}) != null) {
            ArrayNode keyArray =
                    (ArrayNode) Common.getValueByPath(fromObject, new String[]{"generatedSamples"});
            ObjectMapper objectMapper = new ObjectMapper();
            ArrayNode result = objectMapper.createArrayNode();

            for (JsonNode item : keyArray) {
                result.add(generatedVideoFromMldev(apiClient, JsonSerializable.toJsonNode(item), toObject));
            }
            Common.setValueByPath(toObject, new String[]{"generatedVideos"}, result);
        }

        if (Common.getValueByPath(fromObject, new String[]{"raiMediaFilteredCount"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"raiMediaFilteredCount"},
                    Common.getValueByPath(fromObject, new String[]{"raiMediaFilteredCount"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"raiMediaFilteredReasons"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"raiMediaFilteredReasons"},
                    Common.getValueByPath(fromObject, new String[]{"raiMediaFilteredReasons"}));
        }

        return toObject;
    }

    @ExcludeFromGeneratedCoverageReport
    ObjectNode generateVideosOperationFromMldev(
            ApiClient apiClient, JsonNode fromObject, ObjectNode parentObject) {
        ObjectNode toObject = JsonSerializable.objectMapper.createObjectNode();
        if (Common.getValueByPath(fromObject, new String[]{"name"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"name"},
                    Common.getValueByPath(fromObject, new String[]{"name"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"metadata"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"metadata"},
                    Common.getValueByPath(fromObject, new String[]{"metadata"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"done"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"done"},
                    Common.getValueByPath(fromObject, new String[]{"done"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"error"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"error"},
                    Common.getValueByPath(fromObject, new String[]{"error"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"response", "generateVideoResponse"})
                != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"response"},
                    generateVideosResponseFromMldev(
                            apiClient,
                            JsonSerializable.toJsonNode(
                                    Common.getValueByPath(
                                            fromObject, new String[]{"response", "generateVideoResponse"})),
                            toObject));
        }

        return toObject;
    }

    GenerateContentResponse privateGenerateContent(
            String model, List<Content> contents, GenerateContentConfig config) {

        GenerateContentParameters.Builder parameterBuilder = GenerateContentParameters.builder();

        if (!Common.isZero(model)) {
            parameterBuilder.model(model);
        }
        if (!Common.isZero(contents)) {
            parameterBuilder.contents(contents);
        }
        if (!Common.isZero(config)) {
            parameterBuilder.config(config);
        }
        JsonNode parameterNode = JsonSerializable.toJsonNode(parameterBuilder.build());

        ObjectNode body;
        String path;
        body = generateContentParametersToMldev(this.apiClient, parameterNode, null);
        if (body.get("_url") != null) {
            path = Common.formatMap("{model}:generateContent", body.get("_url"));
        } else {
            path = "{model}:generateContent";
        }
        body.remove("_url");

        JsonNode queryParams = body.get("_query");
        if (queryParams != null) {
            body.remove("_query");
            path = String.format("%s?%s", path, Common.urlEncode((ObjectNode) queryParams));
        }

        // TODO: Remove the hack that removes config.
        body.remove("config");

        Optional<HttpOptions> requestHttpOptions = Optional.empty();
        if (config != null) {
            requestHttpOptions = config.httpOptions();
        }

        try (ApiResponse response =
                     this.apiClient.request(
                             "post", path, JsonSerializable.toJsonString(body), requestHttpOptions)) {
            HttpEntity entity = response.getEntity();
            String responseString;
            try {
                responseString = EntityUtils.toString(entity);
            } catch (ParseException | IOException e) {
                throw new GenAiIOException("Failed to read HTTP response.", e);
            }

            JsonNode responseNode = JsonSerializable.stringToJsonNode(responseString);
            responseNode = generateContentResponseFromMldev(this.apiClient, responseNode, null);
            return JsonSerializable.fromJsonNode(responseNode, GenerateContentResponse.class);
        }
    }

    ResponseStream<GenerateContentResponse> privateGenerateContentStream(
            String model, List<Content> contents, GenerateContentConfig config) {

        GenerateContentParameters.Builder parameterBuilder = GenerateContentParameters.builder();

        if (!Common.isZero(model)) {
            parameterBuilder.model(model);
        }
        if (!Common.isZero(contents)) {
            parameterBuilder.contents(contents);
        }
        if (!Common.isZero(config)) {
            parameterBuilder.config(config);
        }
        JsonNode parameterNode = JsonSerializable.toJsonNode(parameterBuilder.build());

        ObjectNode body;
        String path;
        body = generateContentParametersToMldev(this.apiClient, parameterNode, null);
        if (body.get("_url") != null) {
            path = Common.formatMap("{model}:streamGenerateContent?alt=sse", body.get("_url"));
        } else {
            path = "{model}:streamGenerateContent?alt=sse";
        }
        body.remove("_url");

        JsonNode queryParams = body.get("_query");
        if (queryParams != null) {
            body.remove("_query");
            path = String.format("%s?%s", path, Common.urlEncode((ObjectNode) queryParams));
        }

        // TODO: Remove the hack that removes config.
        body.remove("config");

        Optional<HttpOptions> requestHttpOptions = Optional.empty();
        if (config != null) {
            requestHttpOptions = config.httpOptions();
        }

        ApiResponse response =
                this.apiClient.request(
                        "post", path, JsonSerializable.toJsonString(body), requestHttpOptions);
        String converterName;

        converterName = "generateContentResponseFromMldev";
        return new ResponseStream<GenerateContentResponse>(
                GenerateContentResponse.class, response, this, converterName);
    }

    EmbedContentResponse privateEmbedContent(
            String model, List<Content> contents, EmbedContentConfig config) {

        EmbedContentParameters.Builder parameterBuilder = EmbedContentParameters.builder();

        if (!Common.isZero(model)) {
            parameterBuilder.model(model);
        }
        if (!Common.isZero(contents)) {
            parameterBuilder.contents(contents);
        }
        if (!Common.isZero(config)) {
            parameterBuilder.config(config);
        }
        JsonNode parameterNode = JsonSerializable.toJsonNode(parameterBuilder.build());

        ObjectNode body;
        String path;
        body = embedContentParametersToMldev(this.apiClient, parameterNode, null);
        if (body.get("_url") != null) {
            path = Common.formatMap("{model}:batchEmbedContents", body.get("_url"));
        } else {
            path = "{model}:batchEmbedContents";
        }
        body.remove("_url");

        JsonNode queryParams = body.get("_query");
        if (queryParams != null) {
            body.remove("_query");
            path = String.format("%s?%s", path, Common.urlEncode((ObjectNode) queryParams));
        }

        // TODO: Remove the hack that removes config.
        body.remove("config");

        Optional<HttpOptions> requestHttpOptions = Optional.empty();
        if (config != null) {
            requestHttpOptions = config.httpOptions();
        }

        try (ApiResponse response =
                     this.apiClient.request(
                             "post", path, JsonSerializable.toJsonString(body), requestHttpOptions)) {
            HttpEntity entity = response.getEntity();
            String responseString;
            try {
                responseString = EntityUtils.toString(entity);
            } catch (ParseException | IOException e) {
                throw new GenAiIOException("Failed to read HTTP response.", e);
            }

            JsonNode responseNode = JsonSerializable.stringToJsonNode(responseString);
            responseNode = embedContentResponseFromMldev(this.apiClient, responseNode, null);
            return JsonSerializable.fromJsonNode(responseNode, EmbedContentResponse.class);
        }
    }

    GenerateImagesResponse privateGenerateImages(
            String model, String prompt, GenerateImagesConfig config) {

        GenerateImagesParameters.Builder parameterBuilder = GenerateImagesParameters.builder();

        if (!Common.isZero(model)) {
            parameterBuilder.model(model);
        }
        if (!Common.isZero(prompt)) {
            parameterBuilder.prompt(prompt);
        }
        if (!Common.isZero(config)) {
            parameterBuilder.config(config);
        }
        JsonNode parameterNode = JsonSerializable.toJsonNode(parameterBuilder.build());

        ObjectNode body;
        String path;
        body = generateImagesParametersToMldev(this.apiClient, parameterNode, null);
        if (body.get("_url") != null) {
            path = Common.formatMap("{model}:predict", body.get("_url"));
        } else {
            path = "{model}:predict";
        }
        body.remove("_url");

        JsonNode queryParams = body.get("_query");
        if (queryParams != null) {
            body.remove("_query");
            path = String.format("%s?%s", path, Common.urlEncode((ObjectNode) queryParams));
        }

        // TODO: Remove the hack that removes config.
        body.remove("config");

        Optional<HttpOptions> requestHttpOptions = Optional.empty();
        if (config != null) {
            requestHttpOptions = config.httpOptions();
        }

        try (ApiResponse response =
                     this.apiClient.request(
                             "post", path, JsonSerializable.toJsonString(body), requestHttpOptions)) {
            HttpEntity entity = response.getEntity();
            String responseString;
            try {
                responseString = EntityUtils.toString(entity);
            } catch (ParseException | IOException e) {
                throw new GenAiIOException("Failed to read HTTP response.", e);
            }

            JsonNode responseNode = JsonSerializable.stringToJsonNode(responseString);
            responseNode = generateImagesResponseFromMldev(this.apiClient, responseNode, null);
            return JsonSerializable.fromJsonNode(responseNode, GenerateImagesResponse.class);
        }
    }

    /**
     * Fetches information about a model by name.
     *
     * @example ```java Model model = client.models.get("gemini-2.0-flash"); ```
     */
    public Model get(String model, GetModelConfig config) {

        GetModelParameters.Builder parameterBuilder = GetModelParameters.builder();

        if (!Common.isZero(model)) {
            parameterBuilder.model(model);
        }
        if (!Common.isZero(config)) {
            parameterBuilder.config(config);
        }
        JsonNode parameterNode = JsonSerializable.toJsonNode(parameterBuilder.build());

        ObjectNode body;
        String path;
        body = getModelParametersToMldev(this.apiClient, parameterNode, null);
        if (body.get("_url") != null) {
            path = Common.formatMap("{name}", body.get("_url"));
        } else {
            path = "{name}";
        }
        body.remove("_url");

        JsonNode queryParams = body.get("_query");
        if (queryParams != null) {
            body.remove("_query");
            path = String.format("%s?%s", path, Common.urlEncode((ObjectNode) queryParams));
        }

        // TODO: Remove the hack that removes config.
        body.remove("config");

        Optional<HttpOptions> requestHttpOptions = Optional.empty();
        if (config != null) {
            requestHttpOptions = config.httpOptions();
        }

        try (ApiResponse response =
                     this.apiClient.request(
                             "get", path, JsonSerializable.toJsonString(body), requestHttpOptions)) {
            HttpEntity entity = response.getEntity();
            String responseString;
            try {
                responseString = EntityUtils.toString(entity);
            } catch (ParseException | IOException e) {
                throw new GenAiIOException("Failed to read HTTP response.", e);
            }

            JsonNode responseNode = JsonSerializable.stringToJsonNode(responseString);
            responseNode = modelFromMldev(this.apiClient, responseNode, null);
            return JsonSerializable.fromJsonNode(responseNode, Model.class);
        }
    }

    ListModelsResponse privateList(ListModelsConfig config) {

        ListModelsParameters.Builder parameterBuilder = ListModelsParameters.builder();

        if (!Common.isZero(config)) {
            parameterBuilder.config(config);
        }
        JsonNode parameterNode = JsonSerializable.toJsonNode(parameterBuilder.build());

        ObjectNode body;
        String path;
        body = listModelsParametersToMldev(this.apiClient, parameterNode, null);
        if (body.get("_url") != null) {
            path = Common.formatMap("{models_url}", body.get("_url"));
        } else {
            path = "{models_url}";
        }
        body.remove("_url");

        JsonNode queryParams = body.get("_query");
        if (queryParams != null) {
            body.remove("_query");
            path = String.format("%s?%s", path, Common.urlEncode((ObjectNode) queryParams));
        }

        // TODO: Remove the hack that removes config.
        body.remove("config");

        Optional<HttpOptions> requestHttpOptions = Optional.empty();
        if (config != null) {
            requestHttpOptions = config.httpOptions();
        }

        try (ApiResponse response =
                     this.apiClient.request(
                             "get", path, JsonSerializable.toJsonString(body), requestHttpOptions)) {
            HttpEntity entity = response.getEntity();
            String responseString;
            try {
                responseString = EntityUtils.toString(entity);
            } catch (ParseException | IOException e) {
                throw new GenAiIOException("Failed to read HTTP response.", e);
            }

            JsonNode responseNode = JsonSerializable.stringToJsonNode(responseString);
            responseNode = listModelsResponseFromMldev(this.apiClient, responseNode, null);
            return JsonSerializable.fromJsonNode(responseNode, ListModelsResponse.class);
        }
    }

    /**
     * Updates a tuned model by its name.
     *
     * @param model  The name of the tuned model to update
     * @param config A {@link com.google.genai.types.UpdateModelConfig} instance that specifies the
     *               optional configurations
     * @return A {@link com.google.genai.types.Model} instance
     * @example ```java Model model = client.models.update( "tunedModels/12345",
     * UpdateModelConfig.builder() .displayName("New display name") .description("New
     * description") .build()); ```
     */
    public Model update(String model, UpdateModelConfig config) {

        UpdateModelParameters.Builder parameterBuilder = UpdateModelParameters.builder();

        if (!Common.isZero(model)) {
            parameterBuilder.model(model);
        }
        if (!Common.isZero(config)) {
            parameterBuilder.config(config);
        }
        JsonNode parameterNode = JsonSerializable.toJsonNode(parameterBuilder.build());

        ObjectNode body;
        String path;
        body = updateModelParametersToMldev(this.apiClient, parameterNode, null);
        if (body.get("_url") != null) {
            path = Common.formatMap("{name}", body.get("_url"));
        } else {
            path = "{name}";
        }
        body.remove("_url");

        JsonNode queryParams = body.get("_query");
        if (queryParams != null) {
            body.remove("_query");
            path = String.format("%s?%s", path, Common.urlEncode((ObjectNode) queryParams));
        }

        // TODO: Remove the hack that removes config.
        body.remove("config");

        Optional<HttpOptions> requestHttpOptions = Optional.empty();
        if (config != null) {
            requestHttpOptions = config.httpOptions();
        }

        try (ApiResponse response =
                     this.apiClient.request(
                             "patch", path, JsonSerializable.toJsonString(body), requestHttpOptions)) {
            HttpEntity entity = response.getEntity();
            String responseString;
            try {
                responseString = EntityUtils.toString(entity);
            } catch (ParseException | IOException e) {
                throw new GenAiIOException("Failed to read HTTP response.", e);
            }

            JsonNode responseNode = JsonSerializable.stringToJsonNode(responseString);
            responseNode = modelFromMldev(this.apiClient, responseNode, null);
            return JsonSerializable.fromJsonNode(responseNode, Model.class);
        }
    }

    /**
     * Fetches information about a model by name.
     *
     * @example ```java Model model = client.models.delete("tunedModels/12345"); ```
     */
    public DeleteModelResponse delete(String model, DeleteModelConfig config) {

        DeleteModelParameters.Builder parameterBuilder = DeleteModelParameters.builder();

        if (!Common.isZero(model)) {
            parameterBuilder.model(model);
        }
        if (!Common.isZero(config)) {
            parameterBuilder.config(config);
        }
        JsonNode parameterNode = JsonSerializable.toJsonNode(parameterBuilder.build());

        ObjectNode body;
        String path;
        body = deleteModelParametersToMldev(this.apiClient, parameterNode, null);
        if (body.get("_url") != null) {
            path = Common.formatMap("{name}", body.get("_url"));
        } else {
            path = "{name}";
        }
        body.remove("_url");

        JsonNode queryParams = body.get("_query");
        if (queryParams != null) {
            body.remove("_query");
            path = String.format("%s?%s", path, Common.urlEncode((ObjectNode) queryParams));
        }

        // TODO: Remove the hack that removes config.
        body.remove("config");

        Optional<HttpOptions> requestHttpOptions = Optional.empty();
        if (config != null) {
            requestHttpOptions = config.httpOptions();
        }

        try (ApiResponse response =
                     this.apiClient.request(
                             "delete", path, JsonSerializable.toJsonString(body), requestHttpOptions)) {
            HttpEntity entity = response.getEntity();
            String responseString;
            try {
                responseString = EntityUtils.toString(entity);
            } catch (ParseException | IOException e) {
                throw new GenAiIOException("Failed to read HTTP response.", e);
            }

            JsonNode responseNode = JsonSerializable.stringToJsonNode(responseString);
            responseNode = deleteModelResponseFromMldev(this.apiClient, responseNode, null);
            return JsonSerializable.fromJsonNode(responseNode, DeleteModelResponse.class);
        }
    }

    /**
     * Counts tokens given a GenAI model and a list of content.
     *
     * @param model    the name of the GenAI model to use.
     * @param contents a {@link List<com.google.genai.types.Content>} to send to count tokens for.
     * @param config   a {@link com.google.genai.types.CountTokensConfig} instance that specifies the
     *                 optional configurations
     * @return a {@link com.google.genai.types.CountTokensResponse} instance that contains tokens
     * count.
     */
    public CountTokensResponse countTokens(
            String model, List<Content> contents, CountTokensConfig config) {

        CountTokensParameters.Builder parameterBuilder = CountTokensParameters.builder();

        if (!Common.isZero(model)) {
            parameterBuilder.model(model);
        }
        if (!Common.isZero(contents)) {
            parameterBuilder.contents(contents);
        }
        if (!Common.isZero(config)) {
            parameterBuilder.config(config);
        }
        JsonNode parameterNode = JsonSerializable.toJsonNode(parameterBuilder.build());

        ObjectNode body;
        String path;
        body = countTokensParametersToMldev(this.apiClient, parameterNode, null);
        if (body.get("_url") != null) {
            path = Common.formatMap("{model}:countTokens", body.get("_url"));
        } else {
            path = "{model}:countTokens";
        }
        body.remove("_url");

        JsonNode queryParams = body.get("_query");
        if (queryParams != null) {
            body.remove("_query");
            path = String.format("%s?%s", path, Common.urlEncode((ObjectNode) queryParams));
        }

        // TODO: Remove the hack that removes config.
        body.remove("config");

        Optional<HttpOptions> requestHttpOptions = Optional.empty();
        if (config != null) {
            requestHttpOptions = config.httpOptions();
        }

        try (ApiResponse response =
                     this.apiClient.request(
                             "post", path, JsonSerializable.toJsonString(body), requestHttpOptions)) {
            HttpEntity entity = response.getEntity();
            String responseString;
            try {
                responseString = EntityUtils.toString(entity);
            } catch (ParseException | IOException e) {
                throw new GenAiIOException("Failed to read HTTP response.", e);
            }

            JsonNode responseNode = JsonSerializable.stringToJsonNode(responseString);
            responseNode = countTokensResponseFromMldev(this.apiClient, responseNode, null);
            return JsonSerializable.fromJsonNode(responseNode, CountTokensResponse.class);
        }
    }

    /**
     * Generates videos given a GenAI model, and a prompt or an image.
     *
     * <p>This method is experimental.
     *
     * @param model  the name of the GenAI model to use for generating videos
     * @param prompt the text prompt for generating the videos. Optional for image to video use cases.
     * @param image  the input image for generating the videos. Optional if prompt is provided.
     * @param config a {@link com.google.genai.types.GenerateVideosConfig} instance that specifies the
     *               optional configurations
     * @return a {@link com.google.genai.types.GenerateVideosOperation} instance that contains the
     * generated videos.
     */
    public GenerateVideosOperation generateVideos(
            String model, String prompt, Image image, GenerateVideosConfig config) {

        GenerateVideosParameters.Builder parameterBuilder = GenerateVideosParameters.builder();

        if (!Common.isZero(model)) {
            parameterBuilder.model(model);
        }
        if (!Common.isZero(prompt)) {
            parameterBuilder.prompt(prompt);
        }
        if (!Common.isZero(image)) {
            parameterBuilder.image(image);
        }
        if (!Common.isZero(config)) {
            parameterBuilder.config(config);
        }
        JsonNode parameterNode = JsonSerializable.toJsonNode(parameterBuilder.build());

        ObjectNode body;
        String path;
        body = generateVideosParametersToMldev(this.apiClient, parameterNode, null);
        if (body.get("_url") != null) {
            path = Common.formatMap("{model}:predictLongRunning", body.get("_url"));
        } else {
            path = "{model}:predictLongRunning";
        }
        body.remove("_url");

        JsonNode queryParams = body.get("_query");
        if (queryParams != null) {
            body.remove("_query");
            path = String.format("%s?%s", path, Common.urlEncode((ObjectNode) queryParams));
        }

        // TODO: Remove the hack that removes config.
        body.remove("config");

        Optional<HttpOptions> requestHttpOptions = Optional.empty();
        if (config != null) {
            requestHttpOptions = config.httpOptions();
        }

        try (ApiResponse response =
                     this.apiClient.request(
                             "post", path, JsonSerializable.toJsonString(body), requestHttpOptions)) {
            HttpEntity entity = response.getEntity();
            String responseString;
            try {
                responseString = EntityUtils.toString(entity);
            } catch (ParseException | IOException e) {
                throw new GenAiIOException("Failed to read HTTP response.", e);
            }

            JsonNode responseNode = JsonSerializable.stringToJsonNode(responseString);
            responseNode = generateVideosOperationFromMldev(this.apiClient, responseNode, null);
            return JsonSerializable.fromJsonNode(responseNode, GenerateVideosOperation.class);
        }
    }

    /**
     * Generates content given a GenAI model and a list of content.
     *
     * @param model    the name of the GenAI model to use for generation
     * @param contents a {@link List<com.google.genai.types.Content>} to send to the generative model
     * @param config   a {@link com.google.genai.types.GenerateContentConfig} instance that specifies
     *                 the optional configurations
     * @return a {@link com.google.genai.types.GenerateContentResponse} instance that contains
     * response contents and other metadata
     */
    public GenerateContentResponse generateContent(
            String model, List<Content> contents, GenerateContentConfig config) {
        GenerateContentConfig transformedConfig =
                AfcUtil.transformGenerateContentConfig(apiClient, config);
        if (AfcUtil.shouldDisableAfc(transformedConfig)) {
            return privateGenerateContent(model, contents, transformedConfig);
        }
        ImmutableMap<String, Method> functionMap = AfcUtil.getFunctionMap(config);
        if (functionMap.isEmpty()) {
            return privateGenerateContent(model, contents, transformedConfig);
        }
        int remainingRemoteCalls = AfcUtil.getMaxRemoteCallsAfc(transformedConfig);
        int i = 0;
        logger.info(
                String.format(
                        "Automatic function calling is enabled with max remote calls: %d",
                        remainingRemoteCalls));
        GenerateContentResponse response = null;
        List<Content> automaticFunctionCallingHistory = new ArrayList<>(contents);
        while (remainingRemoteCalls > 0) {
            i++;
            response = privateGenerateContent(model, contents, transformedConfig);
            logger.info(String.format("Automatic function calling remote call %d is done", i));
            remainingRemoteCalls--;
            if (remainingRemoteCalls == 0) {
                logger.info("Reached max remote calls for automatic function calling.");
            }
            if (!response.candidates().isPresent()
                    || response.candidates().get().isEmpty()
                    || !response.candidates().get().get(0).content().isPresent()
                    || !response.candidates().get().get(0).content().get().parts().isPresent()
                    || response.candidates().get().get(0).content().get().parts().get().isEmpty()) {
                break;
            }
            ImmutableList<Part> functionResponseParts =
                    AfcUtil.getFunctionResponseParts(response, functionMap);
            if (functionResponseParts.isEmpty()) {
                break;
            }
            Content functionCallContent = response.candidates().get().get(0).content().get();
            Content functionResponseContent =
                    Content.builder().role("user").parts(functionResponseParts).build();
            automaticFunctionCallingHistory.add(functionCallContent);
            automaticFunctionCallingHistory.add(functionResponseContent);
            contents = automaticFunctionCallingHistory;
        }
        if (AfcUtil.shouldAppendAfcHistory(transformedConfig)) {
            ObjectNode responseNode = JsonSerializable.objectMapper.valueToTree(response);
            responseNode.set(
                    "automaticFunctionCallingHistory",
                    JsonSerializable.objectMapper.valueToTree(automaticFunctionCallingHistory));
            response = JsonSerializable.fromJsonNode(responseNode, GenerateContentResponse.class);
        }
        return response;
    }

    /**
     * Generates content given a GenAI model and a content object.
     *
     * @param model   the name of the GenAI model to use for generation
     * @param content a {@link com.google.genai.types.Content} to send to the generative model
     * @param config  a {@link com.google.genai.types.GenerateContentConfig} instance that specifies
     *                the optional configurations
     * @return a {@link com.google.genai.types.GenerateContentResponse} instance that contains
     * response contents and other metadata
     */
    public GenerateContentResponse generateContent(
            String model, Content content, GenerateContentConfig config) {
        return generateContent(model, Transformers.tContents(content), config);
    }

    /**
     * Generates content given a GenAI model and a text string.
     *
     * @param model  the name of the GenAI model to use for generation
     * @param text   the text string to send to the generative model
     * @param config a {@link com.google.genai.types.GenerateContentConfig} instance that specifies
     *               the optional configurations
     * @return a {@link com.google.genai.types.GenerateContentResponse} instance that contains
     * response contents and other metadata
     */
    public GenerateContentResponse generateContent(
            String model, String text, GenerateContentConfig config) {
        return generateContent(model, Transformers.tContents(text), config);
    }

    /**
     * Generates content with streaming support given a GenAI model and a list of content.
     *
     * @param model    the name of the GenAI model to use for generation
     * @param contents a {@link List<com.google.genai.types.Content>} to send to the generative model
     * @param config   a {@link com.google.genai.types.GenerateContentConfig} instance that specifies
     *                 the optional configurations
     * @return a {@link com.google.genai.types.GenerateContentResponse} instance that contains
     * response contents and other metadata
     */
    public ResponseStream<GenerateContentResponse> generateContentStream(
            String model, List<Content> contents, GenerateContentConfig config) {
        GenerateContentConfig transformedConfig =
                AfcUtil.transformGenerateContentConfig(apiClient, config);
        if (AfcUtil.hasCallableTool(apiClient, config)
                && !AfcUtil.shouldDisableAfc(transformedConfig)) {
            logger.warning(
                    "In generateContentStream method, detected that automatic function calling is enabled in"
                            + " the config.AutomaticFunctionCalling(), and callable tool is present in the"
                            + " config.tools() list. Automatic function calling is not supported in streaming"
                            + " methods at the moment, will just return the function call parts from model if"
                            + " there is any.");
        }
        return privateGenerateContentStream(model, contents, transformedConfig);
    }

    /**
     * Generates content with streaming support given a GenAI model and a content object.
     *
     * @param model   the name of the GenAI model to use for generation
     * @param content a {@link com.google.genai.types.Content} to send to the generative model
     * @param config  a {@link com.google.genai.types.GenerateContentConfig} instance that specifies
     *                the optional configurations
     * @return a {@link com.google.genai.types.GenerateContentResponse} instance that contains
     * response contents and other metadata
     */
    public ResponseStream<GenerateContentResponse> generateContentStream(
            String model, Content content, GenerateContentConfig config) {
        return generateContentStream(model, Transformers.tContents(content), config);
    }

    /**
     * Generates content with streaming support given a GenAI model and a text string.
     *
     * @param model  the name of the GenAI model to use for generation
     * @param text   the text string to send to the generative model
     * @param config a {@link com.google.genai.types.GenerateContentConfig} instance that specifies
     *               the optional configurations
     * @return a {@link com.google.genai.types.GenerateContentResponse} instance that contains
     * response contents and other metadata
     */
    public ResponseStream<GenerateContentResponse> generateContentStream(
            String model, String text, GenerateContentConfig config) {
        return generateContentStream(model, Transformers.tContents(text), config);
    }

    /**
     * Counts tokens given a GenAI model and a text string.
     *
     * @param model  the name of the GenAI model to use.
     * @param text   the text string to send to count tokens for.
     * @param config a {@link com.google.genai.types.CountTokensConfig} instance that specifies the
     *               optional configurations
     * @return a {@link com.google.genai.types.CountTokensResponse} instance that contains tokens
     * count.
     */
    public CountTokensResponse countTokens(String model, String text, CountTokensConfig config) {
        return countTokens(model, Transformers.tContents(text), config);
    }

    /**
     * Generates images given a GenAI model and a prompt.
     *
     * @param model  the name of the GenAI model to use for generating images
     * @param prompt the prompt to generate images
     * @param config a {@link com.google.genai.types.GenerateImagesConfig} instance that specifies the
     *               optional configurations
     * @return a {@link com.google.genai.types.GenerateImagesResponse} instance that contains the
     * generated images.
     */
    public GenerateImagesResponse generateImages(
            String model, String prompt, GenerateImagesConfig config) {

        GenerateImagesResponse apiResponse = privateGenerateImages(model, prompt, config);

        SafetyAttributes positivePromptSafetyAttributes = null;
        List<GeneratedImage> generatedImages = new ArrayList<>();

        if (apiResponse.generatedImages().isPresent()) {
            for (GeneratedImage generatedImage : apiResponse.generatedImages().get()) {
                if (generatedImage.safetyAttributes().isPresent()
                        && generatedImage.safetyAttributes().get().contentType().isPresent()
                        && generatedImage
                        .safetyAttributes()
                        .get()
                        .contentType()
                        .get()
                        .equals("Positive Prompt")) {
                    positivePromptSafetyAttributes = generatedImage.safetyAttributes().get();
                } else {
                    generatedImages.add(generatedImage);
                }
            }
        }

        GenerateImagesResponse.Builder builder =
                GenerateImagesResponse.builder().generatedImages(generatedImages);

        if (positivePromptSafetyAttributes != null) {
            builder = builder.positivePromptSafetyAttributes(positivePromptSafetyAttributes);
        }

        GenerateImagesResponse response = builder.build();
        return response;
    }


    /**
     * Embeds content given a GenAI model and a text string.
     *
     * @param model the name of the GenAI model to use for embedding
     * @param text  the text string to send to the embedding model
     * @return a {@link com.google.genai.types.EmbedContentResponse} instance that contains the
     * embedding.
     */
    public EmbedContentResponse embedContent(String model, String text, EmbedContentConfig config) {
        return embedContent(model, ImmutableList.of(text), config);
    }

    /**
     * Embeds content given a GenAI model and a list of text strings.
     *
     * @param model the name of the GenAI model to use for embedding
     * @param texts the list of text strings to send to the embedding model
     * @return a {@link com.google.genai.types.EmbedContentResponse} instance that contains the
     * embedding.
     */
    public EmbedContentResponse embedContent(
            String model, List<String> texts, EmbedContentConfig config) {
        List<Content> contents = new ArrayList<>();
        for (String text : texts) {
            contents.add(Content.fromParts(Part.fromText(text)));
        }
        return privateEmbedContent(model, contents, config);
    }

    /**
     * Makes an API request to list the available models.
     *
     * <p>If `queryBase` is set to True in the {@link ListModelsConfig} or not set (default), the API
     * will return all available base models. If set to False, it will return all tuned models.
     *
     * @param config A {@link ListModelsConfig} for configuring the list request.
     * @return A {@link Pager} object that contains the list of models. The pager is an iterable and
     * automatically queries the next page once the current page is exhausted.
     */
    @SuppressWarnings("PatternMatchingInstanceof")
    public Pager<Model> list(ListModelsConfig config) {
        if (config == null) {
            config = ListModelsConfig.builder().build();
        }
        if (config.filter().isPresent()) {
            throw new IllegalArgumentException("Filter is currently not supported for list models.");
        }
        ListModelsConfig.Builder configBuilder = config.toBuilder();
        if (!config.queryBase().isPresent()) {
            configBuilder.queryBase(true);
        }
        config = configBuilder.build();

        Function<JsonSerializable, Object> request =
                requestConfig -> {
                    if (!(requestConfig instanceof ListModelsConfig)) {
                        throw new GenAiIOException(
                                "Internal error: Pager expected ListModelsConfig but received "
                                        + requestConfig.getClass().getName());
                    }
                    return this.privateList((ListModelsConfig) requestConfig);
                };
        return new Pager<>(
                Pager.PagedItem.MODELS,
                request,
                (ObjectNode) JsonSerializable.toJsonNode(config),
                JsonSerializable.toJsonNode(privateList(config)));
    }
}
