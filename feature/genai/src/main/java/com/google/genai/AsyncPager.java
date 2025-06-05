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
import com.google.common.collect.ImmutableList;
import com.google.genai.errors.GenAiIOException;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * AsyncPager class for handling paginated results asynchronously.
 */
public class AsyncPager<T extends JsonSerializable> extends BasePager<T> {

    private final Function<JsonSerializable, CompletableFuture<JsonNode>> asyncRequest;
    private final CompletableFuture<Void> initializationFuture;

    /**
     * Constructs an AsyncPager.
     */
    AsyncPager(
            BasePager.PagedItem pagedItem,
            Function<JsonSerializable, CompletableFuture<JsonNode>> asyncRequest,
            ObjectNode requestConfig,
            CompletableFuture<JsonNode> responseFuture) {
        super(pagedItem, requestConfig);
        this.asyncRequest = asyncRequest;

        this.initializationFuture =
                responseFuture
                        .thenAccept(
                                response -> {
                                    initNewPage(response);
                                })
                        .exceptionally(
                                e -> {
                                    throw new GenAiIOException(
                                            "Failed to process initial page for AsyncPager: " + e.getMessage());
                                });
    }

    /**
     * Asynchronously fetches the next page of items. This makes a new API request.
     */
    public CompletableFuture<ImmutableList<T>> nextPage() {
        return hasNextPage()
                .thenCompose(
                        hasNext -> {
                            if (!hasNext) {
                                CompletableFuture<ImmutableList<T>> failedFuture = new CompletableFuture<>();
                                failedFuture.completeExceptionally(
                                        new IndexOutOfBoundsException("No more page in the async pager."));
                                return failedFuture;
                            }
                            try {
                                return asyncRequest
                                        .apply(
                                                JsonSerializable.fromJsonNode(
                                                        requestConfig, pagedItem.requestConfigClass()))
                                        .thenApply(
                                                response -> {
                                                    initNewPage((JsonNode) response);
                                                    return page;
                                                });
                            } catch (Exception e) {
                                CompletableFuture<ImmutableList<T>> failedFuture = new CompletableFuture<>();
                                failedFuture.completeExceptionally(
                                        new GenAiIOException("Failed to fetch the next page. " + e.getMessage()));
                                return failedFuture;
                            }
                        });
    }

    /**
     * Asynchronously checks if there is potentially a next page.
     */
    CompletableFuture<Boolean> hasNextPage() {
        return initializationFuture.thenApply(v -> requestConfig.get("pageToken") != null);
    }

    /**
     * Asynchronously processes each item fetched by this pager. The provided consumer action will be
     * applied to each item sequentially across all pages.
     *
     * @param itemAction The action to perform on each item.
     */
    public CompletableFuture<Void> forEach(Consumer<? super T> itemAction) {
        if (itemAction == null) {
            CompletableFuture<Void> failedFuture = new CompletableFuture<>();
            failedFuture.completeExceptionally(new IllegalArgumentException("Action cannot be null."));
            return failedFuture;
        }
        return initializationFuture
                .thenCompose(v -> processPageItemsAndContinue(page, itemAction));
    }

    private CompletableFuture<Void> processPageItemsAndContinue(
            ImmutableList<T> page, Consumer<? super T> itemAction) {

        for (T item : page) {
            try {
                itemAction.accept(item);
            } catch (Exception e) {
                CompletableFuture<Void> failedFuture = new CompletableFuture<>();
                failedFuture.completeExceptionally(
                        new GenAiIOException("Failed to process item. " + e.getMessage()));
                return failedFuture;
            }
        }

        return hasNextPage()
                .thenCompose(
                        hasNext -> {
                            if (hasNext) {
                                return nextPage()
                                        .thenCompose(
                                                nextPageItems -> processPageItemsAndContinue(nextPageItems, itemAction));
                            } else {
                                return CompletableFuture.completedFuture(null);
                            }
                        });
    }

    /**
     * Asynchronously returns the current page of items as a list.
     */
    public CompletableFuture<ImmutableList<T>> page() {
        return initializationFuture.thenApply(v -> page);
    }

    /**
     * Asynchronously returns the name of the item for this pager.
     */
    public CompletableFuture<String> name() {
        return initializationFuture.thenApply(v -> pagedItem.fieldName());
    }

    /**
     * Asynchronously returns the page size for this pager.
     */
    public CompletableFuture<Integer> pageSize() {
        return initializationFuture.thenApply(v -> pageSize);
    }

    /**
     * Asynchronously returns the size of the current page.
     */
    public CompletableFuture<Integer> size() {
        return initializationFuture.thenApply(v -> page.size());
    }
}
