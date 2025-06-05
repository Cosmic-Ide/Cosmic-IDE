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
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.genai.errors.GenAiIOException;
import com.google.genai.types.CachedContent;
import com.google.genai.types.CreateCachedContentConfig;
import com.google.genai.types.DeleteCachedContentConfig;
import com.google.genai.types.DeleteCachedContentResponse;
import com.google.genai.types.GetCachedContentConfig;
import com.google.genai.types.ListCachedContentsConfig;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Async module of {@link Caches}
 */
public final class AsyncCaches {
    Caches caches;

    public AsyncCaches(ApiClient apiClient) {
        this.caches = new Caches(apiClient);
    }

    /**
     * Asynchronously creates a cached content resource.
     *
     * @param model  The model to use.
     * @param config A {@link CreateCachedContentConfig} for configuring the create request.
     * @return A {@link CachedContent} object that contains the info of the created resource.
     */
    public CompletableFuture<CachedContent> create(String model, CreateCachedContentConfig config) {
        return CompletableFuture.supplyAsync(() -> caches.create(model, config));
    }

    /**
     * Asynchronously gets a cached content resource.
     *
     * @param name   The name(resource id) of the cached content to get.
     * @param config A {@link GetCachedContentConfig} for configuring the get request.
     * @return A {@link CachedContent} object that contains the info of the cached content.
     */
    public CompletableFuture<CachedContent> get(String name, GetCachedContentConfig config) {
        return CompletableFuture.supplyAsync(() -> caches.get(name, config));
    }

    /**
     * Asynchronously deletes a cached content resource.
     *
     * @param name   The name(resource id) of the cached content to delete.
     * @param config A {@link DeleteCachedContentConfig} for configuring the delete request.
     */
    public CompletableFuture<DeleteCachedContentResponse> delete(
            String name, DeleteCachedContentConfig config) {
        return CompletableFuture.supplyAsync(() -> caches.delete(name, config));
    }

    /**
     * Asynchronously makes an API request to list the available cached contents.
     *
     * @param config A {@link ListCachedContentsConfig} for configuring the list request.
     * @return A CompletableFuture that resolves to a {@link AsyncPager}. The AsyncPager has a
     * `forEach` method that can be used to asynchronously process items in the page and
     * automatically query the next page once the current page is exhausted.
     */
    @SuppressWarnings("PatternMatchingInstanceof")
    public CompletableFuture<AsyncPager<CachedContent>> list(ListCachedContentsConfig config) {
        Function<JsonSerializable, CompletableFuture<JsonNode>> request =
                requestConfig -> {
                    if (!(requestConfig instanceof ListCachedContentsConfig)) {
                        throw new GenAiIOException(
                                "Internal error: Pager expected ListCachedContentsConfig but received "
                                        + requestConfig.getClass().getName());
                    }
                    return CompletableFuture.supplyAsync(
                            () ->
                                    JsonSerializable.toJsonNode(
                                            caches.privateList((ListCachedContentsConfig) requestConfig)));
                };
        return CompletableFuture.supplyAsync(
                () ->
                        new AsyncPager<>(
                                Pager.PagedItem.CACHED_CONTENTS,
                                request,
                                (ObjectNode) JsonSerializable.toJsonNode(config),
                                request.apply(config)));
    }
}
