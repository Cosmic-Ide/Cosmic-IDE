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
import com.google.genai.errors.GenAiIOException;
import com.google.genai.types.CachedContent;
import com.google.genai.types.CreateCachedContentConfig;
import com.google.genai.types.CreateCachedContentParameters;
import com.google.genai.types.DeleteCachedContentConfig;
import com.google.genai.types.DeleteCachedContentParameters;
import com.google.genai.types.DeleteCachedContentResponse;
import com.google.genai.types.GetCachedContentConfig;
import com.google.genai.types.GetCachedContentParameters;
import com.google.genai.types.HttpOptions;
import com.google.genai.types.ListCachedContentsConfig;
import com.google.genai.types.ListCachedContentsParameters;
import com.google.genai.types.ListCachedContentsResponse;

import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;

import java.io.IOException;
import java.util.Optional;
import java.util.function.Function;

/**
 * Provides methods for managing the cached content. Instantiating this class is not required. After
 * instantiating a {@link Client}, access methods through `client.caches.methodName(...)` directly.
 */
public final class Caches {
    final ApiClient apiClient;

    public Caches(ApiClient apiClient) {
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
    ObjectNode createCachedContentConfigToMldev(
            ApiClient apiClient, JsonNode fromObject, ObjectNode parentObject) {
        ObjectNode toObject = JsonSerializable.objectMapper.createObjectNode();

        if (Common.getValueByPath(fromObject, new String[]{"ttl"}) != null) {
            Common.setValueByPath(
                    parentObject,
                    new String[]{"ttl"},
                    Common.getValueByPath(fromObject, new String[]{"ttl"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"expireTime"}) != null) {
            Common.setValueByPath(
                    parentObject,
                    new String[]{"expireTime"},
                    Common.getValueByPath(fromObject, new String[]{"expireTime"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"displayName"}) != null) {
            Common.setValueByPath(
                    parentObject,
                    new String[]{"displayName"},
                    Common.getValueByPath(fromObject, new String[]{"displayName"}));
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
            Common.setValueByPath(parentObject, new String[]{"contents"}, result);
        }

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

        if (Common.getValueByPath(fromObject, new String[]{"tools"}) != null) {
            ArrayNode keyArray = (ArrayNode) Common.getValueByPath(fromObject, new String[]{"tools"});
            ObjectMapper objectMapper = new ObjectMapper();
            ArrayNode result = objectMapper.createArrayNode();

            for (JsonNode item : keyArray) {
                result.add(toolToMldev(apiClient, JsonSerializable.toJsonNode(item), toObject));
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

        if (!Common.isZero(Common.getValueByPath(fromObject, new String[]{"kmsKeyName"}))) {
            throw new IllegalArgumentException("kmsKeyName parameter is not supported in Gemini API.");
        }

        return toObject;
    }

    @ExcludeFromGeneratedCoverageReport
    ObjectNode createCachedContentParametersToMldev(
            ApiClient apiClient, JsonNode fromObject, ObjectNode parentObject) {
        ObjectNode toObject = JsonSerializable.objectMapper.createObjectNode();
        if (Common.getValueByPath(fromObject, new String[]{"model"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"model"},
                    Transformers.tCachesModel(
                            this.apiClient, Common.getValueByPath(fromObject, new String[]{"model"})));
        }

        if (Common.getValueByPath(fromObject, new String[]{"config"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"config"},
                    createCachedContentConfigToMldev(
                            apiClient,
                            JsonSerializable.toJsonNode(
                                    Common.getValueByPath(fromObject, new String[]{"config"})),
                            toObject));
        }

        return toObject;
    }

    @ExcludeFromGeneratedCoverageReport
    ObjectNode getCachedContentParametersToMldev(
            ApiClient apiClient, JsonNode fromObject, ObjectNode parentObject) {
        ObjectNode toObject = JsonSerializable.objectMapper.createObjectNode();
        if (Common.getValueByPath(fromObject, new String[]{"name"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"_url", "name"},
                    Transformers.tCachedContentName(
                            Common.getValueByPath(fromObject, new String[]{"name"})));
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
    ObjectNode deleteCachedContentParametersToMldev(
            ApiClient apiClient, JsonNode fromObject, ObjectNode parentObject) {
        ObjectNode toObject = JsonSerializable.objectMapper.createObjectNode();
        if (Common.getValueByPath(fromObject, new String[]{"name"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"_url", "name"},
                    Transformers.tCachedContentName(
                            Common.getValueByPath(fromObject, new String[]{"name"})));
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
    ObjectNode listCachedContentsConfigToMldev(
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

        return toObject;
    }

    @ExcludeFromGeneratedCoverageReport
    ObjectNode listCachedContentsParametersToMldev(
            ApiClient apiClient, JsonNode fromObject, ObjectNode parentObject) {
        ObjectNode toObject = JsonSerializable.objectMapper.createObjectNode();
        if (Common.getValueByPath(fromObject, new String[]{"config"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"config"},
                    listCachedContentsConfigToMldev(
                            apiClient,
                            JsonSerializable.toJsonNode(
                                    Common.getValueByPath(fromObject, new String[]{"config"})),
                            toObject));
        }

        return toObject;
    }

    @ExcludeFromGeneratedCoverageReport
    ObjectNode cachedContentFromMldev(
            ApiClient apiClient, JsonNode fromObject, ObjectNode parentObject) {
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

        if (Common.getValueByPath(fromObject, new String[]{"model"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"model"},
                    Common.getValueByPath(fromObject, new String[]{"model"}));
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

        if (Common.getValueByPath(fromObject, new String[]{"expireTime"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"expireTime"},
                    Common.getValueByPath(fromObject, new String[]{"expireTime"}));
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
    ObjectNode deleteCachedContentResponseFromMldev(
            ApiClient apiClient, JsonNode fromObject, ObjectNode parentObject) {
        ObjectNode toObject = JsonSerializable.objectMapper.createObjectNode();

        return toObject;
    }

    @ExcludeFromGeneratedCoverageReport
    ObjectNode listCachedContentsResponseFromMldev(
            ApiClient apiClient, JsonNode fromObject, ObjectNode parentObject) {
        ObjectNode toObject = JsonSerializable.objectMapper.createObjectNode();
        if (Common.getValueByPath(fromObject, new String[]{"nextPageToken"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"nextPageToken"},
                    Common.getValueByPath(fromObject, new String[]{"nextPageToken"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"cachedContents"}) != null) {
            ArrayNode keyArray =
                    (ArrayNode) Common.getValueByPath(fromObject, new String[]{"cachedContents"});
            ObjectMapper objectMapper = new ObjectMapper();
            ArrayNode result = objectMapper.createArrayNode();

            for (JsonNode item : keyArray) {
                result.add(cachedContentFromMldev(apiClient, JsonSerializable.toJsonNode(item), toObject));
            }
            Common.setValueByPath(toObject, new String[]{"cachedContents"}, result);
        }

        return toObject;
    }

    /**
     * Creates a cached content resource.
     *
     * @param model  The model to use.
     * @param config A {@link CreateCachedContentConfig} for configuring the create request.
     * @return A {@link CachedContent} object that contains the info of the created resource.
     */
    public CachedContent create(String model, CreateCachedContentConfig config) {

        CreateCachedContentParameters.Builder parameterBuilder =
                CreateCachedContentParameters.builder();

        if (!Common.isZero(model)) {
            parameterBuilder.model(model);
        }
        if (!Common.isZero(config)) {
            parameterBuilder.config(config);
        }
        JsonNode parameterNode = JsonSerializable.toJsonNode(parameterBuilder.build());

        ObjectNode body;
        String path;
        body = createCachedContentParametersToMldev(this.apiClient, parameterNode, null);
        if (body.get("_url") != null) {
            path = Common.formatMap("cachedContents", body.get("_url"));
        } else {
            path = "cachedContents";
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
            responseNode = cachedContentFromMldev(this.apiClient, responseNode, null);
            return JsonSerializable.fromJsonNode(responseNode, CachedContent.class);
        }
    }

    /**
     * Gets a cached content resource.
     *
     * @param name   The name(resource id) of the cached content to get.
     * @param config A {@link GetCachedContentConfig} for configuring the get request.
     * @return A {@link CachedContent} object that contains the info of the cached content.
     */
    public CachedContent get(String name, GetCachedContentConfig config) {

        GetCachedContentParameters.Builder parameterBuilder = GetCachedContentParameters.builder();

        if (!Common.isZero(name)) {
            parameterBuilder.name(name);
        }
        if (!Common.isZero(config)) {
            parameterBuilder.config(config);
        }
        JsonNode parameterNode = JsonSerializable.toJsonNode(parameterBuilder.build());

        ObjectNode body;
        String path;
        body = getCachedContentParametersToMldev(this.apiClient, parameterNode, null);
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
            responseNode = cachedContentFromMldev(this.apiClient, responseNode, null);
            return JsonSerializable.fromJsonNode(responseNode, CachedContent.class);
        }
    }

    /**
     * Deletes a cached content resource.
     *
     * @param name   The name(resource id) of the cached content to delete.
     * @param config A {@link DeleteCachedContentConfig} for configuring the delete request.
     */
    public DeleteCachedContentResponse delete(String name, DeleteCachedContentConfig config) {

        DeleteCachedContentParameters.Builder parameterBuilder =
                DeleteCachedContentParameters.builder();

        if (!Common.isZero(name)) {
            parameterBuilder.name(name);
        }
        if (!Common.isZero(config)) {
            parameterBuilder.config(config);
        }
        JsonNode parameterNode = JsonSerializable.toJsonNode(parameterBuilder.build());

        ObjectNode body;
        String path;
        body = deleteCachedContentParametersToMldev(this.apiClient, parameterNode, null);
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
            responseNode = deleteCachedContentResponseFromMldev(this.apiClient, responseNode, null);
            return JsonSerializable.fromJsonNode(responseNode, DeleteCachedContentResponse.class);
        }
    }

    ListCachedContentsResponse privateList(ListCachedContentsConfig config) {

        ListCachedContentsParameters.Builder parameterBuilder = ListCachedContentsParameters.builder();

        if (!Common.isZero(config)) {
            parameterBuilder.config(config);
        }
        JsonNode parameterNode = JsonSerializable.toJsonNode(parameterBuilder.build());

        ObjectNode body;
        String path;
        body = listCachedContentsParametersToMldev(this.apiClient, parameterNode, null);
        if (body.get("_url") != null) {
            path = Common.formatMap("cachedContents", body.get("_url"));
        } else {
            path = "cachedContents";
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
            responseNode = listCachedContentsResponseFromMldev(this.apiClient, responseNode, null);
            return JsonSerializable.fromJsonNode(responseNode, ListCachedContentsResponse.class);
        }
    }

    /**
     * Makes an API request to list the available cached contents.
     *
     * @param config A {@link ListCachedContentsConfig} for configuring the list request.
     * @return A {@link Pager} object that contains the list of cached contents. The pager is an
     * iterable and automatically queries the next page once the current page is exhausted.
     */
    @SuppressWarnings("PatternMatchingInstanceof")
    public Pager<CachedContent> list(ListCachedContentsConfig config) {
        Function<JsonSerializable, Object> request =
                requestConfig -> {
                    if (!(requestConfig instanceof ListCachedContentsConfig)) {
                        throw new GenAiIOException(
                                "Internal error: Pager expected ListCachedContentsConfig but received "
                                        + requestConfig.getClass().getName());
                    }
                    return this.privateList((ListCachedContentsConfig) requestConfig);
                };
        return new Pager<>(
                Pager.PagedItem.CACHED_CONTENTS,
                request,
                (ObjectNode) JsonSerializable.toJsonNode(config),
                JsonSerializable.toJsonNode(privateList(config)));
    }
}
