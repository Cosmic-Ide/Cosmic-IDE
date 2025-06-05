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
import com.google.genai.types.FetchPredictOperationConfig;
import com.google.genai.types.FetchPredictOperationParameters;
import com.google.genai.types.GenerateVideosOperation;
import com.google.genai.types.GetOperationConfig;
import com.google.genai.types.GetOperationParameters;
import com.google.genai.types.HttpOptions;

import java.io.IOException;
import java.util.Optional;

import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;

/**
 * Provides methods for managing the long-running operations. Instantiating this class is not
 * required. After instantiating a {@link Client}, access methods through
 * `client.operations.methodName(...)` directly.
 *
 * <p>This module is experimental.
 */
public final class Operations {
    final ApiClient apiClient;

    public Operations(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    @ExcludeFromGeneratedCoverageReport
    ObjectNode getOperationParametersToMldev(
            ApiClient apiClient, JsonNode fromObject, ObjectNode parentObject) {
        ObjectNode toObject = JsonSerializable.objectMapper.createObjectNode();
        if (Common.getValueByPath(fromObject, new String[]{"operationName"}) != null) {
            Common.setValueByPath(
                    toObject,
                    new String[]{"_url", "operationName"},
                    Common.getValueByPath(fromObject, new String[]{"operationName"}));
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


    GenerateVideosOperation privateGetVideosOperation(
            String operationName, GetOperationConfig config) {

        GetOperationParameters.Builder parameterBuilder = GetOperationParameters.builder();

        if (!Common.isZero(operationName)) {
            parameterBuilder.operationName(operationName);
        }
        if (!Common.isZero(config)) {
            parameterBuilder.config(config);
        }
        JsonNode parameterNode = JsonSerializable.toJsonNode(parameterBuilder.build());

        ObjectNode body;
        String path;
        body = getOperationParametersToMldev(this.apiClient, parameterNode, null);
        if (body.get("_url") != null) {
            path = Common.formatMap("{operationName}", body.get("_url"));
        } else {
            path = "{operationName}";
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
            responseNode = generateVideosOperationFromMldev(this.apiClient, responseNode, null);
            return JsonSerializable.fromJsonNode(responseNode, GenerateVideosOperation.class);
        }
    }

    /**
     * Gets the status of a GenerateVideosOperation.
     *
     * @param operation A GenerateVideosOperation.
     * @param config    The configuration for getting the operation.
     * @return A GenerateVideosOperation with the updated status of the operation.
     */
    public GenerateVideosOperation getVideosOperation(
            GenerateVideosOperation operation, GetOperationConfig config) {

        if (!operation.name().isPresent()) {
            throw new Error("Operation name is required.");
        }

        return this.privateGetVideosOperation(operation.name().get(), config);
    }
}
