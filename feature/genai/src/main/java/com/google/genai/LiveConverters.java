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

final class LiveConverters {
    private final ApiClient apiClient;

    public LiveConverters(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    @ExcludeFromGeneratedCoverageReport
    ObjectNode prebuiltVoiceConfigToMldev(ApiClient apiClient, JsonNode fromObject, ObjectNode parentObject) {
        ObjectNode toObject = JsonSerializable.objectMapper.createObjectNode();
        if (Common.getValueByPath(fromObject, new String[]{"voiceName"}) != null) {
            Common.setValueByPath(toObject, new String[]{"voiceName"}, Common.getValueByPath(fromObject, new String[]{"voiceName"}));
        }

        return toObject;
    }

    @ExcludeFromGeneratedCoverageReport
    ObjectNode voiceConfigToMldev(ApiClient apiClient, JsonNode fromObject, ObjectNode parentObject) {
        ObjectNode toObject = JsonSerializable.objectMapper.createObjectNode();
        if (Common.getValueByPath(fromObject, new String[]{"prebuiltVoiceConfig"}) != null) {
            Common.setValueByPath(toObject, new String[]{"prebuiltVoiceConfig"}, prebuiltVoiceConfigToMldev(apiClient, JsonSerializable.toJsonNode(Common.getValueByPath(fromObject, new String[]{"prebuiltVoiceConfig"})), toObject));
        }

        return toObject;
    }

    @ExcludeFromGeneratedCoverageReport
    ObjectNode speakerVoiceConfigToMldev(ApiClient apiClient, JsonNode fromObject, ObjectNode parentObject) {
        ObjectNode toObject = JsonSerializable.objectMapper.createObjectNode();
        if (Common.getValueByPath(fromObject, new String[]{"speaker"}) != null) {
            Common.setValueByPath(toObject, new String[]{"speaker"}, Common.getValueByPath(fromObject, new String[]{"speaker"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"voiceConfig"}) != null) {
            Common.setValueByPath(toObject, new String[]{"voiceConfig"}, voiceConfigToMldev(apiClient, JsonSerializable.toJsonNode(Common.getValueByPath(fromObject, new String[]{"voiceConfig"})), toObject));
        }

        return toObject;
    }

    @ExcludeFromGeneratedCoverageReport
    ObjectNode multiSpeakerVoiceConfigToMldev(ApiClient apiClient, JsonNode fromObject, ObjectNode parentObject) {
        ObjectNode toObject = JsonSerializable.objectMapper.createObjectNode();
        if (Common.getValueByPath(fromObject, new String[]{"speakerVoiceConfigs"}) != null) {
            ArrayNode keyArray = (ArrayNode) Common.getValueByPath(fromObject, new String[]{"speakerVoiceConfigs"});
            ObjectMapper objectMapper = new ObjectMapper();
            ArrayNode result = objectMapper.createArrayNode();

            for (JsonNode item : keyArray) {
                result.add(speakerVoiceConfigToMldev(apiClient, JsonSerializable.toJsonNode(item), toObject));
            }
            Common.setValueByPath(toObject, new String[]{"speakerVoiceConfigs"}, result);
        }

        return toObject;
    }


    @ExcludeFromGeneratedCoverageReport
    ObjectNode speechConfigToMldev(ApiClient apiClient, JsonNode fromObject, ObjectNode parentObject) {
        ObjectNode toObject = JsonSerializable.objectMapper.createObjectNode();
        if (Common.getValueByPath(fromObject, new String[]{"voiceConfig"}) != null) {
            Common.setValueByPath(toObject, new String[]{"voiceConfig"}, voiceConfigToMldev(apiClient, JsonSerializable.toJsonNode(Common.getValueByPath(fromObject, new String[]{"voiceConfig"})), toObject));
        }

        if (Common.getValueByPath(fromObject, new String[]{"multiSpeakerVoiceConfig"}) != null) {
            Common.setValueByPath(toObject, new String[]{"multiSpeakerVoiceConfig"}, multiSpeakerVoiceConfigToMldev(apiClient, JsonSerializable.toJsonNode(Common.getValueByPath(fromObject, new String[]{"multiSpeakerVoiceConfig"})), toObject));
        }

        if (Common.getValueByPath(fromObject, new String[]{"languageCode"}) != null) {
            Common.setValueByPath(toObject, new String[]{"languageCode"}, Common.getValueByPath(fromObject, new String[]{"languageCode"}));
        }

        return toObject;
    }


    @ExcludeFromGeneratedCoverageReport
    ObjectNode videoMetadataToMldev(ApiClient apiClient, JsonNode fromObject, ObjectNode parentObject) {
        ObjectNode toObject = JsonSerializable.objectMapper.createObjectNode();
        if (Common.getValueByPath(fromObject, new String[]{"fps"}) != null) {
            Common.setValueByPath(toObject, new String[]{"fps"}, Common.getValueByPath(fromObject, new String[]{"fps"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"endOffset"}) != null) {
            Common.setValueByPath(toObject, new String[]{"endOffset"}, Common.getValueByPath(fromObject, new String[]{"endOffset"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"startOffset"}) != null) {
            Common.setValueByPath(toObject, new String[]{"startOffset"}, Common.getValueByPath(fromObject, new String[]{"startOffset"}));
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
            Common.setValueByPath(toObject, new String[]{"data"}, Common.getValueByPath(fromObject, new String[]{"data"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"mimeType"}) != null) {
            Common.setValueByPath(toObject, new String[]{"mimeType"}, Common.getValueByPath(fromObject, new String[]{"mimeType"}));
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
            Common.setValueByPath(toObject, new String[]{"fileUri"}, Common.getValueByPath(fromObject, new String[]{"fileUri"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"mimeType"}) != null) {
            Common.setValueByPath(toObject, new String[]{"mimeType"}, Common.getValueByPath(fromObject, new String[]{"mimeType"}));
        }

        return toObject;
    }

    @ExcludeFromGeneratedCoverageReport
    ObjectNode partToMldev(ApiClient apiClient, JsonNode fromObject, ObjectNode parentObject) {
        ObjectNode toObject = JsonSerializable.objectMapper.createObjectNode();
        if (Common.getValueByPath(fromObject, new String[]{"videoMetadata"}) != null) {
            Common.setValueByPath(toObject, new String[]{"videoMetadata"}, videoMetadataToMldev(apiClient, JsonSerializable.toJsonNode(Common.getValueByPath(fromObject, new String[]{"videoMetadata"})), toObject));
        }

        if (Common.getValueByPath(fromObject, new String[]{"thought"}) != null) {
            Common.setValueByPath(toObject, new String[]{"thought"}, Common.getValueByPath(fromObject, new String[]{"thought"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"inlineData"}) != null) {
            Common.setValueByPath(toObject, new String[]{"inlineData"}, blobToMldev(apiClient, JsonSerializable.toJsonNode(Common.getValueByPath(fromObject, new String[]{"inlineData"})), toObject));
        }

        if (Common.getValueByPath(fromObject, new String[]{"fileData"}) != null) {
            Common.setValueByPath(toObject, new String[]{"fileData"}, fileDataToMldev(apiClient, JsonSerializable.toJsonNode(Common.getValueByPath(fromObject, new String[]{"fileData"})), toObject));
        }

        if (Common.getValueByPath(fromObject, new String[]{"thoughtSignature"}) != null) {
            Common.setValueByPath(toObject, new String[]{"thoughtSignature"}, Common.getValueByPath(fromObject, new String[]{"thoughtSignature"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"codeExecutionResult"}) != null) {
            Common.setValueByPath(toObject, new String[]{"codeExecutionResult"}, Common.getValueByPath(fromObject, new String[]{"codeExecutionResult"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"executableCode"}) != null) {
            Common.setValueByPath(toObject, new String[]{"executableCode"}, Common.getValueByPath(fromObject, new String[]{"executableCode"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"functionCall"}) != null) {
            Common.setValueByPath(toObject, new String[]{"functionCall"}, Common.getValueByPath(fromObject, new String[]{"functionCall"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"functionResponse"}) != null) {
            Common.setValueByPath(toObject, new String[]{"functionResponse"}, Common.getValueByPath(fromObject, new String[]{"functionResponse"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"text"}) != null) {
            Common.setValueByPath(toObject, new String[]{"text"}, Common.getValueByPath(fromObject, new String[]{"text"}));
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
            Common.setValueByPath(toObject, new String[]{"role"}, Common.getValueByPath(fromObject, new String[]{"role"}));
        }

        return toObject;
    }


    @ExcludeFromGeneratedCoverageReport
    ObjectNode functionDeclarationToMldev(ApiClient apiClient, JsonNode fromObject, ObjectNode parentObject) {
        ObjectNode toObject = JsonSerializable.objectMapper.createObjectNode();
        if (Common.getValueByPath(fromObject, new String[]{"behavior"}) != null) {
            Common.setValueByPath(toObject, new String[]{"behavior"}, Common.getValueByPath(fromObject, new String[]{"behavior"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"description"}) != null) {
            Common.setValueByPath(toObject, new String[]{"description"}, Common.getValueByPath(fromObject, new String[]{"description"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"name"}) != null) {
            Common.setValueByPath(toObject, new String[]{"name"}, Common.getValueByPath(fromObject, new String[]{"name"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"parameters"}) != null) {
            Common.setValueByPath(toObject, new String[]{"parameters"}, Common.getValueByPath(fromObject, new String[]{"parameters"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"response"}) != null) {
            Common.setValueByPath(toObject, new String[]{"response"}, Common.getValueByPath(fromObject, new String[]{"response"}));
        }

        return toObject;
    }


    @ExcludeFromGeneratedCoverageReport
    ObjectNode intervalToMldev(ApiClient apiClient, JsonNode fromObject, ObjectNode parentObject) {
        ObjectNode toObject = JsonSerializable.objectMapper.createObjectNode();
        if (Common.getValueByPath(fromObject, new String[]{"startTime"}) != null) {
            Common.setValueByPath(toObject, new String[]{"startTime"}, Common.getValueByPath(fromObject, new String[]{"startTime"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"endTime"}) != null) {
            Common.setValueByPath(toObject, new String[]{"endTime"}, Common.getValueByPath(fromObject, new String[]{"endTime"}));
        }

        return toObject;
    }


    @ExcludeFromGeneratedCoverageReport
    ObjectNode googleSearchToMldev(ApiClient apiClient, JsonNode fromObject, ObjectNode parentObject) {
        ObjectNode toObject = JsonSerializable.objectMapper.createObjectNode();
        if (Common.getValueByPath(fromObject, new String[]{"timeRangeFilter"}) != null) {
            Common.setValueByPath(toObject, new String[]{"timeRangeFilter"}, intervalToMldev(apiClient, JsonSerializable.toJsonNode(Common.getValueByPath(fromObject, new String[]{"timeRangeFilter"})), toObject));
        }

        return toObject;
    }


    @ExcludeFromGeneratedCoverageReport
    ObjectNode dynamicRetrievalConfigToMldev(ApiClient apiClient, JsonNode fromObject, ObjectNode parentObject) {
        ObjectNode toObject = JsonSerializable.objectMapper.createObjectNode();
        if (Common.getValueByPath(fromObject, new String[]{"mode"}) != null) {
            Common.setValueByPath(toObject, new String[]{"mode"}, Common.getValueByPath(fromObject, new String[]{"mode"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"dynamicThreshold"}) != null) {
            Common.setValueByPath(toObject, new String[]{"dynamicThreshold"}, Common.getValueByPath(fromObject, new String[]{"dynamicThreshold"}));
        }

        return toObject;
    }

    @ExcludeFromGeneratedCoverageReport
    ObjectNode googleSearchRetrievalToMldev(ApiClient apiClient, JsonNode fromObject, ObjectNode parentObject) {
        ObjectNode toObject = JsonSerializable.objectMapper.createObjectNode();
        if (Common.getValueByPath(fromObject, new String[]{"dynamicRetrievalConfig"}) != null) {
            Common.setValueByPath(toObject, new String[]{"dynamicRetrievalConfig"}, dynamicRetrievalConfigToMldev(apiClient, JsonSerializable.toJsonNode(Common.getValueByPath(fromObject, new String[]{"dynamicRetrievalConfig"})), toObject));
        }

        return toObject;
    }

    @ExcludeFromGeneratedCoverageReport
    ObjectNode enterpriseWebSearchToMldev(ApiClient apiClient, JsonNode fromObject, ObjectNode parentObject) {
        ObjectNode toObject = JsonSerializable.objectMapper.createObjectNode();

        return toObject;
    }

    @ExcludeFromGeneratedCoverageReport
    ObjectNode apiKeyConfigToMldev(ApiClient apiClient, JsonNode fromObject, ObjectNode parentObject) {
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
            Common.setValueByPath(toObject, new String[]{"authType"}, Common.getValueByPath(fromObject, new String[]{"authType"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"googleServiceAccountConfig"}) != null) {
            Common.setValueByPath(toObject, new String[]{"googleServiceAccountConfig"}, Common.getValueByPath(fromObject, new String[]{"googleServiceAccountConfig"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"httpBasicAuthConfig"}) != null) {
            Common.setValueByPath(toObject, new String[]{"httpBasicAuthConfig"}, Common.getValueByPath(fromObject, new String[]{"httpBasicAuthConfig"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"oauthConfig"}) != null) {
            Common.setValueByPath(toObject, new String[]{"oauthConfig"}, Common.getValueByPath(fromObject, new String[]{"oauthConfig"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"oidcConfig"}) != null) {
            Common.setValueByPath(toObject, new String[]{"oidcConfig"}, Common.getValueByPath(fromObject, new String[]{"oidcConfig"}));
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
            ArrayNode keyArray = (ArrayNode) Common.getValueByPath(fromObject, new String[]{"functionDeclarations"});
            ObjectMapper objectMapper = new ObjectMapper();
            ArrayNode result = objectMapper.createArrayNode();

            for (JsonNode item : keyArray) {
                result.add(functionDeclarationToMldev(apiClient, JsonSerializable.toJsonNode(item), toObject));
            }
            Common.setValueByPath(toObject, new String[]{"functionDeclarations"}, result);
        }

        if (!Common.isZero(Common.getValueByPath(fromObject, new String[]{"retrieval"}))) {
            throw new IllegalArgumentException("retrieval parameter is not supported in Gemini API.");
        }

        if (Common.getValueByPath(fromObject, new String[]{"googleSearch"}) != null) {
            Common.setValueByPath(toObject, new String[]{"googleSearch"}, googleSearchToMldev(apiClient, JsonSerializable.toJsonNode(Common.getValueByPath(fromObject, new String[]{"googleSearch"})), toObject));
        }

        if (Common.getValueByPath(fromObject, new String[]{"googleSearchRetrieval"}) != null) {
            Common.setValueByPath(toObject, new String[]{"googleSearchRetrieval"}, googleSearchRetrievalToMldev(apiClient, JsonSerializable.toJsonNode(Common.getValueByPath(fromObject, new String[]{"googleSearchRetrieval"})), toObject));
        }

        if (!Common.isZero(Common.getValueByPath(fromObject, new String[]{"enterpriseWebSearch"}))) {
            throw new IllegalArgumentException("enterpriseWebSearch parameter is not supported in Gemini API.");
        }

        if (!Common.isZero(Common.getValueByPath(fromObject, new String[]{"googleMaps"}))) {
            throw new IllegalArgumentException("googleMaps parameter is not supported in Gemini API.");
        }

        if (Common.getValueByPath(fromObject, new String[]{"urlContext"}) != null) {
            Common.setValueByPath(toObject, new String[]{"urlContext"}, urlContextToMldev(apiClient, JsonSerializable.toJsonNode(Common.getValueByPath(fromObject, new String[]{"urlContext"})), toObject));
        }

        if (Common.getValueByPath(fromObject, new String[]{"codeExecution"}) != null) {
            Common.setValueByPath(toObject, new String[]{"codeExecution"}, Common.getValueByPath(fromObject, new String[]{"codeExecution"}));
        }

        return toObject;
    }


    @ExcludeFromGeneratedCoverageReport
    ObjectNode sessionResumptionConfigToMldev(ApiClient apiClient, JsonNode fromObject, ObjectNode parentObject) {
        ObjectNode toObject = JsonSerializable.objectMapper.createObjectNode();
        if (Common.getValueByPath(fromObject, new String[]{"handle"}) != null) {
            Common.setValueByPath(toObject, new String[]{"handle"}, Common.getValueByPath(fromObject, new String[]{"handle"}));
        }

        if (!Common.isZero(Common.getValueByPath(fromObject, new String[]{"transparent"}))) {
            throw new IllegalArgumentException("transparent parameter is not supported in Gemini API.");
        }

        return toObject;
    }


    @ExcludeFromGeneratedCoverageReport
    ObjectNode audioTranscriptionConfigToMldev(ApiClient apiClient, JsonNode fromObject, ObjectNode parentObject) {
        ObjectNode toObject = JsonSerializable.objectMapper.createObjectNode();

        return toObject;
    }


    @ExcludeFromGeneratedCoverageReport
    ObjectNode automaticActivityDetectionToMldev(ApiClient apiClient, JsonNode fromObject, ObjectNode parentObject) {
        ObjectNode toObject = JsonSerializable.objectMapper.createObjectNode();
        if (Common.getValueByPath(fromObject, new String[]{"disabled"}) != null) {
            Common.setValueByPath(toObject, new String[]{"disabled"}, Common.getValueByPath(fromObject, new String[]{"disabled"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"startOfSpeechSensitivity"}) != null) {
            Common.setValueByPath(toObject, new String[]{"startOfSpeechSensitivity"}, Common.getValueByPath(fromObject, new String[]{"startOfSpeechSensitivity"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"endOfSpeechSensitivity"}) != null) {
            Common.setValueByPath(toObject, new String[]{"endOfSpeechSensitivity"}, Common.getValueByPath(fromObject, new String[]{"endOfSpeechSensitivity"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"prefixPaddingMs"}) != null) {
            Common.setValueByPath(toObject, new String[]{"prefixPaddingMs"}, Common.getValueByPath(fromObject, new String[]{"prefixPaddingMs"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"silenceDurationMs"}) != null) {
            Common.setValueByPath(toObject, new String[]{"silenceDurationMs"}, Common.getValueByPath(fromObject, new String[]{"silenceDurationMs"}));
        }

        return toObject;
    }


    @ExcludeFromGeneratedCoverageReport
    ObjectNode realtimeInputConfigToMldev(ApiClient apiClient, JsonNode fromObject, ObjectNode parentObject) {
        ObjectNode toObject = JsonSerializable.objectMapper.createObjectNode();
        if (Common.getValueByPath(fromObject, new String[]{"automaticActivityDetection"}) != null) {
            Common.setValueByPath(toObject, new String[]{"automaticActivityDetection"}, automaticActivityDetectionToMldev(apiClient, JsonSerializable.toJsonNode(Common.getValueByPath(fromObject, new String[]{"automaticActivityDetection"})), toObject));
        }

        if (Common.getValueByPath(fromObject, new String[]{"activityHandling"}) != null) {
            Common.setValueByPath(toObject, new String[]{"activityHandling"}, Common.getValueByPath(fromObject, new String[]{"activityHandling"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"turnCoverage"}) != null) {
            Common.setValueByPath(toObject, new String[]{"turnCoverage"}, Common.getValueByPath(fromObject, new String[]{"turnCoverage"}));
        }

        return toObject;
    }


    @ExcludeFromGeneratedCoverageReport
    ObjectNode slidingWindowToMldev(ApiClient apiClient, JsonNode fromObject, ObjectNode parentObject) {
        ObjectNode toObject = JsonSerializable.objectMapper.createObjectNode();
        if (Common.getValueByPath(fromObject, new String[]{"targetTokens"}) != null) {
            Common.setValueByPath(toObject, new String[]{"targetTokens"}, Common.getValueByPath(fromObject, new String[]{"targetTokens"}));
        }

        return toObject;
    }

    @ExcludeFromGeneratedCoverageReport
    ObjectNode contextWindowCompressionConfigToMldev(ApiClient apiClient, JsonNode fromObject, ObjectNode parentObject) {
        ObjectNode toObject = JsonSerializable.objectMapper.createObjectNode();
        if (Common.getValueByPath(fromObject, new String[]{"triggerTokens"}) != null) {
            Common.setValueByPath(toObject, new String[]{"triggerTokens"}, Common.getValueByPath(fromObject, new String[]{"triggerTokens"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"slidingWindow"}) != null) {
            Common.setValueByPath(toObject, new String[]{"slidingWindow"}, slidingWindowToMldev(apiClient, JsonSerializable.toJsonNode(Common.getValueByPath(fromObject, new String[]{"slidingWindow"})), toObject));
        }

        return toObject;
    }


    @ExcludeFromGeneratedCoverageReport
    ObjectNode proactivityConfigToMldev(ApiClient apiClient, JsonNode fromObject, ObjectNode parentObject) {
        ObjectNode toObject = JsonSerializable.objectMapper.createObjectNode();
        if (Common.getValueByPath(fromObject, new String[]{"proactiveAudio"}) != null) {
            Common.setValueByPath(toObject, new String[]{"proactiveAudio"}, Common.getValueByPath(fromObject, new String[]{"proactiveAudio"}));
        }

        return toObject;
    }


    @ExcludeFromGeneratedCoverageReport
    ObjectNode liveConnectConfigToMldev(ApiClient apiClient, JsonNode fromObject, ObjectNode parentObject) {
        ObjectNode toObject = JsonSerializable.objectMapper.createObjectNode();

        if (Common.getValueByPath(fromObject, new String[]{"responseModalities"}) != null) {
            Common.setValueByPath(parentObject, new String[]{"setup", "generationConfig", "responseModalities"}, Common.getValueByPath(fromObject, new String[]{"responseModalities"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"temperature"}) != null) {
            Common.setValueByPath(parentObject, new String[]{"setup", "generationConfig", "temperature"}, Common.getValueByPath(fromObject, new String[]{"temperature"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"topP"}) != null) {
            Common.setValueByPath(parentObject, new String[]{"setup", "generationConfig", "topP"}, Common.getValueByPath(fromObject, new String[]{"topP"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"topK"}) != null) {
            Common.setValueByPath(parentObject, new String[]{"setup", "generationConfig", "topK"}, Common.getValueByPath(fromObject, new String[]{"topK"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"maxOutputTokens"}) != null) {
            Common.setValueByPath(parentObject, new String[]{"setup", "generationConfig", "maxOutputTokens"}, Common.getValueByPath(fromObject, new String[]{"maxOutputTokens"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"mediaResolution"}) != null) {
            Common.setValueByPath(parentObject, new String[]{"setup", "generationConfig", "mediaResolution"}, Common.getValueByPath(fromObject, new String[]{"mediaResolution"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"seed"}) != null) {
            Common.setValueByPath(parentObject, new String[]{"setup", "generationConfig", "seed"}, Common.getValueByPath(fromObject, new String[]{"seed"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"speechConfig"}) != null) {
            Common.setValueByPath(parentObject, new String[]{"setup", "generationConfig", "speechConfig"}, speechConfigToMldev(apiClient, JsonSerializable.toJsonNode(Transformers.tLiveSpeechConfig(this.apiClient, Common.getValueByPath(fromObject, new String[]{"speechConfig"}))), toObject));
        }

        if (Common.getValueByPath(fromObject, new String[]{"enableAffectiveDialog"}) != null) {
            Common.setValueByPath(parentObject, new String[]{"setup", "generationConfig", "enableAffectiveDialog"}, Common.getValueByPath(fromObject, new String[]{"enableAffectiveDialog"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"systemInstruction"}) != null) {
            Common.setValueByPath(parentObject, new String[]{"setup", "systemInstruction"}, contentToMldev(apiClient, JsonSerializable.toJsonNode(Transformers.tContent(this.apiClient, Common.getValueByPath(fromObject, new String[]{"systemInstruction"}))), toObject));
        }

        if (Common.getValueByPath(fromObject, new String[]{"tools"}) != null) {
            ArrayNode keyArray = (ArrayNode) Transformers.tTools(this.apiClient, Common.getValueByPath(fromObject, new String[]{"tools"}));
            ObjectMapper objectMapper = new ObjectMapper();
            ArrayNode result = objectMapper.createArrayNode();

            for (JsonNode item : keyArray) {
                result.add(toolToMldev(apiClient, JsonSerializable.toJsonNode(Transformers.tTool(this.apiClient, item)), toObject));
            }
            Common.setValueByPath(parentObject, new String[]{"setup", "tools"}, result);
        }

        if (Common.getValueByPath(fromObject, new String[]{"sessionResumption"}) != null) {
            Common.setValueByPath(parentObject, new String[]{"setup", "sessionResumption"}, sessionResumptionConfigToMldev(apiClient, JsonSerializable.toJsonNode(Common.getValueByPath(fromObject, new String[]{"sessionResumption"})), toObject));
        }

        if (Common.getValueByPath(fromObject, new String[]{"inputAudioTranscription"}) != null) {
            Common.setValueByPath(parentObject, new String[]{"setup", "inputAudioTranscription"}, audioTranscriptionConfigToMldev(apiClient, JsonSerializable.toJsonNode(Common.getValueByPath(fromObject, new String[]{"inputAudioTranscription"})), toObject));
        }

        if (Common.getValueByPath(fromObject, new String[]{"outputAudioTranscription"}) != null) {
            Common.setValueByPath(parentObject, new String[]{"setup", "outputAudioTranscription"}, audioTranscriptionConfigToMldev(apiClient, JsonSerializable.toJsonNode(Common.getValueByPath(fromObject, new String[]{"outputAudioTranscription"})), toObject));
        }

        if (Common.getValueByPath(fromObject, new String[]{"realtimeInputConfig"}) != null) {
            Common.setValueByPath(parentObject, new String[]{"setup", "realtimeInputConfig"}, realtimeInputConfigToMldev(apiClient, JsonSerializable.toJsonNode(Common.getValueByPath(fromObject, new String[]{"realtimeInputConfig"})), toObject));
        }

        if (Common.getValueByPath(fromObject, new String[]{"contextWindowCompression"}) != null) {
            Common.setValueByPath(parentObject, new String[]{"setup", "contextWindowCompression"}, contextWindowCompressionConfigToMldev(apiClient, JsonSerializable.toJsonNode(Common.getValueByPath(fromObject, new String[]{"contextWindowCompression"})), toObject));
        }

        if (Common.getValueByPath(fromObject, new String[]{"proactivity"}) != null) {
            Common.setValueByPath(parentObject, new String[]{"setup", "proactivity"}, proactivityConfigToMldev(apiClient, JsonSerializable.toJsonNode(Common.getValueByPath(fromObject, new String[]{"proactivity"})), toObject));
        }

        return toObject;
    }


    @ExcludeFromGeneratedCoverageReport
    ObjectNode liveConnectParametersToMldev(ApiClient apiClient, JsonNode fromObject, ObjectNode parentObject) {
        ObjectNode toObject = JsonSerializable.objectMapper.createObjectNode();
        if (Common.getValueByPath(fromObject, new String[]{"model"}) != null) {
            Common.setValueByPath(toObject, new String[]{"setup", "model"}, Transformers.tModel(this.apiClient, Common.getValueByPath(fromObject, new String[]{"model"})));
        }

        if (Common.getValueByPath(fromObject, new String[]{"config"}) != null) {
            Common.setValueByPath(toObject, new String[]{"config"}, liveConnectConfigToMldev(apiClient, JsonSerializable.toJsonNode(Common.getValueByPath(fromObject, new String[]{"config"})), toObject));
        }

        return toObject;
    }

    @ExcludeFromGeneratedCoverageReport
    ObjectNode activityStartToMldev(ApiClient apiClient, JsonNode fromObject, ObjectNode parentObject) {
        ObjectNode toObject = JsonSerializable.objectMapper.createObjectNode();

        return toObject;
    }

    @ExcludeFromGeneratedCoverageReport
    ObjectNode activityEndToMldev(ApiClient apiClient, JsonNode fromObject, ObjectNode parentObject) {
        ObjectNode toObject = JsonSerializable.objectMapper.createObjectNode();

        return toObject;
    }


    @ExcludeFromGeneratedCoverageReport
    ObjectNode liveSendRealtimeInputParametersToMldev(ApiClient apiClient, JsonNode fromObject, ObjectNode parentObject) {
        ObjectNode toObject = JsonSerializable.objectMapper.createObjectNode();
        if (Common.getValueByPath(fromObject, new String[]{"media"}) != null) {
            Common.setValueByPath(toObject, new String[]{"mediaChunks"}, Transformers.tBlobs(this.apiClient, Common.getValueByPath(fromObject, new String[]{"media"})));
        }

        if (Common.getValueByPath(fromObject, new String[]{"audio"}) != null) {
            Common.setValueByPath(toObject, new String[]{"audio"}, Transformers.tAudioBlob(this.apiClient, Common.getValueByPath(fromObject, new String[]{"audio"})));
        }

        if (Common.getValueByPath(fromObject, new String[]{"audioStreamEnd"}) != null) {
            Common.setValueByPath(toObject, new String[]{"audioStreamEnd"}, Common.getValueByPath(fromObject, new String[]{"audioStreamEnd"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"video"}) != null) {
            Common.setValueByPath(toObject, new String[]{"video"}, Transformers.tImageBlob(this.apiClient, Common.getValueByPath(fromObject, new String[]{"video"})));
        }

        if (Common.getValueByPath(fromObject, new String[]{"text"}) != null) {
            Common.setValueByPath(toObject, new String[]{"text"}, Common.getValueByPath(fromObject, new String[]{"text"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"activityStart"}) != null) {
            Common.setValueByPath(toObject, new String[]{"activityStart"}, activityStartToMldev(apiClient, JsonSerializable.toJsonNode(Common.getValueByPath(fromObject, new String[]{"activityStart"})), toObject));
        }

        if (Common.getValueByPath(fromObject, new String[]{"activityEnd"}) != null) {
            Common.setValueByPath(toObject, new String[]{"activityEnd"}, activityEndToMldev(apiClient, JsonSerializable.toJsonNode(Common.getValueByPath(fromObject, new String[]{"activityEnd"})), toObject));
        }

        return toObject;
    }


    @ExcludeFromGeneratedCoverageReport
    ObjectNode liveClientSetupToMldev(ApiClient apiClient, JsonNode fromObject, ObjectNode parentObject) {
        ObjectNode toObject = JsonSerializable.objectMapper.createObjectNode();
        if (Common.getValueByPath(fromObject, new String[]{"model"}) != null) {
            Common.setValueByPath(toObject, new String[]{"model"}, Common.getValueByPath(fromObject, new String[]{"model"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"generationConfig"}) != null) {
            Common.setValueByPath(toObject, new String[]{"generationConfig"}, Common.getValueByPath(fromObject, new String[]{"generationConfig"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"systemInstruction"}) != null) {
            Common.setValueByPath(toObject, new String[]{"systemInstruction"}, contentToMldev(apiClient, JsonSerializable.toJsonNode(Transformers.tContent(this.apiClient, Common.getValueByPath(fromObject, new String[]{"systemInstruction"}))), toObject));
        }

        if (Common.getValueByPath(fromObject, new String[]{"tools"}) != null) {
            ArrayNode keyArray = (ArrayNode) Transformers.tTools(this.apiClient, Common.getValueByPath(fromObject, new String[]{"tools"}));
            ObjectMapper objectMapper = new ObjectMapper();
            ArrayNode result = objectMapper.createArrayNode();

            for (JsonNode item : keyArray) {
                result.add(toolToMldev(apiClient, JsonSerializable.toJsonNode(Transformers.tTool(this.apiClient, item)), toObject));
            }
            Common.setValueByPath(toObject, new String[]{"tools"}, result);
        }

        if (Common.getValueByPath(fromObject, new String[]{"realtimeInputConfig"}) != null) {
            Common.setValueByPath(toObject, new String[]{"realtimeInputConfig"}, realtimeInputConfigToMldev(apiClient, JsonSerializable.toJsonNode(Common.getValueByPath(fromObject, new String[]{"realtimeInputConfig"})), toObject));
        }

        if (Common.getValueByPath(fromObject, new String[]{"sessionResumption"}) != null) {
            Common.setValueByPath(toObject, new String[]{"sessionResumption"}, sessionResumptionConfigToMldev(apiClient, JsonSerializable.toJsonNode(Common.getValueByPath(fromObject, new String[]{"sessionResumption"})), toObject));
        }

        if (Common.getValueByPath(fromObject, new String[]{"contextWindowCompression"}) != null) {
            Common.setValueByPath(toObject, new String[]{"contextWindowCompression"}, contextWindowCompressionConfigToMldev(apiClient, JsonSerializable.toJsonNode(Common.getValueByPath(fromObject, new String[]{"contextWindowCompression"})), toObject));
        }

        if (Common.getValueByPath(fromObject, new String[]{"inputAudioTranscription"}) != null) {
            Common.setValueByPath(toObject, new String[]{"inputAudioTranscription"}, audioTranscriptionConfigToMldev(apiClient, JsonSerializable.toJsonNode(Common.getValueByPath(fromObject, new String[]{"inputAudioTranscription"})), toObject));
        }

        if (Common.getValueByPath(fromObject, new String[]{"outputAudioTranscription"}) != null) {
            Common.setValueByPath(toObject, new String[]{"outputAudioTranscription"}, audioTranscriptionConfigToMldev(apiClient, JsonSerializable.toJsonNode(Common.getValueByPath(fromObject, new String[]{"outputAudioTranscription"})), toObject));
        }

        if (Common.getValueByPath(fromObject, new String[]{"proactivity"}) != null) {
            Common.setValueByPath(toObject, new String[]{"proactivity"}, proactivityConfigToMldev(apiClient, JsonSerializable.toJsonNode(Common.getValueByPath(fromObject, new String[]{"proactivity"})), toObject));
        }

        return toObject;
    }


    @ExcludeFromGeneratedCoverageReport
    ObjectNode liveClientContentToMldev(ApiClient apiClient, JsonNode fromObject, ObjectNode parentObject) {
        ObjectNode toObject = JsonSerializable.objectMapper.createObjectNode();
        if (Common.getValueByPath(fromObject, new String[]{"turns"}) != null) {
            ArrayNode keyArray = (ArrayNode) Common.getValueByPath(fromObject, new String[]{"turns"});
            ObjectMapper objectMapper = new ObjectMapper();
            ArrayNode result = objectMapper.createArrayNode();

            for (JsonNode item : keyArray) {
                result.add(contentToMldev(apiClient, JsonSerializable.toJsonNode(item), toObject));
            }
            Common.setValueByPath(toObject, new String[]{"turns"}, result);
        }

        if (Common.getValueByPath(fromObject, new String[]{"turnComplete"}) != null) {
            Common.setValueByPath(toObject, new String[]{"turnComplete"}, Common.getValueByPath(fromObject, new String[]{"turnComplete"}));
        }

        return toObject;
    }


    @ExcludeFromGeneratedCoverageReport
    ObjectNode liveClientRealtimeInputToMldev(ApiClient apiClient, JsonNode fromObject, ObjectNode parentObject) {
        ObjectNode toObject = JsonSerializable.objectMapper.createObjectNode();
        if (Common.getValueByPath(fromObject, new String[]{"mediaChunks"}) != null) {
            Common.setValueByPath(toObject, new String[]{"mediaChunks"}, Common.getValueByPath(fromObject, new String[]{"mediaChunks"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"audio"}) != null) {
            Common.setValueByPath(toObject, new String[]{"audio"}, Common.getValueByPath(fromObject, new String[]{"audio"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"audioStreamEnd"}) != null) {
            Common.setValueByPath(toObject, new String[]{"audioStreamEnd"}, Common.getValueByPath(fromObject, new String[]{"audioStreamEnd"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"video"}) != null) {
            Common.setValueByPath(toObject, new String[]{"video"}, Common.getValueByPath(fromObject, new String[]{"video"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"text"}) != null) {
            Common.setValueByPath(toObject, new String[]{"text"}, Common.getValueByPath(fromObject, new String[]{"text"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"activityStart"}) != null) {
            Common.setValueByPath(toObject, new String[]{"activityStart"}, activityStartToMldev(apiClient, JsonSerializable.toJsonNode(Common.getValueByPath(fromObject, new String[]{"activityStart"})), toObject));
        }

        if (Common.getValueByPath(fromObject, new String[]{"activityEnd"}) != null) {
            Common.setValueByPath(toObject, new String[]{"activityEnd"}, activityEndToMldev(apiClient, JsonSerializable.toJsonNode(Common.getValueByPath(fromObject, new String[]{"activityEnd"})), toObject));
        }

        return toObject;
    }

    @ExcludeFromGeneratedCoverageReport
    ObjectNode functionResponseToMldev(ApiClient apiClient, JsonNode fromObject, ObjectNode parentObject) {
        ObjectNode toObject = JsonSerializable.objectMapper.createObjectNode();
        if (Common.getValueByPath(fromObject, new String[]{"willContinue"}) != null) {
            Common.setValueByPath(toObject, new String[]{"willContinue"}, Common.getValueByPath(fromObject, new String[]{"willContinue"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"scheduling"}) != null) {
            Common.setValueByPath(toObject, new String[]{"scheduling"}, Common.getValueByPath(fromObject, new String[]{"scheduling"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"id"}) != null) {
            Common.setValueByPath(toObject, new String[]{"id"}, Common.getValueByPath(fromObject, new String[]{"id"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"name"}) != null) {
            Common.setValueByPath(toObject, new String[]{"name"}, Common.getValueByPath(fromObject, new String[]{"name"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"response"}) != null) {
            Common.setValueByPath(toObject, new String[]{"response"}, Common.getValueByPath(fromObject, new String[]{"response"}));
        }

        return toObject;
    }


    @ExcludeFromGeneratedCoverageReport
    ObjectNode liveClientToolResponseToMldev(ApiClient apiClient, JsonNode fromObject, ObjectNode parentObject) {
        ObjectNode toObject = JsonSerializable.objectMapper.createObjectNode();
        if (Common.getValueByPath(fromObject, new String[]{"functionResponses"}) != null) {
            ArrayNode keyArray = (ArrayNode) Common.getValueByPath(fromObject, new String[]{"functionResponses"});
            ObjectMapper objectMapper = new ObjectMapper();
            ArrayNode result = objectMapper.createArrayNode();

            for (JsonNode item : keyArray) {
                result.add(functionResponseToMldev(apiClient, JsonSerializable.toJsonNode(item), toObject));
            }
            Common.setValueByPath(toObject, new String[]{"functionResponses"}, result);
        }

        return toObject;
    }

    @ExcludeFromGeneratedCoverageReport
    ObjectNode liveClientMessageToMldev(ApiClient apiClient, JsonNode fromObject, ObjectNode parentObject) {
        ObjectNode toObject = JsonSerializable.objectMapper.createObjectNode();
        if (Common.getValueByPath(fromObject, new String[]{"setup"}) != null) {
            Common.setValueByPath(toObject, new String[]{"setup"}, liveClientSetupToMldev(apiClient, JsonSerializable.toJsonNode(Common.getValueByPath(fromObject, new String[]{"setup"})), toObject));
        }

        if (Common.getValueByPath(fromObject, new String[]{"clientContent"}) != null) {
            Common.setValueByPath(toObject, new String[]{"clientContent"}, liveClientContentToMldev(apiClient, JsonSerializable.toJsonNode(Common.getValueByPath(fromObject, new String[]{"clientContent"})), toObject));
        }

        if (Common.getValueByPath(fromObject, new String[]{"realtimeInput"}) != null) {
            Common.setValueByPath(toObject, new String[]{"realtimeInput"}, liveClientRealtimeInputToMldev(apiClient, JsonSerializable.toJsonNode(Common.getValueByPath(fromObject, new String[]{"realtimeInput"})), toObject));
        }

        if (Common.getValueByPath(fromObject, new String[]{"realtimeInputParameters"}) != null) {
            Common.setValueByPath(toObject, new String[]{"realtime_input"}, liveSendRealtimeInputParametersToMldev(apiClient, JsonSerializable.toJsonNode(Common.getValueByPath(fromObject, new String[]{"realtimeInputParameters"})), toObject));
        }

        if (Common.getValueByPath(fromObject, new String[]{"toolResponse"}) != null) {
            Common.setValueByPath(toObject, new String[]{"toolResponse"}, liveClientToolResponseToMldev(apiClient, JsonSerializable.toJsonNode(Common.getValueByPath(fromObject, new String[]{"toolResponse"})), toObject));
        }

        return toObject;
    }

    @ExcludeFromGeneratedCoverageReport
    ObjectNode liveServerSetupCompleteFromMldev(ApiClient apiClient, JsonNode fromObject, ObjectNode parentObject) {
        ObjectNode toObject = JsonSerializable.objectMapper.createObjectNode();

        return toObject;
    }

    @ExcludeFromGeneratedCoverageReport
    ObjectNode videoMetadataFromMldev(ApiClient apiClient, JsonNode fromObject, ObjectNode parentObject) {
        ObjectNode toObject = JsonSerializable.objectMapper.createObjectNode();
        if (Common.getValueByPath(fromObject, new String[]{"fps"}) != null) {
            Common.setValueByPath(toObject, new String[]{"fps"}, Common.getValueByPath(fromObject, new String[]{"fps"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"endOffset"}) != null) {
            Common.setValueByPath(toObject, new String[]{"endOffset"}, Common.getValueByPath(fromObject, new String[]{"endOffset"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"startOffset"}) != null) {
            Common.setValueByPath(toObject, new String[]{"startOffset"}, Common.getValueByPath(fromObject, new String[]{"startOffset"}));
        }

        return toObject;
    }


    @ExcludeFromGeneratedCoverageReport
    ObjectNode blobFromMldev(ApiClient apiClient, JsonNode fromObject, ObjectNode parentObject) {
        ObjectNode toObject = JsonSerializable.objectMapper.createObjectNode();

        if (Common.getValueByPath(fromObject, new String[]{"data"}) != null) {
            Common.setValueByPath(toObject, new String[]{"data"}, Common.getValueByPath(fromObject, new String[]{"data"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"mimeType"}) != null) {
            Common.setValueByPath(toObject, new String[]{"mimeType"}, Common.getValueByPath(fromObject, new String[]{"mimeType"}));
        }

        return toObject;
    }


    @ExcludeFromGeneratedCoverageReport
    ObjectNode fileDataFromMldev(ApiClient apiClient, JsonNode fromObject, ObjectNode parentObject) {
        ObjectNode toObject = JsonSerializable.objectMapper.createObjectNode();

        if (Common.getValueByPath(fromObject, new String[]{"fileUri"}) != null) {
            Common.setValueByPath(toObject, new String[]{"fileUri"}, Common.getValueByPath(fromObject, new String[]{"fileUri"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"mimeType"}) != null) {
            Common.setValueByPath(toObject, new String[]{"mimeType"}, Common.getValueByPath(fromObject, new String[]{"mimeType"}));
        }

        return toObject;
    }


    @ExcludeFromGeneratedCoverageReport
    ObjectNode partFromMldev(ApiClient apiClient, JsonNode fromObject, ObjectNode parentObject) {
        ObjectNode toObject = JsonSerializable.objectMapper.createObjectNode();
        if (Common.getValueByPath(fromObject, new String[]{"videoMetadata"}) != null) {
            Common.setValueByPath(toObject, new String[]{"videoMetadata"}, videoMetadataFromMldev(apiClient, JsonSerializable.toJsonNode(Common.getValueByPath(fromObject, new String[]{"videoMetadata"})), toObject));
        }

        if (Common.getValueByPath(fromObject, new String[]{"thought"}) != null) {
            Common.setValueByPath(toObject, new String[]{"thought"}, Common.getValueByPath(fromObject, new String[]{"thought"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"inlineData"}) != null) {
            Common.setValueByPath(toObject, new String[]{"inlineData"}, blobFromMldev(apiClient, JsonSerializable.toJsonNode(Common.getValueByPath(fromObject, new String[]{"inlineData"})), toObject));
        }

        if (Common.getValueByPath(fromObject, new String[]{"fileData"}) != null) {
            Common.setValueByPath(toObject, new String[]{"fileData"}, fileDataFromMldev(apiClient, JsonSerializable.toJsonNode(Common.getValueByPath(fromObject, new String[]{"fileData"})), toObject));
        }

        if (Common.getValueByPath(fromObject, new String[]{"thoughtSignature"}) != null) {
            Common.setValueByPath(toObject, new String[]{"thoughtSignature"}, Common.getValueByPath(fromObject, new String[]{"thoughtSignature"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"codeExecutionResult"}) != null) {
            Common.setValueByPath(toObject, new String[]{"codeExecutionResult"}, Common.getValueByPath(fromObject, new String[]{"codeExecutionResult"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"executableCode"}) != null) {
            Common.setValueByPath(toObject, new String[]{"executableCode"}, Common.getValueByPath(fromObject, new String[]{"executableCode"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"functionCall"}) != null) {
            Common.setValueByPath(toObject, new String[]{"functionCall"}, Common.getValueByPath(fromObject, new String[]{"functionCall"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"functionResponse"}) != null) {
            Common.setValueByPath(toObject, new String[]{"functionResponse"}, Common.getValueByPath(fromObject, new String[]{"functionResponse"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"text"}) != null) {
            Common.setValueByPath(toObject, new String[]{"text"}, Common.getValueByPath(fromObject, new String[]{"text"}));
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
            Common.setValueByPath(toObject, new String[]{"role"}, Common.getValueByPath(fromObject, new String[]{"role"}));
        }

        return toObject;
    }

    @ExcludeFromGeneratedCoverageReport
    ObjectNode transcriptionFromMldev(ApiClient apiClient, JsonNode fromObject, ObjectNode parentObject) {
        ObjectNode toObject = JsonSerializable.objectMapper.createObjectNode();
        if (Common.getValueByPath(fromObject, new String[]{"text"}) != null) {
            Common.setValueByPath(toObject, new String[]{"text"}, Common.getValueByPath(fromObject, new String[]{"text"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"finished"}) != null) {
            Common.setValueByPath(toObject, new String[]{"finished"}, Common.getValueByPath(fromObject, new String[]{"finished"}));
        }

        return toObject;
    }


    @ExcludeFromGeneratedCoverageReport
    ObjectNode urlMetadataFromMldev(ApiClient apiClient, JsonNode fromObject, ObjectNode parentObject) {
        ObjectNode toObject = JsonSerializable.objectMapper.createObjectNode();
        if (Common.getValueByPath(fromObject, new String[]{"retrievedUrl"}) != null) {
            Common.setValueByPath(toObject, new String[]{"retrievedUrl"}, Common.getValueByPath(fromObject, new String[]{"retrievedUrl"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"urlRetrievalStatus"}) != null) {
            Common.setValueByPath(toObject, new String[]{"urlRetrievalStatus"}, Common.getValueByPath(fromObject, new String[]{"urlRetrievalStatus"}));
        }

        return toObject;
    }


    @ExcludeFromGeneratedCoverageReport
    ObjectNode urlContextMetadataFromMldev(ApiClient apiClient, JsonNode fromObject, ObjectNode parentObject) {
        ObjectNode toObject = JsonSerializable.objectMapper.createObjectNode();
        if (Common.getValueByPath(fromObject, new String[]{"urlMetadata"}) != null) {
            ArrayNode keyArray = (ArrayNode) Common.getValueByPath(fromObject, new String[]{"urlMetadata"});
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
    ObjectNode liveServerContentFromMldev(ApiClient apiClient, JsonNode fromObject, ObjectNode parentObject) {
        ObjectNode toObject = JsonSerializable.objectMapper.createObjectNode();
        if (Common.getValueByPath(fromObject, new String[]{"modelTurn"}) != null) {
            Common.setValueByPath(toObject, new String[]{"modelTurn"}, contentFromMldev(apiClient, JsonSerializable.toJsonNode(Common.getValueByPath(fromObject, new String[]{"modelTurn"})), toObject));
        }

        if (Common.getValueByPath(fromObject, new String[]{"turnComplete"}) != null) {
            Common.setValueByPath(toObject, new String[]{"turnComplete"}, Common.getValueByPath(fromObject, new String[]{"turnComplete"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"interrupted"}) != null) {
            Common.setValueByPath(toObject, new String[]{"interrupted"}, Common.getValueByPath(fromObject, new String[]{"interrupted"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"groundingMetadata"}) != null) {
            Common.setValueByPath(toObject, new String[]{"groundingMetadata"}, Common.getValueByPath(fromObject, new String[]{"groundingMetadata"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"generationComplete"}) != null) {
            Common.setValueByPath(toObject, new String[]{"generationComplete"}, Common.getValueByPath(fromObject, new String[]{"generationComplete"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"inputTranscription"}) != null) {
            Common.setValueByPath(toObject, new String[]{"inputTranscription"}, transcriptionFromMldev(apiClient, JsonSerializable.toJsonNode(Common.getValueByPath(fromObject, new String[]{"inputTranscription"})), toObject));
        }

        if (Common.getValueByPath(fromObject, new String[]{"outputTranscription"}) != null) {
            Common.setValueByPath(toObject, new String[]{"outputTranscription"}, transcriptionFromMldev(apiClient, JsonSerializable.toJsonNode(Common.getValueByPath(fromObject, new String[]{"outputTranscription"})), toObject));
        }

        if (Common.getValueByPath(fromObject, new String[]{"urlContextMetadata"}) != null) {
            Common.setValueByPath(toObject, new String[]{"urlContextMetadata"}, urlContextMetadataFromMldev(apiClient, JsonSerializable.toJsonNode(Common.getValueByPath(fromObject, new String[]{"urlContextMetadata"})), toObject));
        }

        return toObject;
    }

    @ExcludeFromGeneratedCoverageReport
    ObjectNode functionCallFromMldev(ApiClient apiClient, JsonNode fromObject, ObjectNode parentObject) {
        ObjectNode toObject = JsonSerializable.objectMapper.createObjectNode();
        if (Common.getValueByPath(fromObject, new String[]{"id"}) != null) {
            Common.setValueByPath(toObject, new String[]{"id"}, Common.getValueByPath(fromObject, new String[]{"id"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"args"}) != null) {
            Common.setValueByPath(toObject, new String[]{"args"}, Common.getValueByPath(fromObject, new String[]{"args"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"name"}) != null) {
            Common.setValueByPath(toObject, new String[]{"name"}, Common.getValueByPath(fromObject, new String[]{"name"}));
        }

        return toObject;
    }


    @ExcludeFromGeneratedCoverageReport
    ObjectNode liveServerToolCallFromMldev(ApiClient apiClient, JsonNode fromObject, ObjectNode parentObject) {
        ObjectNode toObject = JsonSerializable.objectMapper.createObjectNode();
        if (Common.getValueByPath(fromObject, new String[]{"functionCalls"}) != null) {
            ArrayNode keyArray = (ArrayNode) Common.getValueByPath(fromObject, new String[]{"functionCalls"});
            ObjectMapper objectMapper = new ObjectMapper();
            ArrayNode result = objectMapper.createArrayNode();

            for (JsonNode item : keyArray) {
                result.add(functionCallFromMldev(apiClient, JsonSerializable.toJsonNode(item), toObject));
            }
            Common.setValueByPath(toObject, new String[]{"functionCalls"}, result);
        }

        return toObject;
    }


    @ExcludeFromGeneratedCoverageReport
    ObjectNode liveServerToolCallCancellationFromMldev(ApiClient apiClient, JsonNode fromObject, ObjectNode parentObject) {
        ObjectNode toObject = JsonSerializable.objectMapper.createObjectNode();
        if (Common.getValueByPath(fromObject, new String[]{"ids"}) != null) {
            Common.setValueByPath(toObject, new String[]{"ids"}, Common.getValueByPath(fromObject, new String[]{"ids"}));
        }

        return toObject;
    }

    @ExcludeFromGeneratedCoverageReport
    ObjectNode modalityTokenCountFromMldev(ApiClient apiClient, JsonNode fromObject, ObjectNode parentObject) {
        ObjectNode toObject = JsonSerializable.objectMapper.createObjectNode();
        if (Common.getValueByPath(fromObject, new String[]{"modality"}) != null) {
            Common.setValueByPath(toObject, new String[]{"modality"}, Common.getValueByPath(fromObject, new String[]{"modality"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"tokenCount"}) != null) {
            Common.setValueByPath(toObject, new String[]{"tokenCount"}, Common.getValueByPath(fromObject, new String[]{"tokenCount"}));
        }

        return toObject;
    }


    @ExcludeFromGeneratedCoverageReport
    ObjectNode usageMetadataFromMldev(ApiClient apiClient, JsonNode fromObject, ObjectNode parentObject) {
        ObjectNode toObject = JsonSerializable.objectMapper.createObjectNode();
        if (Common.getValueByPath(fromObject, new String[]{"promptTokenCount"}) != null) {
            Common.setValueByPath(toObject, new String[]{"promptTokenCount"}, Common.getValueByPath(fromObject, new String[]{"promptTokenCount"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"cachedContentTokenCount"}) != null) {
            Common.setValueByPath(toObject, new String[]{"cachedContentTokenCount"}, Common.getValueByPath(fromObject, new String[]{"cachedContentTokenCount"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"responseTokenCount"}) != null) {
            Common.setValueByPath(toObject, new String[]{"responseTokenCount"}, Common.getValueByPath(fromObject, new String[]{"responseTokenCount"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"toolUsePromptTokenCount"}) != null) {
            Common.setValueByPath(toObject, new String[]{"toolUsePromptTokenCount"}, Common.getValueByPath(fromObject, new String[]{"toolUsePromptTokenCount"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"thoughtsTokenCount"}) != null) {
            Common.setValueByPath(toObject, new String[]{"thoughtsTokenCount"}, Common.getValueByPath(fromObject, new String[]{"thoughtsTokenCount"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"totalTokenCount"}) != null) {
            Common.setValueByPath(toObject, new String[]{"totalTokenCount"}, Common.getValueByPath(fromObject, new String[]{"totalTokenCount"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"promptTokensDetails"}) != null) {
            ArrayNode keyArray = (ArrayNode) Common.getValueByPath(fromObject, new String[]{"promptTokensDetails"});
            ObjectMapper objectMapper = new ObjectMapper();
            ArrayNode result = objectMapper.createArrayNode();

            for (JsonNode item : keyArray) {
                result.add(modalityTokenCountFromMldev(apiClient, JsonSerializable.toJsonNode(item), toObject));
            }
            Common.setValueByPath(toObject, new String[]{"promptTokensDetails"}, result);
        }

        if (Common.getValueByPath(fromObject, new String[]{"cacheTokensDetails"}) != null) {
            ArrayNode keyArray = (ArrayNode) Common.getValueByPath(fromObject, new String[]{"cacheTokensDetails"});
            ObjectMapper objectMapper = new ObjectMapper();
            ArrayNode result = objectMapper.createArrayNode();

            for (JsonNode item : keyArray) {
                result.add(modalityTokenCountFromMldev(apiClient, JsonSerializable.toJsonNode(item), toObject));
            }
            Common.setValueByPath(toObject, new String[]{"cacheTokensDetails"}, result);
        }

        if (Common.getValueByPath(fromObject, new String[]{"responseTokensDetails"}) != null) {
            ArrayNode keyArray = (ArrayNode) Common.getValueByPath(fromObject, new String[]{"responseTokensDetails"});
            ObjectMapper objectMapper = new ObjectMapper();
            ArrayNode result = objectMapper.createArrayNode();

            for (JsonNode item : keyArray) {
                result.add(modalityTokenCountFromMldev(apiClient, JsonSerializable.toJsonNode(item), toObject));
            }
            Common.setValueByPath(toObject, new String[]{"responseTokensDetails"}, result);
        }

        if (Common.getValueByPath(fromObject, new String[]{"toolUsePromptTokensDetails"}) != null) {
            ArrayNode keyArray = (ArrayNode) Common.getValueByPath(fromObject, new String[]{"toolUsePromptTokensDetails"});
            ObjectMapper objectMapper = new ObjectMapper();
            ArrayNode result = objectMapper.createArrayNode();

            for (JsonNode item : keyArray) {
                result.add(modalityTokenCountFromMldev(apiClient, JsonSerializable.toJsonNode(item), toObject));
            }
            Common.setValueByPath(toObject, new String[]{"toolUsePromptTokensDetails"}, result);
        }

        return toObject;
    }

    @ExcludeFromGeneratedCoverageReport
    ObjectNode liveServerGoAwayFromMldev(ApiClient apiClient, JsonNode fromObject, ObjectNode parentObject) {
        ObjectNode toObject = JsonSerializable.objectMapper.createObjectNode();
        if (Common.getValueByPath(fromObject, new String[]{"timeLeft"}) != null) {
            Common.setValueByPath(toObject, new String[]{"timeLeft"}, Common.getValueByPath(fromObject, new String[]{"timeLeft"}));
        }

        return toObject;
    }

    @ExcludeFromGeneratedCoverageReport
    ObjectNode liveServerSessionResumptionUpdateFromMldev(ApiClient apiClient, JsonNode fromObject, ObjectNode parentObject) {
        ObjectNode toObject = JsonSerializable.objectMapper.createObjectNode();
        if (Common.getValueByPath(fromObject, new String[]{"newHandle"}) != null) {
            Common.setValueByPath(toObject, new String[]{"newHandle"}, Common.getValueByPath(fromObject, new String[]{"newHandle"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"resumable"}) != null) {
            Common.setValueByPath(toObject, new String[]{"resumable"}, Common.getValueByPath(fromObject, new String[]{"resumable"}));
        }

        if (Common.getValueByPath(fromObject, new String[]{"lastConsumedClientMessageIndex"}) != null) {
            Common.setValueByPath(toObject, new String[]{"lastConsumedClientMessageIndex"}, Common.getValueByPath(fromObject, new String[]{"lastConsumedClientMessageIndex"}));
        }

        return toObject;
    }


    @ExcludeFromGeneratedCoverageReport
    ObjectNode liveServerMessageFromMldev(ApiClient apiClient, JsonNode fromObject, ObjectNode parentObject) {
        ObjectNode toObject = JsonSerializable.objectMapper.createObjectNode();
        if (Common.getValueByPath(fromObject, new String[]{"setupComplete"}) != null) {
            Common.setValueByPath(toObject, new String[]{"setupComplete"}, liveServerSetupCompleteFromMldev(apiClient, JsonSerializable.toJsonNode(Common.getValueByPath(fromObject, new String[]{"setupComplete"})), toObject));
        }

        if (Common.getValueByPath(fromObject, new String[]{"serverContent"}) != null) {
            Common.setValueByPath(toObject, new String[]{"serverContent"}, liveServerContentFromMldev(apiClient, JsonSerializable.toJsonNode(Common.getValueByPath(fromObject, new String[]{"serverContent"})), toObject));
        }

        if (Common.getValueByPath(fromObject, new String[]{"toolCall"}) != null) {
            Common.setValueByPath(toObject, new String[]{"toolCall"}, liveServerToolCallFromMldev(apiClient, JsonSerializable.toJsonNode(Common.getValueByPath(fromObject, new String[]{"toolCall"})), toObject));
        }

        if (Common.getValueByPath(fromObject, new String[]{"toolCallCancellation"}) != null) {
            Common.setValueByPath(toObject, new String[]{"toolCallCancellation"}, liveServerToolCallCancellationFromMldev(apiClient, JsonSerializable.toJsonNode(Common.getValueByPath(fromObject, new String[]{"toolCallCancellation"})), toObject));
        }

        if (Common.getValueByPath(fromObject, new String[]{"usageMetadata"}) != null) {
            Common.setValueByPath(toObject, new String[]{"usageMetadata"}, usageMetadataFromMldev(apiClient, JsonSerializable.toJsonNode(Common.getValueByPath(fromObject, new String[]{"usageMetadata"})), toObject));
        }

        if (Common.getValueByPath(fromObject, new String[]{"goAway"}) != null) {
            Common.setValueByPath(toObject, new String[]{"goAway"}, liveServerGoAwayFromMldev(apiClient, JsonSerializable.toJsonNode(Common.getValueByPath(fromObject, new String[]{"goAway"})), toObject));
        }

        if (Common.getValueByPath(fromObject, new String[]{"sessionResumptionUpdate"}) != null) {
            Common.setValueByPath(toObject, new String[]{"sessionResumptionUpdate"}, liveServerSessionResumptionUpdateFromMldev(apiClient, JsonSerializable.toJsonNode(Common.getValueByPath(fromObject, new String[]{"sessionResumptionUpdate"})), toObject));
        }

        return toObject;
    }
}
