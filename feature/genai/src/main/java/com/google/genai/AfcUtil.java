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

import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.util.Arrays.stream;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.genai.types.FunctionCall;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import com.google.genai.types.Tool;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

final class AfcUtil {
    private static final Logger logger = Logger.getLogger(AfcUtil.class.getName());
    private static final int DEFAULT_MAX_REMOTE_CALLS_AFC = 10;

    private AfcUtil() {
    }

    static boolean hasCallableTool(ApiClient apiClient, GenerateContentConfig config) {
        if (config == null) {
            return false;
        }
        if (!config.tools().isPresent() || config.tools().get().isEmpty()) {
            return false;
        }
        for (Tool tool : config.tools().get()) {
            if (tool.functions().isPresent() && !tool.functions().get().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    static GenerateContentConfig transformGenerateContentConfig(
            ApiClient apiClient, GenerateContentConfig config) {
        GenerateContentConfig transformedConfig;
        if (config != null && config.tools().isPresent() && !config.tools().get().isEmpty()) {
            ImmutableList<Tool> transformedTools =
                    config.tools().get().stream()
                            .map(tool -> Transformers.tTool(apiClient, tool))
                            .collect(toImmutableList());
            ObjectNode configNode = JsonSerializable.objectMapper.valueToTree(config);
            configNode.set("tools", JsonSerializable.objectMapper.valueToTree(transformedTools));
            transformedConfig = JsonSerializable.fromJsonNode(configNode, GenerateContentConfig.class);
        } else {
            transformedConfig = config;
        }
        return transformedConfig;
    }

    static ImmutableMap<String, Method> getFunctionMap(GenerateContentConfig config) {
        ImmutableMap.Builder<String, Method> functionMapBuilder = ImmutableMap.builder();
        if (config != null && config.tools().isPresent() && !config.tools().get().isEmpty()) {
            for (Tool tool : config.tools().get()) {
                if (tool.functions().isPresent() && !tool.functions().get().isEmpty()) {
                    for (Method method : tool.functions().get()) {
                        functionMapBuilder.put(method.getName(), method);
                    }
                }
            }
        }
        return functionMapBuilder.buildOrThrow();
    }

    static ImmutableList<Part> getFunctionResponseParts(
            GenerateContentResponse response, ImmutableMap<String, Method> functionMap) {
        ImmutableList.Builder<Part> functionResponsePartsBuilder = ImmutableList.builder();
        ImmutableList<Part> responseParts = response.parts();
        ImmutableList<FunctionCall> functionCalls = response.functionCalls();
        if (responseParts == null
                || responseParts.isEmpty()
                || functionCalls == null
                || functionCalls.isEmpty()) {
            return functionResponsePartsBuilder.build();
        }
        for (FunctionCall functionCall : functionCalls) {
            String funcName = functionCall.name().get();
            if (funcName == null || !functionMap.containsKey(funcName)) {
                continue;
            }
            Method method = functionMap.get(funcName);
            ImmutableMap<String, Object> args = ImmutableMap.copyOf(functionCall.args().get());
            try {
                Object funcResponse = getFunctionResponse(method, args);
                if (funcResponse == null) {
                    functionResponsePartsBuilder.add(
                            Part.fromFunctionResponse(funcName, ImmutableMap.of("result", "")));
                } else {
                    functionResponsePartsBuilder.add(
                            Part.fromFunctionResponse(funcName, ImmutableMap.of("result", funcResponse)));
                }
            } catch (Exception e) {
                functionResponsePartsBuilder.add(
                        Part.fromFunctionResponse(funcName, ImmutableMap.of("error", e.toString())));
            }
        }
        return functionResponsePartsBuilder.build();
    }

    static boolean shouldDisableAfc(GenerateContentConfig config) {
        if (config == null) {
            return false;
        }
        if (!config.automaticFunctionCalling().isPresent()) {
            return false;
        }
        Optional<Boolean> disable = config.automaticFunctionCalling().get().disable();
        Optional<Integer> maximumRemoteCalls =
                config.automaticFunctionCalling().get().maximumRemoteCalls();
        if (!disable.isPresent()) {
            return false;
        }
        if (maximumRemoteCalls.isPresent() && maximumRemoteCalls.get() <= 0) {
            logger.warning(
                    String.format(
                            "maxRemoteCalls in AfautomaticFunctionCallingConfig %s is less than or equal to 0."
                                    + " Disabling automatic function calling. Please set maximumRemoteCalls to a"
                                    + " positive integer.",
                            maximumRemoteCalls.get()));
            return true;
        }

        if (disable.get() && maximumRemoteCalls.isPresent() && maximumRemoteCalls.get() > 0) {
            logger.warning(
                    String.format(
                            "`automaticFunctionCalling.disable` is set to `true`. And"
                                    + " `automaticFunctionCalling.maximumRemoteCalls` is a positive number %s."
                                    + " Disabling automatic function calling. If you want to enable automatic"
                                    + " function calling, please set `automaticFunctionCalling.disable` to `false` or"
                                    + " leave it unset.",
                            maximumRemoteCalls.get()));
        }

        return disable.get();
    }

    static int getMaxRemoteCallsAfc(GenerateContentConfig config) {
        if (config == null) {
            return DEFAULT_MAX_REMOTE_CALLS_AFC;
        }
        if (!config.automaticFunctionCalling().isPresent()) {
            return DEFAULT_MAX_REMOTE_CALLS_AFC;
        }
        if (!config.automaticFunctionCalling().get().maximumRemoteCalls().isPresent()) {
            return DEFAULT_MAX_REMOTE_CALLS_AFC;
        }
        return config
                .automaticFunctionCalling()
                .get()
                .maximumRemoteCalls()
                .orElse(DEFAULT_MAX_REMOTE_CALLS_AFC);
    }

    static boolean shouldAppendAfcHistory(GenerateContentConfig config) {
        if (config == null) {
            return true;
        }
        if (!config.automaticFunctionCalling().isPresent()) {
            return true;
        }
        if (!config.automaticFunctionCalling().get().ignoreCallHistory().isPresent()) {
            return true;
        }
        return !config.automaticFunctionCalling().get().ignoreCallHistory().get();
    }

    private static Object getFunctionResponse(
            Method method, ImmutableMap<String, Object> argsFromModel) throws Exception {
        List<Object> argsListFromModel = new ArrayList<>();
        ImmutableList<String> methodParameterNames =
                stream(method.getParameters()).map(Parameter::getName).collect(toImmutableList());
        for (String parameterName : methodParameterNames) {
            if (!argsFromModel.containsKey(parameterName)) {
                throw new IllegalArgumentException(
                        "The parameter \""
                                + parameterName
                                + "\" was not found in the function call part from model. Args in function call"
                                + " part from model are: "
                                + argsFromModel);
            }
            Object argValueFromModel = argsFromModel.get(parameterName);
            switch (argValueFromModel.getClass().getName()) {
                case "java.lang.String":
                    argsListFromModel.add(argValueFromModel);
                    break;
                case "java.lang.Integer":
                    argsListFromModel.add(Integer.parseInt(argValueFromModel.toString()));
                    break;
                case "java.lang.Double":
                    argsListFromModel.add(Double.parseDouble(argValueFromModel.toString()));
                    break;
                case "java.lang.Float":
                    argsListFromModel.add(Float.parseFloat(argValueFromModel.toString()));
                    break;
                case "java.lang.Boolean":
                    argsListFromModel.add(Boolean.parseBoolean(argValueFromModel.toString()));
                    break;
                default:
                    throw new IllegalArgumentException(
                            "The value type of the parameter \""
                                    + parameterName
                                    + "\" is not supported. Supported types are String, Integer, Double, Float, and"
                                    + " Boolean.");
            }
        }

        return method.invoke(null, argsListFromModel.toArray());
    }
}
