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

import java.util.Iterator;
import java.util.function.Function;

/**
 * Pager class for iterating through paginated results.
 */
public class Pager<T extends JsonSerializable> extends BasePager<T> implements Iterable<T> {

    private final PagerIterator iterator;

    /**
     * Constructs a Pager.
     */
    Pager(
            PagedItem pagedItem,
            Function<JsonSerializable, Object> request,
            ObjectNode requestConfig,
            JsonNode response) {
        super(pagedItem, requestConfig);
        initNewPage(response);
        this.iterator = new PagerIterator(request);
    }

    /**
     * Fetches the next page of items. This makes a new API request.
     */
    public ImmutableList<T> nextPage() {
        iterator.fetchNextPage();
        return page();
    }

    /**
     * Returns an iterator for the Pager.
     */
    @Override
    public Iterator<T> iterator() {
        return iterator;
    }

    /**
     * Returns the name of the item for this pager.
     */
    public String name() {
        return pagedItem.fieldName();
    }

    /**
     * Returns the page size for this pager.
     */
    public int pageSize() {
        return pageSize;
    }

    /**
     * Returns the size of the current page.
     */
    public int size() {
        return page.size();
    }

    /**
     * Returns the current page of items as a list.
     */
    public ImmutableList<T> page() {
        return page;
    }

    /**
     * Iterator for the Pager.
     */
    private class PagerIterator implements Iterator<T> {
        private final Function<JsonSerializable, Object> request;
        private int currentIndex;

        PagerIterator(Function<JsonSerializable, Object> request) {
            this.request = request;
            this.currentIndex = 0;
        }

        private void fetchNextPage() {
            if (requestConfig.get("pageToken") == null) {
                throw new IndexOutOfBoundsException("No more page in the pager.");
            }

            try {
                initNewPage(
                        JsonSerializable.toJsonNode(
                                request.apply(
                                        JsonSerializable.fromJsonNode(requestConfig, pagedItem.requestConfigClass()))));
                this.currentIndex = 0;
            } catch (Exception e) {
                throw new GenAiIOException("Failed to fetch the next page. " + e.getMessage());
            }
        }

        @Override
        public boolean hasNext() {
            return (currentIndex < page.size()) || (requestConfig.get("pageToken") != null);
        }

        @Override
        public T next() {
            if (!hasNext()) {
                throw new IndexOutOfBoundsException("No more items in the pager.");
            }
            if (currentIndex < page.size()) {
                T current = page.get(currentIndex);
                currentIndex++;
                return current;
            } else {
                fetchNextPage();
                return next();
            }
        }
    }
}
