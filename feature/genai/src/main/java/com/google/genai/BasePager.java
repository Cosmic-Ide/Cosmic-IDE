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
import com.google.genai.types.CachedContent;
import com.google.genai.types.File;
import com.google.genai.types.ListCachedContentsConfig;
import com.google.genai.types.ListFilesConfig;
import com.google.genai.types.ListModelsConfig;
import com.google.genai.types.Model;

import java.util.ArrayList;
import java.util.List;

/**
 * Base abstract class for pagers.
 */
abstract class BasePager<T extends JsonSerializable> {

    protected final PagedItem pagedItem;
    protected final ObjectNode requestConfig;
    protected ImmutableList<T> page;
    protected int pageSize;
    protected String nextPageToken;
    /**
     * Constructs a BasePager.
     */
    protected BasePager(PagedItem pagedItem, ObjectNode requestConfig) {
        if (pagedItem == null) {
            throw new IllegalArgumentException("PagedItem cannot be null.");
        }
        if (requestConfig == null) {
            throw new IllegalArgumentException("Initial request config cannot be null.");
        }
        this.pagedItem = pagedItem;
        this.requestConfig = requestConfig.deepCopy();
    }

    /**
     * Inits a new page from the response.
     */
    @SuppressWarnings("unchecked")
    protected void initNewPage(JsonNode response) {
        if (response == null) {
            throw new IllegalArgumentException("Response cannot be null.");
        }
        JsonNode responseList = response.get(pagedItem.fieldName());
        if (responseList == null) {
            throw new GenAiIOException(
                    "Response does not contain the requested item. Raw response: "
                            + JsonSerializable.toJsonString(response));
        }
        // Sets the page.
        List<T> page = new ArrayList<>();
        for (JsonNode responseItem : responseList) {
            page.add((T) JsonSerializable.fromJsonNode(responseItem, pagedItem.itemClass()));
        }
        this.page = ImmutableList.copyOf(page);

        // Sets the page size.
        if (requestConfig.get("pageSize") != null) {
            this.pageSize = requestConfig.get("pageSize").intValue();
        } else {
            this.pageSize = this.page.size();
        }

        // Update page_token in the request config.
        if (response.get("nextPageToken") != null) {
            requestConfig.put("pageToken", response.get("nextPageToken").asText());
        } else {
            requestConfig.remove("pageToken");
        }
    }

    /**
     * A enum that represents a type of item for a pager.
     */
    static enum PagedItem {
        MODELS("models", Model.class, ListModelsConfig.class),
        CACHED_CONTENTS("cachedContents", CachedContent.class, ListCachedContentsConfig.class),
        FILES("files", File.class, ListFilesConfig.class);

        private final String fieldName;
        private final Class<? extends JsonSerializable> itemClass;
        private final Class<? extends JsonSerializable> requestConfigClass;

        PagedItem(
                String fieldName,
                Class<? extends JsonSerializable> itemClass,
                Class<? extends JsonSerializable> requestConfigClass) {
            this.fieldName = fieldName;
            this.itemClass = itemClass;
            this.requestConfigClass = requestConfigClass;
        }

        /**
         * Returns the name of the field in the response that contains the item.
         */
        public String fieldName() {
            return fieldName;
        }

        /**
         * Returns the class of the item.
         */
        public Class<? extends JsonSerializable> itemClass() {
            return itemClass;
        }

        /**
         * Returns the class of the request config.
         */
        public Class<? extends JsonSerializable> requestConfigClass() {
            return requestConfigClass;
        }
    }
}
